package com.librarylane.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarylane.catalog.CatalogBookResult;
import com.librarylane.services.BookCatalogSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class BookCatalogControllerContractTest {

    private BookCatalogSearchService service;
    private MockMvc mvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        service = mock(BookCatalogSearchService.class);
        mvc = standaloneSetup(new BookCatalogController(service)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void searchRequiresQuery() throws Exception {
        mvc.perform(get("/api/catalog/books/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchForwardsDecodedUnicodeAndDefaultsToTitle() throws Exception {
        when(service.search("L'étranger", null, "TITLE"))
                .thenReturn(List.of());

        mvc.perform(get("/api/catalog/books/search")
                        .param("q", "L'étranger"))
                .andExpect(status().isOk());

        verify(service).search("L'étranger", null, "TITLE");
    }

    @Test
    void searchResponseKeepsFormatAndNullableEditionFields() throws Exception {
        CatalogBookResult audiobook = audiobook();
        when(service.search("Story", null, "TITLE"))
                .thenReturn(List.of(audiobook));

        mvc.perform(get("/api/catalog/books/search").param("q", "Story"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].format").value("AUDIOBOOK"))
                .andExpect(jsonPath("$[0].pageCount").doesNotExist())
                .andExpect(jsonPath("$[0].audiobookLengthSeconds").value(10860))
                .andExpect(jsonPath("$[0].narrators[0]").value("Cherry Jones"));
    }

    @Test
    void resolveAcceptsAndReturnsCatalogContract() throws Exception {
        CatalogBookResult selected = audiobook();
        when(service.resolve(selected)).thenReturn(selected);

        mvc.perform(post("/api/catalog/books/resolve")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(selected)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").value("audio-1"))
                .andExpect(jsonPath("$.format").value("AUDIOBOOK"));
    }

    private static CatalogBookResult audiobook() {
        return new CatalogBookResult(
                "Apple Audiobooks", "audio-1", "Story (Unabridged)", null,
                List.of("Writer"), List.of("Fiction"), "Description",
                "Audio Publisher", "2000", null, 10860,
                List.of("Cherry Jones"), null, "en", null, null,
                null, null, "Unabridged audiobook", "AUDIOBOOK");
    }
}
