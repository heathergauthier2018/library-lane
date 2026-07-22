package com.librarylane.services;

import com.librarylane.entities.Book;
import com.librarylane.entities.ReadingExperience;
import com.librarylane.repositories.BookRepository;
import com.librarylane.repositories.ReadingExperienceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Transactional(readOnly = true)
    public List<ReadingExperience> getAllReadingExperiences() {
        return readingExperienceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ReadingExperience getReadingExperienceById(Long id) {
        return readingExperienceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reading experience not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<ReadingExperience> getReadingExperiencesByBookId(Long bookId) {
        requireBook(bookId);
        return readingExperienceRepository.findByBookId(bookId);
    }

    @Transactional(readOnly = true)
    public ReadingExperience getCurrentReadingExperienceForBook(Long bookId) {
        requireBook(bookId);
        return readingExperienceRepository
                .findByBookIdAndCurrentExperienceTrue(bookId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Current reading experience not found for book id: " + bookId));
    }

    @Transactional
    public ReadingExperience createReadingExperience(
            ReadingExperience readingExperience) {

        Long bookId = extractBookId(readingExperience);
        Book book = requireBook(bookId);

        // The database owns the new ID and timestamps.
        readingExperience.setId(null);
        readingExperience.setBook(book);

        if (readingExperience.getCurrentExperience() == null) {
            readingExperience.setCurrentExperience(false);
        }

        return readingExperienceRepository.save(readingExperience);
    }

    @Transactional
    public ReadingExperience updateReadingExperience(
            Long id,
            ReadingExperience updatedReadingExperience) {

        ReadingExperience existingReadingExperience =
                getReadingExperienceById(id);

        existingReadingExperience.setExperienceNumber(
                updatedReadingExperience.getExperienceNumber());
        existingReadingExperience.setLabel(updatedReadingExperience.getLabel());
        existingReadingExperience.setExperienceType(
                updatedReadingExperience.getExperienceType());
        existingReadingExperience.setFormat(updatedReadingExperience.getFormat());
        existingReadingExperience.setStatus(updatedReadingExperience.getStatus());
        existingReadingExperience.setStartDate(updatedReadingExperience.getStartDate());
        existingReadingExperience.setFinishDate(updatedReadingExperience.getFinishDate());
        existingReadingExperience.setCurrentPage(updatedReadingExperience.getCurrentPage());
        existingReadingExperience.setTotalPages(updatedReadingExperience.getTotalPages());
        existingReadingExperience.setCurrentListeningSeconds(
                updatedReadingExperience.getCurrentListeningSeconds());
        existingReadingExperience.setTotalListeningSeconds(
                updatedReadingExperience.getTotalListeningSeconds());
        existingReadingExperience.setListeningSpeed(
                updatedReadingExperience.getListeningSpeed());
        existingReadingExperience.setPercentComplete(
                updatedReadingExperience.getPercentComplete());
        existingReadingExperience.setRating(updatedReadingExperience.getRating());
        existingReadingExperience.setPredictedRating(
                updatedReadingExperience.getPredictedRating());
        existingReadingExperience.setCurrentRating(
                updatedReadingExperience.getCurrentRating());
        existingReadingExperience.setInitialRating(
                updatedReadingExperience.getInitialRating());
        existingReadingExperience.setFinalRating(
                updatedReadingExperience.getFinalRating());
        existingReadingExperience.setRereadRating(
                updatedReadingExperience.getRereadRating());
        existingReadingExperience.setEmotionalDevastationRating(
                updatedReadingExperience.getEmotionalDevastationRating());
        existingReadingExperience.setExcitementRating(
                updatedReadingExperience.getExcitementRating());
        existingReadingExperience.setCurrentExcitementRating(
                updatedReadingExperience.getCurrentExcitementRating());
        existingReadingExperience.setExcitementWhileReading(
                updatedReadingExperience.getExcitementWhileReading());
        existingReadingExperience.setRomancePresence(
                updatedReadingExperience.getRomancePresence());
        existingReadingExperience.setRomanceImportance(
                updatedReadingExperience.getRomanceImportance());
        existingReadingExperience.setRomanceRating(
                updatedReadingExperience.getRomanceRating());
        existingReadingExperience.setSpiceRating(
                updatedReadingExperience.getSpiceRating());
        existingReadingExperience.setHorrorRating(
                updatedReadingExperience.getHorrorRating());
        existingReadingExperience.setRomanceNotes(
                updatedReadingExperience.getRomanceNotes());
        existingReadingExperience.setReviewText(
                updatedReadingExperience.getReviewText());
        existingReadingExperience.setPromptResponsesJson(
                updatedReadingExperience.getPromptResponsesJson());
        existingReadingExperience.setDnfReason(
                updatedReadingExperience.getDnfReason());
        existingReadingExperience.setCurrentExperience(
                Boolean.TRUE.equals(updatedReadingExperience.getCurrentExperience()));

        // Never replace the existing book association during an experience edit.
        return readingExperienceRepository.save(existingReadingExperience);
    }

    @Transactional
    public void deleteReadingExperience(Long id) {
        if (!readingExperienceRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Reading experience not found with id: " + id);
        }

        readingExperienceRepository.deleteById(id);
    }

    private Long extractBookId(ReadingExperience readingExperience) {
        if (readingExperience == null ||
                readingExperience.getBook() == null ||
                readingExperience.getBook().getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid book id is required to save a reading experience.");
        }

        return readingExperience.getBook().getId();
    }

    private Book requireBook(Long bookId) {
        /*
         * Do not use findById here. Book's required Library/User association
         * makes Hibernate generate an inner join for that lookup. Older book
         * rows whose library association has not yet been populated can still
         * exist in the books table and be returned by findAll(), but that join
         * makes findById() incorrectly look empty.
         *
         * existsById checks the books table itself. Once confirmed, a JPA
         * reference is sufficient for assigning the reading_experiences.book_id
         * foreign key without loading the joined Library/User graph.
         */
        if (!bookRepository.existsById(bookId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Book not found with id: " + bookId);
        }

        return bookRepository.getReferenceById(bookId);
    }
}