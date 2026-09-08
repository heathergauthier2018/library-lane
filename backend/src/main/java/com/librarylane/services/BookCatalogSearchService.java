package com.librarylane.services;

import com.librarylane.catalog.CatalogBookResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BookCatalogSearchService {

    private static final Pattern NUMERIC_SERIES_NUMBER = Pattern.compile(
            "(?i)\\b(?:book|volume|vol\\.?|part)\\s*(?:number\\s*)?#?\\s*(\\d+(?:\\.\\d+)?)\\b");
    private static final Pattern ORDINAL_SERIES_NUMBER = Pattern.compile(
            "(?i)\\b(first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth)"
                    + "(?:\\s+and\\s+final)?\\s+"
                    + "(?:book|novel|audiobook|volume|installment|entry)\\b");
    private static final Pattern NUMERIC_ORDINAL_SERIES_NUMBER = Pattern.compile(
            "(?i)\\b(\\d+)(?:st|nd|rd|th)\\s+"
                    + "(?:book|novel|audiobook|volume|installment|entry)\\b");
    private static final Pattern ORDINAL_IN_SERIES = Pattern.compile(
            "(?i)\\b(first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth)"
                    + "(?:\\s+and\\s+final)?\\s+"
                    + "(?:(?:book|novel|audiobook|volume|installment|entry)\\s+)?"
                    + "in\\s+(?:the\\s+)?[^.!?]{1,80}\\b(?:series|trilogy)\\b");
    private static final Pattern WORD_SERIES_NUMBER = Pattern.compile(
            "(?i)\\b(?:book|volume|vol\\.?|part)\\s+"
                    + "(one|two|three|four|five|six|seven|eight|nine|ten)\\b");
    private static final Pattern TRAILING_AUDIOBOOK_SERIES = Pattern.compile(
            "\\(([^()]{2,80})\\)\\s*$");
    private static final Pattern EXPLICIT_NAMED_SERIES = Pattern.compile(
            "(?i)\\b(?:first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth)"
                    + "(?:\\s+and\\s+final)?\\s+"
                    + "(?:(?:book|novel|audiobook|installment|entry)\\s+)?"
                    + "(?:in|of)\\s+(?:the\\s+)?"
                    + "(?:#\\s*1\\s+)?"
                    + "([\\p{L}\\p{N}][\\p{L}\\p{N}'’&: -]{1,70}?)\\s+"
                    + "(?:series|trilogy)\\b");
    private static final Pattern NUMBERED_TITLE_SERIES = Pattern.compile(
            "^\\s*([\\p{L}][\\p{L}\\p{N}'’& -]{1,70}?)\\s+"
                    + "(?:book\\s*)?#?(\\d+(?:\\.\\d+)?)\\s*[:\\-–—]\\s*"
                    + "[\\p{L}\\p{N}].+$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NAMED_COLLECTION_IN_DESCRIPTION = Pattern.compile(
            "(?i)\\b(?:first|second|third|fourth|fifth|sixth|seventh|eighth|"
                    + "ninth|tenth|final)\\s+"
                    + "(?:(?:book|novel|volume|installment|entry)\\s+)?"
                    + "(?:in|of)\\s+(?:the\\s+)?"
                    + "([\\p{L}\\p{N}][\\p{L}\\p{N}'’&: -]{1,70}?)\\s+"
                    + "(series|trilogy|saga|chronicles)\\b");
    private static final Pattern CAPITALIZED_COLLECTION_IN_DESCRIPTION =
            Pattern.compile(
                    "\\b((?:[A-Z][\\p{L}\\p{N}'’&.-]*)(?:\\s+"
                            + "(?:[A-Z][\\p{L}\\p{N}'’&.-]*|of|the|and|in)){0,7})"
                            + "\\s+(series|trilogy|saga|chronicles)\\b");

    private final OpenLibraryCatalogService openLibrary;
    private final GoogleBooksCatalogService googleBooks;
    private final WikidataCatalogService wikidata;
    private final AppleAudiobookCatalogService appleAudiobooks;
    private final SpotifyAudiobookMetadataService spotifyAudiobooks;

    @Autowired
    public BookCatalogSearchService(
            OpenLibraryCatalogService openLibrary,
            GoogleBooksCatalogService googleBooks,
            WikidataCatalogService wikidata,
            AppleAudiobookCatalogService appleAudiobooks,
            SpotifyAudiobookMetadataService spotifyAudiobooks) {
        this.openLibrary = openLibrary;
        this.googleBooks = googleBooks;
        this.wikidata = wikidata;
        this.appleAudiobooks = appleAudiobooks;
        this.spotifyAudiobooks = spotifyAudiobooks;
    }

    // Keeps the existing focused unit tests source-compatible. Spring uses
    // the annotated constructor above in the running application.
    BookCatalogSearchService(
            OpenLibraryCatalogService openLibrary,
            GoogleBooksCatalogService googleBooks,
            WikidataCatalogService wikidata,
            AppleAudiobookCatalogService appleAudiobooks) {
        this(openLibrary, googleBooks, wikidata, appleAudiobooks, null);
    }

    public List<CatalogBookResult> search(
            String query,
            String format,
            String searchBy) {
        String cleanedQuery = normalize(query);
        if (cleanedQuery.length() < 3) return List.of();

        // An explicit audiobook-only request remains isolated from the print
        // providers. The normal Add Book search omits format so that all
        // available editions can be presented together.
        if (isAudiobookFormat(format)) {
            return searchAudiobooks(query, format, searchBy, cleanedQuery);
        }

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

        // Google's unfiltered books response is capped and may contain only
        // print records even when a matching e-book exists. During normal
        // all-format discovery, make one additional e-book-filtered request
        // and merge it with the same result set. Explicit format searches keep
        // their existing single-provider-request behavior.
        if (!hasText(format)) {
            try {
                combined.addAll(googleBooks.search(
                        query,
                        "EBOOK",
                        searchBy));
            } catch (RuntimeException error) {
                System.err.println(
                        "Google Books e-book discovery failed: "
                                + error.getMessage());
            }
        }

        // A blank format means "discover every edition", which is the normal
        // Add Book flow. Ask Apple specifically for audiobooks, then merge the
        // returned records without allowing an Apple failure to disturb the
        // existing Open Library + Google Books results.
        if (!hasText(format)) {
            try {
                combined.addAll(appleAudiobooks.search(
                        query,
                        "AUDIOBOOK",
                        searchBy));
            } catch (RuntimeException error) {
                System.err.println("Apple audiobook catalog search failed: " + error.getMessage());
            }
            if (spotifyAudiobooks != null) {
                try {
                    combined.addAll(spotifyAudiobooks.searchLegacyAlbums(
                            query,
                            searchBy));
                } catch (RuntimeException error) {
                    System.err.println(
                            "Spotify legacy audiobook search failed: "
                                    + error.getMessage());
                }
            }
        }

        // Partial reader queries can still reveal the exact work title through
        // a strong print result ("not your perfect..." -> "I Am Not Your
        // Perfect..."). Retry only missing principal formats with that exact
        // discovered title before ranking, so the lead group has the best
        // chance to contain one trustworthy edition of each format.
        if (!hasText(format)
                && "TITLE".equals(normalizeSearchBy(searchBy))) {
            CatalogBookResult discoveredWork = combined.stream()
                    .filter(result -> !isAudiobookFormat(result.format()))
                    .filter(result -> !looksLikeAncillaryRecord(result))
                    .filter(result -> queryFamilyTitleMatch(
                            result,
                            cleanedQuery)
                            || strongWorkTitleMatch(result, cleanedQuery))
                    .max(Comparator.comparingInt(result ->
                            relevance(result, cleanedQuery, searchBy)))
                    .orElse(null);
            if (discoveredWork != null
                    && !canonicalSearchTitle(discoveredWork.title())
                    .equals(canonicalSearchTitle(cleanedQuery))) {
                String exactTitle = discoveredWork.title();
                if (!hasStrongFormatMatch(
                        combined,
                        cleanedQuery,
                        "EBOOK")) {
                    try {
                        combined.addAll(googleBooks.search(
                                exactTitle,
                                "EBOOK",
                                "TITLE"));
                    } catch (RuntimeException error) {
                        System.err.println(
                                "Google Books exact-title e-book discovery failed: "
                                        + error.getMessage());
                    }
                }
                if (!hasStrongFormatMatch(
                        combined,
                        cleanedQuery,
                        "AUDIOBOOK")) {
                    try {
                        combined.addAll(appleAudiobooks.search(
                                exactTitle,
                                "AUDIOBOOK",
                                "TITLE"));
                    } catch (RuntimeException error) {
                        System.err.println(
                                "Apple exact-title audiobook discovery failed: "
                                        + error.getMessage());
                    }
                }
            }
        }

        Map<String, CatalogBookResult> unique = new LinkedHashMap<>();
        combined.stream()
                .map(BookCatalogSearchService::cleanMetadata)
                .filter(result -> matchesRequestedField(result, cleanedQuery, searchBy))
                .filter(result -> shouldShowSingleWorkResult(
                        result,
                        cleanedQuery,
                        searchBy))
                .sorted(Comparator
                        .comparingInt((CatalogBookResult result) ->
                                relevance(result, cleanedQuery, searchBy))
                        .reversed()
                        .thenComparing(CatalogBookResult::title, String.CASE_INSENSITIVE_ORDER))
                .forEach(result -> unique.merge(
                        searchIdentity(result),
                        result,
                        BookCatalogSearchService::merge));

        int limit = "TITLE".equals(normalizeSearchBy(searchBy)) ? 50 : 100;
        List<CatalogBookResult> ranked =
                unique.values().stream().toList();
        if (!hasText(format)
                && "TITLE".equals(normalizeSearchBy(searchBy))) {
            // Balance formats before applying the display limit. Popular and
            // classic works can have more than 50 highly ranked print/e-book
            // editions; truncating first silently discarded an otherwise
            // trustworthy audiobook before it could be promoted.
            return prioritizeMixedFormatLeads(ranked, cleanedQuery)
                    .stream()
                    .limit(limit)
                    .toList();
        }
        return ranked.stream().limit(limit).toList();
    }

    private List<CatalogBookResult> searchAudiobooks(
            String query,
            String format,
            String searchBy,
            String cleanedQuery) {
        List<CatalogBookResult> results = new ArrayList<>();

        try {
            results.addAll(appleAudiobooks.search(query, format, searchBy));
        } catch (RuntimeException error) {
            System.err.println("Apple audiobook catalog search failed: " + error.getMessage());
        }

        if (spotifyAudiobooks != null) {
            try {
                results.addAll(spotifyAudiobooks.searchLegacyAlbums(
                        query,
                        searchBy));
            } catch (RuntimeException error) {
                System.err.println(
                        "Spotify legacy audiobook search failed: "
                                + error.getMessage());
            }
        }

        Map<String, CatalogBookResult> unique = new LinkedHashMap<>();
        results.stream()
                .map(BookCatalogSearchService::cleanMetadata)
                .filter(result -> matchesRequestedField(result, cleanedQuery, searchBy))
                .filter(result -> shouldShowSingleWorkResult(
                        result,
                        cleanedQuery,
                        searchBy))
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

    private static boolean isAudiobookFormat(String format) {
        return "AUDIOBOOK".equalsIgnoreCase(
                format == null ? "" : format.trim());
    }

    /**
     * In an unfiltered title search, readers should not need to scroll through
     * dozens of print editions before discovering that another format exists.
     * Promote at most one strong work-title match for each principal format,
     * then preserve the existing relevance order for every remaining result.
     */
    private static List<CatalogBookResult> prioritizeMixedFormatLeads(
            List<CatalogBookResult> ranked,
            String cleanedQuery) {
        List<CatalogBookResult> prioritized = new ArrayList<>();
        Set<CatalogBookResult> promoted =
                java.util.Collections.newSetFromMap(
                        new java.util.IdentityHashMap<>());
        Set<String> detectedSeriesAuthors = ranked.stream()
                .filter(result -> seriesMatchesQuery(result, cleanedQuery))
                .flatMap(result -> safe(result.authors()).stream())
                .map(BookCatalogSearchService::canonicalPerson)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(
                        java.util.LinkedHashSet::new));

        CatalogBookResult canonicalWork = ranked.stream()
                .filter(result -> !isAudiobookFormat(result.format()))
                .filter(result -> !looksLikeAncillaryRecord(result))
                .filter(result -> strongWorkTitleMatch(
                        result,
                        cleanedQuery))
                .sorted(leadPreference(cleanedQuery))
                .findFirst()
                .orElseGet(() -> ranked.stream()
                        .filter(result ->
                                !isAudiobookFormat(result.format()))
                        .filter(result ->
                                !looksLikeAncillaryRecord(result))
                        .filter(result -> queryFamilyTitleMatch(
                                result,
                                cleanedQuery))
                        .sorted(leadPreference(cleanedQuery))
                        .findFirst()
                        .orElse(null));

        Set<String> canonicalWorkAuthors = canonicalWork == null
                ? Set.of()
                : safe(canonicalWork.authors()).stream()
                  .map(BookCatalogSearchService::canonicalPerson)
                  .filter(value -> !value.isBlank())
                  .collect(java.util.stream.Collectors.toCollection(
                          java.util.LinkedHashSet::new));

        long exactWorkFormatCount = ranked.stream()
                .filter(result -> belongsToAuthorFamily(
                        result,
                        canonicalWorkAuthors))
                .filter(result -> strongWorkTitleMatch(
                        result,
                        cleanedQuery))
                .map(result -> safeTitle(result.format()).toUpperCase(
                        Locale.ROOT))
                .filter(value -> Set.of(
                        "PHYSICAL",
                        "EBOOK",
                        "AUDIOBOOK").contains(value))
                .distinct()
                .count();

        Set<String> canonicalSeriesAuthors =
                exactWorkFormatCount >= 2
                        ? Set.of()
                        : detectedSeriesAuthors;

        for (String format : List.of("PHYSICAL", "EBOOK", "AUDIOBOOK")) {
            CatalogBookResult lead = canonicalSeriesAuthors.isEmpty()
                    ? null
                    : ranked.stream()
                      .filter(result -> format.equalsIgnoreCase(
                              safeTitle(result.format())))
                      .filter(result -> belongsToAuthorFamily(
                              result,
                              canonicalSeriesAuthors))
                      .filter(result -> queryFamilyTitleMatch(
                              result,
                              cleanedQuery))
                      .sorted(leadPreference(cleanedQuery))
                      .findFirst()
                      .orElse(null);

            if (lead == null && canonicalSeriesAuthors.isEmpty()) {
                lead = ranked.stream()
                        .filter(result -> format.equalsIgnoreCase(
                                safeTitle(result.format())))
                        .filter(result -> belongsToAuthorFamily(
                                result,
                                canonicalWorkAuthors))
                        .filter(result -> queryFamilyTitleMatch(
                                result,
                                cleanedQuery)
                                || strongWorkTitleMatch(
                                result,
                                cleanedQuery))
                        .sorted(leadPreference(cleanedQuery))
                        .findFirst()
                        .orElse(null);
            }
            if (lead == null && canonicalSeriesAuthors.isEmpty()) {
                lead = ranked.stream()
                        .filter(result -> format.equalsIgnoreCase(
                                safeTitle(result.format())))
                        .filter(result -> queryFamilyTitleMatch(
                                result,
                                cleanedQuery))
                        .sorted(leadPreference(cleanedQuery))
                        .findFirst()
                        .orElse(null);
            }
            if (lead != null) {
                prioritized.add(lead);
                promoted.add(lead);
            }
        }

        if (!canonicalSeriesAuthors.isEmpty()) {
            List<CatalogBookResult> seriesFamily = ranked.stream()
                    .filter(result -> !promoted.contains(result))
                    .filter(result -> belongsToAuthorFamily(
                            result,
                            canonicalSeriesAuthors))
                    .filter(result -> queryFamilyTitleMatch(
                            result,
                            cleanedQuery))
                    .sorted(Comparator
                            .comparing(
                                    CatalogBookResult::seriesNumber,
                                    Comparator.nullsLast(
                                            Double::compareTo))
                            .thenComparing(
                                    CatalogBookResult::title,
                                    String.CASE_INSENSITIVE_ORDER))
                    .toList();
            for (CatalogBookResult result : seriesFamily) {
                if (!promoted.contains(result)
                        && belongsToAuthorFamily(
                                result,
                                canonicalSeriesAuthors)) {
                    prioritized.add(result);
                    promoted.add(result);
                }
            }
        }

        for (CatalogBookResult result : ranked) {
            if (!promoted.contains(result)) prioritized.add(result);
        }
        return prioritized;
    }

    private static Comparator<CatalogBookResult> leadPreference(
            String cleanedQuery) {
        return Comparator
                .comparingInt((CatalogBookResult result) ->
                        leadPreferenceScore(result, cleanedQuery))
                .reversed();
    }

    private static int leadPreferenceScore(
            CatalogBookResult result,
            String cleanedQuery) {
        int score = strongWorkTitleMatch(result, cleanedQuery) ? 100 : 0;

        if (isEnglish(result.language())) {
            score += 40;
        } else if (!hasText(result.language())) {
            score += 20;
        }

        return score;
    }
    private static boolean hasStrongFormatMatch(
            Iterable<CatalogBookResult> results,
            String cleanedQuery,
            String format) {
        for (CatalogBookResult result : results) {
            if (format.equalsIgnoreCase(safeTitle(result.format()))
                    && queryFamilyTitleMatch(result, cleanedQuery)) {
                return true;
            }
        }
        return false;
    }

    private static boolean seriesMatchesQuery(
            CatalogBookResult result,
            String cleanedQuery) {
        String series = canonicalSeries(result.seriesName());
        String query = canonicalSeries(cleanedQuery);
        return !series.isBlank() && series.equals(query);
    }

    private static boolean belongsToAuthorFamily(
            CatalogBookResult result,
            Set<String> canonicalAuthors) {
        return safe(result.authors()).stream()
                .map(BookCatalogSearchService::canonicalPerson)
                .anyMatch(canonicalAuthors::contains);
    }

    private static boolean queryFamilyTitleMatch(
            CatalogBookResult result,
            String cleanedQuery) {
        if (looksLikeAncillaryRecord(result)) return false;
        String queryTitle = canonicalSearchTitle(cleanedQuery);
        String resultTitle = canonicalSearchTitle(result.title());
        return resultTitle.equals(queryTitle)
                || resultTitle.startsWith(queryTitle + " ")
                || seriesMatchesQuery(result, cleanedQuery);
    }

    private static boolean strongWorkTitleMatch(
            CatalogBookResult result,
            String cleanedQuery) {
        if (looksLikeAncillaryRecord(result)) return false;

        String queryTitle = canonicalSearchTitle(cleanedQuery);
        String resultTitle = canonicalSearchTitle(result.title());
        if (resultTitle.equals(queryTitle)) return true;
        if (queryTitle.split(" ").length >= 4
                && resultTitle.endsWith(" " + queryTitle)) {
            return true;
        }

        // Audiobook storefronts frequently append the series in parentheses,
        // which should not push the actual recording beneath loosely related
        // print editions.
        if (isAudiobookFormat(result.format())) {
            String withoutTrailingParenthetical = canonicalSearchTitle(
                    safeTitle(result.title())
                            .replaceFirst(
                                    "\\s*\\([^)]{2,80}\\)\\s*$",
                                    ""));
            return withoutTrailingParenthetical.equals(queryTitle);
        }
        return false;
    }

    private static boolean isAppleAudiobook(CatalogBookResult result) {
        return result != null
                && isAudiobookFormat(result.format())
                && normalize(result.provider()).contains("apple audiobook");
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

        // Keep an Apple audiobook as an audiobook. Running it through the
        // ordinary merge pipeline could replace edition-specific duration and
        // format. Use a protected enrichment path that borrows only work-level
        // metadata while preserving the selected audio edition.
        if (isAppleAudiobook(selectedResult)) {
            CatalogBookResult resolved = enrichAudiobookWorkFacts(
                    resolveAppleAudiobook(selectedResult));
            if (spotifyAudiobooks == null || !spotifyAudiobooks.isConfigured()) {
                if (spotifyAudiobooks != null) {
                    System.out.println(
                            "[Spotify audiobooks] selection enrichment skipped: "
                                    + "credentials are not configured");
                }
                return resolved;
            }
            try {
                return cleanMetadata(spotifyAudiobooks.enrich(resolved));
            } catch (RuntimeException error) {
                System.err.println(
                        "Spotify audiobook enrichment failed: "
                                + error.getMessage());
                return resolved;
            }
        }

        if (SpotifyAudiobookMetadataService.isLegacyAlbumResult(selectedResult)) {
            CatalogBookResult audio = selectedResult;
            try {
                audio = spotifyAudiobooks == null
                        ? selectedResult
                        : spotifyAudiobooks.enrichLegacyAlbum(selectedResult);
            } catch (RuntimeException error) {
                System.err.println(
                        "Spotify legacy audiobook enrichment failed: "
                                + error.getMessage());
            }
            return cleanMetadata(enrichAudiobookWorkFacts(
                    resolveAppleAudiobook(audio)));
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
                    .filter(candidate -> !looksLikeAncillaryRecord(candidate))
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
            CatalogBookResult wikidataResult = wikidata.enrich(resolved);
            if (wikidataResult != null) {
                resolved = wikidataResult;
            }
        } catch (RuntimeException error) {
            System.err.println("Wikidata work resolution failed: " + error.getMessage());
        }

        return cleanMetadata(resolved);
    }

    private CatalogBookResult enrichAudiobookWorkFacts(
            CatalogBookResult audiobook) {
        try {
            CatalogBookResult enriched = wikidata.enrich(audiobook);
            if (enriched == null
                    || !sameAudiobookWork(
                    audiobook,
                    enriched,
                    audiobook.title())) {
                return audiobook;
            }

            String publicationDate = trustworthyEarlierWorkDate(
                    audiobook.publicationDate(),
                    enriched.publicationDate());
            String seriesName = prefer(
                    displaySeriesName(audiobook.seriesName()),
                    displaySeriesName(enriched.seriesName()));
            Double seriesNumber = audiobook.seriesNumber() != null
                    ? audiobook.seriesNumber() : enriched.seriesNumber();

            return cleanMetadata(new CatalogBookResult(
                    combinedProvider(audiobook.provider(), enriched.provider()),
                    audiobook.providerId(),
                    audiobook.title(),
                    prefer(audiobook.subtitle(), enriched.subtitle()),
                    preferredAuthors(audiobook.authors(), enriched.authors()),
                    union(audiobook.genres(), enriched.genres(), 12),
                    prefer(audiobook.description(), enriched.description()),
                    audiobook.publisher(),
                    publicationDate,
                    null,
                    audiobook.audiobookLengthSeconds(),
                    safe(audiobook.narrators()),
                    prefer(audiobook.coverImageUrl(), enriched.coverImageUrl()),
                    prefer(audiobook.language(), enriched.language()),
                    audiobook.isbn10(),
                    audiobook.isbn13(),
                    seriesName,
                    seriesNumber,
                    audiobook.editionFormat(),
                    "AUDIOBOOK"
            ));
        } catch (RuntimeException error) {
            System.err.println(
                    "Wikidata audiobook work enrichment failed: "
                            + error.getMessage());
            return audiobook;
        }
    }

    private CatalogBookResult resolveAppleAudiobook(
            CatalogBookResult selectedResult) {
        CatalogBookResult selected = cleanMetadata(selectedResult);
        String selectedSeriesName = preferredAudiobookSeries(selected, null);
        String baseTitle = audiobookBaseTitle(
                selected.title(),
                selectedSeriesName);

        List<CatalogBookResult> supplements = new ArrayList<>();
        try {
            supplements.addAll(openLibrary.search(baseTitle, null, "TITLE"));
        } catch (RuntimeException error) {
            System.err.println("Open Library audiobook enrichment failed: " + error.getMessage());
        }

        try {
            supplements.addAll(googleBooks.search(baseTitle, null, "TITLE"));
        } catch (RuntimeException error) {
            System.err.println("Google Books audiobook enrichment failed: " + error.getMessage());
        }

        List<CatalogBookResult> matchedSupplements = supplements.stream()
                .map(BookCatalogSearchService::cleanMetadata)
                .filter(candidate -> !looksLikeAncillaryRecord(candidate))
                .filter(candidate -> sameAudiobookWork(selected, candidate, baseTitle))
                // The Apple request is for the U.S. storefront. A record that
                // explicitly identifies another language is not safe evidence
                // for its publisher, publication date, synopsis, or series.
                .filter(BookCatalogSearchService::acceptableAudiobookSupplement)
                .toList();

        Map<String, CatalogBookResult> enrichedWorks = new LinkedHashMap<>();
        matchedSupplements.forEach(candidate -> enrichedWorks.merge(
                identity(candidate),
                candidate,
                BookCatalogSearchService::merge));

        CatalogBookResult supplement = enrichedWorks.values().stream()
                .max(Comparator.comparingInt(BookCatalogSearchService::completeness))
                .orElse(null);

        Double discoveredSeriesNumber = discoverAudiobookSeriesNumber(
                selected,
                enrichedWorks.values());
        String discoveredSeriesName = discoverAudiobookSeriesName(
                selected,
                enrichedWorks.values());
        if (discoveredSeriesNumber == null
                && hasText(discoveredSeriesName)) {
            discoveredSeriesNumber = inferSeriesPositionFromCatalog(
                    selected,
                    discoveredSeriesName);
        }

        String originalPublicationDate = trustedOriginalPublicationDate(
                selected,
                matchedSupplements);

        if (supplement == null) {
            return withAudiobookBaseTitle(
                    selected,
                    baseTitle,
                    discoveredSeriesName,
                    discoveredSeriesNumber);
        }
        return mergeAudiobookWithWork(
                selected,
                supplement,
                baseTitle,
                discoveredSeriesName,
                discoveredSeriesNumber,
                originalPublicationDate);
    }

    private static boolean sameAudiobookWork(
            CatalogBookResult audiobook,
            CatalogBookResult candidate,
            String baseTitle) {
        if (!normalize(baseTitle).equals(normalize(candidate.title()))) return false;

        List<String> audioAuthors = safe(audiobook.authors()).stream()
                .filter(BookCatalogSearchService::hasText)
                .toList();
        List<String> candidateAuthors = safe(candidate.authors()).stream()
                .filter(BookCatalogSearchService::hasText)
                .toList();

        if (audioAuthors.isEmpty() || candidateAuthors.isEmpty()) return false;
        return audioAuthors.stream().anyMatch(left ->
                candidateAuthors.stream().anyMatch(right ->
                        samePerson(left, right)));
    }

    private static boolean acceptableAudiobookSupplement(
            CatalogBookResult candidate) {
        return !hasText(candidate.language()) || isEnglish(candidate.language());
    }

    private static String trustedOriginalPublicationDate(
            CatalogBookResult audiobook,
            List<CatalogBookResult> supplements) {
        Integer appleYear = publicationYear(audiobook.publicationDate());
        Map<Integer, Set<String>> yearSources = new LinkedHashMap<>();
        addPublicationYearVote(yearSources, appleYear, "apple");

        for (CatalogBookResult supplement : supplements) {
            Integer year = publicationYear(supplement.publicationDate());
            addPublicationYearVote(
                    yearSources,
                    year,
                    publicationProviderFamily(supplement.provider()));
        }

        // Two independent providers agreeing is stronger evidence than one
        // provider's anomalous date. Sets ensure multiple editions returned by
        // the same catalog still count as only one vote.
        Integer consensusYear = yearSources.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= 2)
                .max(Comparator
                        .<Map.Entry<Integer, Set<String>>>comparingInt(
                                entry -> entry.getValue().size())
                        .thenComparingInt(entry -> appleYear == null
                                ? -entry.getKey()
                                : -Math.abs(entry.getKey() - appleYear)))
                .map(Map.Entry::getKey)
                .orElse(null);
        if (consensusYear != null) return String.valueOf(consensusYear);

        // Exact-title, matching-author, English supplements have already been
        // screened above. Their earliest plausible year is work-level evidence
        // and is safer than choosing the most complete later reissue, movie
        // tie-in, or anniversary edition.
        Integer earliestWorkYear = supplements.stream()
                .map(result -> publicationYear(result.publicationDate()))
                .filter(BookCatalogSearchService::plausiblePublicationYear)
                .min(Integer::compareTo)
                .orElse(null);
        if (earliestWorkYear != null) return String.valueOf(earliestWorkYear);

        return audiobook.publicationDate();
    }

    private static void addPublicationYearVote(
            Map<Integer, Set<String>> yearSources,
            Integer year,
            String providerFamily) {
        if (year == null || !hasText(providerFamily)) return;
        yearSources.computeIfAbsent(
                year,
                ignored -> new java.util.LinkedHashSet<>()).add(providerFamily);
    }

    private static String publicationProviderFamily(String provider) {
        String normalized = normalize(provider);
        if (normalized.contains("open library")) return "open-library";
        if (normalized.contains("google books")) return "google-books";
        if (normalized.contains("wikidata")) return "wikidata";
        return normalized;
    }

    private static boolean plausiblePublicationYear(Integer year) {
        int currentYear = java.time.Year.now().getValue();
        return year != null && year >= 1450 && year <= currentYear + 1;
    }

    private static String trustworthyEarlierWorkDate(
            String selectedDate,
            String workDate) {
        Integer selectedYear = publicationYear(selectedDate);
        Integer workYear = publicationYear(workDate);
        if (!plausiblePublicationYear(workYear)) return selectedDate;
        if (!plausiblePublicationYear(selectedYear) || workYear < selectedYear) {
            return String.valueOf(workYear);
        }
        return selectedDate;
    }

    private static CatalogBookResult mergeAudiobookWithWork(
            CatalogBookResult audiobook,
            CatalogBookResult work,
            String baseTitle,
            String discoveredSeriesName,
            Double discoveredSeriesNumber,
            String originalPublicationDate) {
        return cleanMetadata(new CatalogBookResult(
                combinedProvider(audiobook.provider(), work.provider()),
                audiobook.providerId(),
                prefer(work.title(), baseTitle),
                prefer(work.subtitle(), audiobook.subtitle()),
                preferredAuthors(audiobook.authors(), work.authors()),
                union(audiobook.genres(), work.genres(), 12),
                prefer(work.description(), audiobook.description()),
                // A print or large-print publisher is not the publisher of the
                // selected audiobook edition. Leave this blank unless Apple
                // itself supplies it.
                audiobook.publisher(),
                originalPublicationDate,
                null,
                audiobook.audiobookLengthSeconds(),
                safe(audiobook.narrators()),
                prefer(work.coverImageUrl(), audiobook.coverImageUrl()),
                prefer(work.language(), audiobook.language()),
                audiobook.isbn10(),
                audiobook.isbn13(),
                discoveredSeriesName,
                discoveredSeriesNumber,
                audiobook.editionFormat(),
                "AUDIOBOOK"
        ));
    }

    private static String discoverAudiobookSeriesName(
            CatalogBookResult selected,
            Iterable<CatalogBookResult> supplements) {
        String selectedEvidence = preferredAudiobookSeries(
                selected,
                inferAudiobookSeriesName(selected));
        if (hasText(selectedEvidence)) return selectedEvidence;

        Map<String, SeriesCandidate> candidates = new LinkedHashMap<>();
        for (CatalogBookResult supplement : supplements) {
            String name = preferredAudiobookSeries(
                    selected,
                    prefer(
                            supplement.seriesName(),
                            inferAudiobookSeriesName(supplement)));
            if (!hasText(name)) continue;

            String key = canonicalSeries(name);
            if (key.isBlank()) continue;
            String provider = publicationProviderFamily(supplement.provider());
            candidates.compute(key, (ignored, existing) -> {
                if (existing == null) {
                    SeriesCandidate created = new SeriesCandidate(name);
                    created.providers().add(provider);
                    return created;
                }
                existing.providers().add(provider);
                return existing;
            });
        }

        return candidates.values().stream()
                .max(Comparator
                        .comparingInt((SeriesCandidate value) ->
                                value.providers().size())
                        .thenComparingInt(value -> value.name().length()))
                .map(SeriesCandidate::name)
                .orElse(null);
    }

    private static Double inferSeriesNumber(
            CatalogBookResult selected,
            Iterable<CatalogBookResult> supplements) {
        Double inferred = parseStrongSeriesNumber(metadataEvidence(selected));
        if (inferred != null) return inferred;

        for (CatalogBookResult supplement : supplements) {
            inferred = parseStrongSeriesNumber(metadataEvidence(supplement));
            if (inferred != null) return inferred;
        }
        return null;
    }

    private static Double discoverAudiobookSeriesNumber(
            CatalogBookResult selected,
            Iterable<CatalogBookResult> supplements) {
        Map<Double, Set<String>> sources = new LinkedHashMap<>();
        for (CatalogBookResult supplement : supplements) {
            if (supplement.seriesNumber() == null) continue;
            sources.computeIfAbsent(
                            supplement.seriesNumber(),
                            ignored -> new java.util.LinkedHashSet<>())
                    .add(publicationProviderFamily(supplement.provider()));
        }

        // Structured agreement from independent work catalogs is stronger
        // than a storefront value that may have been inferred from marketing
        // copy (for example “#1 bestseller”).
        Double consensus = sources.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= 2)
                .max(Comparator.comparingInt(entry -> entry.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse(null);
        if (consensus != null) return consensus;

        // Prefer an explicit number found in independently retrieved work
        // metadata. The selected storefront record is deliberately checked
        // later because audiobook descriptions often contain unrelated
        // promotional phrases such as “#1 bestseller.”
        for (CatalogBookResult supplement : supplements) {
            Double stronglyInferred =
                    parseStrongSeriesNumber(metadataEvidence(supplement));
            if (stronglyInferred != null) return stronglyInferred;
        }

        if (sources.size() == 1) {
            return sources.keySet().iterator().next();
        }

        // An explicit sentence in the selected audiobook metadata remains
        // useful when the storefront did not supply a structured number.
        // Examples include “the third audiobook in the … series” and “the
        // first book in the … series.” The strong parser deliberately rejects
        // bestseller rankings and other promotional uses of “#1.”
        Double supported =
                parseStrongSeriesNumber(metadataEvidence(selected));
        if (selected.seriesNumber() == null && supported != null) {
            return supported;
        }

        // Keep an existing structured storefront value only when no stronger
        // exact-work evidence contradicts it and the storefront's own explicit
        // evidence supports that same number.
        if (selected.seriesNumber() != null
                && sources.isEmpty()) {
            if (selected.seriesNumber().equals(supported)) {
                return selected.seriesNumber();
            }
        } else if (selected.seriesNumber() != null
                && sources.containsKey(selected.seriesNumber())) {
            return selected.seriesNumber();
        }
        return null;
    }

    /**
     * Derives a missing volume number from the original publication order of
     * matching-author titles in the verified series. At least two distinct
     * titles are required, so a single catalog result can never become book
     * one by default.
     */
    private Double inferSeriesPositionFromCatalog(
            CatalogBookResult selected,
            String seriesName) {
        List<CatalogBookResult> candidates = new ArrayList<>();
        try {
            candidates.addAll(openLibrary.search(
                    seriesName,
                    null,
                    "SERIES"));
        } catch (RuntimeException error) {
            System.err.println(
                    "Open Library series-position lookup failed: "
                            + error.getMessage());
        }
        try {
            candidates.addAll(googleBooks.search(
                    seriesName,
                    null,
                    "SERIES"));
        } catch (RuntimeException error) {
            System.err.println(
                    "Google Books series-position lookup failed: "
                            + error.getMessage());
        }

        String targetSeries = canonicalSeries(seriesName);
        Map<String, CatalogBookResult> distinctTitles = new LinkedHashMap<>();
        distinctTitles.put(canonicalSearchTitle(selected.title()), selected);

        candidates.stream()
                .map(BookCatalogSearchService::cleanMetadata)
                .filter(candidate -> sameAuthor(selected, candidate))
                .filter(candidate -> {
                    String candidateSeries = displaySeriesName(
                            prefer(
                                    candidate.seriesName(),
                                    inferAudiobookSeriesName(candidate)));
                    return hasText(candidateSeries)
                            && canonicalSeries(candidateSeries)
                            .equals(targetSeries);
                })
                .forEach(candidate -> distinctTitles.merge(
                        canonicalSearchTitle(candidate.title()),
                        candidate,
                        BookCatalogSearchService::preferEarlierEdition));

        if (distinctTitles.size() < 2) return null;

        List<CatalogBookResult> ordered = distinctTitles.values().stream()
                .filter(result -> plausiblePublicationYear(
                        publicationYear(result.publicationDate())))
                .sorted(Comparator
                        .comparing(BookCatalogSearchService::publicationSortKey)
                        .thenComparing(
                                CatalogBookResult::title,
                                String.CASE_INSENSITIVE_ORDER))
                .toList();

        String selectedTitle = canonicalSearchTitle(selected.title());
        for (int index = 0; index < ordered.size(); index++) {
            if (canonicalSearchTitle(ordered.get(index).title())
                    .equals(selectedTitle)) {
                return (double) index + 1;
            }
        }
        return null;
    }

    private static CatalogBookResult preferEarlierEdition(
            CatalogBookResult first,
            CatalogBookResult second) {
        return publicationSortKey(first)
                .compareTo(publicationSortKey(second)) <= 0
                ? first : second;
    }

    private static String publicationSortKey(CatalogBookResult result) {
        String value = safeTitle(result.publicationDate());
        Matcher matcher = Pattern.compile(
                        "^(\\d{4})(?:-(\\d{1,2}))?(?:-(\\d{1,2}))?")
                .matcher(value);
        if (!matcher.find()) return "9999-12-31";
        int month = matcher.group(2) == null
                ? 1 : Integer.parseInt(matcher.group(2));
        int day = matcher.group(3) == null
                ? 1 : Integer.parseInt(matcher.group(3));
        return String.format(
                Locale.ROOT,
                "%s-%02d-%02d",
                matcher.group(1),
                month,
                day);
    }

    private static boolean sameAuthor(
            CatalogBookResult first,
            CatalogBookResult second) {
        List<String> firstAuthors = safe(first.authors()).stream()
                .filter(BookCatalogSearchService::hasText)
                .toList();
        return safe(second.authors()).stream()
                .filter(BookCatalogSearchService::hasText)
                .anyMatch(secondAuthor -> firstAuthors.stream()
                        .anyMatch(firstAuthor ->
                                samePerson(firstAuthor, secondAuthor)));
    }

    private static boolean samePerson(String first, String second) {
        String normalizedFirst = normalize(first);
        String normalizedSecond = normalize(second);
        if (normalizedFirst.isBlank() || normalizedSecond.isBlank()) return false;
        if (normalizedFirst.equals(normalizedSecond)
                || normalizedFirst.contains(normalizedSecond)
                || normalizedSecond.contains(normalizedFirst)) {
            return true;
        }

        String[] firstParts = normalizedFirst.split(" ");
        String[] secondParts = normalizedSecond.split(" ");
        if (firstParts.length < 2 || secondParts.length < 2) return false;
        if (!firstParts[firstParts.length - 1]
                .equals(secondParts[secondParts.length - 1])) {
            return false;
        }

        int comparableGivenNames =
                Math.min(firstParts.length, secondParts.length) - 1;
        for (int index = 0; index < comparableGivenNames; index++) {
            if (firstParts[index].charAt(0) != secondParts[index].charAt(0)) {
                return false;
            }
        }
        return comparableGivenNames > 0;
    }

    private static String preferredAudiobookSeries(
            CatalogBookResult audiobook,
            String supplementalSeries) {
        String selectedSeries = displaySeriesName(audiobook.seriesName());
        if (hasText(selectedSeries)) return selectedSeries;

        String titleSeries = displaySeriesName(
                seriesNameFromAudiobookTitle(audiobook.title()));
        if (hasText(titleSeries)) return titleSeries;

        supplementalSeries = displaySeriesName(supplementalSeries);
        if (!hasText(supplementalSeries)) return supplementalSeries;

        String cleaned = supplementalSeries.trim();
        for (String author : safe(audiobook.authors())) {
            String[] nameParts = safeTitle(author).split("\\s+");
            if (nameParts.length == 0) continue;
            String surname = nameParts[nameParts.length - 1];
            if (surname.length() < 2) continue;

            String prefix = surname + " ";
            if (cleaned.regionMatches(true, 0, prefix, 0, prefix.length())) {
                String withoutSurname = cleaned.substring(prefix.length()).trim();
                if (!withoutSurname.isBlank()) return withoutSurname;
            }
        }
        return cleaned;
    }

    private static String inferAudiobookSeriesName(CatalogBookResult result) {
        String evidence = metadataEvidence(result);
        Matcher explicit = EXPLICIT_NAMED_SERIES.matcher(evidence);
        if (explicit.find()) {
            return cleanInferredSeriesName(explicit.group(1), result.authors());
        }

        return null;
    }

    private static String cleanInferredSeriesName(
            String candidate,
            List<String> authors) {
        String cleaned = safeTitle(candidate)
                .replaceFirst("(?i)^the\\s+", "")
                .replaceFirst("(?i)\\s+(?:book|novel|audiobook)$", "")
                .trim();

        for (String author : safe(authors)) {
            String fullName = safeTitle(author);
            if (fullName.isBlank()) continue;
            cleaned = cleaned.replaceFirst(
                    "(?i)^" + Pattern.quote(fullName) + "(?:['’]s)?\\s+",
                    "");

            String[] parts = fullName.split("\\s+");
            String surname = parts[parts.length - 1];
            cleaned = cleaned.replaceFirst(
                    "(?i)^" + Pattern.quote(surname) + "(?:['’]s)?\\s+",
                    "");
        }

        // Provider descriptions often insert awards and marketing between
        // “first book in” and the real series name. Remove the promotional
        // prefix while retaining the words that follow it.
        cleaned = cleaned
                .replaceFirst(
                        "(?i)^.*\\b(?:bestselling|best-selling|best selling)\\s+",
                        "")
                .replaceFirst(
                        "(?i)^(?:acclaimed|beloved|enchanting|phenomenal)\\s+",
                        "")
                .trim();
        return displaySeriesName(cleaned);
    }

    private static String seriesNameFromAudiobookTitle(String rawTitle) {
        Matcher matcher = TRAILING_AUDIOBOOK_SERIES.matcher(safeTitle(rawTitle));
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

        return candidate
                .replaceFirst(
                        "(?i)\\s*[,;:-]?\\s*(?:book|volume|vol\\.?)\\s*#?\\s*\\d+(?:\\.\\d+)?\\s*$",
                        "")
                .trim();
    }

    private static String metadataEvidence(CatalogBookResult result) {
        return String.join(" ",
                safeTitle(result.title()),
                result.subtitle() == null ? "" : result.subtitle(),
                result.seriesName() == null ? "" : result.seriesName(),
                result.description() == null ? "" : result.description());
    }

    private static Double parseSeriesNumber(String evidence) {
        Matcher numeric = NUMERIC_SERIES_NUMBER.matcher(evidence);
        if (numeric.find()) {
            try {
                return Double.valueOf(numeric.group(1));
            } catch (NumberFormatException ignored) {
                // Continue to word-based evidence below.
            }
        }

        Matcher numericOrdinal =
                NUMERIC_ORDINAL_SERIES_NUMBER.matcher(evidence);
        if (numericOrdinal.find()) {
            return parseNumber(numericOrdinal.group(1));
        }

        Matcher ordinal = ORDINAL_SERIES_NUMBER.matcher(evidence);
        if (ordinal.find()) return numberWord(ordinal.group(1));

        Matcher ordinalInSeries = ORDINAL_IN_SERIES.matcher(evidence);
        if (ordinalInSeries.find()) return numberWord(ordinalInSeries.group(1));

        Matcher word = WORD_SERIES_NUMBER.matcher(evidence);
        if (word.find()) return numberWord(word.group(1));
        return null;
    }

    /**
     * Accepts only wording that directly connects a number to a volume or a
     * named series. This deliberately excludes promotional wording such as
     * “the first book to…” and “#1 bestseller.”
     */
    private static Double parseStrongSeriesNumber(String evidence) {
        Matcher numeric = NUMERIC_SERIES_NUMBER.matcher(evidence);
        if (numeric.find()) {
            try {
                return Double.valueOf(numeric.group(1));
            } catch (NumberFormatException ignored) {
                // Continue to explicit named-series wording.
            }
        }

        Matcher numericOrdinal =
                NUMERIC_ORDINAL_SERIES_NUMBER.matcher(evidence);
        if (numericOrdinal.find()) {
            return parseNumber(numericOrdinal.group(1));
        }

        Matcher ordinalInSeries = ORDINAL_IN_SERIES.matcher(evidence);
        if (ordinalInSeries.find()) {
            return numberWord(ordinalInSeries.group(1));
        }

        Matcher word = WORD_SERIES_NUMBER.matcher(evidence);
        if (word.find()) return numberWord(word.group(1));
        return null;
    }

    private static Double numberWord(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "first", "one" -> 1.0;
            case "second", "two" -> 2.0;
            case "third", "three" -> 3.0;
            case "fourth", "four" -> 4.0;
            case "fifth", "five" -> 5.0;
            case "sixth", "six" -> 6.0;
            case "seventh", "seven" -> 7.0;
            case "eighth", "eight" -> 8.0;
            case "ninth", "nine" -> 9.0;
            case "tenth", "ten" -> 10.0;
            default -> null;
        };
    }

    private static CatalogBookResult withAudiobookBaseTitle(
            CatalogBookResult audiobook,
            String baseTitle,
            String discoveredSeriesName,
            Double discoveredSeriesNumber) {
        return new CatalogBookResult(
                audiobook.provider(),
                audiobook.providerId(),
                baseTitle,
                audiobook.subtitle(),
                audiobook.authors(),
                audiobook.genres(),
                audiobook.description(),
                audiobook.publisher(),
                audiobook.publicationDate(),
                null,
                audiobook.audiobookLengthSeconds(),
                audiobook.narrators(),
                audiobook.coverImageUrl(),
                audiobook.language(),
                audiobook.isbn10(),
                audiobook.isbn13(),
                discoveredSeriesName,
                discoveredSeriesNumber,
                audiobook.editionFormat(),
                "AUDIOBOOK"
        );
    }

    private static String audiobookBaseTitle(
            String rawTitle,
            String seriesName) {
        String title = rawTitle == null ? "" : rawTitle.trim();

        // Remove edition-only parentheticals first. This is generic metadata
        // cleanup; it does not contain title-specific or series-specific data.
        String previous;
        do {
            previous = title;
            title = title.replaceFirst(
                    "(?i)\\s*\\([^)]*(?:dramatized|adaptation|unabridged|abridged|part\\s*\\d+)[^)]*\\)\\s*$",
                    "").trim();
        } while (!title.equals(previous));

        // Remove recognized store-edition labels after a colon or dash while
        // leaving ordinary subtitles untouched.
        title = title.replaceFirst(
                "(?i)\\s*[:\\-–—]\\s*(?:special edition|anniversary edition|"
                        + "collector['’]?s edition|unabridged|abridged|"
                        + "dramatized adaptation|dramatized|audiobook)\\s*$",
                "").trim();

        if (hasText(seriesName)) {
            String suffix = " (" + seriesName.trim() + ")";
            if (title.toLowerCase(Locale.ROOT)
                    .endsWith(suffix.toLowerCase(Locale.ROOT))) {
                title = title.substring(0, title.length() - suffix.length()).trim();
            }
        }

        // Apple commonly labels an audiobook as "Title (Series 1)". Once the
        // trailing parenthetical has been accepted as series evidence, remove
        // the entire storefront label so print-catalog lookups use the actual
        // work title rather than the decorated edition title.
        if (seriesNameFromAudiobookTitle(title) != null) {
            title = title.replaceFirst(
                    "\\s*\\([^()]{2,80}\\)\\s*$",
                    "").trim();
        }

        return title.isBlank() ? safeTitle(rawTitle) : title;
    }

    private static String safeTitle(String value) {
        return value == null ? "" : value.trim();
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

        if ("TITLE".equals(searchBy)) {
            String canonicalTarget = canonicalSearchTitle(result.title());
            String canonicalQuery = canonicalSearchTitle(query);
            if (canonicalTarget.equals(canonicalQuery)) {
                score += 1400;
            } else if (canonicalTarget.startsWith(canonicalQuery)) {
                score += 250;
            }
            if (seriesMatchesQuery(result, query)) score += 1800;
            if (looksLikeAncillaryRecord(result)) score -= 900;
            if (looksLikeMultiBookBundle(result.title())) score -= 3000;
        }

        if (result.coverImageUrl() != null) score += 25;
        if (result.pageCount() != null && result.pageCount() > 0) score += 20;
        if (result.isbn13() != null || result.isbn10() != null) score += 15;
        if (result.seriesName() != null) score += 10;
        return score;
    }

    private static String canonicalSearchTitle(String value) {
        return normalize(value)
                .replaceFirst("^the\\s+", "")
                .replaceFirst(
                        "\\s+(?:special edition|anniversary edition|"
                                + "collector s edition|unabridged|abridged|"
                                + "dramatized adaptation|dramatized|audiobook)$",
                        "")
                .trim();
    }

    private static boolean looksLikeAncillaryWork(String value) {
        String title = normalize(value);
        return title.contains(" word search")
                || title.contains(" coloring book")
                || title.contains(" activity book")
                || title.contains(" canvas bag")
                || title.contains(" tote bag")
                || title.contains(" book analysis")
                || title.contains(" book summary")
                || title.contains(" teacher guide")
                || title.contains(" teacher s guide")
                || title.contains(" study guide")
                || title.contains(" workbook")
                || title.contains(" literature unit")
                || title.contains(" unofficial guide")
                || title.contains(" companion")
                || title.contains(" amazing facts")
                || title.contains(" and philosophy")
                || title.startsWith("summary of ")
                || title.startsWith("analysis of ")
                || title.startsWith("author of ");
    }

    private static boolean looksLikeAncillaryRecord(
            CatalogBookResult result) {
        if (result == null) return false;
        if (looksLikeAncillaryWork(result.title())) return true;

        String evidence = normalize(String.join(" ",
                safeTitle(result.subtitle()),
                String.join(" ", safe(result.authors())),
                safeTitle(result.publisher()),
                safeTitle(result.description())));
        return evidence.contains("creativity in the classroom")
                || evidence.contains("classroom complete press")
                || evidence.contains("novel units")
                || evidence.contains("on the mark press")
                || evidence.contains("teacher guide")
                || evidence.contains("teacher s guide")
                || evidence.contains("study guide")
                || evidence.contains("student workbook")
                || evidence.contains("lesson plan")
                || evidence.contains("chapter analysis")
                || evidence.contains("comprehension questions")
                || evidence.contains("literature unit");
    }

    private static boolean looksLikeMultiBookBundle(String value) {
        String title = normalize(value);
        return title.contains(" box set")
                || title.contains(" boxed set")
                || title.contains(" complete series")
                || title.contains(" complete collection")
                || title.contains(" holiday collection")
                || title.contains(" omnibus")
                || title.matches(".*\\bbooks?\\s+\\d+\\s*[-–—]\\s*\\d+\\b.*")
                || title.matches(".*\\b\\d+\\s*[-–—]\\s*book\\s+(?:set|collection)\\b.*");
    }

    private static boolean shouldShowSingleWorkResult(
            CatalogBookResult result,
            String cleanedQuery,
            String rawSearchBy) {
        if (!"TITLE".equals(normalizeSearchBy(rawSearchBy))) return true;
        if (looksLikeAncillaryRecord(result)
                && !looksLikeAncillaryWork(cleanedQuery)) {
            return false;
        }
        if (!looksLikeMultiBookBundle(result.title())) return true;

        // A reader may intentionally type the complete bundle title. Partial
        // matches for one novel, however, should describe that novel rather
        // than a multi-book product.
        return canonicalSearchTitle(result.title())
                .equals(canonicalSearchTitle(cleanedQuery));
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
        String identityTitle = isAudiobookFormat(result.format())
                ? audiobookIdentityTitle(result)
                : normalize(result.title());
        return identityTitle + "|"
                + canonicalPrimaryAuthor(result.authors()) + "|"
                + normalize(result.format());
    }

    private static String audiobookIdentityTitle(CatalogBookResult result) {
        String title = audiobookBaseTitle(
                result.title(),
                preferredAudiobookSeries(result, null));
        return canonicalSearchTitle(title);
    }

    private static String searchIdentity(CatalogBookResult result) {
        String isbn13 = safeTitle(result.isbn13())
                .replaceAll("[^0-9Xx]", "")
                .toUpperCase(Locale.ROOT);
        String isbn10 = safeTitle(result.isbn10())
                .replaceAll("[^0-9Xx]", "")
                .toUpperCase(Locale.ROOT);

        String editionIdentity;
        if (!isbn13.isBlank()) {
            editionIdentity = "isbn13:" + isbn13;
        } else if (!isbn10.isBlank()) {
            editionIdentity = "isbn10:" + isbn10;
        } else {
            String language = normalize(result.language());
            editionIdentity = language.isBlank()
                    ? "language:unknown"
                    : "language:" + language;
        }

        return identity(result) + "|" + editionIdentity;
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
        SeriesInference inferredSeries = inferSeriesMetadata(result);

        if (!hasText(seriesName)
                || canonicalSeries(seriesName).equals(
                canonicalSeries(result.title()))) {
            seriesName = prefer(inferredSeries.name(), seriesName);
        }
        if (seriesNumber == null && hasText(inferredSeries.name())) {
            seriesNumber = inferredSeries.number();
        }
        if (seriesNumber == null && hasText(seriesName)) {
            seriesNumber = parseStrongSeriesNumber(metadataEvidence(result));
        }

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

    private static SeriesInference inferSeriesMetadata(
            CatalogBookResult result) {
        Matcher titleMatcher = NUMBERED_TITLE_SERIES.matcher(
                safeTitle(result.title()));
        if (titleMatcher.matches()) {
            return new SeriesInference(
                    displaySeriesName(titleMatcher.group(1)),
                    parseNumber(titleMatcher.group(2)));
        }

        String description = result.description();
        if (!hasText(description)) return new SeriesInference(null, null);

        Matcher explicitMatcher =
                NAMED_COLLECTION_IN_DESCRIPTION.matcher(description);
        if (explicitMatcher.find()) {
            return new SeriesInference(
                    collectionDisplayName(
                            explicitMatcher.group(1),
                            explicitMatcher.group(2)),
                    ordinalSeriesNumber(explicitMatcher.group()));
        }

        Matcher capitalizedMatcher =
                CAPITALIZED_COLLECTION_IN_DESCRIPTION.matcher(description);
        while (capitalizedMatcher.find()) {
            String candidate = displaySeriesName(capitalizedMatcher.group(1));
            if (!hasText(candidate) || isGenericCollectionName(candidate)) {
                continue;
            }
            return new SeriesInference(
                    collectionDisplayName(
                            candidate,
                            capitalizedMatcher.group(2)),
                    null);
        }

        return new SeriesInference(null, null);
    }

    private static String collectionDisplayName(
            String rawName,
            String rawKind) {
        String name = displaySeriesName(rawName);
        if (!hasText(name)) return null;
        String kind = normalize(rawKind);
        if (kind.equals("trilogy")
                && !normalize(name).endsWith(" trilogy")) {
            return name + " Trilogy";
        }
        if (kind.equals("saga") && !normalize(name).endsWith(" saga")) {
            return name + " Saga";
        }
        if (kind.equals("chronicles")
                && !normalize(name).endsWith(" chronicles")) {
            return name + " Chronicles";
        }
        return name;
    }

    private static boolean isGenericCollectionName(String value) {
        String normalized = normalize(value);
        return normalized.equals("book")
                || normalized.equals("chapter book")
                || normalized.equals("bestselling")
                || normalized.equals("best selling")
                || normalized.equals("tv")
                || normalized.equals("television")
                || normalized.equals("film")
                || normalized.equals("movie")
                || normalized.contains("new york times")
                || normalized.contains("bestselling chapter book")
                || normalized.contains("best selling chapter book");
    }

    private static Double parseNumber(String value) {
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Double ordinalSeriesNumber(String value) {
        if (!hasText(value)) return null;
        Matcher matcher = Pattern.compile(
                "(?i)\\b(first|second|third|fourth|fifth|sixth|seventh|"
                        + "eighth|ninth|tenth)\\b").matcher(value);
        if (!matcher.find()) return null;
        return switch (matcher.group(1).toLowerCase(Locale.ROOT)) {
            case "first" -> 1.0;
            case "second" -> 2.0;
            case "third" -> 3.0;
            case "fourth" -> 4.0;
            case "fifth" -> 5.0;
            case "sixth" -> 6.0;
            case "seventh" -> 7.0;
            case "eighth" -> 8.0;
            case "ninth" -> 9.0;
            case "tenth" -> 10.0;
            default -> null;
        };
    }

    private record SeriesInference(String name, Double number) {}

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
            addGenre(cleaned, normalized, "biograph", "Biographies & Memoirs");
            addGenre(cleaned, normalized, "memoir", "Biographies & Memoirs");
            addGenre(cleaned, normalized, "nonfiction", "Nonfiction");
            addGenre(cleaned, normalized, "non-fiction", "Nonfiction");
            addGenre(cleaned, normalized, "music", "Music");
            addGenre(cleaned, normalized, "sports", "Sports");

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
        addGenreFromAny(cleaned, synopsis, "Fantasy",
                "fantasy", "magical", "magic", "enchanted", "enchantment",
                "curse", "cursed", "dragon", "faerie", "fairy tale");
        addGenreFromAny(cleaned, synopsis, "Romance",
                "romance", "romantic", "love story", "true love",
                "star crossed", "love spell", "prince of hearts");
        addGenreFromAny(cleaned, synopsis, "Biographies & Memoirs",
                "biography", "memoir", "life story", "recounts the life",
                "story of his life", "story of her life");

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
        String original = value.trim();
        if (original.matches("(?:a|an)\\s+[a-z][a-z -]{1,40}")) {
            return null;
        }
        String cleaned = value
                .replace('_', ' ')
                .replaceFirst("(?i)^\\s*serie?s?\\s*[:=-]\\s*", "")
                .replaceFirst("(?i)\\s*(?:,|-|:)?\\s*(?:book\\s*)?[#:]?\\s*\\d+(?:\\.\\d+)?\\s*$", "")
                .trim();
        if (cleaned.isBlank()) return null;

        String normalized = normalize(cleaned);
        if (normalized.equals("its")
                || normalized.equals("this")
                || normalized.equals("new")
                || normalized.equals("a new")
                || normalized.equals("an exciting new")
                || normalized.equals("new series")
                || normalized.equals("a new series")
                || normalized.startsWith("bestselling ")
                || normalized.startsWith("best selling ")
                || normalized.contains("best selling")
                || normalized.contains("bestseller")
                || normalized.contains("bestselling")
                || normalized.contains("fairytale sensation")
                || normalized.contains("fairy tale sensation")
                || normalized.startsWith("new york times ")
                || normalized.startsWith("award winning ")
                || (normalized.split(" ").length <= 3
                && (normalized.startsWith("a new ")
                || normalized.startsWith("the new ")))) {
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

    private record SeriesCandidate(
            String name,
            Set<String> providers) {
        private SeriesCandidate(String name) {
            this(name, new java.util.LinkedHashSet<>());
        }
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
