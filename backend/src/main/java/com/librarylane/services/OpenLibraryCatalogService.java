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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OpenLibraryCatalogService {

    private static final String SEARCH_URL = "https://openlibrary.org/search.json";
    private static final String WORK_URL = "https://openlibrary.org/works/{workId}.json";
    private static final String EDITIONS_URL =
            "https://openlibrary.org/works/{workId}/editions.json?limit=100";
    private static final String EDITION_URL = "https://openlibrary.org/isbn/{isbn}.json";

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

    /**
     * Replaces flattened work-search fields with one coherent English
     * edition. Search documents combine publishers, ISBNs, dates, and
     * languages from every edition and those array positions are unrelated.
     */
    public CatalogBookResult enrichWithBestEdition(CatalogBookResult result) {
        if (result == null || !"OPEN_LIBRARY".equals(result.provider())) {
            return result;
        }
        String workId = normalizeWorkId(result.providerId());
        if (workId == null) return result;

        try {
            JsonNode response = restClient.get()
                    .uri(EDITIONS_URL, workId)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.path("entries").isArray()) {
                return result;
            }

            JsonNode best = null;
            int bestScore = Integer.MIN_VALUE;
            for (JsonNode edition : response.path("entries")) {
                int score = editionScore(edition, result);
                if (score > bestScore) {
                    best = edition;
                    bestScore = score;
                }
            }
            return best == null || bestScore < 0
                    ? result : mergeEdition(result, best);
        } catch (RuntimeException error) {
            System.err.println(
                    "Open Library editions lookup failed for " + workId
                            + ": " + error.getMessage());
            return result;
        }
    }

    public CatalogBookResult enrichAudiobookEdition(
            CatalogBookResult selected,
            String rawWorkId) {
        if (selected == null
                || !"AUDIOBOOK".equalsIgnoreCase(selected.format())) {
            return selected;
        }
        String workId = normalizeWorkId(rawWorkId);
        if (workId == null) return selected;

        try {
            JsonNode response = restClient.get()
                    .uri(EDITIONS_URL, workId)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.path("entries").isArray()) {
                return selected;
            }

            JsonNode best = null;
            int bestScore = Integer.MIN_VALUE;
            for (JsonNode edition : response.path("entries")) {
                int score = audiobookEditionScore(edition, selected);
                if (score > bestScore) {
                    best = edition;
                    bestScore = score;
                }
            }
            return best == null || bestScore < 0
                    ? selected : mergeAudiobookEdition(selected, best);
        } catch (RuntimeException error) {
            System.err.println(
                    "Open Library audiobook-edition lookup failed for "
                            + workId + ": " + error.getMessage());
            return selected;
        }
    }

    private static int audiobookEditionScore(
            JsonNode edition,
            CatalogBookResult selected) {
        String editionTitle = normalize(text(edition.path("title")));
        String selectedTitle = normalize(selected.title());
        if (!(editionTitle.equals(selectedTitle)
                || selectedTitle.startsWith(editionTitle + " "))) return -1000;

        String format = normalize(text(edition.path("physical_format")));
        if (!(format.contains("audio")
                || format.contains("sound recording"))) return -1000;

        List<String> languages = editionLanguages(edition.path("languages"));
        if (!languages.isEmpty()
                && languages.stream().noneMatch(OpenLibraryCatalogService::isEnglish)) {
            return -1000;
        }

        int score = 500;
        Integer selectedYear = publicationYear(selected.publicationDate());
        Integer editionYear = publicationYear(text(edition.path("publish_date")));
        if (selectedYear != null && editionYear != null) {
            score += Math.max(0, 500 - Math.abs(selectedYear - editionYear) * 100);
        }
        String publishers = normalize(String.join(" ",
                strings(edition.path("publishers"))));
        if (hasText(selected.publisher())
                && publishers.contains(normalize(selected.publisher()))) score += 150;
        String notes = editionNotes(edition);
        if (notes.toLowerCase(Locale.ROOT).contains("narrated by")) score += 100;
        if (notes.toLowerCase(Locale.ROOT).contains("length:")) score += 100;
        if (!strings(edition.path("isbn_13")).isEmpty()) score += 25;
        return score;
    }

    private static CatalogBookResult mergeAudiobookEdition(
            CatalogBookResult selected,
            JsonNode edition) {
        String notes = editionNotes(edition);
        List<String> narrators = selected.narrators().isEmpty()
                ? audiobookNarrators(notes) : selected.narrators();
        Integer duration = selected.audiobookLengthSeconds() != null
                ? selected.audiobookLengthSeconds() : audiobookDuration(notes);
        Double seriesNumber = selected.seriesNumber() != null
                ? selected.seriesNumber()
                : seriesNumber(text(edition.path("subtitle")));
        List<String> isbn10s = strings(edition.path("isbn_10"));
        List<String> isbn13s = strings(edition.path("isbn_13"));

        return new CatalogBookResult(
                selected.provider(), selected.providerId(), selected.title(),
                selected.subtitle(), selected.authors(), selected.genres(),
                selected.description(), selected.publisher(),
                preferText(text(edition.path("publish_date")),
                        selected.publicationDate()),
                null, duration, narrators, selected.coverImageUrl(), "eng",
                hasText(selected.isbn10()) ? selected.isbn10() : first(isbn10s),
                hasText(selected.isbn13()) ? selected.isbn13() : first(isbn13s),
                selected.seriesName(), seriesNumber,
                hasText(selected.editionFormat())
                        ? selected.editionFormat()
                        : text(edition.path("edition_name")),
                selected.format());
    }

    private static String editionNotes(JsonNode edition) {
        JsonNode notes = edition.path("notes");
        return notes.isTextual() ? text(notes) : text(notes.path("value"));
    }

    private static List<String> audiobookNarrators(String notes) {
        Matcher matcher = Pattern.compile(
                "(?im)^Narrated by:\\s*(.+)$").matcher(notes);
        if (!matcher.find()) return List.of();
        return java.util.Arrays.stream(matcher.group(1).split("\\s*(?:,|&| and )\\s*"))
                .map(String::trim).filter(OpenLibraryCatalogService::hasText).toList();
    }

    private static Integer audiobookDuration(String notes) {
        Matcher matcher = Pattern.compile(
                "(?i)Length:\\s*(?:(\\d+)\\s*hours?)?\\s*(?:(\\d+)\\s*minutes?)?")
                .matcher(notes);
        if (!matcher.find()) return null;
        int hours = matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1));
        int minutes = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
        int seconds = hours * 3600 + minutes * 60;
        return seconds > 0 ? seconds : null;
    }

    private static int editionScore(
            JsonNode edition,
            CatalogBookResult selected) {
        String title = text(edition.path("title"));
        if (!normalize(title).equals(normalize(selected.title()))) return -1000;

        List<String> languages = editionLanguages(edition.path("languages"));
        if (!languages.isEmpty()
                && languages.stream().noneMatch(OpenLibraryCatalogService::isEnglish)) {
            return -1000;
        }

        String physicalFormat = normalize(text(edition.path("physical_format")));
        boolean audio = physicalFormat.contains("audio")
                || physicalFormat.contains("sound recording");
        boolean ebook = physicalFormat.contains("ebook")
                || physicalFormat.contains("electronic");
        boolean wantsEbook = "EBOOK".equalsIgnoreCase(selected.format());
        if (audio || (wantsEbook && !ebook) || (!wantsEbook && ebook)) return -1000;

        int score = 500;
        if (!languages.isEmpty()) score += 150;
        if (wantsEbook) score += 250;
        if (!wantsEbook && (physicalFormat.contains("hardcover")
                || physicalFormat.contains("paperback"))) score += 100;
        if (!wantsEbook && physicalFormat.contains("hardcover")) score += 20;
        if (!strings(edition.path("publishers")).isEmpty()) score += 50;
        if (integer(edition.path("number_of_pages")) != null) score += 50;
        if (!strings(edition.path("isbn_13")).isEmpty()) score += 50;
        if (edition.path("covers").isArray()
                && !edition.path("covers").isEmpty()) score += 25;

        Integer editionYear = publicationYear(text(edition.path("publish_date")));
        Integer workYear = publicationYear(selected.publicationDate());
        if (editionYear != null && workYear != null) {
            score += Math.max(0, 100 - Math.abs(editionYear - workYear));
        }
        return score;
    }

    private static CatalogBookResult mergeEdition(
            CatalogBookResult selected,
            JsonNode edition) {
        List<String> isbn10s = strings(edition.path("isbn_10"));
        List<String> isbn13s = strings(edition.path("isbn_13"));
        List<String> publishers = strings(edition.path("publishers"));
        Integer coverId = edition.path("covers").isArray()
                && !edition.path("covers").isEmpty()
                ? integer(edition.path("covers").get(0)) : null;
        String cover = coverId == null ? selected.coverImageUrl()
                : "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg";
        String physicalFormat = text(edition.path("physical_format"));

        return new CatalogBookResult(
                selected.provider(), selected.providerId(), selected.title(),
                selected.subtitle(), selected.authors(), selected.genres(),
                selected.description(), first(publishers),
                preferText(text(edition.path("publish_date")), selected.publicationDate()),
                integer(edition.path("number_of_pages")),
                selected.audiobookLengthSeconds(), selected.narrators(), cover,
                "eng", first(isbn10s), first(isbn13s), selected.seriesName(),
                selected.seriesNumber(), hasText(physicalFormat)
                        ? physicalFormat : selected.editionFormat(), selected.format());
    }

    private static List<String> editionLanguages(JsonNode node) {
        List<String> languages = new ArrayList<>();
        if (!node.isArray()) return languages;
        node.forEach(language -> languages.add(text(language.path("key"))));
        return languages;
    }

    private static boolean isEnglish(String value) {
        String normalized = normalize(value);
        return normalized.endsWith(" eng") || normalized.equals("eng")
                || normalized.equals("en");
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
    /**
     * Returns the page count attached to one exact Open Library edition.
     * Work-level median pagination is deliberately not used here because it
     * can describe a different hardcover, paperback, or digital edition.
     */
    public Integer findPageCountByIsbn(String rawIsbn) {
        String isbn = rawIsbn == null
                ? ""
                : rawIsbn.replaceAll("[^0-9Xx]", "");
        if (isbn.length() != 10 && isbn.length() != 13) return null;

        try {
            JsonNode edition = restClient.get()
                    .uri(EDITION_URL, isbn)
                    .retrieve()
                    .body(JsonNode.class);
            if (edition == null
                    || edition.isMissingNode()
                    || edition.isNull()) {
                return null;
            }

            Integer pages = integer(edition.path("number_of_pages"));
            return pages != null && pages > 0 ? pages : null;
        } catch (RuntimeException error) {
            System.err.println(
                    "Open Library edition lookup failed for ISBN "
                            + isbn + ": " + error.getMessage());
            return null;
        }
    }
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
                preferredLanguage(strings(doc.path("language"))),
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

    private static String preferredLanguage(List<String> languages) {
        return languages.stream()
                .filter(OpenLibraryCatalogService::hasText)
                .filter(language -> {
                    String normalized = language.trim().toLowerCase(
                            Locale.ROOT);
                    return "en".equals(normalized)
                            || "eng".equals(normalized)
                            || normalized.startsWith("en-");
                })
                .findFirst()
                .orElse(first(languages));
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

    private static String normalize(String value) {
        return clean(value).toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
