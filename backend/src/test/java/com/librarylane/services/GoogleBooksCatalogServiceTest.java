package com.librarylane.services;

import com.librarylane.catalog.CatalogBookResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleBooksCatalogServiceTest {

    private MockRestServiceServer server;
    private GoogleBooksCatalogService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new GoogleBooksCatalogService(builder, "");
    }

    @Test
    void ebookFilterIsAuthoritativeAndCoverUsesCleanLargerArtwork() {
        String response = """
                {
                  "items": [{
                    "id": "iron-flame-ebook",
                    "volumeInfo": {
                      "title": "Iron Flame",
                      "authors": ["Rebecca Yarros"],
                      "imageLinks": {
                        "thumbnail": "http://books.google.com/cover?zoom=1&edge=curl&source=gbs_api"
                      }
                    },
                    "saleInfo": {"isEbook": false}
                  }]
                }
                """;

        server.expect(twice(), requestTo(containsString("filter=ebooks")))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        List<CatalogBookResult> results =
                service.search("Iron Flame", "EBOOK", "TITLE");

        assertFalse(results.isEmpty());
        assertEquals("EBOOK", results.getFirst().format());
        assertEquals(
                "https://books.google.com/cover?zoom=2&source=gbs_api",
                results.getFirst().coverImageUrl());
        server.verify();
    }
}
