package com.librarylane.services;

import com.librarylane.catalog.CatalogBookResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppleAudiobookEnrichmentTest {

    @Mock
    private OpenLibraryCatalogService openLibrary;

    @Mock
    private GoogleBooksCatalogService googleBooks;

    @Mock
    private WikidataCatalogService wikidata;

    @Mock
    private AppleAudiobookCatalogService appleAudiobooks;

    @Mock
    private SpotifyAudiobookMetadataService spotifyAudiobooks;

    private BookCatalogSearchService service;

    @BeforeEach
    void setUp() {
        service = new BookCatalogSearchService(
                openLibrary,
                googleBooks,
                wikidata,
                appleAudiobooks,
                spotifyAudiobooks);
    }

    @Test
    void selectedAudiobookBorrowsSeriesAndSynopsisWithoutLosingAudioData() {
        CatalogBookResult audiobook = new CatalogBookResult(
                "Apple Audiobooks",
                "apple-123",
                "Fourth Wing (Empyrean)",
                null,
                List.of("Rebecca Yarros"),
                List.of("Fantasy"),
                "Apple promotional description",
                null,
                "2023-05-02T07:00:00Z",
                null,
                72_000,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                "Audiobook",
                "AUDIOBOOK");

        CatalogBookResult work = new CatalogBookResult(
                "Open Library",
                "ol-456",
                "Fourth Wing",
                null,
                List.of("Rebecca Yarros"),
                List.of("Fantasy", "Romance"),
                "A clean work-level synopsis.",
                "Red Tower Books",
                "2023",
                500,
                null,
                List.of(),
                "https://example.test/fourth-wing.jpg",
                "en",
                null,
                "9781649374042",
                "Empyrean",
                1.0,
                null,
                "PHYSICAL");

        when(openLibrary.search("Fourth Wing", null, "TITLE"))
                .thenReturn(List.of(work));
        when(googleBooks.search("Fourth Wing", null, "TITLE"))
                .thenReturn(List.of());

        CatalogBookResult resolved = service.resolve(audiobook);

        assertEquals("Fourth Wing", resolved.title());
        assertEquals("Empyrean", resolved.seriesName());
        assertEquals(1.0, resolved.seriesNumber());
        assertEquals("A clean work-level synopsis.", resolved.description());
        assertEquals(72_000, resolved.audiobookLengthSeconds());
        assertEquals("AUDIOBOOK", resolved.format());
        assertEquals("apple-123", resolved.providerId());
    }

    @Test
    void selectedAudiobookKeepsCleanSeriesNameAndInfersBookNumber() {
        CatalogBookResult audiobook = new CatalogBookResult(
                "Apple Audiobooks",
                "apple-123",
                "Fourth Wing (Empyrean)",
                null,
                List.of("Rebecca Yarros"),
                List.of("Fantasy"),
                "Apple promotional description",
                null,
                "2023-05-02T07:00:00Z",
                null,
                72_000,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                "Audiobook",
                "AUDIOBOOK");

        CatalogBookResult noisySupplement = new CatalogBookResult(
                "Google Books",
                "google-456",
                "Fourth Wing",
                null,
                List.of("Rebecca Yarros"),
                List.of("Fantasy", "Romance"),
                "The first in Rebecca Yarros' Empyrean series.",
                "Red Tower Books",
                "2023",
                500,
                null,
                List.of(),
                null,
                "en",
                null,
                null,
                "Yarros Empyrean",
                null,
                null,
                "PHYSICAL");

        when(openLibrary.search("Fourth Wing", null, "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("Fourth Wing", null, "TITLE"))
                .thenReturn(List.of(noisySupplement));

        CatalogBookResult resolved = service.resolve(audiobook);

        assertEquals("Empyrean", resolved.seriesName());
        assertEquals(1.0, resolved.seriesNumber());
    }

    @Test
    void exactOpenLibraryWorkDateWinsWithoutBorrowingPrintPublisher() {
        CatalogBookResult audiobook = audiobook(
                "A Carnival Story",
                "A welcoming fantasy description.",
                "2017-01-31T08:00:00Z");
        CatalogBookResult italianEdition = work(
                "A Carnival Story",
                "Una descrizione molto completa.",
                "OMR Biblioteca Univ. Rizzoli",
                "2000",
                "it",
                null,
                null);
        CatalogBookResult englishEdition = work(
                "A Carnival Story",
                "An English work description.",
                "Flatiron Books",
                "2017",
                "en",
                null,
                null);

        when(openLibrary.search("A Carnival Story", null, "TITLE"))
                .thenReturn(List.of(italianEdition, englishEdition));
        when(googleBooks.search("A Carnival Story", null, "TITLE"))
                .thenReturn(List.of());

        CatalogBookResult resolved = service.resolve(audiobook);

        assertEquals("2017", resolved.publicationDate());
        assertNull(resolved.publisher());
        assertEquals("An English work description.", resolved.description());
    }

    @Test
    void originalWorkYearReplacesLaterAppleStorefrontEditionYear() {
        CatalogBookResult audiobook = audiobook(
                "An Older Space Adventure",
                "A classic journey into another world.",
                "2014-03-14T08:00:00Z");
        CatalogBookResult originalWork = work(
                "An Older Space Adventure",
                "A classic journey into another world.",
                "A Print Publisher",
                "1998",
                "en",
                null,
                null);

        when(openLibrary.search("An Older Space Adventure", null, "TITLE"))
                .thenReturn(List.of(originalWork));
        when(googleBooks.search("An Older Space Adventure", null, "TITLE"))
                .thenReturn(List.of());

        CatalogBookResult resolved = service.resolve(audiobook);

        assertEquals("1998", resolved.publicationDate());
        assertNull(resolved.publisher());
    }

    @Test
    void twoProviderConsensusOverridesOneAnomalousWorkYear() {
        CatalogBookResult audiobook = audiobook(
                "A Carnival Story",
                "A welcoming fantasy description.",
                "2017-01-31T08:00:00Z");
        CatalogBookResult anomalousOpenLibraryWork = catalogWork(
                "Open Library",
                "A Carnival Story",
                "A work description.",
                "A Print Publisher",
                "2000",
                "en",
                null,
                null);
        CatalogBookResult corroboratingGoogleEdition = catalogWork(
                "Google Books",
                "A Carnival Story",
                "A second description.",
                "Another Print Publisher",
                "2017-01-31",
                "en",
                null,
                null);

        when(openLibrary.search("A Carnival Story", null, "TITLE"))
                .thenReturn(List.of(anomalousOpenLibraryWork));
        when(googleBooks.search("A Carnival Story", null, "TITLE"))
                .thenReturn(List.of(corroboratingGoogleEdition));

        CatalogBookResult resolved = service.resolve(audiobook);

        assertEquals("2017", resolved.publicationDate());
        assertNull(resolved.publisher());
    }

    @Test
    void promotionalSeriesFragmentsAreRejectedInsteadOfDisplayed() {
        CatalogBookResult audiobook = audiobook(
                "A Sequel With Promotional Copy",
                "The beloved fairytale sensation is the sequel to the globally bestselling first story.",
                "2022-09-13T07:00:00Z");
        CatalogBookResult noisyWork = work(
                "A Sequel With Promotional Copy",
                "Promotional description.",
                "A Print Publisher",
                "2022",
                "en",
                "Worldwide Best-selling",
                2.0);

        when(openLibrary.search("A Sequel With Promotional Copy", null, "TITLE"))
                .thenReturn(List.of(noisyWork));
        when(googleBooks.search("A Sequel With Promotional Copy", null, "TITLE"))
                .thenReturn(List.of());

        CatalogBookResult resolved = service.resolve(audiobook);

        assertNull(resolved.seriesName());
        assertEquals(2.0, resolved.seriesNumber());
    }

    @Test
    void genericMarketingPhraseIsNotAcceptedAsSeriesName() {
        CatalogBookResult audiobook = audiobook(
                "A Broken Heart Story",
                "The launch of a new series filled with magic.",
                "2021-09-28T07:00:00Z");
        CatalogBookResult noisyWork = work(
                "A Broken Heart Story",
                "The launch of a new series filled with magic.",
                "Flatiron Books",
                "2021",
                "en",
                "A New",
                null);

        when(openLibrary.search("A Broken Heart Story", null, "TITLE"))
                .thenReturn(List.of(noisyWork));
        when(googleBooks.search("A Broken Heart Story", null, "TITLE"))
                .thenReturn(List.of());

        CatalogBookResult resolved = service.resolve(audiobook);

        assertNull(resolved.seriesName());
        assertNull(resolved.seriesNumber());
        assertEquals(List.of("Fantasy"), resolved.genres());
    }

    @Test
    void explicitSeriesSentenceProvidesSeriesNameAndNumber() {
        CatalogBookResult audiobook = audiobook(
                "The Final Adventure",
                "The third and final audiobook in the Carnival Tales series is a romantic fantasy.",
                "2019-05-07T07:00:00Z");

        when(openLibrary.search("The Final Adventure", null, "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("The Final Adventure", null, "TITLE"))
                .thenReturn(List.of());

        CatalogBookResult resolved = service.resolve(audiobook);

        assertEquals("Carnival Tales", resolved.seriesName());
        assertEquals(3.0, resolved.seriesNumber());
        assertEquals(List.of("Fantasy", "Romance"), resolved.genres());
    }

    @Test
    void promotionalPrefixBeforeNamedSeriesIsRemoved() {
        CatalogBookResult audiobook = audiobook(
                "The Enchanted Beginning",
                "The first book in the #1 worldwide bestselling "
                        + "Enchanted Kingdom series is a romantic fantasy.",
                "2021-09-28T07:00:00Z");

        when(openLibrary.search("The Enchanted Beginning", null, "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("The Enchanted Beginning", null, "TITLE"))
                .thenReturn(List.of());

        CatalogBookResult resolved = service.resolve(audiobook);

        assertEquals("Enchanted Kingdom", resolved.seriesName());
        assertEquals(1.0, resolved.seriesNumber());
    }

    @Test
    void independentWorkCatalogsOverrideFalseStorefrontBookNumber() {
        CatalogBookResult audiobook = new CatalogBookResult(
                "Apple Audiobooks",
                "apple-false-number",
                "The Storm Conclusion",
                null,
                List.of("A Writer"),
                List.of("Fantasy"),
                "A concluding adventure in the Stormbound series.",
                null,
                "2025-01-21T08:00:00Z",
                null,
                86_120,
                List.of("Primary Narrator", "Guest Narrator"),
                null,
                "en",
                null,
                null,
                "Stormbound",
                1.0,
                "Audiobook",
                "AUDIOBOOK");
        CatalogBookResult openLibraryWork = catalogWork(
                "Open Library",
                "The Storm Conclusion",
                "The third book in the Stormbound series.",
                "A Print Publisher",
                "2025",
                "en",
                "Stormbound",
                3.0);
        CatalogBookResult googleWork = catalogWork(
                "Google Books",
                "The Storm Conclusion",
                "Stormbound, book three.",
                "A Print Publisher",
                "2025",
                "en",
                "Stormbound",
                3.0);

        when(openLibrary.search("The Storm Conclusion", null, "TITLE"))
                .thenReturn(List.of(openLibraryWork));
        when(googleBooks.search("The Storm Conclusion", null, "TITLE"))
                .thenReturn(List.of(googleWork));

        CatalogBookResult resolved = service.resolve(audiobook);

        assertEquals("Stormbound", resolved.seriesName());
        assertEquals(3.0, resolved.seriesNumber());
        assertEquals(
                List.of("Primary Narrator", "Guest Narrator"),
                resolved.narrators());
        assertEquals(86_120, resolved.audiobookLengthSeconds());
    }

    @Test
    void legacySpotifyAlbumCanAppearWhenAppleHasNoMatchingEdition() {
        CatalogBookResult legacy = new CatalogBookResult(
                "Spotify Audiobook Albums",
                "album:legacy-fixture",
                "An Older Arena Story",
                null,
                List.of("A Writer"),
                List.of(),
                null,
                null,
                "2008",
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                "Unabridged",
                "AUDIOBOOK");

        when(appleAudiobooks.search(
                "An Older Arena Story",
                "AUDIOBOOK",
                "TITLE"))
                .thenReturn(List.of());
        when(spotifyAudiobooks.searchLegacyAlbums(
                "An Older Arena Story",
                "TITLE"))
                .thenReturn(List.of(legacy));

        List<CatalogBookResult> results = service.search(
                "An Older Arena Story",
                "AUDIOBOOK",
                "TITLE");

        assertEquals(1, results.size());
        assertEquals("album:legacy-fixture", results.getFirst().providerId());
        assertEquals("AUDIOBOOK", results.getFirst().format());
    }

    @Test
    void appleAndLegacySpotifyEditionsMergeIntoOneCompleteAudiobook() {
        CatalogBookResult apple = audiobook(
                "An Older Arena Story",
                "The Apple storefront edition.",
                "2018-10-30T07:00:00Z");
        CatalogBookResult legacy = new CatalogBookResult(
                "Spotify Audiobook Albums",
                "album:legacy-fixture",
                "An Older Arena Story",
                null,
                List.of("A Writer"),
                List.of(),
                null,
                "A Listening Publisher",
                "2008",
                null,
                30_600,
                List.of("An Excellent Narrator"),
                null,
                null,
                null,
                null,
                null,
                null,
                "Unabridged",
                "AUDIOBOOK");

        when(appleAudiobooks.search(
                "An Older Arena Story",
                "AUDIOBOOK",
                "TITLE"))
                .thenReturn(List.of(apple));
        when(spotifyAudiobooks.searchLegacyAlbums(
                "An Older Arena Story",
                "TITLE"))
                .thenReturn(List.of(legacy));

        List<CatalogBookResult> results = service.search(
                "An Older Arena Story",
                "AUDIOBOOK",
                "TITLE");

        assertEquals(1, results.size());
        CatalogBookResult merged = results.getFirst();
        assertEquals("AUDIOBOOK", merged.format());
        assertEquals(30_600, merged.audiobookLengthSeconds());
        assertEquals(
                List.of("An Excellent Narrator"),
                merged.narrators());
        assertTrue(merged.provider().contains("Apple Audiobooks"));
        assertTrue(merged.provider().contains("Spotify Audiobook Albums"));
    }

    @Test
    void editionQualifiedExactTitleRanksAboveAnalysisAndGuideResults() {
        CatalogBookResult analysis = audiobook(
                "The Arena Games by A Famous Writer (Book Analysis)",
                "An analysis of the bestselling novel.",
                "2022-01-01T08:00:00Z");
        CatalogBookResult companion = audiobook(
                "The Arena Games Companion",
                "An unofficial companion.",
                "2020-01-01T08:00:00Z");
        CatalogBookResult specialEdition = audiobook(
                "The Arena Games: Special Edition",
                "The complete audiobook.",
                "2018-10-30T07:00:00Z");

        when(appleAudiobooks.search(
                "The Arena Games",
                "AUDIOBOOK",
                "TITLE"))
                .thenReturn(List.of(analysis, companion, specialEdition));

        List<CatalogBookResult> results = service.search(
                "The Arena Games",
                "AUDIOBOOK",
                "TITLE");

        assertEquals("The Arena Games: Special Edition", results.getFirst().title());
    }

    @Test
    void editionSuffixIsRemovedBeforeOriginalWorkEnrichment() {
        CatalogBookResult audiobook = audiobook(
                "The Arena Games: Special Edition",
                "The complete audiobook.",
                "2018-10-30T07:00:00Z");
        CatalogBookResult originalWork = work(
                "The Arena Games",
                "The original work description.",
                "A Print Publisher",
                "2008",
                "en",
                "The Arena Games",
                1.0);

        when(openLibrary.search("The Arena Games", null, "TITLE"))
                .thenReturn(List.of(originalWork));
        when(googleBooks.search("The Arena Games", null, "TITLE"))
                .thenReturn(List.of());

        CatalogBookResult resolved = service.resolve(audiobook);

        assertEquals("The Arena Games", resolved.title());
        assertEquals("2008", resolved.publicationDate());
        assertEquals("The Arena Games", resolved.seriesName());
        assertEquals(1.0, resolved.seriesNumber());
    }

    private static CatalogBookResult audiobook(
            String title,
            String description,
            String publicationDate) {
        return new CatalogBookResult(
                "Apple Audiobooks",
                "apple-fixture",
                title,
                null,
                List.of("A Writer"),
                List.of("Fiction"),
                description,
                null,
                publicationDate,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                "Audiobook",
                "AUDIOBOOK");
    }

    private static CatalogBookResult work(
            String title,
            String description,
            String publisher,
            String publicationDate,
            String language,
            String seriesName,
            Double seriesNumber) {
        return catalogWork(
                "Open Library",
                title,
                description,
                publisher,
                publicationDate,
                language,
                seriesName,
                seriesNumber);
    }

    private static CatalogBookResult catalogWork(
            String provider,
            String title,
            String description,
            String publisher,
            String publicationDate,
            String language,
            String seriesName,
            Double seriesNumber) {
        return new CatalogBookResult(
                provider,
                "work-fixture-" + provider + "-" + language,
                title,
                null,
                List.of("A Writer"),
                List.of("Fiction"),
                description,
                publisher,
                publicationDate,
                400,
                null,
                List.of(),
                null,
                language,
                null,
                null,
                seriesName,
                seriesNumber,
                null,
                "PHYSICAL");
    }
}
