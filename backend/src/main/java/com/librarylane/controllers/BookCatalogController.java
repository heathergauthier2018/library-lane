package com.librarylane.controllers;

import com.librarylane.catalog.CatalogBookResult;
import com.librarylane.services.BookCatalogSearchService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/books")
@CrossOrigin(origins = "*")
public class BookCatalogController {

    private final BookCatalogSearchService catalogSearchService;

    public BookCatalogController(BookCatalogSearchService catalogSearchService) {
        this.catalogSearchService = catalogSearchService;
    }

    @GetMapping("/search")
    public List<CatalogBookResult> search(
            @RequestParam String q,
            @RequestParam(required = false) String format,
            @RequestParam(required = false, defaultValue = "TITLE") String searchBy) {
        return catalogSearchService.search(q, format, searchBy);
    }

    @PostMapping("/resolve")
    public CatalogBookResult resolve(@RequestBody CatalogBookResult selectedResult) {
        return catalogSearchService.resolve(selectedResult);
    }
}