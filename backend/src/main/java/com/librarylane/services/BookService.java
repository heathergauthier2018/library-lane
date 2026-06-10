package com.librarylane.services;

import com.librarylane.entities.Book;
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
        existingBook.setPublisher(updatedBook.getPublisher());
        existingBook.setPublicationDate(updatedBook.getPublicationDate());
        existingBook.setPageCount(updatedBook.getPageCount());
        existingBook.setCoverImageUrl(updatedBook.getCoverImageUrl());
        existingBook.setLanguage(updatedBook.getLanguage());
        existingBook.setFavorite(updatedBook.getFavorite());
        existingBook.setOwned(updatedBook.getOwned());
        existingBook.setWishlist(updatedBook.getWishlist());
        existingBook.setDnf(updatedBook.getDnf());
        existingBook.setCurrentStatus(updatedBook.getCurrentStatus());

        return bookRepository.save(existingBook);
    }

    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }
}