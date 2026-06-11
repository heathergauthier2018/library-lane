package com.librarylane.services;

import com.librarylane.entities.Book;
import com.librarylane.entities.ReadingExperience;
import com.librarylane.repositories.BookRepository;
import com.librarylane.repositories.ReadingExperienceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReadingExperienceService {

    private final ReadingExperienceRepository readingExperienceRepository;
    private final BookRepository bookRepository;

    public ReadingExperienceService(
            ReadingExperienceRepository readingExperienceRepository,
            BookRepository bookRepository) {
        this.readingExperienceRepository = readingExperienceRepository;
        this.bookRepository = bookRepository;
    }

    public List<ReadingExperience> getAllReadingExperiences() {
        return readingExperienceRepository.findAll();
    }

    public ReadingExperience getReadingExperienceById(Long id) {
        return readingExperienceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reading experience not found with id: " + id));
    }

    public List<ReadingExperience> getReadingExperiencesByBookId(Long bookId) {
        return readingExperienceRepository.findByBookId(bookId);
    }

    public ReadingExperience getCurrentReadingExperienceForBook(Long bookId) {
        return readingExperienceRepository.findByBookIdAndCurrentExperienceTrue(bookId)
                .orElseThrow(() -> new RuntimeException("Current reading experience not found for book id: " + bookId));
    }

    public ReadingExperience createReadingExperience(ReadingExperience readingExperience) {
        Long bookId = readingExperience.getBook().getId();

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + bookId));

        readingExperience.setBook(book);

        return readingExperienceRepository.save(readingExperience);
    }

    public ReadingExperience updateReadingExperience(Long id, ReadingExperience updatedReadingExperience) {
        ReadingExperience existingReadingExperience = getReadingExperienceById(id);

        existingReadingExperience.setExperienceNumber(updatedReadingExperience.getExperienceNumber());
        existingReadingExperience.setExperienceType(updatedReadingExperience.getExperienceType());
        existingReadingExperience.setFormat(updatedReadingExperience.getFormat());
        existingReadingExperience.setStatus(updatedReadingExperience.getStatus());
        existingReadingExperience.setStartDate(updatedReadingExperience.getStartDate());
        existingReadingExperience.setFinishDate(updatedReadingExperience.getFinishDate());
        existingReadingExperience.setCurrentPage(updatedReadingExperience.getCurrentPage());
        existingReadingExperience.setTotalPages(updatedReadingExperience.getTotalPages());
        existingReadingExperience.setCurrentListeningSeconds(updatedReadingExperience.getCurrentListeningSeconds());
        existingReadingExperience.setTotalListeningSeconds(updatedReadingExperience.getTotalListeningSeconds());
        existingReadingExperience.setListeningSpeed(updatedReadingExperience.getListeningSpeed());
        existingReadingExperience.setPercentComplete(updatedReadingExperience.getPercentComplete());
        existingReadingExperience.setRating(updatedReadingExperience.getRating());
        existingReadingExperience.setReviewText(updatedReadingExperience.getReviewText());
        existingReadingExperience.setDnfReason(updatedReadingExperience.getDnfReason());
        existingReadingExperience.setCurrentExperience(updatedReadingExperience.getCurrentExperience());

        return readingExperienceRepository.save(existingReadingExperience);
    }

    public void deleteReadingExperience(Long id) {
        readingExperienceRepository.deleteById(id);
    }
}