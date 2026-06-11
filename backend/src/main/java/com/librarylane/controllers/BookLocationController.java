package com.librarylane.controllers;

import com.librarylane.entities.BookLocation;
import com.librarylane.services.BookLocationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/book-locations")
@CrossOrigin(origins = "*")
public class BookLocationController {

    private final BookLocationService bookLocationService;

    public BookLocationController(BookLocationService bookLocationService) {
        this.bookLocationService = bookLocationService;
    }

    @GetMapping
    public List<BookLocation> getAllLocations() {
        return bookLocationService.getAllLocations();
    }

    @GetMapping("/{id}")
    public BookLocation getLocationById(@PathVariable Long id) {
        return bookLocationService.getLocationById(id);
    }

    @PostMapping
    public BookLocation createLocation(@RequestBody BookLocation location) {
        return bookLocationService.createLocation(location);
    }

    @PutMapping("/{id}")
    public BookLocation updateLocation(
            @PathVariable Long id,
            @RequestBody BookLocation location) {
        return bookLocationService.updateLocation(id, location);
    }

    @DeleteMapping("/{id}")
    public void deleteLocation(@PathVariable Long id) {
        bookLocationService.deleteLocation(id);
    }

    @GetMapping("/book/{bookId}")
    public List<BookLocation> getLocationsByBookId(@PathVariable Long bookId) {
        return bookLocationService.getLocationsByBookId(bookId);
    }

    @GetMapping("/favorites")
    public List<BookLocation> getFavoriteLocations() {
        return bookLocationService.getFavoriteLocations();
    }
}