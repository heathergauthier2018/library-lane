package com.librarylane.services;

import com.librarylane.catalog.CatalogBookResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class BookCatalogSearchService {

    private final OpenLibraryCatalogService openLibrary;
    private final GoogleBooksCatalogService googleBooks;
    private final WikidataCatalogService wikidata;

    public BookCatalogSearchService(
            OpenLibraryCatalogService openLibrary,
            GoogleBooksCatalogService googleBooks,
            WikidataCatalogService wikidata) {
        this.openLibrary = openLibrary;
        this.googleBooks = googleBooks;
        this.wikidata = wikidata;
    }

    public List<CatalogBookResult> search(
            String query,
            String format,
            String searchBy) {
        String cleanedQuery = normalize(query);
        if (cleanedQuery.length() < 3) return List.of();

        List<CatalogBookResult> combined = new ArrayList<>();
        // A temporary failure or rate limit from either provider must not make
        // the entire Library Lane catalog unavailable.
        try {
            combined.addAll(openLibrary.search(query, format, searchBy));
        } catch (RuntimeException error) {
            System.err.println("Open Library catalog search failed: " + error.getMessage());
        }

        try {
            combined.addAll(googleBooks.search(query, format, searchBy));
        } catch (RuntimeException error) {
            System.err.println("Google Books catalog search failed: " + error.getMessage());
        }

        Map<String, CatalogBookResult> unique = new LinkedHashMap<>();
        combined.stream()
                .map(BookCatalogSearchService::cleanMetadata)
                .filter(result -> matchesRequestedField(result, cleanedQuery, searchBy))
                .sorted(Comparator
                        .comparingInt((CatalogBookResult result) ->
                                relevance(result, cleanedQuery, searchBy))
                        .reversed()
                        .thenComparing(CatalogBookResult::title, String.CASE_INSENSITIVE_ORDER))
                .forEach(result -> unique.merge(
                        identity(result),
                        result,
                        BookCatalogSearchService::merge));

        int limit = "TITLE".equals(normalizeSearchBy(searchBy)) ? 50 : 100;
        return unique.values().stream().limit(limit).toList();
    }

    /**
     * Resolves one result after the reader selects it. Expensive detail
     * requests belong here rather than in live typing, so search remains fast
     * and external providers are not called once for every displayed result.
     */
    public CatalogBookResult resolve(CatalogBookResult selectedResult) {
        if (selectedResult == null) {
            throw new IllegalArgumentException("A catalog result is required");
        }

        CatalogBookResult resolved = selectedResult;
        String provider = normalize(selectedResult.provider());

        // Follow the selected record's own work identity. Never merge every
        // catalog hit that merely shares a title; that was the source of the
        // duplicate authors, foreign descriptions, and false publication
        // years seen in the previous checkpoint.
        try {
            if (provider.contains("open library")) {
                resolved = openLibrary.enrichWithWorkDetails(resolved);
            } else if (hasText(selectedResult.isbn13()) || hasText(selectedResult.isbn10())) {
                CatalogBookResult work = openLibrary.findWorkByIsbn(
                        hasText(selectedResult.isbn13())
                                ? selectedResult.isbn13()
                                : selectedResult.isbn10());
                if (work != null && sameWork(selectedResult, work)) {
                    resolved = mergeSelectedWithWork(resolved, work);
                }
            }
        } catch (RuntimeException error) {
            System.err.println("Open Library work resolution failed: " + error.getMessage());
        }

        try {
            CatalogBookResult current = resolved;
            CatalogBookResult supplement = googleBooks.search(
                            current.title(), current.format(), "TITLE")
                    .stream()
                    .filter(candidate -> exactSameWork(current, candidate))
                    .max(Comparator.comparingInt(BookCatalogSearchService::completeness))
                    .orElse(null);
            if (supplement != null) {
                resolved = mergeSelectedWithSupplement(resolved, supplement);
            }
        } catch (RuntimeException error) {
            System.err.println("Google Books work enrichment failed: " + error.getMessage());
        }

        try {
            resolved = wikidata.enrich(resolved);
        } catch (RuntimeException error) {
            System.err.println("Wikidata work resolution failed: " + error.getMessage());
        }

        return cleanMetadata(resolved);
    }

    private static boolean sameWork(
            CatalogBookResult selected,
            CatalogBookResult candidate) {
        if (!normalize(selected.title()).equals(normalize(candidate.title()))) return false;

        List<String> selectedAuthors = safe(selected.authors()).stream()
                .map(BookCatalogSearchService::canonicalPerson)
                .filter(value -> !value.isBlank())
                .toList();
        List<String> candidateAuthors = safe(candidate.authors()).stream()
                .map(BookCatalogSearchService::canonicalPerson)
                .filter(value -> !value.isBlank())
                .toList();

        // Some old catalog records omit authors. Exact normalized title is
        // still useful in that case, but conflicting named authors must never
        // be merged merely because two books share a title.
        if (selectedAuthors.isEmpty() || candidateAuthors.isEmpty()) return false;
        return selectedAuthors.stream().anyMatch(left ->
                candidateAuthors.stream().anyMatch(right ->
                        left.equals(right) || left.contains(right) || right.contains(left)));
    }

    private static boolean matchesRequestedField(
            CatalogBookResult result,
            String query,
            String rawSearchBy) {
        String searchBy = normalizeSearchBy(rawSearchBy);
        String target = switch (searchBy) {
            case "AUTHOR" -> normalize(String.join(" ", safe(result.authors())));
            case "SERIES" -> normalize(result.seriesName());
            default -> normalize(result.title());
        };

        // Provider-side field queries do most of the filtering. Keep a small
        // fuzzy tolerance for partial typing and catalog spelling variants.
        if ("SERIES".equals(searchBy)) {
            if (!target.isBlank() && seriesMatches(target, query)) return true;
            return descriptionContainsSeriesEvidence(result.description(), query);
        }

        if (target.isBlank()) return false;

        if ("AUTHOR".equals(searchBy)) {
            // A complete or partially typed full name must match as a phrase.
            // Do not return every author named Lisa for "Lisa McMann" merely
            // because one token overlaps.
            return target.contains(query)
                    || (!query.isBlank() && query.contains(target));
        }

        return target.contains(query)
                || (!query.isBlank() && query.contains(target))
                || tokenOverlap(target, query) >= 0.66;
    }

    private static boolean seriesMatches(String target, String query) {
        String canonicalTarget = canonicalSeries(target);
        String canonicalQuery = canonicalSeries(query);
        if (canonicalTarget.isBlank() || canonicalQuery.isBlank()) return false;

        // Supports live partial typing ("empy" -> "Empyrean") without
        // accepting titles/series such as "Above Empyrean" merely because
        // they contain the same word later in the value.
        return canonicalTarget.equals(canonicalQuery)
                || canonicalTarget.startsWith(canonicalQuery)
                || canonicalQuery.startsWith(canonicalTarget);
    }

    private static String canonicalSeries(String value) {
        return normalize(value)
                .replaceFirst("^the\\s+", "")
                .replaceFirst("\\s+(?:book\\s*)?\\d+(?:\\.\\d+)?$", "")
                .replaceFirst("\\s+series$", "")
                .trim();
    }

    private static int relevance(
            CatalogBookResult result,
            String query,
            String rawSearchBy) {
        String searchBy = normalizeSearchBy(rawSearchBy);
        String target = switch (searchBy) {
            case "AUTHOR" -> normalize(String.join(" ", safe(result.authors())));
            case "SERIES" -> normalize(result.seriesName());
            default -> normalize(result.title());
        };

        int score = 0;
        if (target.equals(query)) score += 1000;
        else if (target.startsWith(query)) score += 700;
        else if (target.contains(query)) score += 500;
        score += (int) (tokenOverlap(target, query) * 300);
        if (result.coverImageUrl() != null) score += 25;
        if (result.pageCount() != null && result.pageCount() > 0) score += 20;
        if (result.isbn13() != null || result.isbn10() != null) score += 15;
        if (result.seriesName() != null) score += 10;
        return score;
    }

    private static double tokenOverlap(String target, String query) {
        if (target.isBlank() || query.isBlank()) return 0;
        String[] queryTokens = query.split(" ");
        int matches = 0;
        for (String token : queryTokens) {
            if (token.length() >= 2 && target.contains(token)) matches++;
        }
        return (double) matches / queryTokens.length;
    }

    private static String identity(CatalogBookResult result) {
        // Phase 1 presents works, not every individual edition. This also lets
        // Open Library's series/page data enrich Google Books' description.
        return normalize(result.title()) + "|"
                + canonicalPrimaryAuthor(result.authors()) + "|"
                + normalize(result.format());
    }

    private static CatalogBookResult merge(
            CatalogBookResult first,
            CatalogBookResult second) {
        return new CatalogBookResult(
                combinedProvider(first.provider(), second.provider()),
                prefer(first.providerId(), second.providerId()),
                prefer(first.title(), second.title()),
                prefer(first.subtitle(), second.subtitle()),
                preferredAuthors(first.authors(), second.authors()),
                union(first.genres(), second.genres(), 12),
                preferredDescription(first, second),
                prefer(first.publisher(), second.publisher()),
                preferredSearchPublication(first, second),
                preferPositive(first.pageCount(), second.pageCount()),
                preferPositive(first.audiobookLengthSeconds(), second.audiobookLengthSeconds()),
                union(first.narrators(), second.narrators(), 8),
                prefer(first.coverImageUrl(), second.coverImageUrl()),
                prefer(first.language(), second.language()),
                prefer(first.isbn10(), second.isbn10()),
                prefer(first.isbn13(), second.isbn13()),
                prefer(first.seriesName(), second.seriesName()),
                first.seriesNumber() != null ? first.seriesNumber() : second.seriesNumber(),
                prefer(first.editionFormat(), second.editionFormat()),
                prefer(first.format(), second.format())
        );
    }

    private static CatalogBookResult cleanMetadata(CatalogBookResult result) {
        String seriesName = displaySeriesName(result.seriesName());
        Double seriesNumber = result.seriesNumber();

        return new CatalogBookResult(
                result.provider(),
                result.providerId(),
                result.title(),
                result.subtitle(),
                safe(result.authors()),
                cleanGenres(result.genres(), result.description()),
                cleanDescription(result.description()),
                result.publisher(),
                result.publicationDate(),
                result.pageCount(),
                result.audiobookLengthSeconds(),
                safe(result.narrators()),
                result.coverImageUrl(),
                result.language(),
                result.isbn10(),
                result.isbn13(),
                seriesName,
                seriesNumber,
                result.editionFormat(),
                result.format()
        );
    }

    private static List<String> cleanGenres(
            List<String> values,
            String description) {
        List<String> cleaned = new ArrayList<>();
        for (String rawValue : safe(values)) {
            if (rawValue == null) continue;
            String value = rawValue.replace('_', ' ').trim();
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

            addGenre(cleaned, normalized, "fantasy", "Fantasy");
            addGenre(cleaned, normalized, "romance", "Romance");
            addGenre(cleaned, normalized, "dragon", "Dragons & Mythical Creatures");
            addGenre(cleaned, normalized, "action", "Action & Adventure");
            addGenre(cleaned, normalized, "adventure", "Action & Adventure");
            addGenre(cleaned, normalized, "science fiction", "Science Fiction");
            addGenre(cleaned, normalized, "mystery", "Mystery");
            addGenre(cleaned, normalized, "thriller", "Thriller");
            addGenre(cleaned, normalized, "horror", "Horror");
            addGenre(cleaned, normalized, "historical", "Historical Fiction");
            addGenre(cleaned, normalized, "young adult", "Young Adult");
            addGenre(cleaned, normalized, "juvenile", "Young Adult");
            addGenre(cleaned, normalized, "paranormal", "Paranormal");
            addGenre(cleaned, normalized, "supernatural", "Paranormal");
            addGenre(cleaned, normalized, "psychological", "Psychological Fiction");
            addGenre(cleaned, normalized, "realistic", "Realistic Fiction");
            addGenre(cleaned, normalized, "contemporary", "Contemporary");
            addGenre(cleaned, normalized, "coming of age", "Coming of Age");

            if (cleaned.size() >= 6) break;
        }

        String synopsis = normalize(description);
        addGenreFromAny(cleaned, synopsis, "Paranormal",
                "paranormal", "supernatural", "vampire", "vampyre",
                "psychic", "clairvoyant", "prophetic dream");
        if (cleaned.size() < 6 && !cleaned.contains("Paranormal")
                && synopsis.contains("dream")
                && (synopsis.contains("ability")
                || synopsis.contains("other people s dreams")
                || synopsis.contains("people s dreams"))) {
            cleaned.add("Paranormal");
        }
        addGenreFromAny(cleaned, synopsis, "Psychological Fiction",
                "psychological", "post traumatic", "ptsd", "trauma");
        addGenreFromAny(cleaned, synopsis, "Mystery",
                "mystery", "detective", "investigate", "investigation");
        addGenreFromAny(cleaned, synopsis, "Thriller",
                "thriller", "stalker", "stalking", "suspense");
        addGenreFromAny(cleaned, synopsis, "Coming of Age",
                "coming of age", "growing up");

        return cleaned;
    }

    private static void addGenreFromAny(
            List<String> genres,
            String source,
            String display,
            String... markers) {
        if (source.isBlank() || genres.contains(display) || genres.size() >= 6) return;
        for (String marker : markers) {
            if (source.contains(marker)) {
                genres.add(display);
                return;
            }
        }
    }

    private static void addGenre(
            List<String> genres,
            String source,
            String marker,
            String display) {
        if (source.contains(marker) && !genres.contains(display)) genres.add(display);
    }

    private static String displaySeriesName(String value) {
        if (value == null || value.isBlank()) return null;
        String cleaned = value
                .replace('_', ' ')
                .replaceFirst("(?i)^\\s*serie?s?\\s*[:=-]\\s*", "")
                .replaceFirst("(?i)\\s*(?:,|-|:)?\\s*(?:book\\s*)?[#:]?\\s*\\d+(?:\\.\\d+)?\\s*$", "")
                .trim();
        if (cleaned.isBlank()) return null;

        String normalized = normalize(cleaned);
        if (normalized.startsWith("bestselling ")
                || normalized.startsWith("best selling ")
                || normalized.startsWith("new york times ")
                || normalized.startsWith("award winning ")) {
            return null;
        }

        String[] words = cleaned.toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder display = new StringBuilder();
        for (int index = 0; index < words.length; index++) {
            String word = words[index];
            boolean smallWord = index > 0 && (word.equals("the") || word.equals("of")
                    || word.equals("and") || word.equals("in") || word.equals("a")
                    || word.equals("an"));
            if (display.length() > 0) display.append(' ');
            display.append(smallWord ? word
                    : Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return display.toString();
    }

    private static CatalogBookResult mergeSelectedWithWork(
            CatalogBookResult selected,
            CatalogBookResult work) {
        return new CatalogBookResult(
                combinedProvider(selected.provider(), work.provider()),
                selected.providerId(),
                selected.title(),
                prefer(selected.subtitle(), work.subtitle()),
                preferredAuthors(selected.authors(), work.authors()),
                union(selected.genres(), work.genres(), 12),
                preferredDescription(selected, work),
                selected.publisher(),
                prefer(work.publicationDate(), selected.publicationDate()),
                selected.pageCount(),
                selected.audiobookLengthSeconds(),
                safe(selected.narrators()),
                prefer(selected.coverImageUrl(), work.coverImageUrl()),
                prefer(selected.language(), work.language()),
                selected.isbn10(),
                selected.isbn13(),
                prefer(work.seriesName(), selected.seriesName()),
                work.seriesNumber() != null ? work.seriesNumber() : selected.seriesNumber(),
                selected.editionFormat(),
                selected.format()
        );
    }

    private static CatalogBookResult mergeSelectedWithSupplement(
            CatalogBookResult selected,
            CatalogBookResult supplement) {
        return new CatalogBookResult(
                combinedProvider(selected.provider(), supplement.provider()),
                selected.providerId(),
                selected.title(),
                prefer(selected.subtitle(), supplement.subtitle()),
                enrichedAuthors(selected.authors(), supplement.authors()),
                union(selected.genres(), supplement.genres(), 12),
                preferredDescription(selected, supplement),
                selected.publisher(),
                selected.publicationDate(),
                selected.pageCount(),
                selected.audiobookLengthSeconds(),
                safe(selected.narrators()),
                prefer(selected.coverImageUrl(), supplement.coverImageUrl()),
                prefer(selected.language(), supplement.language()),
                selected.isbn10(),
                selected.isbn13(),
                prefer(selected.seriesName(), supplement.seriesName()),
                selected.seriesNumber() != null
                        ? selected.seriesNumber() : supplement.seriesNumber(),
                selected.editionFormat(),
                selected.format()
        );
    }

    private static List<String> enrichedAuthors(
            List<String> selected,
            List<String> supplement) {
        List<String> left = deduplicatePeople(selected);
        List<String> right = deduplicatePeople(supplement);
        if (left.isEmpty()) return right;
        if (right.isEmpty()) return left;
        if (right.size() <= 4 && peopleContained(left, right)) return right;
        return left;
    }

    private static boolean exactSameWork(
            CatalogBookResult selected,
            CatalogBookResult candidate) {
        return normalize(selected.title()).equals(normalize(candidate.title()))
                && sameWork(selected, candidate);
    }

    private static int completeness(CatalogBookResult result) {
        int score = safe(result.authors()).size() * 10
                + safe(result.genres()).size() * 4;
        if (hasText(result.description())) score += 30;
        if (hasText(result.seriesName())) score += 20;
        if (result.seriesNumber() != null) score += 20;
        if (isEnglish(result.language())) score += 10;
        return score;
    }

    private static List<String> preferredAuthors(
            List<String> first,
            List<String> second) {
        List<String> left = deduplicatePeople(first);
        List<String> right = deduplicatePeople(second);
        if (left.isEmpty()) return right;
        if (right.isEmpty()) return left;

        // When one source contains the same credited authors plus extra noisy
        // names (translators, malformed aliases, duplicate catalog credits),
        // prefer the smaller consensus list.
        if (right.size() < left.size() && peopleContained(right, left)) return right;
        if (left.size() < right.size() && peopleContained(left, right)) return left;
        return left;
    }

    private static boolean peopleContained(List<String> smaller, List<String> larger) {
        Set<String> largeKeys = larger.stream()
                .map(BookCatalogSearchService::canonicalPerson)
                .collect(java.util.stream.Collectors.toSet());
        return smaller.stream()
                .map(BookCatalogSearchService::canonicalPerson)
                .allMatch(largeKeys::contains);
    }

    private static List<String> deduplicatePeople(List<String> values) {
        Map<String, String> people = new LinkedHashMap<>();
        for (String value : safe(values)) {
            if (!hasText(value)) continue;
            people.putIfAbsent(canonicalPerson(value), value.trim());
        }
        return people.values().stream().limit(4).toList();
    }

    private static String canonicalPrimaryAuthor(List<String> authors) {
        return safe(authors).stream()
                .filter(BookCatalogSearchService::hasText)
                .map(BookCatalogSearchService::canonicalPerson)
                .sorted()
                .findFirst()
                .orElse("");
    }

    private static String canonicalPerson(String value) {
        return java.util.Arrays.stream(normalize(value).split(" "))
                .filter(token -> !token.isBlank())
                .sorted()
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private static String preferredDescription(
            CatalogBookResult first,
            CatalogBookResult second) {
        boolean firstEnglish = isEnglish(first.language());
        boolean secondEnglish = isEnglish(second.language());
        if (hasText(first.description()) && (firstEnglish || !secondEnglish)) {
            return first.description();
        }
        if (hasText(second.description())) return second.description();
        return first.description();
    }

    private static boolean isEnglish(String language) {
        String normalized = normalize(language);
        return normalized.equals("en") || normalized.equals("eng")
                || normalized.equals("english");
    }

    private static String preferredSearchPublication(
            CatalogBookResult first,
            CatalogBookResult second) {
        // Open Library search records carry work-level first_publish_year.
        // Google dates are commonly edition dates. Prefer work-level evidence
        // rather than blindly choosing the smallest number in any record.
        boolean firstWork = normalize(first.provider()).contains("open library");
        boolean secondWork = normalize(second.provider()).contains("open library");
        if (firstWork && hasText(first.publicationDate())) return first.publicationDate();
        if (secondWork && hasText(second.publicationDate())) return second.publicationDate();
        return prefer(first.publicationDate(), second.publicationDate());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String combinedProvider(String first, String second) {
        if (first == null) return second;
        if (second == null || first.contains(second)) return first;
        return first + "+" + second;
    }

    private static String prefer(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static String longer(String first, String second) {
        if (first == null) return second;
        if (second == null) return first;
        return second.length() > first.length() ? second : first;
    }

    private static String cleanDescription(String value) {
        if (value == null || value.isBlank()) return null;
        String cleaned = value.trim();
        String normalized = normalize(cleaned);

        boolean hasPageMeasurement = normalized.matches(".*\\b\\d+ pages?\\b.*");
        boolean hasCentimeters = normalized.matches(".*\\b\\d+ cm\\b.*");
        boolean hasLexile = normalized.matches(".*\\b\\d+l lexile\\b.*")
                || normalized.contains("lexile");

        if (cleaned.length() < 320
                && hasPageMeasurement
                && (hasCentimeters || hasLexile)) {
            return null;
        }

        if (normalized.equals("no description available")
                || normalized.equals("description not available")) {
            return null;
        }

        return cleaned;
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
        var matcher = java.util.regex.Pattern.compile("\\b(1[0-9]{3}|20[0-9]{2}|2100)\\b")
                .matcher(value);
        if (!matcher.find()) return null;
        try {
            return Integer.valueOf(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean descriptionContainsSeriesEvidence(
            String description,
            String normalizedQuery) {
        String text = normalize(description);
        if (text.isBlank() || normalizedQuery.isBlank()) return false;
        int queryPosition = text.indexOf(normalizedQuery);
        if (queryPosition < 0) return false;

        int start = Math.max(0, queryPosition - 35);
        int end = Math.min(text.length(), queryPosition + normalizedQuery.length() + 35);
        String nearby = text.substring(start, end);
        return nearby.contains("series")
                || nearby.contains("book one")
                || nearby.contains("book two")
                || nearby.contains("book three")
                || nearby.matches(".*\\bbook \\d+(?:\\.\\d+)?\\b.*");
    }

    private static Integer preferPositive(Integer first, Integer second) {
        if (first != null && first > 0) return first;
        return second != null && second > 0 ? second : null;
    }

    private static List<String> union(
            List<String> first,
            List<String> second,
            int limit) {
        List<String> merged = new ArrayList<>();
        for (String value : safe(first)) {
            if (value != null && !value.isBlank() && !merged.contains(value)) merged.add(value);
        }
        for (String value : safe(second)) {
            if (value != null && !value.isBlank() && !merged.contains(value)) merged.add(value);
        }
        return merged.stream().limit(limit).toList();
    }

    private static String normalizeSearchBy(String value) {
        String normalized = value == null ? "TITLE" : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "AUTHOR", "SERIES" -> normalized;
            default -> "TITLE";
        };
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }

}
