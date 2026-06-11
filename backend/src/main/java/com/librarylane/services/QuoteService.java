package com.librarylane.services;

import com.librarylane.entities.Book;
import com.librarylane.entities.Quote;
import com.librarylane.repositories.BookRepository;
import com.librarylane.repositories.QuoteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final BookRepository bookRepository;

    public QuoteService(QuoteRepository quoteRepository, BookRepository bookRepository) {
        this.quoteRepository = quoteRepository;
        this.bookRepository = bookRepository;
    }

    public List<Quote> getAllQuotes() {
        return quoteRepository.findAll();
    }

    public Quote getQuoteById(Long id) {
        return quoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quote not found with id: " + id));
    }

    public Quote createQuote(Quote quote) {
        Long bookId = quote.getBook().getId();

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + bookId));

        quote.setBook(book);

        return quoteRepository.save(quote);
    }

    public Quote updateQuote(Long id, Quote updatedQuote) {
        Quote existingQuote = getQuoteById(id);

        existingQuote.setQuoteText(updatedQuote.getQuoteText());
        existingQuote.setPageNumber(updatedQuote.getPageNumber());
        existingQuote.setChapterName(updatedQuote.getChapterName());
        existingQuote.setCharacterName(updatedQuote.getCharacterName());
        existingQuote.setFavorite(updatedQuote.getFavorite());
        existingQuote.setNotes(updatedQuote.getNotes());

        return quoteRepository.save(existingQuote);
    }

    public void deleteQuote(Long id) {
        quoteRepository.deleteById(id);
    }

    public List<Quote> getQuotesByBookId(Long bookId) {
        return quoteRepository.findByBookId(bookId);
    }

    public List<Quote> getFavoriteQuotes() {
        return quoteRepository.findByFavoriteTrue();
    }
}