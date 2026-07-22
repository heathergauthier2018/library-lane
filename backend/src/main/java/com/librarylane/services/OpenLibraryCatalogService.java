package com.librarylane.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.librarylane.catalog.CatalogBookResult;
import com.librarylane.catalog.OpenLibraryWorkDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class OpenLibraryCatalogService {

    private static final String SEARCH_URL = "https://openlibrary.org/search.json";
    private static final String WORK_URL = "https://openlibrary.org/works/{workId}.json";

    private final RestClient restClient;

    public OpenLibraryCatalogService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * Loads the richer Open Library work record associated with a search
     * result. The providerId produced by this service is already the work ID,
     * but this method also accepts values such as /works/OL123W.
     *
     * A missing or temporarily unavailable work must not make the entire
     * Library Lane catalog fail, so an unsuccessful lookup returns empty.
     */
    public Optional<OpenLibraryWorkDetails> getWorkDetails(String rawWorkId) {
        String workId = normalizeWorkId(rawWorkId);
        if (workId == null) return Optional.empty();

        try {
            JsonNode work = restClient.get()
                    .uri(WORK_URL, workId)
                    .retrieve()
                    .body(JsonNode.class);

            if (work == null || work.isMissingNode() || work.isNull()) {
                return Optional.empty();
            }

            return Optional.of(new OpenLibraryWorkDetails(
                    workId,
                    text(work.path("title")),
                    text(work.path("subtitle")),
                    descriptionText(work.path("description")),
                    strings(work.path("subjects")),
                    flexibleStrings(work.path("series")),
                    authorKeys(work.path("authors")),
                    text(work.path("first_publish_date"))
            ));
        } catch (RuntimeException error) {
            System.err.println(
                    "Open Library work lookup failed for " + workId
                            + ": " + error.getMessage()
            );
            return Optional.empty();
        }
    }

    /**
     * Combines a lightweight Open Library search result with its richer work
     * record. Search/edition values remain the first choice because they refer
     * to the edition the reader selected; work metadata fills only missing or
     * incomplete fields.
     */
    public CatalogBookResult enrichWithWorkDetails(CatalogBookResult result) {
        if (result == null || !"OPEN_LIBRARY".equals(result.provider())) {
            return result;
        }

        Optional<OpenLibraryWorkDetails> detailsLookup =
                getWorkDetails(result.providerId());

        if (detailsLookup.isEmpty()) return result;

        OpenLibraryWorkDetails details = detailsLookup.get();
        List<String> mergedGenres = mergeGenres(
                result.genres(),
                usefulGenres(details.subjects())
        );

        String rawWorkSeries = first(details.seriesNames());
        String resolvedSeriesName = hasText(result.seriesName())
                ? result.seriesName()
                : cleanSeriesName(rawWorkSeries);

        Double resolvedSeriesNumber = result.seriesNumber() != null
                ? result.seriesNumber()
                : seriesNumber(rawWorkSeries);

        return new CatalogBookResult(
                result.provider(),
                result.providerId(),
                preferText(result.title(), details.title()),
                preferText(result.subtitle(), details.subtitle()),
                result.authors(),
                mergedGenres,
                preferDescription(result.description(), details.description()),
                result.publisher(),
                earliestPublication(result.publicationDate(), details.firstPublishDate()),
                result.pageCount(),
                result.audiobookLengthSeconds(),
                result.narrators(),
                result.coverImageUrl(),
                result.language(),
                result.isbn10(),
                result.isbn13(),
                resolvedSeriesName,
                resolvedSeriesNumber,
                result.editionFormat(),
                result.format()
        );
    }

    public List<CatalogBookResult> search(
            String rawQuery,
            String requestedFormat,
            String rawSearchBy) {
        String query = clean(rawQuery);
        if (query.length() < 3) return List.of();

        String searchBy = normalizeSearchBy(rawSearchBy);
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromUriString(SEARCH_URL)
                .queryParam("limit", 100)
                .queryParam("fields", String.join(",",
                        "key", "title", "subtitle", "author_name", "subject",
                        "first_publish_year", "publish_date", "publisher",
                        "number_of_pages_median", "cover_i", "isbn", "language",
                        "series", "ebook_access"));

        if ("AUTHOR".equals(searchBy)) {
            uri.queryParam("q", authorPrefixQuery(query));
        } else if ("SERIES".equals(searchBy)) {
            // Open Library's Search API uses Solr/Lucene field syntax for
            // fields that do not have a dedicated query parameter.
            String prefix = prefixQuery(query);
            uri.queryParam("q", "series:(" + prefix + ") OR subject:("
                    + prefix + ")");
        } else {
            uri.queryParam("title", query);
        }

        URI requestUri = uri.build().encode().toUri();
        JsonNode response = restClient.get()
                .uri(requestUri)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.path("docs").isArray()) {
            return List.of();
        }

        boolean allFormats = requestedFormat == null || requestedFormat.isBlank();
        String format = normalizeFormat(requestedFormat);
        List<CatalogBookResult> results = new ArrayList<>();
        for (JsonNode doc : response.path("docs")) {
            CatalogBookResult mapped = map(doc, format, searchBy, query);
            if (mapped != null) results.add(mapped);
            if (allFormats) {
                CatalogBookResult ebook = map(doc, "EBOOK", searchBy, query);
                if (ebook != null) results.add(ebook);
            }
        }
        return results;
    }

    private static String authorPrefixQuery(String query) {
        return "author:(" + prefixQuery(query) + ")";
    }

    private static String prefixQuery(String query) {
        String[] terms = query.trim().split("\\s+");
        List<String> pieces = new ArrayList<>();
        for (int index = 0; index < terms.length; index++) {
            String escaped = escapeLucene(terms[index]);
            if (escaped.isBlank()) continue;
            pieces.add(index == terms.length - 1 ? escaped + "*" : escaped);
        }
        return String.join(" AND ", pieces);
    }

    /**
     * Finds the Open Library work attached to one exact ISBN. ISBN resolution
     * is safe for cross-format enrichment because it follows an edition into
     * its owning work instead of guessing from a shared title.
     */
    public CatalogBookResult findWorkByIsbn(String rawIsbn) {
        String isbn = rawIsbn == null ? "" : rawIsbn.replaceAll("[^0-9Xx]", "");
        if (isbn.length() != 10 && isbn.length() != 13) return null;

        URI requestUri = UriComponentsBuilder
                .fromUriString(SEARCH_URL)
                .queryParam("isbn", isbn)
                .queryParam("limit", 5)
                .queryParam("fields", String.join(",",
                        "key", "title", "subtitle", "author_name", "subject",
                        "first_publish_year", "publisher",
                        "number_of_pages_median", "cover_i", "isbn", "language",
                        "series", "ebook_access"))
                .build().encode().toUri();

        JsonNode response = restClient.get()
                .uri(requestUri)
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !response.path("docs").isArray()) return null;

        for (JsonNode doc : response.path("docs")) {
            CatalogBookResult result = map(doc, "PHYSICAL", "TITLE", "");
            if (result != null) return enrichWithWorkDetails(result);
        }
        return null;
    }

    private CatalogBookResult map(
            JsonNode doc,
            String requestedFormat,
            String searchBy,
            String searchedValue) {
        String title = text(doc.path("title"));
        if (title == null) return null;

        String ebookAccess = text(doc.path("ebook_access"));
        boolean hasEbook = ebookAccess != null &&
                !"no_ebook".equalsIgnoreCase(ebookAccess);

        if ("AUDIOBOOK".equals(requestedFormat)) {
            // Open Library does not provide dependable narrator/runtime data.
            return null;
        }
        if ("EBOOK".equals(requestedFormat) && !hasEbook) return null;

        String resultFormat = "EBOOK".equals(requestedFormat)
                ? "EBOOK"
                : "PHYSICAL";

        List<String> isbns = strings(doc.path("isbn"));
        String isbn10 = firstIsbn(isbns, 10);
        String isbn13 = firstIsbn(isbns, 13);
        Integer coverId = integer(doc.path("cover_i"));
        String cover = coverId == null
                ? null
                : "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg";

        List<String> allSubjects = strings(doc.path("subject"));
        List<String> subjects = usefulGenres(allSubjects);

        List<String> seriesValues = strings(doc.path("series"));
        String seriesName = matchingSeriesValue(seriesValues, searchedValue);

        if ((seriesName == null || seriesName.isBlank()) && "SERIES".equals(searchBy)) {
            /*
             * Open Library frequently omits its `series` field even when a
             * work belongs to a series. Subjects such as
             * "Serie:The_Empyrean" and subtitles/descriptions often retain
             * that information. Only fill the requested series when the
             * document itself contains credible series evidence. Never label
             * every provider result with the user's query: Open Library may
             * return title/keyword matches that are not members of the series.
             */
            seriesName = inferRequestedSeries(doc, allSubjects, searchedValue);
        }
        Double seriesNumber = seriesNumber(seriesName);

        List<String> publishers = strings(doc.path("publisher"));
        List<String> publishDates = strings(doc.path("publish_date"));
        Integer firstYear = integer(doc.path("first_publish_year"));

        return new CatalogBookResult(
                "OPEN_LIBRARY",
                valueOrEmpty(text(doc.path("key"))).replace("/works/", ""),
                title,
                text(doc.path("subtitle")),
                strings(doc.path("author_name")),
                subjects,
                null,
                publishers.isEmpty() ? null : publishers.get(0),
                firstYear == null
                        ? (publishDates.isEmpty() ? null : publishDates.get(0))
                        : String.valueOf(firstYear),
                integer(doc.path("number_of_pages_median")),
                null,
                List.of(),
                cover,
                first(strings(doc.path("language"))),
                isbn10,
                isbn13,
                cleanSeriesName(seriesName),
                seriesNumber,
                resultFormat.equals("EBOOK")
                        ? "E-book catalog record"
                        : "Book catalog record",
                resultFormat
        );
    }

    private static String normalizeSearchBy(String value) {
        String normalized = value == null ? "TITLE" : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "AUTHOR", "SERIES" -> normalized;
            default -> "TITLE";
        };
    }

    private static String normalizeFormat(String value) {
        String normalized = value == null ? "PHYSICAL" : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "EBOOK", "AUDIOBOOK" -> normalized;
            default -> "PHYSICAL";
        };
    }

    private static String escapeLucene(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String matchingSeriesValue(
            List<String> seriesValues,
            String searchedValue) {
        if (seriesValues == null || seriesValues.isEmpty()) return null;

        String normalizedQuery = canonicalSeries(searchedValue);
        if (normalizedQuery.isBlank()) return seriesValues.get(0);

        return seriesValues.stream()
                .filter(value -> {
                    String normalizedSeries = canonicalSeries(value);
                    return normalizedSeries.equals(normalizedQuery)
                            || normalizedSeries.startsWith(normalizedQuery)
                            || normalizedQuery.startsWith(normalizedSeries);
                })
                .findFirst()
                .orElse(null);
    }

    private static String inferRequestedSeries(
            JsonNode doc,
            List<String> subjects,
            String searchedValue) {
        String normalizedQuery = normalizeForMatch(searchedValue);
        if (normalizedQuery.isBlank()) return null;

        for (String subject : subjects) {
            String normalizedSubject = normalizeForMatch(subject);
            boolean markedAsSeries = normalizedSubject.startsWith("series ")
                    || normalizedSubject.startsWith("serie ")
                    || normalizedSubject.contains(" book series ");

            if (markedAsSeries && normalizedSubject.contains(normalizedQuery)) {
                return seriesNameFromSubject(subject, searchedValue);
            }
        }

        String subtitle = normalizeForMatch(text(doc.path("subtitle")));
        if (containsSeriesEvidence(subtitle, normalizedQuery)) {
            return searchedValue;
        }

        return null;
    }

    private static boolean containsSeriesEvidence(String value, String query) {
        if (value.isBlank() || !value.contains(query)) return false;
        return value.contains("series")
                || value.contains("book")
                || value.matches(".*\\b" + java.util.regex.Pattern.quote(query)
                + "\\b\\s*[,#:-]?\\s*\\d+(?:\\.\\d+)?.*");
    }

    private static String seriesNameFromSubject(String subject, String fallback) {
        if (subject == null) return fallback;
        String value = subject
                .replaceFirst("(?i)^\\s*serie?s?\\s*[:=-]\\s*", "")
                .replace('_', ' ')
                .trim();
        return value.isBlank() ? fallback : titleCase(value);
    }

    private static List<String> usefulGenres(List<String> subjects) {
        List<String> genres = new ArrayList<>();
        for (String subject : subjects) {
            if (subject == null) continue;
            String value = subject.replace('_', ' ').trim();
            String normalized = value.toLowerCase(Locale.ROOT);
            if (normalized.isBlank()
                    || normalized.startsWith("nyt:")
                    || normalized.startsWith("serie:")
                    || normalized.startsWith("series:")
                    || normalized.contains("new york times bestseller")
                    || normalized.contains("combined-print")
                    || normalized.matches(".*=\\d{4}-\\d{2}-\\d{2}.*")) {
                continue;
            }
            if (!genres.contains(value)) genres.add(value);
            if (genres.size() == 8) break;
        }
        return genres;
    }

    private static List<String> mergeGenres(
            List<String> firstValues,
            List<String> secondValues) {
        List<String> merged = new ArrayList<>();

        if (firstValues != null) {
            for (String value : firstValues) {
                if (hasText(value) && !containsIgnoreCase(merged, value)) {
                    merged.add(value.trim());
                }
            }
        }

        if (secondValues != null) {
            for (String value : secondValues) {
                if (hasText(value) && !containsIgnoreCase(merged, value)) {
                    merged.add(value.trim());
                }
                if (merged.size() >= 12) break;
            }
        }

        return merged.stream().limit(12).toList();
    }

    private static boolean containsIgnoreCase(List<String> values, String target) {
        return values.stream().anyMatch(value -> value.equalsIgnoreCase(target.trim()));
    }

    private static String preferText(String primary, String fallback) {
        return hasText(primary) ? primary : fallback;
    }

    private static String preferDescription(String primary, String fallback) {
        String validPrimary = meaningfulDescription(primary) ? primary : null;
        String validFallback = meaningfulDescription(fallback) ? fallback : null;

        if (!hasText(validPrimary)) return validFallback;
        if (!hasText(validFallback)) return validPrimary;

        // A substantially richer work description may replace a very short
        // catalog fragment, while a valid selected-edition description keeps
        // priority when both are meaningful.
        if (validPrimary.trim().length() < 120
                && validFallback.trim().length() > validPrimary.trim().length() + 80) {
            return validFallback;
        }
        return validPrimary;
    }

    private static boolean meaningfulDescription(String value) {
        if (!hasText(value)) return false;
        String normalized = normalizeForMatch(value);
        boolean hasPages = normalized.matches(".*\\b\\d+ pages?\\b.*");
        boolean hasCentimeters = normalized.matches(".*\\b\\d+ cm\\b.*");
        boolean hasLexile = normalized.contains("lexile")
                || normalized.matches(".*\\b\\d+l\\b.*");

        if (value.trim().length() < 320
                && hasPages
                && (hasCentimeters || hasLexile)) {
            return false;
        }

        return !normalized.equals("no description available")
                && !normalized.equals("description not available");
    }

    private static String earliestPublication(String first, String second) {
        Integer firstYear = publicationYear(first);
        Integer secondYear = publicationYear(second);
        if (firstYear == null) return second;
        if (secondYear == null) return first;
        return String.valueOf(Math.min(firstYear, secondYear));
    }

    private static Integer publicationYear(String value) {
        if (value == null) return null;
        var matcher = java.util.regex.Pattern
                .compile("\\b(1[0-9]{3}|20[0-9]{2}|2100)\\b")
                .matcher(value);
        if (!matcher.find()) return null;
        try {
            return Integer.valueOf(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String titleCase(String value) {
        String[] words = value.toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < words.length; index++) {
            String word = words[index];
            if (word.isBlank()) continue;
            boolean smallWord = index > 0 && (word.equals("the") || word.equals("of")
                    || word.equals("and") || word.equals("in") || word.equals("a")
                    || word.equals("an"));
            if (result.length() > 0) result.append(' ');
            result.append(smallWord ? word
                    : Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return result.toString();
    }

    private static String normalizeForMatch(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static String canonicalSeries(String value) {
        return normalizeForMatch(value)
                .replaceFirst("^the\\s+", "")
                .replaceFirst("\\s+(?:book\\s*)?\\d+(?:\\.\\d+)?$", "")
                .replaceFirst("\\s+series$", "")
                .trim();
    }

    private static String cleanSeriesName(String value) {
        if (value == null) return null;
        String cleaned = value
                .replace('_', ' ')
                .replaceFirst("(?i)^\\s*serie?s?\\s*[:=-]\\s*", "")
                .replaceFirst("(?i)\\s*(?:,|-|:)?\\s*(?:book\\s*)?[#:]?\\s*\\d+(?:\\.\\d+)?\\s*$", "")
                .replaceFirst("[,;:-]+\\s*$", "")
                .trim();
        return titleCase(cleaned);
    }

    private static Double seriesNumber(String value) {
        if (value == null) return null;
        var matcher = java.util.regex.Pattern
                .compile("(?:#|book\\s*)?(\\d+(?:\\.\\d+)?)\\s*$", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(value);
        if (!matcher.find()) return null;
        try {
            return Double.valueOf(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static List<String> strings(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            String value = text(item);
            if (value != null && !values.contains(value)) values.add(value);
        });
        return values;
    }

    /**
     * Open Library descriptions may be plain strings or objects containing a
     * value property. Supporting both shapes prevents valid descriptions from
     * being silently lost.
     */
    private static String descriptionText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        if (node.isTextual()) return text(node);
        if (node.isObject()) return text(node.path("value"));
        return null;
    }

    /**
     * Some Open Library fields appear as an array in one record and a single
     * string in another. Convert either representation into one safe list.
     */
    private static List<String> flexibleStrings(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) return strings(node);

        String value = text(node);
        return value == null ? List.of() : List.of(value);
    }

    /**
     * Work authors are references such as {"author":{"key":"/authors/..."}}.
     * Keep their keys now so the resolver can request structured author names
     * later rather than guessing co-authors from a description.
     */
    private static List<String> authorKeys(JsonNode authorsNode) {
        if (authorsNode == null || !authorsNode.isArray()) return List.of();

        List<String> keys = new ArrayList<>();
        authorsNode.forEach(entry -> {
            String key = text(entry.path("author").path("key"));
            if (key == null) key = text(entry.path("key"));
            if (key != null && !keys.contains(key)) keys.add(key);
        });
        return keys;
    }

    private static String normalizeWorkId(String value) {
        if (value == null || value.isBlank()) return null;

        String cleaned = value.trim()
                .replaceFirst("^https?://openlibrary\\.org", "")
                .replaceFirst("^/works/", "")
                .replaceFirst("\\.json$", "")
                .trim();

        return cleaned.matches("OL\\d+W") ? cleaned : null;
    }

    private static String firstIsbn(List<String> values, int length) {
        return values.stream()
                .map(value -> value.replaceAll("[^0-9Xx]", ""))
                .filter(value -> value.length() == length)
                .findFirst()
                .orElse(null);
    }

    private static String first(List<String> values) {
        return values.isEmpty() ? null : values.get(0);
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        String value = node.asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Integer integer(JsonNode node) {
        return node != null && node.isNumber() ? node.intValue() : null;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
