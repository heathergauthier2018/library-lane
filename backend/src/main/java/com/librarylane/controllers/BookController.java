package com.librarylane.controllers;

import com.librarylane.entities.Book;
import com.librarylane.services.BookService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = "*")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Long id) {
        return bookService.getBookById(id);
    }

    @PostMapping
    public Book createBook(@RequestBody Book book) {
        return bookService.createBook(book);
    }

    @PutMapping("/{id}")
    public Book updateBook(
            @PathVariable Long id,
            @RequestBody Book book) {
        return bookService.updateBook(id, book);
    }

    @DeleteMapping("/{id}")
    public void deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
    }

    @GetMapping("/search")
    public List<Book> searchBooksByTitle(@RequestParam String title) {
        return bookService.searchBooksByTitle(title);
    }

    @GetMapping("/wishlist")
    public List<Book> getWishlistBooks() {
        return bookService.getWishlistBooks();
    }

    @GetMapping("/tbr")
    public List<Book> getTbrBooks() {
        return bookService.getTbrBooks();
    }

    @GetMapping("/currently-reading")
    public List<Book> getCurrentlyReadingBooks() {
        return bookService.getCurrentlyReadingBooks();
    }

    @GetMapping("/completed")
    public List<Book> getCompletedBooks() {
        return bookService.getCompletedBooks();
    }

    @GetMapping("/dnf")
    public List<Book> getDnfBooks() {
        return bookService.getDnfBooks();
    }

    @GetMapping("/paused")
    public List<Book> getPausedBooks() {
        return bookService.getPausedBooks();
    }

    @GetMapping("/favorites")
    public List<Book> getFavoriteBooks() {
        return bookService.getFavoriteBooks();
    }

    @GetMapping("/owned")
    public List<Book> getOwnedBooks() {
        return bookService.getOwnedBooks();
    }
}