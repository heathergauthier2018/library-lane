package com.librarylane.services;

import com.librarylane.entities.Book;
import com.librarylane.entities.BookLocation;
import com.librarylane.repositories.BookLocationRepository;
import com.librarylane.repositories.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookLocationService {

    private final BookLocationRepository bookLocationRepository;
    private final BookRepository bookRepository;

    public BookLocationService(
            BookLocationRepository bookLocationRepository,
            BookRepository bookRepository) {
        this.bookLocationRepository = bookLocationRepository;
        this.bookRepository = bookRepository;
    }

    public List<BookLocation> getAllLocations() {
        return bookLocationRepository.findAll();
    }

    public BookLocation getLocationById(Long id) {
        return bookLocationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book location not found with id: " + id));
    }

    public BookLocation createLocation(BookLocation location) {
        Long bookId = location.getBook().getId();

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + bookId));

        location.setBook(book);

        return bookLocationRepository.save(location);
    }

    public BookLocation updateLocation(Long id, BookLocation updatedLocation) {
        BookLocation existingLocation = getLocationById(id);

        existingLocation.setLocationName(updatedLocation.getLocationName());
        existingLocation.setLocationType(updatedLocation.getLocationType());
        existingLocation.setDescription(updatedLocation.getDescription());
        existingLocation.setNotes(updatedLocation.getNotes());
        existingLocation.setFavorite(updatedLocation.getFavorite());

        return bookLocationRepository.save(existingLocation);
    }

    public void deleteLocation(Long id) {
        bookLocationRepository.deleteById(id);
    }

    public List<BookLocation> getLocationsByBookId(Long bookId) {
        return bookLocationRepository.findByBookId(bookId);
    }

    public List<BookLocation> getFavoriteLocations() {
        return bookLocationRepository.findByFavoriteTrue();
    }
}