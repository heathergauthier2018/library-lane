package com.librarylane.services;

import com.librarylane.catalog.CatalogBookResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenLibraryCatalogServiceTest {

    private MockRestServiceServer server;
    private OpenLibraryCatalogService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new OpenLibraryCatalogService(builder);
    }

    @Test
    void mixedLanguageWorkPrefersEnglishAndKeepsItsCover() {
        String response = """
                {
                  "docs": [{
                    "key": "/works/OL17352669W",
                    "title": "A Court of Thorns and Roses",
                    "author_name": ["Sarah J. Maas"],
                    "first_publish_year": 2013,
                    "number_of_pages_median": 454,
                    "cover_i": 15254465,
                    "language": ["ger", "tur", "spa", "eng"],
                    "ebook_access": "printdisabled",
                    "isbn": ["9781619634442", "9781619634459"]
                  }]
                }
                """;

        server.expect(requestTo(containsString("openlibrary.org/search.json")))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        List<CatalogBookResult> results = service.search(
                "A Court of Thorns and Roses", null, "TITLE");

        assertEquals(2, results.size());
        assertEquals(List.of("PHYSICAL", "EBOOK"), results.stream()
                .map(CatalogBookResult::format)
                .toList());
        results.forEach(result -> {
            assertEquals("eng", result.language());
            assertEquals("Sarah J. Maas", result.authors().getFirst());
            assertEquals(454, result.pageCount());
            assertNotNull(result.coverImageUrl());
        });
        server.verify();
    }

    @Test
    void resolvesPhysicalAndEbookToSeparateCoherentEnglishEditions() {
        String editions = """
                {"entries": [
                  {
                    "title": "A Court of Thorns and Roses",
                    "languages": [{"key":"/languages/eng"}],
                    "physical_format": "Hardcover",
                    "publishers": ["Bloomsbury"],
                    "publish_date": "2015-05",
                    "number_of_pages": 432,
                    "isbn_10": ["1619634449"],
                    "isbn_13": ["9781619634442"],
                    "covers": [15254465]
                  },
                  {
                    "title": "A Court of Thorns and Roses",
                    "languages": [{"key":"/languages/eng"}],
                    "physical_format": "eBook",
                    "publishers": ["Bloomsbury USA Childrens"],
                    "publish_date": "May 05, 2015",
                    "number_of_pages": 432,
                    "isbn_10": ["1619634457"],
                    "isbn_13": ["9781619634459"],
                    "covers": [8738584]
                  }
                ]}
                """;
        server.expect(requestTo(containsString("/works/OL17352669W/editions.json")))
                .andRespond(withSuccess(editions, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/works/OL17352669W/editions.json")))
                .andRespond(withSuccess(editions, MediaType.APPLICATION_JSON));

        CatalogBookResult aggregate = new CatalogBookResult(
                "OPEN_LIBRARY", "OL17352669W", "A Court of Thorns and Roses",
                null, List.of("Sarah J. Maas"), List.of("Fantasy"), null,
                "dtv", "2013", 454, null, List.of(),
                "https://covers.openlibrary.org/b/id/15254465-L.jpg", "eng",
                "2732487597", "9781526641168", null, null,
                "Book catalog record", "PHYSICAL");

        CatalogBookResult physical = service.enrichWithBestEdition(aggregate);
        CatalogBookResult ebook = service.enrichWithBestEdition(new CatalogBookResult(
                aggregate.provider(), aggregate.providerId(), aggregate.title(),
                aggregate.subtitle(), aggregate.authors(), aggregate.genres(),
                aggregate.description(), aggregate.publisher(), aggregate.publicationDate(),
                aggregate.pageCount(), aggregate.audiobookLengthSeconds(),
                aggregate.narrators(), aggregate.coverImageUrl(), aggregate.language(),
                aggregate.isbn10(), aggregate.isbn13(), aggregate.seriesName(),
                aggregate.seriesNumber(), "E-book catalog record", "EBOOK"));

        assertEquals("Bloomsbury", physical.publisher());
        assertEquals("9781619634442", physical.isbn13());
        assertEquals("Hardcover", physical.editionFormat());
        assertEquals("Bloomsbury USA Childrens", ebook.publisher());
        assertEquals("9781619634459", ebook.isbn13());
        assertEquals("eBook", ebook.editionFormat());
        server.verify();
    }

    @Test
    void enrichesStandardAudiobookFromMatchingEditionNotes() {
        String editions = """
                {"entries": [
                  {
                    "title": "A Court of Thorns and Roses",
                    "publishers": ["Recorded Books, Inc."],
                    "publish_date": "05/05/2015",
                    "physical_format": "Audiobook",
                    "isbn_13": ["9781490676623"],
                    "subtitle": "Book #1",
                    "edition_name": "Unabridged",
                    "notes": {"value": "Narrated by: Jennifer Ikeda\\r\\nLength: 16 hours 7 minutes"}
                  },
                  {
                    "title": "A Court of Thorns and Roses",
                    "publishers": ["Recorded Books"],
                    "publish_date": "May 30, 2025",
                    "physical_format": "audible audiobook",
                    "notes": {"value": "Narrated by: Elizabeth Evans"}
                  }
                ]}
                """;
        server.expect(requestTo(containsString("/works/OL17352669W/editions.json")))
                .andRespond(withSuccess(editions, MediaType.APPLICATION_JSON));

        CatalogBookResult selected = new CatalogBookResult(
                "Apple Audiobooks", "1637712934", "A Court of Thorns and Roses",
                null, List.of("Sarah J. Maas"), List.of("Fantasy"), "Description",
                "Recorded Books", "2015-05-05T07:00:00Z", null, null,
                List.of(), "https://example.com/apple.jpg", null, null, null,
                "Court of Thorns and Roses", null, "Audiobook", "AUDIOBOOK");

        CatalogBookResult enriched = service.enrichAudiobookEdition(
                selected, "OL17352669W");

        assertEquals("1637712934", enriched.providerId());
        assertEquals("05/05/2015", enriched.publicationDate());
        assertEquals(List.of("Jennifer Ikeda"), enriched.narrators());
        assertEquals(58_020, enriched.audiobookLengthSeconds());
        assertEquals("9781490676623", enriched.isbn13());
        assertEquals(1.0, enriched.seriesNumber());
        assertEquals("https://example.com/apple.jpg", enriched.coverImageUrl());
        server.verify();
    }
}
