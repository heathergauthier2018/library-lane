package com.librarylane.services;

import com.librarylane.catalog.CatalogBookResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookCatalogAutomationMatrixTest {

    @Mock private OpenLibraryCatalogService openLibrary;
    @Mock private GoogleBooksCatalogService googleBooks;
    @Mock private WikidataCatalogService wikidata;
    @Mock private AppleAudiobookCatalogService appleAudiobooks;

    private BookCatalogSearchService service;

    @BeforeEach
    void setUp() {
        service = new BookCatalogSearchService(
                openLibrary, googleBooks, wikidata, appleAudiobooks);
        configureMixedFormatCatalog();
    }

    @TestFactory
    Stream<DynamicTest> titleNormalizationMatrixKeepsAllFormats() {
        List<String> queries = new ArrayList<>();
        List<String> bases = List.of(
                "because of winn",
                "Because of Winn-Dixie",
                "BECAUSE OF WINN DIXIE",
                "because   of   winn dixie",
                "Because of Winn Dixie!");
        for (String base : bases) {
            queries.add(base);
            queries.add("  " + base + "  ");
            queries.add(base.replace("i", "I"));
            queries.add(base.toLowerCase(Locale.ROOT));
        }

        return queries.stream().distinct().map(query ->
                DynamicTest.dynamicTest("query: " + query, () -> {
                    List<CatalogBookResult> results =
                            service.search(query, null, "TITLE");
                    assertTrue(results.size() >= 3);
                    assertEquals(
                            List.of("PHYSICAL", "EBOOK", "AUDIOBOOK"),
                            results.stream().limit(3)
                                    .map(CatalogBookResult::format)
                                    .toList());
                    assertTrue(results.stream().limit(3)
                            .flatMap(result -> result.authors().stream())
                            .allMatch("Kate DiCamillo"::equals));
                }));
    }

    @Test
    void ancillaryEvidenceAcrossFieldsCannotEnterLeadResults() {
        List<CatalogBookResult> results =
                service.search("Because of Winn-Dixie", null, "TITLE");

        assertFalse(results.stream().anyMatch(result ->
                "workbook".equals(result.providerId())));
        assertFalse(results.stream().anyMatch(result ->
                result.description() != null
                        && result.description().contains("chapter analysis")));
    }

    @Test
    void providerFailureStillReturnsOtherAvailableFormats() {
        when(openLibrary.search(
                anyString(), nullable(String.class), anyString()))
                .thenThrow(new RuntimeException("Open Library unavailable"));

        List<CatalogBookResult> results =
                service.search("Because of Winn-Dixie", null, "TITLE");

        assertTrue(results.stream().anyMatch(result ->
                "EBOOK".equals(result.format())));
        assertTrue(results.stream().anyMatch(result ->
                "AUDIOBOOK".equals(result.format())));
    }

    @Test
    void resolveRejectsExactTitleAncillarySynopsisAndGenres() {
        CatalogBookResult selected = result(
                "selected", "Kate DiCamillo",
                "A lonely girl adopts a remarkable stray dog.",
                "Walker Books", "PHYSICAL");
        CatalogBookResult misleadingSupplement = new CatalogBookResult(
                "Google Books", "bad-supplement",
                "Because of Winn-Dixie", null,
                List.of("Kate DiCamillo"),
                List.of("Biographies & Memoirs"),
                "The perfect companion and study guide with chapter analysis.",
                "Novel Units", "2000", 182, null, List.of(), null, "en",
                null, null, null, null, "Book edition", "PHYSICAL");
        when(googleBooks.search(
                eq("Because of Winn-Dixie"), eq("PHYSICAL"), eq("TITLE")))
                .thenReturn(List.of(misleadingSupplement));

        CatalogBookResult resolved = service.resolve(selected);

        assertEquals("A lonely girl adopts a remarkable stray dog.",
                resolved.description());
        assertFalse(resolved.genres().contains("Biographies & Memoirs"));
    }

    private void configureMixedFormatCatalog() {
        CatalogBookResult physical = result(
                "physical", "Kate DiCamillo",
                "A lonely girl adopts a remarkable stray dog.",
                "Walker Books", "PHYSICAL");
        CatalogBookResult ebook = result(
                "ebook", "Kate DiCamillo",
                "A lonely girl adopts a remarkable stray dog.",
                "Walker Books", "EBOOK");
        CatalogBookResult audiobook = result(
                "audiobook", "Kate DiCamillo",
                "The unabridged recording of the novel.",
                "Listening Library", "AUDIOBOOK");
        CatalogBookResult workbook = result(
                "workbook", "Creativity in the Classroom",
                "A study guide with chapter analysis and comprehension questions.",
                "CreateSpace Independent Publishing", "PHYSICAL");

        when(openLibrary.search(
                anyString(), nullable(String.class), anyString()))
                .thenReturn(List.of(physical));
        when(googleBooks.search(
                anyString(), nullable(String.class), anyString()))
                .thenAnswer(invocation -> "EBOOK".equals(invocation.getArgument(1))
                        ? List.of(ebook)
                        : List.of(workbook));
        when(appleAudiobooks.search(
                anyString(), nullable(String.class), anyString()))
                .thenReturn(List.of(audiobook));
    }

    private static CatalogBookResult result(
            String id,
            String author,
            String description,
            String publisher,
            String format) {
        boolean audio = "AUDIOBOOK".equals(format);
        return new CatalogBookResult(
                audio ? "Apple Audiobooks" : "Google Books",
                id,
                audio
                        ? "Because of Winn-Dixie (Unabridged)"
                        : "Because of Winn-Dixie",
                null,
                List.of(author),
                List.of(audio ? "Juvenile Fiction" : "Young Adult"),
                description,
                publisher,
                "2000",
                audio ? null : 182,
                audio ? 10_860 : null,
                audio ? List.of("Cherry Jones") : List.of(),
                null,
                "en",
                null,
                null,
                null,
                null,
                audio ? "Unabridged audiobook" : "Book edition",
                format);
    }
}
