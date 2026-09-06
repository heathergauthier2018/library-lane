package com.librarylane.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarylane.catalog.CatalogBookResult;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Audiobook-only catalog provider backed by Apple's public Search API.
 *
 * This service deliberately returns no results for physical books or e-books.
 * It also does not use Apple previews or promotional artwork, keeping the
 * provider isolated from Library Lane's existing book-search presentation.
 */
@Service
public class AppleAudiobookCatalogService {

    private static final String APPLE_SEARCH_URL =
            "https://itunes.apple.com/search";
    private static final int MAX_RESULTS = 50;
    private static final long CACHE_TTL_MILLIS = Duration.ofMinutes(15).toMillis();
    private static final int REQUESTS_PER_MINUTE = 18;
    private static final long RATE_WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();
    private static final Pattern TRAILING_PARENTHETICAL =
            Pattern.compile("\\(([^()]{2,80})\\)\\s*$");
    private static final Pattern SERIES_NUMBER = Pattern.compile(
            "(?i)\\b(?:book|volume)\\s*(?:number\\s*)?#?\\s*(\\d+(?:\\.\\d+)?)\\b");
    private static final Pattern TRAILING_PARENTHETICAL_SERIES_NUMBER =
            Pattern.compile("\\([^()]{2,70}?\\s+(\\d+(?:\\.\\d+)?)\\)\\s*$");
    private static final Pattern BRACKETED_NARRATOR = Pattern.compile(
            "(?i)\\[\\s*narrator\\s+(.{2,80}?)(?:['’]s)?\\s*]");
    private static final Pattern CREDITED_NARRATOR = Pattern.compile(
            "(?i)\\b(?:narrated|performed|read)\\s+by\\s+"
                    + "(.{2,80}?)(?=\\s+(?:who|whose|brings|returns|delivers)\\b|[\\].;:]|$)");
    private static final Pattern LABELED_NARRATOR = Pattern.compile(
            "(?i)\\bnarrator\\s+(.{2,80}?)(?:['’]s\\s+narration|\\s+returns|"
                    + "\\s+brings|\\s+reads|[\\].;:]|$)");
    private static final Pattern AUDIOBOOK_PART = Pattern.compile(
            "(?i)\\b(?:part\\s*)?(\\d+)\\s+of\\s+(\\d+)\\b");
    private static final Pattern TRAILING_AUDIOBOOK_QUALIFIERS = Pattern.compile(
            "(?i)(?:\\s*[\\[(]?(?:unabridged|abridged|audiobook|"
                    + "dramatized(?:\\s+adaptation)?|graphic\\s+audio|"
                    + "part\\s*\\d+|\\d+\\s+of\\s+\\d+)"
                    + "[\\])]?\\s*)+$");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Deque<Long> requestTimes = new ArrayDeque<>();

    public AppleAudiobookCatalogService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public List<CatalogBookResult> search(
            String rawQuery,
            String requestedFormat,
            String rawSearchBy) {

        if (!"AUDIOBOOK".equalsIgnoreCase(safeText(requestedFormat))) {
            return List.of();
        }

        String query = safeText(rawQuery);
        if (query.length() < 3) return List.of();

        String searchBy = normalizeSearchBy(rawSearchBy);
        String cacheKey = normalize(query) + "|" + searchBy;
        CacheEntry cached = cache.get(cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiresAt() > now) {
            return cached.results();
        }

        Map<String, CatalogBookResult> unique = new LinkedHashMap<>();
        addMatchingResults(
                fetch(query, searchBy, true),
                query,
                searchBy,
                unique);

        // Apple often stores an edition qualifier after an inserted part
        // marker, for example "Fourth Wing (1 of 2) [Dramatized
        // Adaptation]". Searching the reader's literal phrase can therefore
        // return nothing. Search the work title as a second, additive query
        // and apply the requested edition/part intent locally.
        String baseQuery = audiobookBaseTitle(query);
        if ("TITLE".equals(searchBy)
                && hasExplicitAudiobookIntent(query)
                && !normalize(baseQuery).equals(normalize(query))) {
            addMatchingResults(
                    fetch(baseQuery, searchBy, true),
                    query,
                    searchBy,
                    unique);
            addMatchingResults(
                    fetch(baseQuery, searchBy, false),
                    query,
                    searchBy,
                    unique);
        }

        // Apple's field-specific search can return related records while
        // omitting the closest title, especially when the store title carries
        // an edition suffix. A merely non-empty set is therefore not enough.
        // Merge a general search whenever there is no strong field match.
        boolean strongFieldMatch =
                hasStrongFieldMatch(unique.values(), query, searchBy);
        boolean needsSeriesDiscovery =
                "TITLE".equals(searchBy)
                        && !hasExplicitAudiobookIntent(query)
                        && exactTitleMatchNeedsSeriesDiscovery(
                        unique.values(),
                        query);
        if ((!strongFieldMatch || needsSeriesDiscovery)
                && ("TITLE".equals(searchBy) || "AUTHOR".equals(searchBy))) {
            addMatchingResults(
                    fetch(query, searchBy, false),
                    query,
                    searchBy,
                    unique);
        }

        AudiobookIntent requestedIntent = audiobookIntent(query);
        List<CatalogBookResult> results =
                coalesceEquivalentAudiobooks(unique.values()).stream()
                        .sorted((left, right) -> Integer.compare(
                                editionPreference(right, requestedIntent)
                                        + audiobookRichness(right),
                                editionPreference(left, requestedIntent)
                                        + audiobookRichness(left)))
                        .toList();
        cache.put(cacheKey, new CacheEntry(now + CACHE_TTL_MILLIS, results));
        return results;
    }

    private static int editionPreference(
            CatalogBookResult result,
            AudiobookIntent requested) {
        AudiobookIntent offered = audiobookIntent(result.title());
        int score = 0;
        if (!requested.dramatized() && !offered.dramatized()) score += 100;
        if (requested.dramatized() && offered.dramatized()) score += 100;
        if (requested.partNumber() != null
                && requested.partNumber().equals(offered.partNumber())) {
            score += 50;
        }
        if (requested.partNumber() == null
                && offered.partNumber() != null) {
            score -= 10;
        }
        return score;
    }

    private static boolean hasStrongFieldMatch(
            Iterable<CatalogBookResult> results,
            String rawQuery,
            String searchBy) {
        String query = "TITLE".equals(searchBy)
                ? canonicalTitle(rawQuery)
                : normalize(rawQuery);

        for (CatalogBookResult result : results) {
            String target = "TITLE".equals(searchBy)
                    ? canonicalTitle(result.title())
                    : normalize(String.join(" ", result.authors()));
            if (!target.isBlank() && target.equals(query)) return true;
        }
        return false;
    }

    private static boolean exactTitleMatchNeedsSeriesDiscovery(
            Iterable<CatalogBookResult> results,
            String rawQuery) {
        String query = canonicalTitle(rawQuery);
        for (CatalogBookResult result : results) {
            if (!canonicalTitle(result.title()).equals(query)) continue;
            if (safeText(result.seriesName()).isBlank()
                    || result.seriesNumber() == null) {
                return true;
            }
        }
        return false;
    }

    private static List<CatalogBookResult> coalesceEquivalentAudiobooks(
            Iterable<CatalogBookResult> results) {
        Map<String, CatalogBookResult> consolidated = new LinkedHashMap<>();
        for (CatalogBookResult result : results) {
            String authors = result.authors().stream()
                    .map(AppleAudiobookCatalogService::normalize)
                    .sorted()
                    .reduce((left, right) -> left + "|" + right)
                    .orElse("");
            String title = result.title();
            if (!safeText(result.seriesName()).isBlank()
                    && result.seriesNumber() != null) {
                title = TRAILING_PARENTHETICAL.matcher(title)
                        .replaceFirst("")
                        .trim();
            }
            String key = canonicalTitle(title)
                    + "|" + audiobookEditionKey(result.title())
                    + "|" + authors;
            consolidated.merge(
                    key,
                    result,
                    AppleAudiobookCatalogService::mergeEquivalentAudiobooks);
        }
        return new ArrayList<>(consolidated.values());
    }

    private static String audiobookEditionKey(String title) {
        AudiobookIntent intent = audiobookIntent(title);
        if (intent.dramatized()) {
            return "dramatized:"
                    + (intent.partNumber() == null ? "" : intent.partNumber())
                    + ":"
                    + (intent.partTotal() == null ? "" : intent.partTotal());
        }
        if (intent.abridged()) return "abridged";
        if (intent.unabridged()) return "unabridged";
        return "standard";
    }

    private static CatalogBookResult mergeEquivalentAudiobooks(
            CatalogBookResult first,
            CatalogBookResult second) {
        CatalogBookResult richer =
                audiobookRichness(second) > audiobookRichness(first)
                        ? second : first;
        CatalogBookResult other = richer == first ? second : first;

        return new CatalogBookResult(
                richer.provider(),
                richer.providerId(),
                richer.title(),
                firstNonBlank(richer.subtitle(), other.subtitle()),
                !richer.authors().isEmpty()
                        ? richer.authors() : other.authors(),
                !richer.genres().isEmpty()
                        ? richer.genres() : other.genres(),
                longerText(richer.description(), other.description()),
                firstNonBlank(richer.publisher(), other.publisher()),
                earlierDate(richer.publicationDate(), other.publicationDate()),
                richer.pageCount() != null
                        ? richer.pageCount() : other.pageCount(),
                richer.audiobookLengthSeconds() != null
                        ? richer.audiobookLengthSeconds()
                        : other.audiobookLengthSeconds(),
                mergeNames(richer.narrators(), other.narrators()),
                firstNonBlank(richer.coverImageUrl(), other.coverImageUrl()),
                firstNonBlank(richer.language(), other.language()),
                firstNonBlank(richer.isbn10(), other.isbn10()),
                firstNonBlank(richer.isbn13(), other.isbn13()),
                firstNonBlank(richer.seriesName(), other.seriesName()),
                richer.seriesNumber() != null
                        ? richer.seriesNumber() : other.seriesNumber(),
                firstNonBlank(richer.editionFormat(), other.editionFormat()),
                "AUDIOBOOK");
    }

    private static int audiobookRichness(CatalogBookResult result) {
        int score = 0;
        if (!safeText(result.seriesName()).isBlank()) score += 30;
        if (result.seriesNumber() != null) score += 30;
        if (result.audiobookLengthSeconds() != null) score += 12;
        score += Math.min(12, result.narrators().size() * 6);
        if (!safeText(result.description()).isBlank()) score += 8;
        if (!safeText(result.coverImageUrl()).isBlank()) score += 4;
        return score;
    }

    private static List<String> mergeNames(
            List<String> first,
            List<String> second) {
        Map<String, String> names = new LinkedHashMap<>();
        for (String value : first) {
            if (!safeText(value).isBlank()) names.putIfAbsent(normalize(value), value);
        }
        for (String value : second) {
            if (!safeText(value).isBlank()) names.putIfAbsent(normalize(value), value);
        }
        return names.values().stream().limit(6).toList();
    }

    private static String longerText(String first, String second) {
        String left = safeText(first);
        String right = safeText(second);
        if (left.isBlank()) return blankToNull(right);
        if (right.isBlank()) return blankToNull(left);
        return left.length() >= right.length() ? left : right;
    }

    private static String earlierDate(String first, String second) {
        String left = safeText(first);
        String right = safeText(second);
        if (left.isBlank()) return blankToNull(right);
        if (right.isBlank()) return blankToNull(left);
        return left.compareTo(right) <= 0 ? left : right;
    }

    private static String firstNonBlank(String first, String second) {
        return safeText(first).isBlank() ? blankToNull(second) : first;
    }

    private static String canonicalTitle(String value) {
        return normalize(audiobookBaseTitle(value))
                .replaceFirst("^the\\s+", "")
                .trim();
    }

    private JsonNode fetch(
            String query,
            String searchBy,
            boolean useFieldAttribute) {
        long now = System.currentTimeMillis();
        if (!reserveRequest(now)) return null;

        URI uri = buildSearchUri(query, searchBy, useFieldAttribute);

        // Apple's Search API can label JSON as text/javascript. Reading it
        // directly as JsonNode makes Spring reject the response before Jackson
        // can parse it, so accept the payload as text and parse it explicitly.
        String responseBody = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);
        if (responseBody == null || responseBody.isBlank()) return null;

        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException(
                    "Apple returned an unreadable audiobook response",
                    error);
        }
    }

    private void addMatchingResults(
            JsonNode response,
            String query,
            String searchBy,
            Map<String, CatalogBookResult> unique) {
        if (response == null || !response.path("results").isArray()) return;

        for (JsonNode item : response.path("results")) {
            CatalogBookResult result = mapResult(item);
            if (result == null || !matchesRequestedField(result, query, searchBy)) {
                continue;
            }
            unique.putIfAbsent(result.providerId(), result);
        }
    }

    private URI buildSearchUri(
            String query,
            String searchBy,
            boolean useFieldAttribute) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(APPLE_SEARCH_URL)
                .queryParam("term", query)
                .queryParam("country", "US")
                .queryParam("media", "audiobook")
                .queryParam("entity", "audiobook")
                .queryParam("limit", MAX_RESULTS)
                .queryParam("explicit", "Yes");

        if (useFieldAttribute && "AUTHOR".equals(searchBy)) {
            builder.queryParam("attribute", "authorTerm");
        } else if (useFieldAttribute && "TITLE".equals(searchBy)) {
            builder.queryParam("attribute", "titleTerm");
        }

        // Apple does not expose a series-specific search attribute. SERIES
        // therefore uses the general audiobook search and is filtered locally.
        return builder.build().encode().toUri();
    }

    private CatalogBookResult mapResult(JsonNode item) {
        String providerId = firstText(item, "collectionId", "trackId");
        String title = firstText(item, "collectionName", "trackName");
        if (providerId.isBlank() || title.isBlank()) return null;

        String author = firstText(item, "artistName");
        String genre = firstText(item, "primaryGenreName");
        String description = cleanAppleDescription(
                firstText(item, "description", "longDescription"));
        String releaseDate = firstText(item, "releaseDate");
        String artworkUrl = firstText(
                item,
                "artworkUrl600",
                "artworkUrl512",
                "artworkUrl100");
        if (!artworkUrl.isBlank()) {
            artworkUrl = artworkUrl
                    .replace("100x100bb", "600x600bb")
                    .replace("100x100-75", "600x600-75");
        }
        String seriesName = extractSeriesName(title);
        // Only a number structurally attached to the catalog title is safe.
        // Marketing copy such as "#1 New York Times bestseller" must never
        // turn every volume into book one.
        Double seriesNumber = extractSeriesNumber(title);
        List<String> narrators = extractNarrators(description);

        Integer lengthSeconds = null;
        JsonNode length = item.path("trackTimeMillis");
        if (length.canConvertToLong() && length.asLong() > 0) {
            long seconds = length.asLong() / 1000L;
            if (seconds <= Integer.MAX_VALUE) {
                lengthSeconds = (int) seconds;
            }
        }

        return new CatalogBookResult(
                "Apple Audiobooks",
                providerId,
                title,
                null,
                author.isBlank() ? List.of() : List.of(author),
                genre.isBlank() ? List.of() : List.of(genre),
                description,
                null,
                blankToNull(releaseDate),
                null,
                lengthSeconds,
                narrators,
                blankToNull(artworkUrl),
                null,
                null,
                null,
                seriesName,
                seriesNumber,
                editionFormat(title),
                "AUDIOBOOK"
        );
    }

    private static String editionFormat(String title) {
        AudiobookIntent intent = audiobookIntent(title);
        if (intent.dramatized()) {
            String part = intent.partNumber() == null
                    ? ""
                    : " – Part " + intent.partNumber()
                      + (intent.partTotal() == null
                         ? ""
                         : " of " + intent.partTotal());
            return "Dramatized Adaptation" + part;
        }
        if (intent.abridged()) return "Abridged Audiobook";
        if (intent.unabridged()) return "Unabridged Audiobook";
        return "Audiobook";
    }

    private synchronized boolean reserveRequest(long now) {
        while (!requestTimes.isEmpty()
                && now - requestTimes.peekFirst() >= RATE_WINDOW_MILLIS) {
            requestTimes.removeFirst();
        }
        if (requestTimes.size() >= REQUESTS_PER_MINUTE) return false;
        requestTimes.addLast(now);
        return true;
    }

    private static String cleanAppleDescription(String value) {
        if (value == null || value.isBlank()) return null;

        String cleaned = HtmlUtils.htmlUnescape(value);
        // Some records contain entities inside already-escaped markup.
        cleaned = HtmlUtils.htmlUnescape(cleaned)
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(?:p|div|li|ul|ol|h[1-6])>", "\n")
                .replaceAll("(?i)<li[^>]*>", "• ")
                .replaceAll("<[^>]+>", "")
                .replace('\u00A0', ' ')
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll(" *\\n+ *", "\n")
                .trim();

        return cleaned.isBlank() ? null : cleaned;
    }

    private static String extractSeriesName(String title) {
        Matcher matcher = TRAILING_PARENTHETICAL.matcher(safeText(title));
        if (!matcher.find()) return null;

        String candidate = matcher.group(1).trim();
        String normalized = normalize(candidate);
        if (normalized.contains("dramatized")
                || normalized.contains("adaptation")
                || normalized.contains("audiobook")
                || normalized.contains("unabridged")
                || normalized.contains("abridged")
                || normalized.matches(".*\\bpart \\d+.*")) {
            return null;
        }
        // A parenthetical subtitle or marketing phrase is not series
        // metadata. Apple gives no dedicated series field, so accept this
        // storefront convention only when the label carries a structural
        // volume number, such as "(Caraval 2)".
        if (!candidate.matches(
                "(?i).+\\s+(?:(?:book|volume)\\s*)?#?\\d+(?:\\.\\d+)?\\s*$")) {
            return null;
        }
        return candidate
                .replaceFirst(
                        "(?i)\\s+(?:book\\s*)?#?\\d+(?:\\.\\d+)?\\s*$",
                        "")
                .trim();
    }

    private static Double extractSeriesNumber(String evidence) {
        Matcher matcher = SERIES_NUMBER.matcher(safeText(evidence));
        if (!matcher.find()) {
            matcher = TRAILING_PARENTHETICAL_SERIES_NUMBER.matcher(
                    safeText(evidence));
            if (!matcher.find() || safeText(extractSeriesName(evidence)).isBlank()) {
                return null;
            }
        }
        try {
            return Double.valueOf(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static List<String> extractNarrators(String description) {
        if (description == null || description.isBlank()) return List.of();

        Map<String, String> people = new LinkedHashMap<>();
        collectNarrator(BRACKETED_NARRATOR, description, people);
        collectNarrator(CREDITED_NARRATOR, description, people);
        collectNarrator(LABELED_NARRATOR, description, people);
        return people.values().stream().limit(4).toList();
    }

    private static void collectNarrator(
            Pattern pattern,
            String description,
            Map<String, String> people) {
        Matcher matcher = pattern.matcher(description);
        while (matcher.find()) {
            String credit = matcher.group(1)
                    .replaceFirst("(?i)['’]s$", "")
                    .replaceAll("(?i)\\s+(?:narration|performance)$", "")
                    .trim();

            for (String rawName : credit.split("(?i)\\s*(?:,|\\band\\b|&)\\s*")) {
                String name = rawName
                        .replaceAll("^[\\[\\(\\s]+|[\\]\\)\\s]+$", "")
                        .trim();
                if (!looksLikePersonName(name)) continue;
                people.putIfAbsent(normalize(name), name);
            }
        }
    }

    private static boolean looksLikePersonName(String value) {
        if (value == null || value.isBlank()) return false;
        String[] words = value.split("\\s+");
        if (words.length < 2 || words.length > 5) return false;
        for (String word : words) {
            if (!word.matches("[\\p{L}][\\p{L}.'’\\-]*")) return false;
        }
        return true;
    }

    private static boolean matchesRequestedField(
            CatalogBookResult result,
            String rawQuery,
            String searchBy) {
        if ("TITLE".equals(searchBy)) {
            String queryTitle = canonicalTitle(rawQuery);
            String targetTitle = canonicalTitle(result.title());
            if (queryTitle.isBlank() || targetTitle.isBlank()
                    || !(targetTitle.contains(queryTitle)
                    || queryTitle.contains(targetTitle))) {
                return false;
            }

            AudiobookIntent requested = audiobookIntent(rawQuery);
            AudiobookIntent candidate = audiobookIntent(result.title());
            if (requested.dramatized() && !candidate.dramatized()) {
                return false;
            }
            if (requested.unabridged() && candidate.abridged()) {
                return false;
            }
            if (requested.abridged() && !candidate.abridged()) {
                return false;
            }
            if (requested.partNumber() != null
                    && !requested.partNumber().equals(candidate.partNumber())) {
                return false;
            }
            return true;
        }

        String query = normalize(rawQuery);
        String target = switch (searchBy) {
            case "AUTHOR" -> normalize(String.join(" ", result.authors()));
            case "SERIES" -> normalize(
                    safeText(result.seriesName()) + " " + safeText(result.description()));
            default -> normalize(result.title());
        };

        if (target.isBlank()) return false;
        return target.contains(query) || query.contains(target);
    }

    private static boolean hasExplicitAudiobookIntent(String value) {
        AudiobookIntent intent = audiobookIntent(value);
        return intent.dramatized()
                || intent.abridged()
                || intent.unabridged()
                || intent.partNumber() != null;
    }

    private static AudiobookIntent audiobookIntent(String value) {
        String normalized = normalize(value);
        Matcher part = AUDIOBOOK_PART.matcher(normalized);
        Integer partNumber = part.find()
                ? Integer.valueOf(part.group(1)) : null;
        Integer partTotal = partNumber == null
                ? null : Integer.valueOf(part.group(2));
        boolean dramatized = normalized.contains("dramatized")
                || normalized.contains("graphic audio");
        boolean unabridged = normalized.contains("unabridged");
        boolean abridged = !unabridged && normalized.contains("abridged");
        return new AudiobookIntent(
                dramatized,
                abridged,
                unabridged,
                partNumber,
                partTotal);
    }

    private static String audiobookBaseTitle(String value) {
        String cleaned = safeText(value);
        cleaned = TRAILING_AUDIOBOOK_QUALIFIERS.matcher(cleaned)
                .replaceFirst("");
        cleaned = cleaned
                .replaceAll("(?i)\\s*[\\[(]?\\d+\\s+of\\s+\\d+[\\])]?\\s*", " ")
                .replaceAll("(?i)\\s*[\\[(]?part\\s*\\d+[\\])]?\\s*$", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned;
    }

    private static String normalizeSearchBy(String value) {
        String normalized = safeText(value).toUpperCase();
        return switch (normalized) {
            case "AUTHOR", "SERIES" -> normalized;
            default -> "TITLE";
        };
    }

    private static String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = value.asText("").trim();
                if (!text.isBlank()) return text;
            }
        }
        return "";
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        String cleaned = safeText(value);
        return cleaned.isBlank() ? null : cleaned;
    }

    private static String normalize(String value) {
        return safeText(value)
                .toLowerCase()
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }

    private record CacheEntry(long expiresAt, List<CatalogBookResult> results) {
    }

    private record AudiobookIntent(
            boolean dramatized,
            boolean abridged,
            boolean unabridged,
            Integer partNumber,
            Integer partTotal) {
    }
}
