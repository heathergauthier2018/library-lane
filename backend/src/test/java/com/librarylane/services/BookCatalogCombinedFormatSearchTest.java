package com.librarylane.services;

import com.librarylane.catalog.CatalogBookResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookCatalogCombinedFormatSearchTest {

    @Mock
    private OpenLibraryCatalogService openLibrary;

    @Mock
    private GoogleBooksCatalogService googleBooks;

    @Mock
    private WikidataCatalogService wikidata;

    @Mock
    private AppleAudiobookCatalogService appleAudiobooks;

    private BookCatalogSearchService service;

    @BeforeEach
    void setUp() {
        service = new BookCatalogSearchService(
                openLibrary,
                googleBooks,
                wikidata,
                appleAudiobooks);
    }

    @Test
    void unfilteredSearchReturnsPhysicalEbookAndAudiobookEditionsTogether() {
        when(openLibrary.search("Queen", null, "TITLE"))
                .thenReturn(List.of(result(
                        "Open Library",
                        "ol-1",
                        "Queen",
                        "PHYSICAL")));
        when(googleBooks.search("Queen", null, "TITLE"))
                .thenReturn(List.of(result(
                        "Google Books",
                        "gb-1",
                        "Queen",
                        "EBOOK")));
        when(googleBooks.search("Queen", "EBOOK", "TITLE"))
                .thenReturn(List.of());
        when(appleAudiobooks.search("Queen", "AUDIOBOOK", "TITLE"))
                .thenReturn(List.of(result(
                        "Apple Audiobooks",
                        "apple-1",
                        "Queen",
                        "AUDIOBOOK")));

        List<CatalogBookResult> results =
                service.search("Queen", null, "TITLE");

        Set<String> formats = results.stream()
                .map(CatalogBookResult::format)
                .collect(Collectors.toSet());

        assertEquals(
                Set.of("PHYSICAL", "EBOOK", "AUDIOBOOK"),
                formats);
        verify(appleAudiobooks)
                .search("Queen", "AUDIOBOOK", "TITLE");
    }

    @Test
    void strongestExactEditionOfEachFormatLeadsMixedTitleResults() {
        List<CatalogBookResult> printResults = new ArrayList<>();
        printResults.add(result(
                "Open Library",
                "physical-exact",
                "The Clockwork Wing",
                "PHYSICAL"));
        for (int index = 1; index <= 12; index++) {
            printResults.add(result(
                    "Open Library",
                    "related-" + index,
                    "The Clockwork Wing Companion Volume " + index,
                    "PHYSICAL"));
        }

        when(openLibrary.search("The Clockwork Wing", null, "TITLE"))
                .thenReturn(printResults);
        when(googleBooks.search("The Clockwork Wing", null, "TITLE"))
                .thenReturn(List.of(result(
                        "Google Books",
                        "ebook-exact",
                        "The Clockwork Wing",
                        "EBOOK")));
        when(googleBooks.search(
                "The Clockwork Wing",
                "EBOOK",
                "TITLE"))
                .thenReturn(List.of());
        when(appleAudiobooks.search(
                "The Clockwork Wing",
                "AUDIOBOOK",
                "TITLE"))
                .thenReturn(List.of(result(
                        "Apple Audiobooks",
                        "audio-exact",
                        "The Clockwork Wing (Skybound)",
                        "AUDIOBOOK")));

        List<CatalogBookResult> results = service.search(
                "The Clockwork Wing",
                null,
                "TITLE");

        assertEquals(
                List.of("PHYSICAL", "EBOOK", "AUDIOBOOK"),
                results.stream()
                        .limit(3)
                        .map(CatalogBookResult::format)
                        .toList());
        assertEquals(
                "The Clockwork Wing (Skybound)",
                results.get(2).title());
    }

    @Test
    void balancesFormatsBeforeTruncatingLargeExactTitleResultSets() {
        List<CatalogBookResult> printResults = new ArrayList<>();
        for (int index = 0; index < 55; index++) {
            printResults.add(resultWithMetadata(
                    "Open Library",
                    "classic-print-" + index,
                    "The Garden Chronicle",
                    index == 0
                            ? "L. M. Example"
                            : "Edition Contributor " + index,
                    null,
                    "PHYSICAL"));
        }
        CatalogBookResult ebook = resultWithMetadata(
                "Google Books",
                "classic-ebook",
                "The Garden Chronicle",
                "L. M. Example",
                null,
                "EBOOK");
        CatalogBookResult audiobook = resultWithMetadata(
                "Apple Audiobooks",
                "classic-audio",
                "The Garden Chronicle",
                "L.M. Example",
                null,
                "AUDIOBOOK");

        when(openLibrary.search("the garden chronicle", null, "TITLE"))
                .thenReturn(printResults);
        when(googleBooks.search("the garden chronicle", null, "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("the garden chronicle", "EBOOK", "TITLE"))
                .thenReturn(List.of(ebook));
        when(appleAudiobooks.search(
                "the garden chronicle",
                "AUDIOBOOK",
                "TITLE"))
                .thenReturn(List.of(audiobook));

        List<CatalogBookResult> results = service.search(
                "the garden chronicle",
                null,
                "TITLE");

        assertEquals(50, results.size());
        assertEquals(
                List.of("PHYSICAL", "EBOOK", "AUDIOBOOK"),
                results.stream()
                        .limit(3)
                        .map(CatalogBookResult::format)
                        .toList());
        assertEquals(
                List.of("L. M. Example", "L. M. Example", "L.M. Example"),
                results.stream()
                        .limit(3)
                        .map(result -> result.authors().getFirst())
                        .toList());
    }

    @Test
    void canonicalSeriesAuthorLeadsBroadFranchiseTitleResults() {
        CatalogBookResult adjacentPhysical = resultWithMetadata(
                "Open Library",
                "adjacent-physical",
                "The Star Wizard",
                "An Adjacent Writer",
                null,
                "PHYSICAL");
        CatalogBookResult canonicalPhysical = resultWithMetadata(
                "Open Library",
                "canonical-physical",
                "The Star Wizard and the Hidden Gate",
                "The Canonical Writer",
                "The Star Wizard",
                "PHYSICAL");
        CatalogBookResult adjacentEbook = resultWithMetadata(
                "Google Books",
                "adjacent-ebook",
                "The Star Wizard",
                "Another Adjacent Writer",
                null,
                "EBOOK");
        CatalogBookResult canonicalEbook = resultWithMetadata(
                "Google Books",
                "canonical-ebook",
                "The Star Wizard and the Hidden Gate",
                "The Canonical Writer",
                "The Star Wizard",
                "EBOOK");
        CatalogBookResult adjacentAudio = resultWithMetadata(
                "Apple Audiobooks",
                "adjacent-audio",
                "The Star Wizard",
                "A Third Adjacent Writer",
                null,
                "AUDIOBOOK");
        CatalogBookResult canonicalAudio = resultWithMetadata(
                "Apple Audiobooks",
                "canonical-audio",
                "The Star Wizard and the Hidden Gate",
                "The Canonical Writer",
                null,
                "AUDIOBOOK");

        when(openLibrary.search("The Star Wizard", null, "TITLE"))
                .thenReturn(List.of(adjacentPhysical, canonicalPhysical));
        when(googleBooks.search("The Star Wizard", null, "TITLE"))
                .thenReturn(List.of(adjacentEbook));
        when(googleBooks.search("The Star Wizard", "EBOOK", "TITLE"))
                .thenReturn(List.of(canonicalEbook));
        when(appleAudiobooks.search(
                "The Star Wizard",
                "AUDIOBOOK",
                "TITLE"))
                .thenReturn(List.of(adjacentAudio, canonicalAudio));

        List<CatalogBookResult> results = service.search(
                "The Star Wizard",
                null,
                "TITLE");

        assertEquals(
                List.of(
                        "The Canonical Writer",
                        "The Canonical Writer",
                        "The Canonical Writer"),
                results.stream()
                        .limit(3)
                        .map(result -> result.authors().getFirst())
                        .toList());
        assertEquals(
                List.of("PHYSICAL", "EBOOK", "AUDIOBOOK"),
                results.stream()
                        .limit(3)
                        .map(CatalogBookResult::format)
                        .toList());
    }

    @Test
    void exactPrintAuthorSelectsTheMatchingAudiobookLead() {
        CatalogBookResult print = resultWithMetadata(
                "Open Library",
                "finale-print",
                "Finale",
                "Stephanie Garber",
                null,
                "PHYSICAL");
        CatalogBookResult wrongAudio = resultWithMetadata(
                "Apple Audiobooks",
                "finale-wrong-audio",
                "Finale",
                "D. T. Max",
                null,
                "AUDIOBOOK");
        CatalogBookResult matchingAudio = resultWithMetadata(
                "Apple Audiobooks",
                "finale-matching-audio",
                "Finale (Caraval 3)",
                "Stephanie Garber",
                "Caraval",
                "AUDIOBOOK");

        when(openLibrary.search("Finale", null, "TITLE"))
                .thenReturn(List.of(print));
        when(googleBooks.search("Finale", null, "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("Finale", "EBOOK", "TITLE"))
                .thenReturn(List.of());
        when(appleAudiobooks.search("Finale", "AUDIOBOOK", "TITLE"))
                .thenReturn(List.of(wrongAudio, matchingAudio));

        List<CatalogBookResult> results =
                service.search("Finale", null, "TITLE");

        assertEquals(
                "Stephanie Garber",
                results.stream()
                        .filter(result -> "AUDIOBOOK".equals(result.format()))
                        .findFirst()
                        .orElseThrow()
                        .authors()
                        .getFirst());
    }

    @Test
    void exactWorkAuthorBeatsAnUnrelatedInferredSeries() {
        CatalogBookResult exactPhysical = resultWithMetadata(
                "Open Library",
                "fourth-wing-print",
                "Fourth Wing",
                "Rebecca Yarros",
                null,
                "PHYSICAL");
        CatalogBookResult unrelatedSeries = resultWithMetadata(
                "Google Books",
                "dragon-academy",
                "The Fourth Wing The Dragon Academy",
                "Kelly Emberfall",
                "Fourth Wing",
                "EBOOK");
        CatalogBookResult exactEbook = resultWithMetadata(
                "Google Books",
                "fourth-wing-ebook",
                "Fourth Wing",
                "Rebecca Yarros",
                null,
                "EBOOK");
        CatalogBookResult wordSearch = resultWithMetadata(
                "Google Books",
                "word-search",
                "Fourth Wing: The Official Word Search Book",
                "Entangled",
                null,
                "PHYSICAL");
        CatalogBookResult canvasBag = resultWithMetadata(
                "Google Books",
                "canvas-bag",
                "Fourth Wing Shadows of Navarre Canvas Bag",
                "Paperblanks",
                null,
                "PHYSICAL");
        CatalogBookResult exactAudiobook = resultWithMetadata(
                "Apple Audiobooks",
                "fourth-wing-audio",
                "Fourth Wing (Empyrean)",
                "Rebecca Yarros",
                "The Empyrean",
                "AUDIOBOOK");

        when(openLibrary.search("Fourth Wing", null, "TITLE"))
                .thenReturn(List.of(exactPhysical));
        when(googleBooks.search("Fourth Wing", null, "TITLE"))
                .thenReturn(List.of(
                        unrelatedSeries,
                        exactEbook,
                        wordSearch,
                        canvasBag));
        when(googleBooks.search("Fourth Wing", "EBOOK", "TITLE"))
                .thenReturn(List.of());
        when(appleAudiobooks.search(
                "Fourth Wing",
                "AUDIOBOOK",
                "TITLE"))
                .thenReturn(List.of(exactAudiobook));

        List<CatalogBookResult> results = service.search(
                "Fourth Wing",
                null,
                "TITLE");

        assertEquals(
                List.of("PHYSICAL", "EBOOK", "AUDIOBOOK"),
                results.stream()
                        .limit(3)
                        .map(CatalogBookResult::format)
                        .toList());
        assertEquals(
                List.of(
                        "Rebecca Yarros",
                        "Rebecca Yarros",
                        "Rebecca Yarros"),
                results.stream()
                        .limit(3)
                        .map(result -> result.authors().getFirst())
                        .toList());
        assertFalse(results.stream().anyMatch(result -> {
            String title = result.title().toLowerCase();
            return title.contains("word search")
                    || title.contains("canvas bag");
        }));
    }

    @Test
    void leadFormatsPreferEnglishAndStrongTitles() {
        CatalogBookResult turkishPhysical = resultWithLanguageAndDescription(
                "Open Library",
                "onyx-tr",
                "Onyx Storm",
                "Rebecca Yarros",
                null,
                "tr",
                null,
                "PHYSICAL");
        CatalogBookResult englishPhysical = resultWithLanguageAndDescription(
                "Open Library",
                "onyx-en",
                "Onyx Storm",
                "Rebecca Yarros",
                null,
                "en",
                null,
                "PHYSICAL");
        CatalogBookResult englishEbook = resultWithLanguageAndDescription(
                "Google Books",
                "onyx-ebook-en",
                "Onyx Storm",
                "Rebecca Yarros",
                null,
                "en",
                null,
                "EBOOK");
        CatalogBookResult germanAudiobook =
                resultWithLanguageAndDescription(
                        "Apple Audiobooks",
                        "onyx-audio-de",
                        "Onyx Storm - Flammengeküsst-Reihe",
                        "Rebecca Yarros",
                        "Flammengeküsst-Reihe",
                        null,
                        null,
                        "AUDIOBOOK");
        CatalogBookResult englishAudiobook =
                resultWithLanguageAndDescription(
                        "Apple Audiobooks",
                        "onyx-audio-en",
                        "Onyx Storm (Empyrean, Book 3)",
                        "Rebecca Yarros",
                        "The Empyrean",
                        "en",
                        null,
                        "AUDIOBOOK");

        when(openLibrary.search("Onyx Storm", null, "TITLE"))
                .thenReturn(List.of(
                        turkishPhysical,
                        englishPhysical));
        when(googleBooks.search("Onyx Storm", null, "TITLE"))
                .thenReturn(List.of(englishEbook));
        when(googleBooks.search("Onyx Storm", "EBOOK", "TITLE"))
                .thenReturn(List.of());
        when(appleAudiobooks.search(
                "Onyx Storm",
                "AUDIOBOOK",
                "TITLE"))
                .thenReturn(List.of(
                        germanAudiobook,
                        englishAudiobook));

        List<CatalogBookResult> results = service.search(
                "Onyx Storm",
                null,
                "TITLE");

        assertEquals(
                List.of(
                        "onyx-en",
                        "onyx-ebook-en",
                        "onyx-audio-en"),
                results.stream()
                        .limit(3)
                        .map(CatalogBookResult::providerId)
                        .toList());
    }

    @Test
    void televisionMarketingCopyIsNotInferredAsSeriesMetadata() {
        CatalogBookResult result = resultWithLanguageAndDescription(
                "Google Books",
                "iron-flame",
                "Iron Flame",
                "Rebecca Yarros",
                null,
                "en",
                "A #1 New York Times bestseller. A television series is in development.",
                "EBOOK");

        when(openLibrary.search("Iron Flame", null, "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("Iron Flame", null, "TITLE"))
                .thenReturn(List.of(result));
        when(googleBooks.search("Iron Flame", "EBOOK", "TITLE"))
                .thenReturn(List.of());
        when(appleAudiobooks.search(
                "Iron Flame",
                "AUDIOBOOK",
                "TITLE"))
                .thenReturn(List.of());

        List<CatalogBookResult> results = service.search(
                "Iron Flame",
                null,
                "TITLE");

        assertFalse(results.getFirst().seriesName() != null);
        assertFalse(results.getFirst().seriesNumber() != null);
    }
    @Test
    void readingOrderUsesNumberAssociatedWithCurrentTitle() {
        CatalogBookResult ironFlame = resultWithLanguageAndDescription(
                "Google Books",
                "iron-reading-order",
                "Iron Flame",
                "Rebecca Yarros",
                "The Empyrean",
                "en",
                "Reading Order: Book #1 Fourth Wing, Book #2 Iron Flame, Book #3 Onyx Storm.",
                "EBOOK");

        when(openLibrary.search("Iron Flame", null, "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("Iron Flame", null, "TITLE"))
                .thenReturn(List.of(ironFlame));
        when(googleBooks.search("Iron Flame", "EBOOK", "TITLE"))
                .thenReturn(List.of());
        when(appleAudiobooks.search(
                "Iron Flame",
                "AUDIOBOOK",
                "TITLE"))
                .thenReturn(List.of());

        List<CatalogBookResult> results = service.search(
                "Iron Flame",
                null,
                "TITLE");

        assertEquals(2.0, results.getFirst().seriesNumber());
    }
    @Test
    void appleFailureDoesNotRemoveExistingBookResults() {
        when(openLibrary.search("Queen", null, "TITLE"))
                .thenReturn(List.of(result(
                        "Open Library",
                        "ol-1",
                        "Queen",
                        "PHYSICAL")));
        when(googleBooks.search("Queen", null, "TITLE"))
                .thenReturn(List.of(result(
                        "Google Books",
                        "gb-1",
                        "Queen",
                        "EBOOK")));
        when(googleBooks.search("Queen", "EBOOK", "TITLE"))
                .thenReturn(List.of());
        when(appleAudiobooks.search("Queen", "AUDIOBOOK", "TITLE"))
                .thenThrow(new RuntimeException("Apple unavailable"));

        List<CatalogBookResult> results =
                service.search("Queen", null, "TITLE");

        Set<String> formats = results.stream()
                .map(CatalogBookResult::format)
                .collect(Collectors.toSet());

        assertEquals(Set.of("PHYSICAL", "EBOOK"), formats);
    }

    @Test
    void explicitPhysicalSearchPrefersEnglishButKeepsForeignEditions() {
        CatalogBookResult turkish = resultWithLanguageAndDescription(
                "Open Library",
                "onyx-tr-explicit",
                "Onyx Storm",
                "Rebecca Yarros",
                "The Empyrean",
                "tr",
                null,
                "PHYSICAL");
        CatalogBookResult english = resultWithLanguageAndDescription(
                "Google Books",
                "onyx-en-explicit",
                "Onyx Storm",
                "Rebecca Yarros",
                null,
                "en",
                null,
                "PHYSICAL");
        CatalogBookResult deluxe = resultWithLanguageAndDescription(
                "Google Books",
                "onyx-deluxe",
                "Onyx Storm (Deluxe Limited Edition)",
                "Rebecca Yarros",
                "The Empyrean",
                "en",
                null,
                "PHYSICAL");

        CatalogBookResult wrongAuthor = resultWithLanguageAndDescription(
                "Open Library",
                "onyx-wrong-author",
                "Onyx Storm",
                "Prabhu Tl",
                null,
                "en",
                null,
                "PHYSICAL");

        when(openLibrary.search("Onyx Storm", "PHYSICAL", "TITLE"))
                .thenReturn(List.of(wrongAuthor, turkish));
        when(googleBooks.search("Onyx Storm", "PHYSICAL", "TITLE"))
                .thenReturn(List.of(english, deluxe));

        List<CatalogBookResult> results =
                service.search("Onyx Storm", "PHYSICAL", "TITLE");

        assertEquals("onyx-en-explicit", results.getFirst().providerId());
        List<String> rankedIds = results.stream()
                .map(CatalogBookResult::providerId)
                .toList();
        assertTrue(rankedIds.indexOf("onyx-deluxe")
                < rankedIds.indexOf("onyx-wrong-author"));
        assertTrue(results.stream().anyMatch(result ->
                "onyx-tr-explicit".equals(result.providerId())));
    }
    @Test
    void explicitPhysicalSearchRejectsMislabeledEbookResults() {
        CatalogBookResult physical = result(
                "Open Library",
                "physical-queen",
                "Queen",
                "PHYSICAL");
        CatalogBookResult ebook = result(
                "Google Books",
                "ebook-queen",
                "Queen",
                "EBOOK");

        when(openLibrary.search("Queen", "PHYSICAL", "TITLE"))
                .thenReturn(List.of(physical));
        when(googleBooks.search("Queen", "PHYSICAL", "TITLE"))
                .thenReturn(List.of(ebook));

        List<CatalogBookResult> results =
                service.search("Queen", "PHYSICAL", "TITLE");

        assertEquals(1, results.size());
        assertEquals("PHYSICAL", results.getFirst().format());
        assertEquals("physical-queen", results.getFirst().providerId());
    }
    @Test
    void explicitPhysicalSearchDoesNotInvokeApple() {
        when(openLibrary.search("Queen", "PHYSICAL", "TITLE"))
                .thenReturn(List.of(result(
                        "Open Library",
                        "ol-1",
                        "Queen",
                        "PHYSICAL")));
        when(googleBooks.search("Queen", "PHYSICAL", "TITLE"))
                .thenReturn(List.of());

        service.search("Queen", "PHYSICAL", "TITLE");

        verify(appleAudiobooks, never())
                .search("Queen", "AUDIOBOOK", "TITLE");
    }

    private static CatalogBookResult result(
            String provider,
            String providerId,
            String title,
            String format) {
        return new CatalogBookResult(
                provider,
                providerId,
                title,
                null,
                List.of("Test Author"),
                List.of(),
                null,
                null,
                null,
                "PHYSICAL".equals(format) ? 300 : null,
                "AUDIOBOOK".equals(format) ? 36_000 : null,
                List.of(),
                null,
                "en",
                null,
                null,
                null,
                null,
                format.equals("AUDIOBOOK") ? "Audiobook" : null,
                format);
    }

    private static CatalogBookResult resultWithLanguageAndDescription(
            String provider,
            String providerId,
            String title,
            String author,
            String seriesName,
            String language,
            String description,
            String format) {
        return new CatalogBookResult(
                provider,
                providerId,
                title,
                null,
                List.of(author),
                List.of(),
                description,
                null,
                null,
                "PHYSICAL".equals(format) ? 300 : null,
                "AUDIOBOOK".equals(format) ? 36_000 : null,
                List.of(),
                null,
                language,
                null,
                null,
                seriesName,
                null,
                "AUDIOBOOK".equals(format) ? "Audiobook" : null,
                format);
    }
    private static CatalogBookResult resultWithMetadata(
            String provider,
            String providerId,
            String title,
            String author,
            String seriesName,
            String format) {
        return new CatalogBookResult(
                provider,
                providerId,
                title,
                null,
                List.of(author),
                List.of(),
                null,
                null,
                null,
                "PHYSICAL".equals(format) ? 300 : null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                seriesName,
                null,
                null,
                format);
    }
}
