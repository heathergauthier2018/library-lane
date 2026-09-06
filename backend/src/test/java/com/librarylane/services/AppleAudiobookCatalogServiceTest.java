package com.librarylane.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarylane.catalog.CatalogBookResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AppleAudiobookCatalogServiceTest {

    private MockRestServiceServer server;
    private AppleAudiobookCatalogService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new AppleAudiobookCatalogService(builder, new ObjectMapper());
    }

    @Test
    void parsesAppleJsonWhenResponseIsLabeledAsJavascript() {
        String json = """
                {
                  "resultCount": 1,
                  "results": [
                    {
                      "wrapperType": "audiobook",
                      "collectionId": 12345,
                      "collectionName": "Fourth Wing",
                      "artistName": "Rebecca Yarros",
                      "primaryGenreName": "Fiction",
                      "description": "&lt;ul&gt;&lt;li&gt;Now with&amp;#xa0;an extended chapter.&lt;/li&gt;&lt;/ul&gt;",
                      "releaseDate": "2023-05-02T07:00:00Z",
                      "artworkUrl100": "https://example.test/100x100bb.jpg",
                      "trackTimeMillis": 7200000
                    }
                  ]
                }
                """;

        MediaType appleContentType = new MediaType(
                "text",
                "javascript",
                StandardCharsets.UTF_8);

        server.expect(request -> {
                    String uri = request.getURI().toString();
                    assertTrue(uri.contains("media=audiobook"));
                    assertTrue(uri.contains("entity=audiobook"));
                    assertTrue(uri.contains("attribute=titleTerm"));
                })
                .andRespond(withSuccess(json, appleContentType));
        expectEmptyGeneralSearch();

        List<CatalogBookResult> results = service.search(
                "Fourth Wing",
                "AUDIOBOOK",
                "TITLE");

        assertEquals(1, results.size());
        assertEquals("Fourth Wing", results.getFirst().title());
        assertEquals("AUDIOBOOK", results.getFirst().format());
        assertEquals(7_200, results.getFirst().audiobookLengthSeconds());
        assertEquals(
                "https://example.test/600x600bb.jpg",
                results.getFirst().coverImageUrl());
        assertEquals(
                "• Now with an extended chapter.",
                results.getFirst().description());
        server.verify();
    }

    @Test
    void extractsNarratorOnlyFromAnExplicitDescriptionCredit() {
        String json = """
                {
                  "resultCount": 1,
                  "results": [
                    {
                      "wrapperType": "audiobook",
                      "collectionId": 98765,
                      "collectionName": "A Legendary Story",
                      "artistName": "A Writer",
                      "primaryGenreName": "Fiction",
                      "description": "[Narrator Rebecca Soler's] narration draws the listener into this world.",
                      "releaseDate": "2018-05-29T07:00:00Z"
                    }
                  ]
                }
                """;

        server.expect(request -> {})
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        expectEmptyGeneralSearch();

        List<CatalogBookResult> results = service.search(
                "A Legendary Story",
                "AUDIOBOOK",
                "TITLE");

        assertEquals(List.of("Rebecca Soler"), results.getFirst().narrators());
        server.verify();
    }

    @Test
    void bestsellerCopyDoesNotBecomeBookOne() {
        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {
                          "resultCount": 1,
                          "results": [
                            {
                              "collectionId": 54321,
                              "collectionName": "Finale",
                              "artistName": "Stephanie Garber",
                              "primaryGenreName": "Fantasy",
                              "description": "The #1 New York Times bestselling conclusion to the Caraval trilogy.",
                              "releaseDate": "2019-05-07T07:00:00Z"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));
        expectEmptyGeneralSearch();

        List<CatalogBookResult> results = service.search(
                "Finale",
                "AUDIOBOOK",
                "TITLE");

        assertEquals(1, results.size());
        assertNull(results.getFirst().seriesNumber());
        server.verify();
    }

    @Test
    void retriesGeneralSearchWhenFocusedResultsAreRelatedButNotStrongMatches() {
        server.expect(request -> assertTrue(
                        request.getURI().toString().contains("attribute=titleTerm")))
                .andRespond(withSuccess(
                        """
                        {
                          "resultCount": 1,
                          "results": [
                            {
                              "collectionId": 13579,
                              "collectionName": "The Arena Games by A Famous Writer (Book Analysis)",
                              "artistName": "A Summary Publisher",
                              "primaryGenreName": "Reference",
                              "description": "A critical analysis of the popular novel.",
                              "releaseDate": "2022-01-01T08:00:00Z"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(request -> {
                    String uri = request.getURI().toString();
                    assertTrue(uri.contains("media=audiobook"));
                    assertTrue(uri.contains("entity=audiobook"));
                    assertTrue(uri.contains("limit=50"));
                    assertFalse(uri.contains("attribute=titleTerm"));
                })
                .andRespond(withSuccess(
                        """
                        {
                          "resultCount": 1,
                          "results": [
                            {
                              "collectionId": 24680,
                              "collectionName": "The Arena Games: Special Edition",
                              "artistName": "A Famous Writer",
                              "primaryGenreName": "Kids & Young Adults",
                              "description": "An actor narrates the first audiobook in a bestselling trilogy.",
                              "releaseDate": "2018-10-30T07:00:00Z"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        List<CatalogBookResult> results = service.search(
                "The Arena Games",
                "AUDIOBOOK",
                "TITLE");

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(result ->
                "The Arena Games: Special Edition".equals(result.title())));
        server.verify();
    }

    @Test
    void ranksStandardRecordingBeforeDramatizedPartsWithoutRemovingThem() {
        String json = """
                {
                  "resultCount": 3,
                  "results": [
                    {
                      "collectionId": 111,
                      "collectionName": "The Clockwork Garden (1 of 2) [Dramatized Adaptation]",
                      "artistName": "A Writer",
                      "primaryGenreName": "Fantasy",
                      "releaseDate": "2024-01-01T08:00:00Z"
                    },
                    {
                      "collectionId": 222,
                      "collectionName": "The Clockwork Garden",
                      "artistName": "A Writer",
                      "primaryGenreName": "Fantasy",
                      "releaseDate": "2020-01-01T08:00:00Z"
                    },
                    {
                      "collectionId": 333,
                      "collectionName": "The Clockwork Garden (2 of 2) [Dramatized Adaptation]",
                      "artistName": "A Writer",
                      "primaryGenreName": "Fantasy",
                      "releaseDate": "2024-02-01T08:00:00Z"
                    }
                  ]
                }
                """;

        server.expect(request -> {})
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        expectEmptyGeneralSearch();

        List<CatalogBookResult> results = service.search(
                "The Clockwork Garden",
                "AUDIOBOOK",
                "TITLE");

        assertEquals(3, results.size());
        assertEquals("The Clockwork Garden", results.getFirst().title());
        assertTrue(results.stream().anyMatch(result ->
                result.title().contains("1 of 2")));
        assertTrue(results.stream().anyMatch(result ->
                result.title().contains("2 of 2")));
        server.verify();
    }

    @Test
    void explicitDramatizedPartSearchUsesTheBaseTitleAndKeepsThePart() {
        server.expect(request -> assertTrue(
                        request.getURI().toString()
                                .contains("attribute=titleTerm")))
                .andRespond(withSuccess(
                        """
                        {"resultCount":0,"results":[]}
                        """,
                        MediaType.APPLICATION_JSON));

        String dramatizedResults = """
                {
                  "resultCount": 2,
                  "results": [
                    {
                      "collectionId": 111,
                      "collectionName": "Fourth Wing (1 of 2) [Dramatized Adaptation]",
                      "artistName": "Rebecca Yarros",
                      "primaryGenreName": "Fantasy"
                    },
                    {
                      "collectionId": 222,
                      "collectionName": "Fourth Wing (2 of 2) [Dramatized Adaptation]",
                      "artistName": "Rebecca Yarros",
                      "primaryGenreName": "Fantasy"
                    }
                  ]
                }
                """;

        server.expect(request -> assertTrue(
                        request.getURI().toString()
                                .contains("attribute=titleTerm")))
                .andRespond(withSuccess(
                        dramatizedResults,
                        MediaType.APPLICATION_JSON));
        server.expect(request -> assertFalse(
                        request.getURI().toString()
                                .contains("attribute=titleTerm")))
                .andRespond(withSuccess(
                        """
                        {"resultCount":0,"results":[]}
                        """,
                        MediaType.APPLICATION_JSON));

        List<CatalogBookResult> results = service.search(
                "Fourth Wing 1 of 2 dramatized adaptation",
                "AUDIOBOOK",
                "TITLE");

        assertEquals(1, results.size());
        assertTrue(results.getFirst().title().contains("1 of 2"));
        assertEquals(
                "Dramatized Adaptation – Part 1 of 2",
                results.getFirst().editionFormat());
        server.verify();
    }

    @Test
    void exactTitleSearchMergesTheRicherSeriesLabeledRecord() {
        server.expect(request -> assertTrue(
                        request.getURI().toString().contains("attribute=titleTerm")))
                .andRespond(withSuccess(
                        """
                        {
                          "resultCount": 1,
                          "results": [
                            {
                              "collectionId": 101,
                              "collectionName": "Legendary",
                              "artistName": "Stephanie Garber",
                              "primaryGenreName": "Fantasy",
                              "releaseDate": "2018-05-29T07:00:00Z",
                              "artworkUrl100": "https://example.test/100x100bb.jpg"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(request -> assertFalse(
                        request.getURI().toString().contains("attribute=titleTerm")))
                .andRespond(withSuccess(
                        """
                        {
                          "resultCount": 1,
                          "results": [
                            {
                              "collectionId": 202,
                              "collectionName": "Legendary (Caraval 2)",
                              "artistName": "Stephanie Garber",
                              "primaryGenreName": "Fantasy",
                              "description": "[Narrator Rebecca Soler's] narration brings Caraval to life.",
                              "releaseDate": "2024-01-05T08:00:00Z",
                              "trackTimeMillis": 41340000,
                              "artworkUrl100": "https://example.test/100x100bb.jpg"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        List<CatalogBookResult> results = service.search(
                "Legendary",
                "AUDIOBOOK",
                "TITLE");

        assertEquals(1, results.size());
        CatalogBookResult result = results.getFirst();
        assertEquals("Legendary (Caraval 2)", result.title());
        assertEquals("Caraval", result.seriesName());
        assertEquals(2.0, result.seriesNumber());
        assertEquals("2018-05-29T07:00:00Z", result.publicationDate());
        assertEquals(41_340, result.audiobookLengthSeconds());
        assertEquals(List.of("Rebecca Soler"), result.narrators());
        server.verify();
    }

    @Test
    void parentheticalMarketingCopyDoesNotBecomeASeries() {
        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {
                          "resultCount": 1,
                          "results": [
                            {
                              "collectionId": 303,
                              "collectionName": "Verity (A Successful Thriller)",
                              "artistName": "Colleen Hoover",
                              "primaryGenreName": "Fiction"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));
        expectEmptyGeneralSearch();

        CatalogBookResult result = service.search(
                "Verity",
                "AUDIOBOOK",
                "TITLE").getFirst();

        assertNull(result.seriesName());
        assertNull(result.seriesNumber());
        server.verify();
    }

    @Test
    void richerSeriesRecordingRanksBeforeUnrelatedExactTitle() {
        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {
                          "resultCount": 1,
                          "results": [
                            {
                              "collectionId": 404,
                              "collectionName": "Finale",
                              "artistName": "D. T. Max"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));
        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {
                          "resultCount": 1,
                          "results": [
                            {
                              "collectionId": 405,
                              "collectionName": "Finale (Caraval 3)",
                              "artistName": "Stephanie Garber",
                              "trackTimeMillis": 48240000
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        List<CatalogBookResult> results = service.search(
                "Finale",
                "AUDIOBOOK",
                "TITLE");

        assertEquals("Finale (Caraval 3)", results.getFirst().title());
        server.verify();
    }

    private void expectEmptyGeneralSearch() {
        server.expect(request -> assertFalse(
                        request.getURI().toString().contains("attribute=titleTerm")))
                .andRespond(withSuccess(
                        """
                        {"resultCount":0,"results":[]}
                        """,
                        MediaType.APPLICATION_JSON));
    }
}
