package com.librarylane.controllers;

import com.librarylane.entities.Quote;
import com.librarylane.services.QuoteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quotes")
@CrossOrigin(origins = "*")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping
    public List<Quote> getAllQuotes() {
        return quoteService.getAllQuotes();
    }

    @GetMapping("/{id}")
    public Quote getQuoteById(@PathVariable Long id) {
        return quoteService.getQuoteById(id);
    }

    @PostMapping
    public Quote createQuote(@RequestBody Quote quote) {
        return quoteService.createQuote(quote);
    }

    @PutMapping("/{id}")
    public Quote updateQuote(
            @PathVariable Long id,
            @RequestBody Quote quote) {
        return quoteService.updateQuote(id, quote);
    }

    @DeleteMapping("/{id}")
    public void deleteQuote(@PathVariable Long id) {
        quoteService.deleteQuote(id);
    }

    @GetMapping("/book/{bookId}")
    public List<Quote> getQuotesByBookId(@PathVariable Long bookId) {
        return quoteService.getQuotesByBookId(bookId);
    }

    @GetMapping("/favorites")
    public List<Quote> getFavoriteQuotes() {
        return quoteService.getFavoriteQuotes();
    }
}