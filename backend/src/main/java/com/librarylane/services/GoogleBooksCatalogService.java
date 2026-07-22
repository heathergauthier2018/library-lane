package com.librarylane.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.librarylane.catalog.CatalogBookResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GoogleBooksCatalogService {

    private static final String GOOGLE_BOOKS_URL =
            "https://www.googleapis.com/books/v1/volumes";
    private static final int MAX_RESULTS = 40;
    private static final Pattern SERIES_PATTERN = Pattern.compile(
            "(?i)(?:in|of)\\s+(?:[\\p{L}.'’\\-]+['’]s\\s+)?(?:the\\s+)?([\\p{L}0-9&'’\\- ]{2,50}?)\\s+series"
    );

    private final RestClient restClient;
    private final String apiKey;

    public GoogleBooksCatalogService(
            RestClient.Builder restClientBuilder,
            @Value("${library-lane.google-books.api-key:}") String apiKey) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public List<CatalogBookResult> search(
            String rawQuery,
            String requestedFormat,
            String rawSearchBy) {

        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() < 3) return List.of();

        SearchBy searchBy = SearchBy.from(rawSearchBy);
        String providerQuery = switch (searchBy) {
            // Google applies a field keyword to the term immediately after it.
            // Prefix each word instead of placing an encoded quoted phrase
            // after one keyword; the latter produced empty result sets.
            case TITLE -> fieldQuery("intitle", query);
            case AUTHOR -> fieldQuery("inauthor", query);
            // Google Books has no inseries operator. A quoted general search,
            // followed by local series/description relevance scoring, is the
            // most dependable search available from this provider.
            case SERIES -> query;
        };

        String normalizedFormat = normalize(requestedFormat);
        List<ScoredResult> candidates = new ArrayList<>();
        collectCandidates(
                fetch(providerQuery, requestedFormat),
                normalizedFormat,
                searchBy,
                query,
                candidates
        );

        // Combine the field-specific search with Google's general relevance
        // search. The latter is especially important while the reader is only
        // partway through a word (for example "iro" or "reb").
        if (searchBy != SearchBy.SERIES) {
            collectCandidates(
                    fetch(query, requestedFormat),
                    normalizedFormat,
                    searchBy,
                    query,
                    candidates
            );
        }

        if (searchBy == SearchBy.AUTHOR) {
            String[] terms = query.trim().split("\\s+");
            if (terms.length > 1 && terms[terms.length - 1].length() < 4) {
                collectCandidates(
                        fetch("inauthor:" + terms[0], requestedFormat),
                        normalizedFormat,
                        searchBy,
                        query,
                        candidates
                );
            }
        }

        candidates.sort(Comparator.comparingInt(ScoredResult::score).reversed());

        // Google occasionally repeats the same volume. Keep distinct ISBNs and
        // editions, but remove identical provider records.
        Map<String, CatalogBookResult> unique = new LinkedHashMap<>();
        for (ScoredResult candidate : candidates) {
            CatalogBookResult result = candidate.result();
            unique.putIfAbsent(result.providerId(), result);
        }

        return new ArrayList<>(unique.values());
    }

    private JsonNode fetch(String providerQuery, String requestedFormat) {
        URI uri = buildSearchUri(providerQuery, requestedFormat);
        return restClient.get().uri(uri).retrieve().body(JsonNode.class);
    }

    private void collectCandidates(
            JsonNode response,
            String normalizedFormat,
            SearchBy searchBy,
            String query,
            List<ScoredResult> candidates) {

        if (response == null || !response.path("items").isArray()) return;

        for (JsonNode item : response.path("items")) {
            CatalogBookResult result = mapResult(item, searchBy, query);

            if ("AUDIOBOOK".equals(normalizedFormat)) continue;

            int score = relevance(result, item, query, searchBy);
            // Google frequently marks a print-volume record as an e-book when
            // an EPUB preview exists. Format is therefore a preference boost,
            // not a hard exclusion rule.
            if (normalizedFormat.equals(result.format())) score += 75;
            if (score > 0) candidates.add(new ScoredResult(result, score));
        }
    }

    private URI buildSearchUri(String providerQuery, String requestedFormat) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(GOOGLE_BOOKS_URL)
                .queryParam("q", providerQuery)
                .queryParam("printType", "books")
                .queryParam("projection", "full")
                .queryParam("orderBy", "relevance")
                .queryParam("maxResults", MAX_RESULTS);

        if ("EBOOK".equals(normalize(requestedFormat))) {
            builder.queryParam("filter", "ebooks");
        }
        if (!apiKey.isBlank()) builder.queryParam("key", apiKey);

        return builder.build().encode().toUri();
    }

    private static String fieldQuery(String field, String query) {
        List<String> parts = new ArrayList<>();
        for (String term : query.trim().split("\\s+")) {
            if (!term.isBlank()) parts.add(field + ":" + term);
        }
        return String.join(" ", parts);
    }

    private CatalogBookResult mapResult(
            JsonNode item,
            SearchBy searchBy,
            String query) {

        JsonNode volume = item.path("volumeInfo");
        boolean isEbook = item.path("saleInfo").path("isEbook").asBoolean(false);
        String format = isEbook ? "EBOOK" : "PHYSICAL";

        String cover = firstText(
                volume.path("imageLinks").path("extraLarge"),
                volume.path("imageLinks").path("large"),
                volume.path("imageLinks").path("medium"),
                volume.path("imageLinks").path("thumbnail"),
                volume.path("imageLinks").path("smallThumbnail")
        );
        if (cover != null) cover = cover.replace("http://", "https://");

        String isbn10 = null;
        String isbn13 = null;
        for (JsonNode identifier : volume.path("industryIdentifiers")) {
            String type = text(identifier.path("type"));
            String value = text(identifier.path("identifier"));
            if ("ISBN_10".equals(type)) isbn10 = value;
            if ("ISBN_13".equals(type)) isbn13 = value;
        }

        JsonNode series = volume.path("seriesInfo");
        String description = text(volume.path("description"));
        String seriesName = firstText(
                series.path("shortSeriesBookTitle"),
                series.path("seriesName")
        );
        if (seriesName == null) seriesName = inferSeriesName(description);
        // Never stamp the user's series query onto every Google result. Google
        // can return broad keyword matches; series membership must come from
        // structured metadata, a description, or the aggregator's verified
        // title map.

        Set<String> genres = new LinkedHashSet<>(strings(volume.path("categories")));
        addHelpfulGenres(genres, description);

        return new CatalogBookResult(
                "GOOGLE_BOOKS",
                text(item.path("id")),
                valueOrEmpty(text(volume.path("title"))),
                text(volume.path("subtitle")),
                strings(volume.path("authors")),
                new ArrayList<>(genres),
                description,
                text(volume.path("publisher")),
                text(volume.path("publishedDate")),
                positiveInteger(volume.path("pageCount")),
                null,
                List.of(),
                cover,
                text(volume.path("language")),
                isbn10,
                isbn13,
                seriesName,
                number(series.path("bookDisplayNumber")),
                isEbook ? "E-Book edition" : "Physical edition",
                format
        );
    }

    private static int relevance(
            CatalogBookResult result,
            JsonNode item,
            String query,
            SearchBy searchBy) {

        String needle = normalize(query);
        String title = normalize(result.title());
        String authors = normalize(String.join(" ", result.authors()));
        String series = normalize(result.seriesName());
        String description = normalize(text(item.path("volumeInfo").path("description")));

        return switch (searchBy) {
            case TITLE -> {
                if (title.equals(needle)) yield 1000;
                if (title.startsWith(needle)) yield 850;
                if (title.contains(needle)) yield 700;
                yield allTermsMatch(title, needle) ? 450 : 0;
            }
            case AUTHOR -> {
                if (authors.equals(needle)) yield 1000;
                if (authors.contains(needle)) yield 850;
                yield allTermsMatch(authors, needle) ? 600 : 0;
            }
            case SERIES -> {
                if (series.equals(needle)) yield 1000;
                if (series.contains(needle)) yield 850;
                if (description.contains(needle)) yield 700;
                if (title.contains(needle)) yield 600;
                String searchable = title + " " + series + " " + description;
                yield allMeaningfulTermsMatch(searchable, needle) ? 400 : 0;
            }
        };
    }

    private static boolean allTermsMatch(String haystack, String needle) {
        for (String term : needle.split("\\s+")) {
            if (!term.isBlank() && !haystack.contains(term)) return false;
        }
        return true;
    }

    private static boolean allMeaningfulTermsMatch(String haystack, String needle) {
        boolean foundMeaningfulTerm = false;
        for (String term : needle.split("\\s+")) {
            if (term.length() <= 2 || "the".equals(term)) continue;
            foundMeaningfulTerm = true;
            if (!haystack.contains(term)) return false;
        }
        return foundMeaningfulTerm;
    }

    private static String inferSeriesName(String description) {
        if (description == null || description.isBlank()) return null;
        Matcher matcher = SERIES_PATTERN.matcher(description);
        if (!matcher.find()) return null;
        String value = matcher.group(1).trim();
        return value.length() > 50 ? null : value;
    }

    private static void addHelpfulGenres(Set<String> genres, String description) {
        String text = normalize(description);
        if (text.isBlank()) return;
        if (containsAny(text, "fantasy", "dragon", "magic", "magical")) genres.add("Fantasy");
        if (containsAny(text, "romance", "romantic", "love story")) genres.add("Romance");
        if (containsAny(text, "science fiction", "sci-fi", "space opera")) genres.add("Science Fiction");
        if (containsAny(text, "mystery", "detective")) genres.add("Mystery");
        if (containsAny(text, "thriller", "suspense")) genres.add("Thriller");
        if (containsAny(text, "horror", "terrifying")) genres.add("Horror");
        if (containsAny(text, "historical fiction")) genres.add("Historical Fiction");
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    private static List<String> strings(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        node.forEach(value -> {
            String item = text(value);
            if (item != null && !item.isBlank()) values.add(item);
        });
        return values;
    }

    private static String firstText(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            String value = text(node);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        String value = node.asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private static Integer positiveInteger(JsonNode node) {
        if (node == null || !node.isNumber()) return null;
        int value = node.intValue();
        return value > 0 ? value : null;
    }

    private static Double number(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        if (node.isNumber()) return node.doubleValue();
        try {
            return Double.valueOf(node.asText());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private enum SearchBy {
        TITLE, AUTHOR, SERIES;

        private static SearchBy from(String value) {
            try {
                return SearchBy.valueOf(value == null ? "TITLE" : value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return TITLE;
            }
        }
    }

    private record ScoredResult(CatalogBookResult result, int score) {}
}
