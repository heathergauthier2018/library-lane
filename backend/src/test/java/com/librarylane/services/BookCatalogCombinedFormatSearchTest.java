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
                        "de",
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
    void explicitAudiobookSearchRanksEnglishBeforeRicherForeignEdition() {
        CatalogBookResult foreign = resultWithLanguageAndDescription(
                "Apple Audiobooks",
                "great-foreign-audio",
                "Great and Precious Things",
                "Rebecca Yarros",
                "Legacy",
                "de",
                "A richly described foreign-language production.",
                "AUDIOBOOK");
        CatalogBookResult english = resultWithLanguageAndDescription(
                "Apple Audiobooks",
                "great-english-audio",
                "Great and Precious Things",
                "Rebecca Yarros",
                null,
                "en",
                null,
                "AUDIOBOOK");

        when(appleAudiobooks.search(
                "Great and Precious Things", "AUDIOBOOK", "TITLE"))
                .thenReturn(List.of(foreign, english));

        List<CatalogBookResult> results = service.search(
                "Great and Precious Things", "AUDIOBOOK", "TITLE");

        assertEquals("great-english-audio", results.getFirst().providerId());
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

    @Test
    void partialTitleDiscoveryKeepsDistinctLegitimatePrefixMatches() {
        CatalogBookResult thornsPrint = resultWithMetadata(
                "Open Library", "thorns-print",
                "A Court of Thorns and Roses", "Sarah J. Maas", null,
                "PHYSICAL");
        CatalogBookResult thornsEbook = resultWithMetadata(
                "Google Books", "thorns-ebook",
                "A Court of Thorns and Roses", "Sarah J. Maas", null,
                "EBOOK");
        CatalogBookResult mist = resultWithMetadata(
                "Open Library", "mist-print",
                "A Court of Mist and Fury", "Sarah J. Maas", null,
                "PHYSICAL");
        CatalogBookResult wings = resultWithMetadata(
                "Apple Audiobooks", "wings-audio",
                "A Court of Wings and Ruin", "Sarah J. Maas",
                "Court of Thorns and Roses", "AUDIOBOOK");
        CatalogBookResult unrelated = resultWithMetadata(
                "Open Library", "inquiry-print",
                "A Court of Inquiry", "Grace Richmond", null,
                "PHYSICAL");

        when(openLibrary.search("a court of", null, "TITLE"))
                .thenReturn(List.of(thornsPrint, mist, unrelated));
        when(googleBooks.search("a court of", null, "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("a court of", "EBOOK", "TITLE"))
                .thenReturn(List.of(thornsEbook));
        when(appleAudiobooks.search("a court of", "AUDIOBOOK", "TITLE"))
                .thenReturn(List.of(wings));

        List<CatalogBookResult> results = service.search(
                "a court of", null, "TITLE");

        assertEquals(
                List.of(
                        "A Court of Mist and Fury",
                        "A Court of Thorns and Roses",
                        "A Court of Wings and Ruin"),
                results.stream().limit(3).map(CatalogBookResult::title).toList());
        assertEquals(1, results.stream().filter(result ->
                result.title().equals("A Court of Thorns and Roses")).count());
        assertTrue(results.stream().anyMatch(result ->
                result.providerId().equals("inquiry-print")));
    }

    @Test
    void partialStandaloneTitlePreservesFormatsDespiteNoisyDiscoveryResults() {
        CatalogBookResult physical = resultWithMetadata(
                "Open Library", "winn-physical",
                "Because of Winn-Dixie", "Kate DiCamillo", null,
                "PHYSICAL");
        CatalogBookResult teacherGuide = resultWithMetadata(
                "Open Library", "winn-teacher-guide",
                "Teacher's Guide Classroom Worksheets Because of Winn-Dixie",
                "Guide Author", null, "PHYSICAL");
        CatalogBookResult ebook = resultWithMetadata(
                "Google Books", "winn-ebook",
                "Because of Winn-Dixie", "Kate DiCamillo", null,
                "EBOOK");
        CatalogBookResult audiobook = resultWithMetadata(
                "Apple Audiobooks", "winn-audio",
                "Because of Winn-Dixie", "Kate DiCamillo", null,
                "AUDIOBOOK");

        when(openLibrary.search("because of winn", null, "TITLE"))
                .thenReturn(List.of(teacherGuide, physical));
        when(googleBooks.search("because of winn", null, "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("because of winn", "EBOOK", "TITLE"))
                .thenReturn(List.of(ebook));
        when(appleAudiobooks.search(
                "because of winn", "AUDIOBOOK", "TITLE"))
                .thenReturn(List.of(audiobook));

        List<CatalogBookResult> results = service.search(
                "because of winn", null, "TITLE");

        assertEquals(
                List.of("PHYSICAL", "EBOOK", "AUDIOBOOK"),
                results.stream()
                        .limit(3)
                        .map(CatalogBookResult::format)
                        .toList());
        assertTrue(results.stream().limit(3).allMatch(result ->
                result.title().equals("Because of Winn-Dixie")));
        assertFalse(results.stream().anyMatch(result ->
                result.providerId().equals("winn-teacher-guide")));
    }

    @Test
    void partialTitleKeepsAudiobookWhenReaderOmitsLeadingTitleWords() {
        CatalogBookResult physical = resultWithLanguageAndDescription(
                "Open Library", "daughter-physical",
                "I Am Not Your Perfect Mexican Daughter",
                "Erika L. Sanchez", null, "eng", null, "PHYSICAL");
        CatalogBookResult alternatePhysical = resultWithMetadata(
                "Open Library", "daughter-physical-alternate",
                "I Am Not Your Perfect Mexican Daughter",
                "Erika L. Sanchez", null, "PHYSICAL");
        CatalogBookResult audiobook = resultWithLanguageAndDescription(
                "Apple Audiobooks", "daughter-audio",
                "I Am Not Your Perfect Mexican Daughter (Unabridged)",
                "Erika L. Sanchez", null, null, null, "AUDIOBOOK");

        when(openLibrary.search(
                "not your perfect mexican daughter", null, "TITLE"))
                .thenReturn(List.of(physical, alternatePhysical));
        when(googleBooks.search(
                "not your perfect mexican daughter", null, "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search(
                "not your perfect mexican daughter", "EBOOK", "TITLE"))
                .thenReturn(List.of());
        when(appleAudiobooks.search(
                "not your perfect mexican daughter", "AUDIOBOOK", "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search(
                "I Am Not Your Perfect Mexican Daughter", "EBOOK", "TITLE"))
                .thenReturn(List.of());
        when(appleAudiobooks.search(
                "I Am Not Your Perfect Mexican Daughter",
                "AUDIOBOOK",
                "TITLE"))
                .thenReturn(List.of(audiobook));

        List<CatalogBookResult> results = service.search(
                "not your perfect mexican daughter", null, "TITLE");

        assertEquals(
                List.of("PHYSICAL", "AUDIOBOOK"),
                results.stream()
                        .limit(2)
                        .map(CatalogBookResult::format)
                        .toList());
        assertTrue(results.stream().limit(2).allMatch(result ->
                result.title().contains("Not Your Perfect Mexican Daughter")));
    }

    @Test
    void exactAudiobookAuthorGuidesCanonicalPhysicalLead() {
        CatalogBookResult adaptation = resultWithMetadata(
                "Open Library", "anne-adaptation",
                "Anne of Green Gables", "Adaptation Staff", null,
                "PHYSICAL");
        CatalogBookResult original = resultWithMetadata(
                "Open Library", "anne-original",
                "Anne of Green Gables", "Lucy Maud Montgomery", null,
                "PHYSICAL");
        CatalogBookResult audiobook = resultWithLanguageAndDescription(
                "Apple Audiobooks", "anne-audio",
                "Anne of Green Gables", "L. M. Montgomery", null,
                null, null, "AUDIOBOOK");

        when(openLibrary.search("anne of green", null, "TITLE"))
                .thenReturn(List.of(adaptation, original));
        when(googleBooks.search("anne of green", null, "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("anne of green", "EBOOK", "TITLE"))
                .thenReturn(List.of());
        when(appleAudiobooks.search(
                "anne of green", "AUDIOBOOK", "TITLE"))
                .thenReturn(List.of(audiobook));

        List<CatalogBookResult> results = service.search(
                "anne of green", null, "TITLE");

        assertEquals("anne-original", results.getFirst().providerId());
        assertEquals("anne-audio", results.get(1).providerId());
    }

    @Test
    void companionJournalAndDiaryDoNotDisplaceCanonicalAnneLeads() {
        CatalogBookResult journal = resultWithMetadata(
                "Open Library", "anne-journal",
                "Anne of Green Gables Journal", "Lucy Maud Montgomery",
                null, "PHYSICAL");
        CatalogBookResult diary = resultWithMetadata(
                "Open Library", "anne-diary",
                "The Anne of Green Gables Diary", "Lucy Maud Montgomery",
                null, "EBOOK");
        CatalogBookResult collection = resultWithMetadata(
                "Open Library", "anne-eight-novels",
                "The Anne of Green Gables novels [8 novels]",
                "Lucy Maud Montgomery", null, "EBOOK");
        CatalogBookResult adaptation = resultWithMetadata(
                "Open Library", "anne-adaptation-ebook",
                "Anne of Green Gables", "Adaptation Writer",
                null, "EBOOK");
        CatalogBookResult original = resultWithMetadata(
                "Open Library", "anne-original",
                "Anne of Green Gables", "Lucy Maud Montgomery",
                null, "PHYSICAL");
        CatalogBookResult audiobook = resultWithLanguageAndDescription(
                "Apple Audiobooks", "anne-audio",
                "Anne of Green Gables", "L. M. Montgomery", null,
                null, null, "AUDIOBOOK");

        when(openLibrary.search("anne of green", null, "TITLE"))
                .thenReturn(List.of(
                        journal,
                        diary,
                        collection,
                        adaptation,
                        original));
        when(googleBooks.search("anne of green", null, "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("anne of green", "EBOOK", "TITLE"))
                .thenReturn(List.of());
        when(appleAudiobooks.search(
                "anne of green", "AUDIOBOOK", "TITLE"))
                .thenReturn(List.of(audiobook));

        List<CatalogBookResult> results = service.search(
                "anne of green", null, "TITLE");

        assertEquals(
                List.of("anne-original", "anne-audio"),
                results.stream()
                        .limit(2)
                        .map(CatalogBookResult::providerId)
                        .toList());
    }

    @Test
    void exactAcotarSearchRejectsBundlesForeignAndDramatizedLeads() {
        CatalogBookResult bundle = resultWithLanguageAndDescription(
                "Open Library", "acotar-bundle",
                "Throne of Glass / A Court of Thorns and Roses",
                "Sarah J. Maas", null, "eng", null, "PHYSICAL");
        CatalogBookResult physical = resultWithLanguageAndDescription(
                "Open Library", "acotar-physical",
                "A Court of Thorns and Roses",
                "Sarah J. Maas", null, "eng", null, "PHYSICAL");
        CatalogBookResult workbook = resultWithLanguageAndDescription(
                "Open Library", "acotar-workbook",
                "Workbook of A Court of Thorns and Roses",
                "Houseam Elmassi", null, "eng", null, "PHYSICAL");
        CatalogBookResult germanEbook = resultWithLanguageAndDescription(
                "Open Library", "acotar-ebook-de",
                "A Court of Thorns and Roses",
                "Sarah J. Maas", null, "ger", null, "EBOOK");
        CatalogBookResult dramatized = new CatalogBookResult(
                "Apple Audiobooks", "acotar-drama",
                "A Court of Thorns and Roses (1 of 2) [Dramatized Adaptation]",
                null, List.of("Sarah J. Maas"), List.of(), null,
                "Graphic Audio LLC", null, null, 20_000, List.of(),
                "https://example.com/drama.jpg", null, null, null,
                "Court of Thorns and Roses", 1.0,
                "Dramatized Adaptation - Part 1 of 2", "AUDIOBOOK");
        CatalogBookResult germanAudio = resultWithLanguageAndDescription(
                "Apple Audiobooks", "acotar-audio-de",
                "Dornen und Rosen: A Court of Thorns and Roses",
                "Sarah J. Maas", null, null, null, "AUDIOBOOK");
        CatalogBookResult englishAudio = resultWithLanguageAndDescription(
                "Apple Audiobooks", "acotar-audio-en",
                "A Court of Thorns and Roses (10th Anniversary Recording) (Court of Thorns and Roses)",
                "Sarah J. Maas", "Court of Thorns and Roses", null, null,
                "AUDIOBOOK");

        when(openLibrary.search(
                "a court of thorns and roses", null, "TITLE"))
                .thenReturn(List.of(workbook, bundle, physical, germanEbook));
        when(googleBooks.search(
                "a court of thorns and roses", null, "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search(
                "a court of thorns and roses", "EBOOK", "TITLE"))
                .thenReturn(List.of());
        when(appleAudiobooks.search(
                "a court of thorns and roses", "AUDIOBOOK", "TITLE"))
                .thenReturn(List.of(dramatized, germanAudio, englishAudio));

        List<CatalogBookResult> results = service.search(
                "a court of thorns and roses", null, "TITLE");

        assertEquals(
                List.of("acotar-physical", "acotar-audio-en"),
                results.stream().limit(2).map(CatalogBookResult::providerId).toList());
        assertFalse(results.stream().anyMatch(result ->
                result.providerId().equals("acotar-bundle")
                        || result.providerId().equals("acotar-workbook")
                        || result.providerId().equals("acotar-drama")));
        assertFalse(results.stream().limit(3).anyMatch(result ->
                "ger".equals(result.language())
                        || result.providerId().equals("acotar-audio-de")));
    }

    @Test
    void exactTitleRejectsMislabeledAudioCompanionsAndCoverlessPrint() {
        CatalogBookResult mislabeledGraphicAudio = new CatalogBookResult(
                "Open Library", "acotar-graphic-audio-physical",
                "A Court of Thorns and Roses - Part 2",
                null, List.of("Sarah J. Maas"), List.of(), null,
                "Graphic Audio LLC", "2022", null, null, List.of(),
                "https://example.com/graphic-audio.jpg", "eng",
                "1685082777", "9781685082772", null, null,
                "Book catalog record", "PHYSICAL");
        CatalogBookResult coverlessPhysical = new CatalogBookResult(
                "Open Library", "acotar-coverless-physical",
                "A Court of Thorns and Roses",
                null, List.of(), List.of(), null,
                "Perfection Learning Corporation", "2019", null, null,
                List.of(), null, null, "1663616574", "9781663616579",
                null, null, "Book catalog record", "PHYSICAL");
        CatalogBookResult standardPhysical = new CatalogBookResult(
                "Google Books", "acotar-standard-physical",
                "A Court of Thorns and Roses",
                null, List.of("Sarah J. Maas"), List.of("Fantasy"),
                "Canonical story description.", "Bloomsbury", "2015",
                432, null, List.of(),
                "https://example.com/acotar-standard.jpg", "en",
                "1619634449", "9781619634442",
                "A Court of Thorns and Roses", 1.0,
                "Hardcover", "PHYSICAL");
        CatalogBookResult standardEbook = new CatalogBookResult(
                "Google Books", "acotar-standard-ebook",
                "A Court of Thorns and Roses",
                null, List.of("Sarah J. Maas"), List.of("Fantasy"),
                "Canonical story description.", "Bloomsbury", "2015",
                432, null, List.of(),
                "https://example.com/acotar-ebook.jpg", "en",
                null, "9781619634459",
                "A Court of Thorns and Roses", 1.0,
                "Ebook", "EBOOK");
        CatalogBookResult anniversaryAudio = new CatalogBookResult(
                "Apple Audiobooks", "acotar-anniversary-audio",
                "A Court of Thorns and Roses (10th Anniversary Recording)",
                null, List.of("Sarah J. Maas"), List.of("Fantasy"),
                "Anniversary recording.", "Recorded Books", "2025",
                null, 57_600, List.of("Elizabeth Evans"),
                "https://example.com/acotar-audio.jpg", "en",
                null, null, "A Court of Thorns and Roses", 1.0,
                "Unabridged Audiobook", "AUDIOBOOK");
        CatalogBookResult standardAudio = new CatalogBookResult(
                "Apple Audiobooks", "acotar-standard-audio",
                "A Court of Thorns and Roses (Court of Thorns and Roses)",
                null, List.of("Sarah J. Maas"), List.of("Fantasy"),
                "Canonical story description.", "Recorded Books", "2015",
                null, null, List.of(),
                "https://example.com/acotar-standard-audio.jpg", null,
                null, null, null, null,
                "Audiobook", "AUDIOBOOK");
        CatalogBookResult companion = new CatalogBookResult(
                "Open Library", "acotar-conversations",
                "Conversations on A Court of Thorns and Roses by Sarah J. Maas",
                null, List.of("daily Books"), List.of(), null,
                "CreateSpace", "2016", 66, null, List.of(),
                "https://example.com/conversations.jpg", "eng",
                "1540811441", "9781540811448", null, null,
                "Paperback", "PHYSICAL");

        when(openLibrary.search(
                "A Court of Thorns and Roses", null, "TITLE"))
                .thenReturn(List.of(
                        mislabeledGraphicAudio,
                        coverlessPhysical,
                        companion));
        when(googleBooks.search(
                "A Court of Thorns and Roses", null, "TITLE"))
                .thenReturn(List.of(standardPhysical));
        when(googleBooks.search(
                "A Court of Thorns and Roses", "EBOOK", "TITLE"))
                .thenReturn(List.of(standardEbook));
        when(appleAudiobooks.search(
                "A Court of Thorns and Roses", "AUDIOBOOK", "TITLE"))
                .thenReturn(List.of(anniversaryAudio, standardAudio));

        List<CatalogBookResult> results = service.search(
                "A Court of Thorns and Roses", null, "TITLE");

        assertEquals(
                List.of(
                        "acotar-standard-physical",
                        "acotar-standard-ebook",
                        "acotar-standard-audio"),
                results.stream()
                        .limit(3)
                        .map(CatalogBookResult::providerId)
                        .toList());
        assertTrue(results.stream().allMatch(result ->
                result.coverImageUrl() != null
                        && !result.coverImageUrl().isBlank()));
        assertFalse(results.stream().anyMatch(result ->
                result.providerId().equals("acotar-graphic-audio-physical")
                        || result.providerId().equals("acotar-coverless-physical")
                        || result.providerId().equals("acotar-conversations")));
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
