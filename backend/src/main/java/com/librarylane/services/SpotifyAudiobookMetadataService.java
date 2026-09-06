package com.librarylane.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarylane.catalog.CatalogBookResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional selection-time audiobook enrichment.
 *
 * Spotify is never used to decide whether an Apple search result exists.
 * When credentials are absent, Spotify is unavailable, or no exact
 * title-and-author match is found, the selected Apple result is returned
 * unchanged.
 */
@Service
public class SpotifyAudiobookMetadataService {

    private static final String TOKEN_URL =
            "https://accounts.spotify.com/api/token";
    private static final String SEARCH_URL =
            "https://api.spotify.com/v1/search";
    private static final String AUDIOBOOK_URL =
            "https://api.spotify.com/v1/audiobooks/";
    private static final String ALBUM_URL =
            "https://api.spotify.com/v1/albums/";
    private static final int CHAPTER_PAGE_SIZE = 50;
    private static final int MAX_CHAPTER_PAGES = 20;
    private static final int ALBUM_TRACK_PAGE_SIZE = 50;
    private static final int MAX_ALBUM_TRACK_PAGES = 20;
    private static final long SEARCH_CACHE_MILLIS =
            Duration.ofMinutes(15).toMillis();
    private static final long TOKEN_SAFETY_MILLIS =
            Duration.ofSeconds(30).toMillis();

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String clientSecret;
    private final Map<String, SearchCacheEntry> legacySearchCache =
            new ConcurrentHashMap<>();

    private volatile String accessToken;
    private volatile long accessTokenExpiresAt;

    public SpotifyAudiobookMetadataService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${SPOTIFY_CLIENT_ID:}") String clientId,
            @Value("${SPOTIFY_CLIENT_SECRET:}") String clientSecret) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.clientId = safeText(clientId);
        this.clientSecret = safeText(clientSecret);
    }

    public boolean isConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    public CatalogBookResult enrich(CatalogBookResult selected) {
        if (selected == null
                || !"AUDIOBOOK".equalsIgnoreCase(safeText(selected.format()))) {
            return selected;
        }
        if (!isConfigured()) {
            diagnostic("enrichment skipped: Spotify credentials are not configured");
            return selected;
        }

        String token = accessToken();
        JsonNode candidate = findExactAudiobook(selected, token);
        if (candidate == null) {
            diagnostic("no exact dedicated-audiobook match for “"
                    + safeText(selected.title())
                    + "”; checking legacy albums");
            CatalogBookResult legacy = searchLegacyAlbums(
                    selected.title(),
                    "TITLE")
                    .stream()
                    .filter(result -> sameLegacyWork(selected, result))
                    .findFirst()
                    .orElse(null);
            if (legacy == null) {
                diagnostic("no exact legacy-album match for “"
                        + safeText(selected.title()) + "”");
                return selected;
            }
            return mergeSelectedWithLegacy(
                    selected,
                    enrichLegacyAlbum(legacy));
        }

        String spotifyId = text(candidate, "id");
        if (spotifyId.isBlank()) {
            diagnostic("dedicated-audiobook match had no Spotify ID");
            return selected;
        }

        JsonNode details = getJson(
                URI.create(AUDIOBOOK_URL + spotifyId + "?market=US"),
                token);
        if (details == null) details = candidate;

        List<String> narrators = names(details.path("narrators"));
        Integer lengthSeconds = totalLengthSeconds(spotifyId, token);
        String publisher = text(details, "publisher");
        diagnostic("matched dedicated audiobook “"
                + text(candidate, "name") + "”"
                + metadataSummary(narrators, lengthSeconds));

        CatalogBookResult dedicated = new CatalogBookResult(
                appendProvider(selected.provider(), "Spotify Audiobooks"),
                // Once Spotify metadata is displayed, retain Spotify's own ID
                // so the frontend can provide the required link back to the
                // exact audiobook.
                spotifyId,
                selected.title(),
                selected.subtitle(),
                selected.authors(),
                selected.genres(),
                selected.description(),
                prefer(selected.publisher(), blankToNull(publisher)),
                selected.publicationDate(),
                selected.pageCount(),
                selected.audiobookLengthSeconds() != null
                        ? selected.audiobookLengthSeconds() : lengthSeconds,
                mergeNames(narrators, selected.narrators()),
                selected.coverImageUrl(),
                selected.language(),
                selected.isbn10(),
                selected.isbn13(),
                selected.seriesName(),
                selected.seriesNumber(),
                selected.editionFormat(),
                selected.format()
        );

        // A legacy album may still fill a missing runtime or provide fallback
        // metadata for a sparse dedicated record. A verified narrator remains
        // authoritative and is protected by mergeSelectedWithLegacy below.
        if (dedicated.audiobookLengthSeconds() == null
                || dedicated.narrators() == null
                || dedicated.narrators().size() <= 1) {
            try {
                CatalogBookResult legacy = searchLegacyAlbums(
                        selected.title(),
                        "TITLE")
                        .stream()
                        .filter(result -> sameLegacyWork(selected, result))
                        .findFirst()
                        .orElse(null);
                if (legacy != null) {
                    return mergeSelectedWithLegacy(
                            dedicated,
                            enrichLegacyAlbum(legacy));
                }
            } catch (RuntimeException error) {
                // Legacy albums are only a best-effort supplement. Never
                // discard a successful dedicated audiobook match because
                // that optional lookup is unavailable or rejected.
                diagnostic("optional legacy augmentation failed; preserving "
                        + "dedicated audiobook metadata: "
                        + error.getMessage());
            }
        }
        return dedicated;
    }

    /**
     * Searches Spotify's album catalog for older audiobooks that predate
     * Spotify's dedicated audiobook objects. This is an independent source:
     * it never replaces or suppresses Apple results.
     */
    public List<CatalogBookResult> searchLegacyAlbums(
            String rawQuery,
            String rawSearchBy) {
        String query = safeText(rawQuery);
        String searchBy = safeText(rawSearchBy).toUpperCase(Locale.ROOT);
        if (query.length() < 3 || "SERIES".equals(searchBy)) return List.of();
        if (!isConfigured()) {
            diagnostic("legacy-album search skipped: Spotify credentials are not configured");
            return List.of();
        }

        String cacheKey = normalize(query) + "|" + searchBy;
        SearchCacheEntry cached = legacySearchCache.get(cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiresAt() > now) {
            return cached.results();
        }

        String token = accessToken();
        Map<String, CatalogBookResult> unique = new LinkedHashMap<>();
        List<String> searchVariants = "TITLE".equals(searchBy)
                ? List.of(query, query + " unabridged")
                : List.of(query);

        for (String searchVariant : searchVariants) {
            URI uri = UriComponentsBuilder
                    .fromUriString(SEARCH_URL)
                    .queryParam("q", searchVariant)
                    .queryParam("type", "album")
                    .queryParam("market", "US")
                    // Spotify currently rejects 20 for this legacy album
                    // search path even though other search types accept it.
                    .queryParam("limit", 10)
                    .build()
                    .encode()
                    .toUri();

            JsonNode response = getJson(uri, token);
            if (response == null
                    || !response.path("albums").path("items").isArray()) {
                continue;
            }

            for (JsonNode album : response.path("albums").path("items")) {
                CatalogBookResult mapped = mapLegacyAlbum(
                        album,
                        query,
                        searchBy);
                if (mapped != null) {
                    unique.putIfAbsent(mapped.providerId(), mapped);
                }
            }
        }

        List<CatalogBookResult> immutable =
                List.copyOf(unique.values());
        legacySearchCache.put(
                cacheKey,
                new SearchCacheEntry(now + SEARCH_CACHE_MILLIS, immutable));
        diagnostic("legacy-album search found " + immutable.size()
                + " trustworthy candidate(s) for “" + query + "”");
        return immutable;
    }

    public CatalogBookResult enrichLegacyAlbum(CatalogBookResult selected) {
        if (selected == null || !isLegacyAlbumResult(selected)) return selected;
        if (!isConfigured()) {
            diagnostic("legacy-album enrichment skipped: credentials are not configured");
            return selected;
        }

        String albumId = selected.providerId().substring("album:".length());
        String token = accessToken();
        JsonNode details = getJson(
                URI.create(ALBUM_URL + albumId + "?market=US"),
                token);

        AlbumTrackMetadata tracks = albumTrackMetadata(
                albumId,
                token,
                selected.authors());
        List<String> narrators = tracks.narrators();
        Integer lengthSeconds = tracks.lengthSeconds();
        String publisher = text(details, "label");
        String releaseDate = text(details, "release_date");

        diagnostic("matched legacy audiobook album “"
                + safeText(selected.title()) + "”"
                + metadataSummary(narrators, lengthSeconds));

        return new CatalogBookResult(
                selected.provider(),
                selected.providerId(),
                selected.title(),
                selected.subtitle(),
                selected.authors(),
                selected.genres(),
                selected.description(),
                prefer(selected.publisher(), blankToNull(publisher)),
                prefer(selected.publicationDate(), blankToNull(releaseDate)),
                selected.pageCount(),
                selected.audiobookLengthSeconds() != null
                        ? selected.audiobookLengthSeconds() : lengthSeconds,
                mergeNames(narrators, selected.narrators()),
                selected.coverImageUrl(),
                selected.language(),
                selected.isbn10(),
                selected.isbn13(),
                selected.seriesName(),
                selected.seriesNumber(),
                selected.editionFormat(),
                "AUDIOBOOK"
        );
    }

    public static boolean isLegacyAlbumResult(CatalogBookResult result) {
        return result != null
                && normalize(result.provider()).contains("spotify audiobook album")
                && safeText(result.providerId()).startsWith("album:");
    }

    private static boolean sameLegacyWork(
            CatalogBookResult selected,
            CatalogBookResult legacy) {
        if (!canonicalTitle(selected.title())
                .equals(canonicalTitle(legacy.title()))) {
            return false;
        }
        if (selected.authors() == null || selected.authors().isEmpty()
                || legacy.authors() == null || legacy.authors().isEmpty()) {
            return false;
        }
        return selected.authors().stream()
                .map(SpotifyAudiobookMetadataService::normalize)
                .anyMatch(left -> legacy.authors().stream()
                        .map(SpotifyAudiobookMetadataService::normalize)
                        .anyMatch(right -> left.equals(right)
                                || left.contains(right)
                                || right.contains(left)));
    }

    private static CatalogBookResult mergeSelectedWithLegacy(
            CatalogBookResult selected,
            CatalogBookResult legacy) {
        return new CatalogBookResult(
                appendProvider(selected.provider(), legacy.provider()),
                legacy.providerId(),
                selected.title(),
                selected.subtitle(),
                selected.authors(),
                selected.genres(),
                selected.description(),
                prefer(selected.publisher(), legacy.publisher()),
                selected.publicationDate(),
                selected.pageCount(),
                selected.audiobookLengthSeconds() != null
                        ? selected.audiobookLengthSeconds()
                        : legacy.audiobookLengthSeconds(),
                selected.narrators() != null
                        && !selected.narrators().isEmpty()
                        ? selected.narrators()
                        : legacy.narrators(),
                prefer(selected.coverImageUrl(), legacy.coverImageUrl()),
                selected.language(),
                selected.isbn10(),
                selected.isbn13(),
                selected.seriesName(),
                selected.seriesNumber(),
                prefer(selected.editionFormat(), legacy.editionFormat()),
                "AUDIOBOOK"
        );
    }

    private static CatalogBookResult mapLegacyAlbum(
            JsonNode album,
            String rawQuery,
            String searchBy) {
        String spotifyId = text(album, "id");
        String albumTitle = text(album, "name");
        List<String> artists = names(album.path("artists"));
        if (spotifyId.isBlank() || albumTitle.isBlank() || artists.isEmpty()) {
            return null;
        }

        String query = normalize(rawQuery);
        String title = canonicalTitle(baseLegacyTitle(albumTitle));
        String authorText = normalize(String.join(" ", artists));
        boolean requestedFieldMatches = "AUTHOR".equals(searchBy)
                ? authorText.contains(query)
                : title.contains(canonicalTitle(query))
                  || canonicalTitle(query).contains(title);
        if (!requestedFieldMatches
                || !hasLegacyAudiobookEvidence(albumTitle, album.path("total_tracks").asInt(0))) {
            return null;
        }

        String edition = legacyEditionLabel(albumTitle);
        String cover = firstImageUrl(album.path("images"));
        return new CatalogBookResult(
                "Spotify Audiobook Albums",
                "album:" + spotifyId,
                baseLegacyTitle(albumTitle),
                null,
                artists,
                List.of(),
                null,
                null,
                blankToNull(text(album, "release_date")),
                null,
                null,
                List.of(),
                blankToNull(cover),
                null,
                null,
                null,
                null,
                null,
                blankToNull(edition),
                "AUDIOBOOK"
        );
    }

    private static List<String> mergeNames(
            List<String> primary,
            List<String> secondary) {
        Map<String, String> unique = new LinkedHashMap<>();
        if (primary != null) {
            for (String value : primary) {
                String key = normalize(value);
                if (!key.isBlank()) unique.putIfAbsent(key, value.trim());
            }
        }
        if (secondary != null) {
            for (String value : secondary) {
                String key = normalize(value);
                if (!key.isBlank()) unique.putIfAbsent(key, value.trim());
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static boolean hasLegacyAudiobookEvidence(
            String title,
            int totalTracks) {
        String normalized = normalize(title);
        boolean labeled = normalized.contains("unabridged")
                || normalized.contains("abridged")
                || normalized.contains("audiobook")
                || normalized.matches(".*\\bbook\\s*\\d+.*");
        return labeled || totalTracks >= 50;
    }

    private AlbumTrackMetadata albumTrackMetadata(
            String albumId,
            String token,
            List<String> authors) {
        long totalMillis = 0;
        int offset = 0;
        Map<String, String> narratorCandidates = new LinkedHashMap<>();
        List<String> canonicalAuthors = authors == null
                ? List.of()
                : authors.stream().map(SpotifyAudiobookMetadataService::normalize).toList();

        for (int page = 0; page < MAX_ALBUM_TRACK_PAGES; page++) {
            URI uri = UriComponentsBuilder
                    .fromUriString(ALBUM_URL + albumId + "/tracks")
                    .queryParam("market", "US")
                    .queryParam("limit", ALBUM_TRACK_PAGE_SIZE)
                    .queryParam("offset", offset)
                    .build()
                    .encode()
                    .toUri();
            JsonNode response = getJson(uri, token);
            if (response == null || !response.path("items").isArray()) break;

            int count = 0;
            for (JsonNode track : response.path("items")) {
                long duration = track.path("duration_ms").asLong(0);
                if (duration > 0) totalMillis += duration;
                for (String artist : names(track.path("artists"))) {
                    String normalized = normalize(artist);
                    if (normalized.isBlank()
                            || normalized.equals("various artists")
                            || canonicalAuthors.stream().anyMatch(author ->
                            author.equals(normalized)
                                    || author.contains(normalized)
                                    || normalized.contains(author))) {
                        continue;
                    }
                    narratorCandidates.putIfAbsent(normalized, artist);
                }
                count++;
            }

            int total = response.path("total").asInt(offset + count);
            offset += count;
            if (count == 0 || offset >= total) break;
        }

        Integer seconds = null;
        if (totalMillis > 0 && totalMillis / 1000L <= Integer.MAX_VALUE) {
            seconds = (int) (totalMillis / 1000L);
        }
        return new AlbumTrackMetadata(
                new ArrayList<>(narratorCandidates.values()),
                seconds);
    }

    private JsonNode findExactAudiobook(
            CatalogBookResult selected,
            String token) {
        String author = selected.authors() == null || selected.authors().isEmpty()
                ? "" : selected.authors().getFirst();
        String searchText = String.join(" ",
                safeText(selected.title()),
                safeText(author)).trim();

        URI uri = UriComponentsBuilder
                .fromUriString(SEARCH_URL)
                .queryParam("q", searchText)
                .queryParam("type", "audiobook")
                .queryParam("market", "US")
                .queryParam("limit", 10)
                .build()
                .encode()
                .toUri();

        JsonNode response = getJson(uri, token);
        if (response == null
                || !response.path("audiobooks").path("items").isArray()) {
            return null;
        }

        JsonNode best = null;
        int bestScore = -1;
        for (JsonNode item : response.path("audiobooks").path("items")) {
            int score = matchScore(selected, item);
            if (score > bestScore) {
                best = item;
                bestScore = score;
            }
        }
        return bestScore >= 100 ? best : null;
    }

    private static int matchScore(
            CatalogBookResult selected,
            JsonNode candidate) {
        String selectedTitle = canonicalTitle(selected.title());
        String candidateTitle = canonicalTitle(text(candidate, "name"));
        if (selectedTitle.isBlank() || candidateTitle.isBlank()) return -1;

        boolean titleMatch = selectedTitle.equals(candidateTitle)
                || selectedTitle.startsWith(candidateTitle)
                || candidateTitle.startsWith(selectedTitle);
        if (!titleMatch) return -1;

        List<String> selectedAuthors = selected.authors() == null
                ? List.of() : selected.authors();
        List<String> candidateAuthors = names(candidate.path("authors"));
        if (selectedAuthors.isEmpty() || candidateAuthors.isEmpty()) return -1;

        boolean authorMatch = selectedAuthors.stream()
                .map(SpotifyAudiobookMetadataService::normalize)
                .anyMatch(left -> candidateAuthors.stream()
                        .map(SpotifyAudiobookMetadataService::normalize)
                        .anyMatch(right -> left.equals(right)
                                || left.contains(right)
                                || right.contains(left)));
        if (!authorMatch) return -1;

        int score = selectedTitle.equals(candidateTitle) ? 200 : 100;
        // Multiple audiobook editions can share the same title and author.
        // Prefer the candidate whose structured metadata names the fuller
        // cast instead of accepting the first exact match, which may be a
        // bonus-chapter or guest-narrator edition.
        score += Math.min(
                names(candidate.path("narrators")).size(),
                8) * 10;
        return score;
    }

    private Integer totalLengthSeconds(String spotifyId, String token) {
        long totalMillis = 0;
        int offset = 0;

        for (int page = 0; page < MAX_CHAPTER_PAGES; page++) {
            URI uri = UriComponentsBuilder
                    .fromUriString(AUDIOBOOK_URL + spotifyId + "/chapters")
                    .queryParam("market", "US")
                    .queryParam("limit", CHAPTER_PAGE_SIZE)
                    .queryParam("offset", offset)
                    .build()
                    .encode()
                    .toUri();
            JsonNode response = getJson(uri, token);
            if (response == null || !response.path("items").isArray()) break;

            int count = 0;
            for (JsonNode chapter : response.path("items")) {
                long duration = chapter.path("duration_ms").asLong(0);
                if (duration > 0) totalMillis += duration;
                count++;
            }

            int total = response.path("total").asInt(offset + count);
            offset += count;
            if (count == 0 || offset >= total) break;
        }

        if (totalMillis <= 0) return null;
        long seconds = totalMillis / 1000L;
        return seconds <= Integer.MAX_VALUE ? (int) seconds : null;
    }

    private synchronized String accessToken() {
        long now = System.currentTimeMillis();
        if (accessToken != null && now < accessTokenExpiresAt) {
            return accessToken;
        }

        String credentials = clientId + ":" + clientSecret;
        String authorization = "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));

        String responseBody = restClient.post()
                .uri(TOKEN_URL)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(String.class);
        JsonNode response = parse(responseBody);
        String token = text(response, "access_token");
        if (token.isBlank()) {
            throw new IllegalStateException(
                    "Spotify did not return an access token");
        }

        long expiresInSeconds = response.path("expires_in").asLong(3600);
        accessToken = token;
        accessTokenExpiresAt = now
                + Math.max(0, expiresInSeconds * 1000L - TOKEN_SAFETY_MILLIS);
        return accessToken;
    }

    private JsonNode getJson(URI uri, String token) {
        String body = restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(String.class);
        return parse(body);
    }

    private JsonNode parse(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException(
                    "Spotify returned an unreadable response",
                    error);
        }
    }

    private static List<String> names(JsonNode values) {
        if (values == null || !values.isArray()) return List.of();
        Map<String, String> unique = new LinkedHashMap<>();
        for (JsonNode value : values) {
            String name = text(value, "name");
            if (!name.isBlank()) unique.putIfAbsent(normalize(name), name);
        }
        return new ArrayList<>(unique.values());
    }

    private static String canonicalTitle(String value) {
        return normalize(value)
                .replaceAll(
                        "\\b(?:special edition|unabridged|abridged|"
                                + "dramatized adaptation|dramatized|audiobook)\\b",
                        " ")
                .replaceAll("\\b(?:book|series)\\s*\\d+\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String baseLegacyTitle(String value) {
        return safeText(value)
                .replaceFirst("\\s*\\[[^]]*(?i:unabridged|abridged)[^]]*]\\s*$", "")
                .replaceFirst("\\s*\\([^)]*(?i:unabridged|abridged)[^)]*\\)\\s*$", "")
                .trim();
    }

    private static String legacyEditionLabel(String value) {
        String normalized = normalize(value);
        if (normalized.contains("unabridged")) return "Unabridged";
        if (normalized.contains("abridged")) return "Abridged";
        return "Legacy audiobook";
    }

    private static String firstImageUrl(JsonNode images) {
        if (images == null || !images.isArray()) return "";
        for (JsonNode image : images) {
            String url = text(image, "url");
            if (!url.isBlank()) return url;
        }
        return "";
    }

    private static String metadataSummary(
            List<String> narrators,
            Integer lengthSeconds) {
        return " (narrators="
                + (narrators == null ? 0 : narrators.size())
                + ", runtime="
                + (lengthSeconds == null ? "missing" : lengthSeconds + "s")
                + ")";
    }

    private static void diagnostic(String message) {
        System.out.println("[Spotify audiobooks] " + message);
    }

    private static String appendProvider(String first, String second) {
        if (normalize(first).contains(normalize(second))) return first;
        if (safeText(first).isBlank()) return second;
        return first + " + " + second;
    }

    private static String text(JsonNode node, String field) {
        if (node == null) return "";
        return node.path(field).asText("").trim();
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalize(String value) {
        return safeText(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }

    private static String blankToNull(String value) {
        String cleaned = safeText(value);
        return cleaned.isBlank() ? null : cleaned;
    }

    private static String prefer(String first, String second) {
        return safeText(first).isBlank() ? second : first;
    }

    private record AlbumTrackMetadata(
            List<String> narrators,
            Integer lengthSeconds) {
    }

    private record SearchCacheEntry(
            long expiresAt,
            List<CatalogBookResult> results) {
    }
}
