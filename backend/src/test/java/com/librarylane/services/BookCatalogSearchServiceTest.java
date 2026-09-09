package com.librarylane.services;

import com.librarylane.catalog.CatalogBookResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookCatalogSearchServiceTest {

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
                appleAudiobooks
        );
    }

    @Test
    void physicalSearchKeepsExistingProvidersAndNeverCallsApple() {
        when(openLibrary.search("Wake", "PHYSICAL", "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("Wake", "PHYSICAL", "TITLE"))
                .thenReturn(List.of());

        service.search("Wake", "PHYSICAL", "TITLE");

        verify(openLibrary).search("Wake", "PHYSICAL", "TITLE");
        verify(googleBooks).search("Wake", "PHYSICAL", "TITLE");
        verifyNoInteractions(appleAudiobooks);
    }

    @Test
    void ebookSearchKeepsExistingProvidersAndNeverCallsApple() {
        when(openLibrary.search("Wake", "EBOOK", "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("Wake", "EBOOK", "TITLE"))
                .thenReturn(List.of());

        service.search("Wake", "EBOOK", "TITLE");

        verify(openLibrary).search("Wake", "EBOOK", "TITLE");
        verify(googleBooks).search("Wake", "EBOOK", "TITLE");
        verifyNoInteractions(appleAudiobooks);
    }

    @Test
    void audiobookSearchCallsOnlyApple() {
        CatalogBookResult audiobook = audiobookResult();
        when(appleAudiobooks.search("Fourth Wing", "AUDIOBOOK", "TITLE"))
                .thenReturn(List.of(audiobook));

        List<CatalogBookResult> results =
                service.search("Fourth Wing", "AUDIOBOOK", "TITLE");

        assertEquals(1, results.size());
        assertEquals("AUDIOBOOK", results.getFirst().format());
        assertEquals(72000, results.getFirst().audiobookLengthSeconds());
        verify(appleAudiobooks)
                .search("Fourth Wing", "AUDIOBOOK", "TITLE");
        verifyNoInteractions(openLibrary, googleBooks, wikidata);
    }

    @Test
    void exactIsbnEditionCanFillAMissingPhysicalPageCount() {
        CatalogBookResult selected = new CatalogBookResult(
                "GOOGLE_BOOKS",
                "google-fourth-wing",
                "Fourth Wing",
                null,
                List.of("Rebecca Yarros"),
                List.of("Fantasy"),
                "A fantasy novel.",
                "Entangled: Red Tower Books",
                "2023",
                null,
                null,
                List.of(),
                "https://example.com/fourth-wing.jpg",
                "en",
                null,
                "9781649374042",
                "The Empyrean",
                1.0,
                "Physical edition",
                "PHYSICAL");

        when(openLibrary.findWorkByIsbn("9781649374042"))
                .thenReturn(null);
        when(openLibrary.findPageCountByIsbn("9781649374042"))
                .thenReturn(517);
        when(googleBooks.search(
                "Fourth Wing",
                "PHYSICAL",
                "TITLE"))
                .thenReturn(List.of());

        CatalogBookResult resolved = service.resolve(selected);

        assertEquals(517, resolved.pageCount());
        assertEquals("9781649374042", resolved.isbn13());
        assertEquals("PHYSICAL", resolved.format());
    }
    @Test
    void numberedTitleInfersSeriesNameAndBookNumberWithoutHardCoding() {
        CatalogBookResult numberedTitle = printResult(
                "Magic Tree House 1: Valley of the Dinosaurs",
                "Mary Pope Osborne",
                "A time-travel adventure.",
                null,
                null);
        when(openLibrary.search(
                "Magic Tree House 1: Valley of the Dinosaurs",
                "PHYSICAL",
                "TITLE")).thenReturn(List.of());
        when(googleBooks.search(
                "Magic Tree House 1: Valley of the Dinosaurs",
                "PHYSICAL",
                "TITLE")).thenReturn(List.of(numberedTitle));

        CatalogBookResult result = service.search(
                "Magic Tree House 1: Valley of the Dinosaurs",
                "PHYSICAL",
                "TITLE").getFirst();

        assertEquals("Magic Tree House", result.seriesName());
        assertEquals(1.0, result.seriesNumber());
    }

    @Test
    void strongerTrilogyEvidenceReplacesSelfTitledSeriesLabel() {
        CatalogBookResult selfTitledSeries = printResult(
                "Wake",
                "Lisa McMann",
                "The first book in the Wake trilogy follows Janie and her dreams.",
                "Wake",
                1.0);
        when(openLibrary.search("Wake", "PHYSICAL", "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("Wake", "PHYSICAL", "TITLE"))
                .thenReturn(List.of(selfTitledSeries));

        CatalogBookResult result =
                service.search("Wake", "PHYSICAL", "TITLE").getFirst();

        assertEquals("Wake Trilogy", result.seriesName());
        assertEquals(1.0, result.seriesNumber());
    }

    @Test
    void numericOrdinalDescriptionSuppliesMissingSeriesNumber() {
        CatalogBookResult audiobook = new CatalogBookResult(
                "Apple Audiobooks",
                "apple-fifth",
                "Tell Me to Fight (Tell Me)",
                null,
                List.of("Charlotte Byrd"),
                List.of("Romance"),
                "Dive into the dangerous 5th book of the addictive Tell Me series.",
                null,
                "2019",
                null,
                13_380,
                List.of("Jenny K", "Russell Newton"),
                null,
                "en",
                null,
                null,
                "Tell Me",
                null,
                "Audiobook",
                "AUDIOBOOK");
        when(appleAudiobooks.search(
                "Tell Me to Fight",
                "AUDIOBOOK",
                "TITLE")).thenReturn(List.of(audiobook));

        CatalogBookResult result = service.search(
                "Tell Me to Fight",
                "AUDIOBOOK",
                "TITLE").getFirst();

        assertEquals("Tell Me", result.seriesName());
        assertEquals(5.0, result.seriesNumber());
    }

    @Test
    void initialsAndExpandedAuthorNameResolveTheSameAudiobookWork() {
        CatalogBookResult selected = new CatalogBookResult(
                "Apple Audiobooks",
                "apple-anne",
                "Anne of Green Gables",
                null,
                List.of("L. M. Montgomery"),
                List.of("Young Adult"),
                "Anne arrives at Green Gables.",
                null,
                "1935",
                null,
                37_680,
                List.of("Kate Burton"),
                null,
                "en",
                null,
                null,
                "Anne of Green Gables",
                null,
                "Audiobook",
                "AUDIOBOOK");
        CatalogBookResult work = printResult(
                "Anne of Green Gables",
                "Lucy Maud Montgomery",
                "The first novel in the Anne of Green Gables series.",
                "Anne of Green Gables",
                1.0);
        work = new CatalogBookResult(
                work.provider(),
                work.providerId(),
                work.title(),
                work.subtitle(),
                work.authors(),
                work.genres(),
                work.description(),
                work.publisher(),
                "1908",
                work.pageCount(),
                work.audiobookLengthSeconds(),
                work.narrators(),
                work.coverImageUrl(),
                work.language(),
                work.isbn10(),
                work.isbn13(),
                work.seriesName(),
                work.seriesNumber(),
                work.editionFormat(),
                work.format());
        when(openLibrary.search(
                "Anne of Green Gables",
                null,
                "TITLE")).thenReturn(List.of(work));
        when(googleBooks.search(
                "Anne of Green Gables",
                null,
                "TITLE")).thenReturn(List.of());

        CatalogBookResult result = service.resolve(selected);

        assertEquals("1908", result.publicationDate());
        assertEquals(1.0, result.seriesNumber());
    }

    @Test
    void biographyAndMemoirGenresArePreserved() {
        CatalogBookResult audiobook = new CatalogBookResult(
                "Apple Audiobooks",
                "apple-blue",
                "Blue",
                null,
                List.of("Steve Aoki", "Daniel Paisner"),
                List.of("Biographies & Memoirs"),
                "A memoir recounting the life of a musician.",
                null,
                "2019",
                null,
                22_800,
                List.of("Greg Chun", "Steve Aoki"),
                null,
                "en",
                null,
                null,
                null,
                null,
                "Audiobook",
                "AUDIOBOOK");
        when(appleAudiobooks.search("Blue", "AUDIOBOOK", "TITLE"))
                .thenReturn(List.of(audiobook));

        CatalogBookResult result =
                service.search("Blue", "AUDIOBOOK", "TITLE").getFirst();

        assertEquals(List.of("Biographies & Memoirs"), result.genres());
    }

    @Test
    void descriptivePhraseDoesNotBecomeAStandaloneSeries() {
        CatalogBookResult audiobook = new CatalogBookResult(
                "Apple Audiobooks",
                "apple-verity",
                "Verity",
                null,
                List.of("Colleen Hoover"),
                List.of("Thriller"),
                "The first book in a successful series of suspenseful releases.",
                null,
                "2018",
                null,
                28_080,
                List.of("Vanessa Johansson"),
                null,
                "en",
                null,
                null,
                null,
                null,
                "Audiobook",
                "AUDIOBOOK");
        when(appleAudiobooks.search("Verity", "AUDIOBOOK", "TITLE"))
                .thenReturn(List.of(audiobook));

        CatalogBookResult result =
                service.search("Verity", "AUDIOBOOK", "TITLE").getFirst();

        assertEquals(null, result.seriesName());
        assertEquals(null, result.seriesNumber());
    }

    @Test
    void singleNovelSearchExcludesMultiBookBundles() {
        CatalogBookResult novel = printResult(
                "Caraval",
                "Stephanie Garber",
                "A fantasy novel.",
                "Caraval",
                1.0);
        CatalogBookResult bundle = printResult(
                "Caraval Holiday Collection",
                "Stephanie Garber",
                "A boxed collection of multiple books.",
                "Caraval",
                null);
        when(openLibrary.search("Caraval", "PHYSICAL", "TITLE"))
                .thenReturn(List.of(bundle));
        when(googleBooks.search("Caraval", "PHYSICAL", "TITLE"))
                .thenReturn(List.of(novel));

        List<CatalogBookResult> results =
                service.search("Caraval", "PHYSICAL", "TITLE");

        assertEquals(1, results.size());
        assertEquals("Caraval", results.getFirst().title());
    }

    @Test
    void decoratedAndPlainAudiobookTitlesMergeIntoOneWork() {
        CatalogBookResult plain = audiobookResult(
                "Caraval",
                "apple-plain",
                "Caraval",
                1.0);
        CatalogBookResult decorated = audiobookResult(
                "Caraval (Caraval 1)",
                "apple-decorated",
                null,
                null);
        when(appleAudiobooks.search("Caraval", "AUDIOBOOK", "TITLE"))
                .thenReturn(List.of(plain, decorated));

        List<CatalogBookResult> results =
                service.search("Caraval", "AUDIOBOOK", "TITLE");

        assertEquals(1, results.size());
        assertEquals("Caraval", results.getFirst().seriesName());
        assertEquals(1.0, results.getFirst().seriesNumber());
    }

    @Test
    void omittedOpeningWordsDoNotBuryTheMatchingAudiobook() {
        CatalogBookResult print = printResult(
                "I Am Not Your Perfect Mexican Daughter",
                "Erika L. Sánchez",
                "A coming-of-age novel.",
                null,
                null);
        CatalogBookResult audio = new CatalogBookResult(
                "Apple Audiobooks",
                "apple-mexican-daughter",
                "I Am Not Your Perfect Mexican Daughter",
                null,
                List.of("Erika L. Sánchez"),
                List.of("Young Adult"),
                "An unabridged recording.",
                null,
                "2017",
                null,
                34_800,
                List.of("Kyla Garcia"),
                null,
                "en",
                null,
                null,
                null,
                null,
                "Unabridged Audiobook",
                "AUDIOBOOK");
        when(openLibrary.search(
                "not your perfect mexican daughter",
                null,
                "TITLE")).thenReturn(List.of(print));
        when(googleBooks.search(
                "not your perfect mexican daughter",
                null,
                "TITLE")).thenReturn(List.of());
        when(googleBooks.search(
                "not your perfect mexican daughter",
                "EBOOK",
                "TITLE")).thenReturn(List.of());
        CatalogBookResult ebook = new CatalogBookResult(
                "Google Books",
                "ebook-mexican-daughter",
                "I Am Not Your Perfect Mexican Daughter",
                null,
                List.of("Erika L. Sánchez"),
                List.of("Young Adult"),
                "A coming-of-age novel.",
                null,
                "2017",
                null,
                null,
                List.of(),
                null,
                "en",
                null,
                null,
                null,
                null,
                "E-book",
                "EBOOK");
        when(googleBooks.search(
                "I Am Not Your Perfect Mexican Daughter",
                "EBOOK",
                "TITLE")).thenReturn(List.of(ebook));
        when(appleAudiobooks.search(
                "not your perfect mexican daughter",
                "AUDIOBOOK",
                "TITLE")).thenReturn(List.of(audio));

        List<CatalogBookResult> results = service.search(
                "not your perfect mexican daughter",
                null,
                "TITLE");

        assertEquals(
                List.of("PHYSICAL", "EBOOK", "AUDIOBOOK"),
                results.stream().limit(3)
                        .map(CatalogBookResult::format)
                        .toList());
    }

    @Test
    void resolvingAppleAudiobookUsesProtectedWorkMetadataLookups() {
        CatalogBookResult selected = audiobookResult();
        when(openLibrary.search("Fourth Wing", null, "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("Fourth Wing", null, "TITLE"))
                .thenReturn(List.of());

        CatalogBookResult resolved = service.resolve(selected);

        assertEquals("Apple Audiobooks", resolved.provider());
        assertEquals("AUDIOBOOK", resolved.format());
        assertEquals(72000, resolved.audiobookLengthSeconds());
        verify(openLibrary).search("Fourth Wing", null, "TITLE");
        verify(googleBooks).search("Fourth Wing", null, "TITLE");
        // Audiobook resolution now asks Wikidata for guarded work-level facts
        // such as original publication year and series metadata.
        verify(wikidata).enrich(any(CatalogBookResult.class));
        verify(appleAudiobooks, never())
                .search("Fourth Wing", "AUDIOBOOK", "TITLE");
    }

    private static CatalogBookResult audiobookResult() {
        return audiobookResult(
                "Fourth Wing",
                "123456",
                null,
                null);
    }

    private static CatalogBookResult audiobookResult(
            String title,
            String providerId,
            String seriesName,
            Double seriesNumber) {
        return new CatalogBookResult(
                "Apple Audiobooks",
                providerId,
                title,
                null,
                List.of("Rebecca Yarros"),
                List.of("Fantasy"),
                "An audiobook description.",
                null,
                "2023-05-02T07:00:00Z",
                null,
                72000,
                List.of(),
                null,
                null,
                null,
                null,
                seriesName,
                seriesNumber,
                "Audiobook",
                "AUDIOBOOK"
        );
    }

    @Test
    void wikidataCanImproveAnEquivalentAudiobookSeriesName() {
        CatalogBookResult selected = audiobookResult(
                "Fourth Wing (Empyrean)",
                "apple-fourth-wing-wikidata",
                "Yarros' Empyrean",
                1.0);
        CatalogBookResult wikidataWork = printResult(
                "Fourth Wing",
                "Rebecca Yarros",
                "The first book in The Empyrean series.",
                "The Empyrean",
                1.0);

        when(openLibrary.search("Fourth Wing", null, "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("Fourth Wing", null, "TITLE"))
                .thenReturn(List.of());
        when(wikidata.enrich(
                org.mockito.ArgumentMatchers.any(
                        CatalogBookResult.class)))
                .thenReturn(wikidataWork);

        CatalogBookResult resolved = service.resolve(selected);

        assertEquals("The Empyrean", resolved.seriesName());
        assertEquals(1.0, resolved.seriesNumber());
    }
    @Test
    void independentlySupportedSeriesNameReplacesStorefrontAttribution() {
        CatalogBookResult selected = audiobookResult(
                "Fourth Wing (Empyrean)",
                "apple-fourth-wing",
                "Yarros' Empyrean",
                1.0);
        CatalogBookResult work = printResult(
                "Fourth Wing",
                "Rebecca Yarros",
                "The first book in The Empyrean series.",
                "Yarros' Empyrean",
                1.0);

        when(openLibrary.search("Fourth Wing", null, "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("Fourth Wing", null, "TITLE"))
                .thenReturn(List.of(work));

        CatalogBookResult resolved = service.resolve(selected);

        assertEquals("The Empyrean", resolved.seriesName());
        assertEquals(1.0, resolved.seriesNumber());
    }
    @Test
    void appleParentheticalSeriesNumberUsesCleanWorkTitle() {
        CatalogBookResult selected = new CatalogBookResult(
                "Apple Audiobooks",
                "apple-caraval",
                "Caraval (Caraval 1)",
                null,
                List.of("Stephanie Garber"),
                List.of("Fantasy"),
                "An audiobook description.",
                null,
                "2017",
                null,
                36000,
                List.of("Rebecca Soler"),
                null,
                "en",
                null,
                null,
                null,
                null,
                "Audiobook",
                "AUDIOBOOK");
        when(openLibrary.search("Caraval", null, "TITLE"))
                .thenReturn(List.of());
        when(googleBooks.search("Caraval", null, "TITLE"))
                .thenReturn(List.of());

        CatalogBookResult resolved = service.resolve(selected);

        assertEquals("Caraval", resolved.title());
        assertEquals("Caraval", resolved.seriesName());
        verify(openLibrary).search("Caraval", null, "TITLE");
        verify(googleBooks).search("Caraval", null, "TITLE");
    }

    @Test
    void partialTitleSearchLeadsWithRealPhysicalEbookAndAudiobook() {
        CatalogBookResult workbook = new CatalogBookResult(
                "GOOGLE_BOOKS", "workbook", "Because of Winn Dixie", null,
                List.of("Creativity in the Classroom"), List.of("Education"),
                "A chapter analysis workbook with comprehension questions.",
                "CreateSpace Independent Publishing", "2014", 100, null,
                List.of(), null, "en", null, null, null, null,
                "Paperback", "PHYSICAL");
        CatalogBookResult physical = printResult(
                "Because of Winn-Dixie", "Kate DiCamillo",
                "A girl adopts a stray dog.", null, null);
        CatalogBookResult ebook = new CatalogBookResult(
                "GOOGLE_BOOKS", "ebook", "Because of Winn-Dixie", null,
                List.of("Kate DiCamillo"), List.of("Juvenile Fiction"),
                "A girl adopts a stray dog.", "Candlewick Press", "2000",
                192, null, List.of(), null, "en", null, null, null, null,
                "E-Book", "EBOOK");
        CatalogBookResult audiobook = new CatalogBookResult(
                "APPLE_AUDIOBOOK", "audio",
                "Because of Winn-Dixie (Unabridged)", null,
                List.of("Kate DiCamillo"), List.of("Juvenile Fiction"),
                "The unabridged audiobook of Kate DiCamillo's novel.",
                "Listening Library", "2001", null, 12600,
                List.of("Cherry Jones"), null, "en", null, null, null, null,
                "Unabridged audiobook", "AUDIOBOOK");

        when(openLibrary.search("because of winn", null, "TITLE"))
                .thenReturn(List.of(physical));
        when(googleBooks.search("because of winn", null, "TITLE"))
                .thenReturn(List.of(workbook));
        when(googleBooks.search("because of winn", "EBOOK", "TITLE"))
                .thenReturn(List.of(ebook));
        when(appleAudiobooks.search(
                "because of winn", "AUDIOBOOK", "TITLE"))
                .thenReturn(List.of(audiobook));

        List<CatalogBookResult> results =
                service.search("because of winn", null, "TITLE");

        assertEquals(
                List.of("PHYSICAL", "EBOOK", "AUDIOBOOK"),
                results.stream().limit(3)
                        .map(CatalogBookResult::format)
                        .toList());
        assertEquals(
                List.of("Kate DiCamillo", "Kate DiCamillo", "Kate DiCamillo"),
                results.stream().limit(3)
                        .map(result -> result.authors().getFirst())
                        .toList());
    }

    @Test
    void audiobookResolutionRejectsAncillaryWorkbookDescription() {
        CatalogBookResult selected = new CatalogBookResult(
                "APPLE_AUDIOBOOK", "audio",
                "Because of Winn-Dixie (Unabridged)", null,
                List.of("Kate DiCamillo"), List.of("Juvenile Fiction"),
                "The verified audiobook description.", "Listening Library",
                "2001", null, 12600, List.of("Cherry Jones"), null, "en",
                null, null, null, null, "Unabridged audiobook", "AUDIOBOOK");
        CatalogBookResult print = new CatalogBookResult(
                "GOOGLE_BOOKS", "ancillary",
                "Because of Winn-Dixie", null,
                List.of("Kate DiCamillo"), List.of("Education"),
                "A chapter analysis workbook with comprehension questions.",
                "Novel Units", "2014", 100, null, List.of(), null, "en",
                null, null, null, null, "Teacher guide", "PHYSICAL");
        when(openLibrary.search("Because of Winn-Dixie", null, "TITLE"))
                .thenReturn(List.of(print));
        when(googleBooks.search("Because of Winn-Dixie", null, "TITLE"))
                .thenReturn(List.of());

        CatalogBookResult resolved = service.resolve(selected);

        assertEquals("The verified audiobook description.",
                resolved.description());
        assertEquals(List.of("Cherry Jones"), resolved.narrators());
    }

    private static CatalogBookResult printResult(
            String title,
            String author,
            String description,
            String seriesName,
            Double seriesNumber) {
        return new CatalogBookResult(
                "GOOGLE_BOOKS",
                "test-id",
                title,
                null,
                List.of(author),
                List.of(),
                description,
                "Test Publisher",
                "2011",
                200,
                null,
                List.of(),
                "https://example.com/cover.jpg",
                "en",
                null,
                null,
                seriesName,
                seriesNumber,
                "Physical edition",
                "PHYSICAL"
        );
    }
}
