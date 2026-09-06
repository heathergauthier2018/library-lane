package com.librarylane.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarylane.catalog.CatalogBookResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SpotifyAudiobookMetadataServiceTest {

    private MockRestServiceServer server;
    private SpotifyAudiobookMetadataService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new SpotifyAudiobookMetadataService(
                builder,
                new ObjectMapper(),
                "test-client",
                "test-secret");
    }

    @Test
    void exactTitleAndAuthorAddNarratorsPublisherAndChapterRuntime() {
        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {"access_token":"test-token","expires_in":3600}
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {
                          "audiobooks": {
                            "items": [
                              {
                                "id": "guest-only-edition",
                                "name": "The Arena Games: Special Edition",
                                "authors": [{"name":"A Famous Writer"}],
                                "narrators": [{"name":"A Guest Narrator"}]
                              },
                              {
                                "id": "spotify-123",
                                "name": "The Arena Games: Special Edition",
                                "authors": [{"name":"A Famous Writer"}],
                                "narrators": [
                                  {"name":"An Excellent Narrator"},
                                  {"name":"A Guest Narrator"}
                                ]
                              }
                            ]
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {
                          "id": "spotify-123",
                          "name": "The Arena Games: Special Edition",
                          "authors": [{"name":"A Famous Writer"}],
                          "narrators": [{"name":"An Excellent Narrator"}],
                          "publisher": "A Listening Publisher"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {
                          "total": 2,
                          "items": [
                            {"duration_ms": 1800000},
                            {"duration_ms": 2400000}
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        CatalogBookResult enriched = service.enrich(
                audiobookWithNarrators(List.of("A Guest Narrator")));

        assertEquals(
                List.of(
                        "An Excellent Narrator",
                        "A Guest Narrator"),
                enriched.narrators());
        assertEquals(4_200, enriched.audiobookLengthSeconds());
        assertEquals("A Listening Publisher", enriched.publisher());
        assertEquals("Apple Audiobooks + Spotify Audiobooks", enriched.provider());
        assertEquals("spotify-123", enriched.providerId());
        server.verify();
    }

    @Test
    void sameTitleByDifferentAuthorIsRejected() {
        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {"access_token":"test-token","expires_in":3600}
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {
                          "audiobooks": {
                            "items": [
                              {
                                "id": "wrong-work",
                                "name": "The Arena Games",
                                "authors": [{"name":"A Different Writer"}],
                                "narrators": [{"name":"The Wrong Narrator"}]
                              }
                            ]
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {"albums":{"items":[]}}
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {"albums":{"items":[]}}
                        """,
                        MediaType.APPLICATION_JSON));

        CatalogBookResult original = audiobook();
        CatalogBookResult result = service.enrich(original);

        assertEquals(original, result);
        server.verify();
    }

    @Test
    void optionalLegacyFailurePreservesDedicatedAudiobookMetadata() {
        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {"access_token":"test-token","expires_in":3600}
                        """,
                        MediaType.APPLICATION_JSON));
        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {"audiobooks":{"items":[{
                          "id":"spotify-123",
                          "name":"The Arena Games: Special Edition",
                          "authors":[{"name":"A Famous Writer"}],
                          "narrators":[{"name":"An Excellent Narrator"}]
                        }]}}
                        """,
                        MediaType.APPLICATION_JSON));
        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {
                          "id":"spotify-123",
                          "publisher":"A Listening Publisher",
                          "narrators":[{"name":"An Excellent Narrator"}]
                        }
                        """,
                        MediaType.APPLICATION_JSON));
        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {"total":1,"items":[{"duration_ms":4200000}]}
                        """,
                        MediaType.APPLICATION_JSON));
        server.expect(request -> {
            assertTrue(request.getURI().getQuery().contains("limit=10"));
        }).andRespond(withBadRequest());

        CatalogBookResult enriched = service.enrich(audiobook());

        assertEquals("spotify-123", enriched.providerId());
        assertEquals(4_200, enriched.audiobookLengthSeconds());
        assertEquals(List.of("An Excellent Narrator"), enriched.narrators());
        assertEquals("A Listening Publisher", enriched.publisher());
        server.verify();
    }

    @Test
    void dedicatedAudiobookMissFallsBackToExactLegacyAlbum() {
        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {"access_token":"test-token","expires_in":3600}
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {"audiobooks":{"items":[]}}
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {
                          "albums": {
                            "items": [
                              {
                                "id":"legacy-audio",
                                "name":"The Arena Games: Special Edition [Unabridged]",
                                "artists":[{"name":"A Famous Writer"}],
                                "release_date":"2018-10-30",
                                "total_tracks":75,
                                "images":[]
                              }
                            ]
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {"albums":{"items":[]}}
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {
                          "id":"legacy-audio",
                          "label":"A Listening Publisher",
                          "release_date":"2018-10-30"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {
                          "total":2,
                          "items":[
                            {
                              "duration_ms":1800000,
                              "artists":[{"name":"An Excellent Narrator"}]
                            },
                            {
                              "duration_ms":2400000,
                              "artists":[{"name":"An Excellent Narrator"}]
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        CatalogBookResult enriched = service.enrich(audiobook());

        assertEquals(4_200, enriched.audiobookLengthSeconds());
        assertEquals(
                List.of("An Excellent Narrator"),
                enriched.narrators());
        assertEquals("A Listening Publisher", enriched.publisher());
        assertEquals("album:legacy-audio", enriched.providerId());
        assertTrue(enriched.provider()
                .contains("Spotify Audiobook Albums"));
        server.verify();
    }

    @Test
    void legacyAlbumDiscoveryKeepsAudiobookAndRejectsMusicAlbum() {
        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {"access_token":"test-token","expires_in":3600}
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {
                          "albums": {
                            "items": [
                              {
                                "id": "legacy-audio",
                                "name": "The Clockwork Garden [Unabridged]",
                                "artists": [{"name":"A Famous Writer"}],
                                "release_date": "2009-04-14",
                                "total_tracks": 72,
                                "images": [{"url":"https://example.test/audio.jpg"}]
                              },
                              {
                                "id": "music-album",
                                "name": "The Clockwork Garden",
                                "artists": [{"name":"A Famous Writer"}],
                                "release_date": "2020-01-01",
                                "total_tracks": 12,
                                "images": []
                              }
                            ]
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {"albums":{"items":[]}}
                        """,
                        MediaType.APPLICATION_JSON));

        List<CatalogBookResult> results =
                service.searchLegacyAlbums("The Clockwork Garden", "TITLE");

        assertEquals(1, results.size());
        assertEquals("album:legacy-audio", results.getFirst().providerId());
        assertEquals("The Clockwork Garden", results.getFirst().title());
        assertEquals("Unabridged", results.getFirst().editionFormat());
        assertTrue(SpotifyAudiobookMetadataService.isLegacyAlbumResult(
                results.getFirst()));
        server.verify();
    }

    @Test
    void legacyAlbumTracksProvideRuntimeNarratorAndPublisher() {
        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {"access_token":"test-token","expires_in":3600}
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {
                          "id":"legacy-audio",
                          "label":"A Listening Publisher",
                          "release_date":"2009-04-14"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(request -> {})
                .andRespond(withSuccess(
                        """
                        {
                          "total":3,
                          "items":[
                            {
                              "duration_ms":1800000,
                              "artists":[
                                {"name":"A Famous Writer"},
                                {"name":"An Excellent Narrator"}
                              ]
                            },
                            {
                              "duration_ms":2400000,
                              "artists":[{"name":"An Excellent Narrator"}]
                            },
                            {
                              "duration_ms":600000,
                              "artists":[{"name":"An Excellent Narrator"}]
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        CatalogBookResult selected = new CatalogBookResult(
                "Spotify Audiobook Albums",
                "album:legacy-audio",
                "The Clockwork Garden",
                null,
                List.of("A Famous Writer"),
                List.of(),
                null,
                null,
                "2009-04-14",
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

        CatalogBookResult enriched = service.enrichLegacyAlbum(selected);

        assertEquals(4_800, enriched.audiobookLengthSeconds());
        assertEquals(
                List.of("An Excellent Narrator"),
                enriched.narrators());
        assertEquals("A Listening Publisher", enriched.publisher());
        server.verify();
    }

    private static CatalogBookResult audiobook() {
        return audiobookWithNarrators(List.of());
    }

    private static CatalogBookResult audiobookWithNarrators(
            List<String> narrators) {
        return new CatalogBookResult(
                "Apple Audiobooks",
                "apple-123",
                "The Arena Games: Special Edition",
                null,
                List.of("A Famous Writer"),
                List.of("Young Adult"),
                "A description.",
                null,
                "2018",
                null,
                null,
                narrators,
                null,
                "en",
                null,
                null,
                "The Arena Games",
                1.0,
                "Audiobook",
                "AUDIOBOOK");
    }
}
