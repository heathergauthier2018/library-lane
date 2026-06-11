package com.librarylane.services;

import com.librarylane.entities.Book;
import com.librarylane.enums.ReadingStatus;
import com.librarylane.repositories.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
    }

    public Book createBook(Book book) {
        return bookRepository.save(book);
    }

    public Book updateBook(Long id, Book updatedBook) {
        Book existingBook = getBookById(id);

        existingBook.setTitle(updatedBook.getTitle());
        existingBook.setSubtitle(updatedBook.getSubtitle());
        existingBook.setDescription(updatedBook.getDescription());
        existingBook.setIsbn10(updatedBook.getIsbn10());
        existingBook.setIsbn13(updatedBook.getIsbn13());
        existingBook.setPublisher(updatedBook.getPublisher());
        existingBook.setPublicationDate(updatedBook.getPublicationDate());
        existingBook.setPageCount(updatedBook.getPageCount());
        existingBook.setAudiobookLengthSeconds(updatedBook.getAudiobookLengthSeconds());
        existingBook.setCoverImageUrl(updatedBook.getCoverImageUrl());
        existingBook.setLanguage(updatedBook.getLanguage());
        existingBook.setSeriesName(updatedBook.getSeriesName());
        existingBook.setSeriesNumber(updatedBook.getSeriesNumber());
        existingBook.setPersonalNotes(updatedBook.getPersonalNotes());
        existingBook.setPrimaryFormat(updatedBook.getPrimaryFormat());
        existingBook.setCurrentStatus(updatedBook.getCurrentStatus());
        existingBook.setFavorite(updatedBook.getFavorite());
        existingBook.setOwned(updatedBook.getOwned());
        existingBook.setWishlist(updatedBook.getWishlist());
        existingBook.setDnf(updatedBook.getDnf());

        return bookRepository.save(existingBook);
    }

    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }

    public List<Book> searchBooksByTitle(String title) {
        return bookRepository.findByTitleContainingIgnoreCase(title);
    }

    public List<Book> getWishlistBooks() {
        return bookRepository.findByWishlistTrue();
    }

    public List<Book> getTbrBooks() {
        return bookRepository.findByOwnedTrueAndCurrentStatus(ReadingStatus.TBR);
    }

    public List<Book> getCurrentlyReadingBooks() {
        return bookRepository.findByCurrentStatus(ReadingStatus.CURRENTLY_READING);
    }

    public List<Book> getCompletedBooks() {
        return bookRepository.findByCurrentStatus(ReadingStatus.COMPLETED);
    }

    public List<Book> getDnfBooks() {
        return bookRepository.findByDnfTrue();
    }

    public List<Book> getPausedBooks() {
        return bookRepository.findByCurrentStatus(ReadingStatus.PAUSED);
    }

    public List<Book> getFavoriteBooks() {
        return bookRepository.findByFavoriteTrue();
    }

    public List<Book> getOwnedBooks() {
        return bookRepository.findByOwnedTrue();
    }
}