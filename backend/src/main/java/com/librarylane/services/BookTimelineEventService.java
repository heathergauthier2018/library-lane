package com.librarylane.services;

import com.librarylane.entities.Book;
import com.librarylane.entities.BookTimelineEvent;
import com.librarylane.repositories.BookRepository;
import com.librarylane.repositories.BookTimelineEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookTimelineEventService {

    private final BookTimelineEventRepository bookTimelineEventRepository;
    private final BookRepository bookRepository;

    public BookTimelineEventService(
            BookTimelineEventRepository bookTimelineEventRepository,
            BookRepository bookRepository) {
        this.bookTimelineEventRepository = bookTimelineEventRepository;
        this.bookRepository = bookRepository;
    }

    public List<BookTimelineEvent> getAllTimelineEvents() {
        return bookTimelineEventRepository.findAll();
    }

    public BookTimelineEvent getTimelineEventById(Long id) {
        return bookTimelineEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Timeline event not found with id: " + id));
    }

    public BookTimelineEvent createTimelineEvent(BookTimelineEvent timelineEvent) {
        Long bookId = timelineEvent.getBook().getId();

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + bookId));

        timelineEvent.setBook(book);

        return bookTimelineEventRepository.save(timelineEvent);
    }

    public BookTimelineEvent updateTimelineEvent(
            Long id,
            BookTimelineEvent updatedTimelineEvent) {

        BookTimelineEvent existingTimelineEvent = getTimelineEventById(id);

        existingTimelineEvent.setEventTitle(updatedTimelineEvent.getEventTitle());
        existingTimelineEvent.setEventOrder(updatedTimelineEvent.getEventOrder());
        existingTimelineEvent.setChapterName(updatedTimelineEvent.getChapterName());
        existingTimelineEvent.setPageNumber(updatedTimelineEvent.getPageNumber());
        existingTimelineEvent.setEventDescription(updatedTimelineEvent.getEventDescription());
        existingTimelineEvent.setNotes(updatedTimelineEvent.getNotes());
        existingTimelineEvent.setSpoilerWarning(updatedTimelineEvent.getSpoilerWarning());

        return bookTimelineEventRepository.save(existingTimelineEvent);
    }

    public void deleteTimelineEvent(Long id) {
        bookTimelineEventRepository.deleteById(id);
    }

    public List<BookTimelineEvent> getTimelineEventsByBookId(Long bookId) {
        return bookTimelineEventRepository.findByBookId(bookId);
    }

    public List<BookTimelineEvent> getTimelineEventsByBookIdOrdered(Long bookId) {
        return bookTimelineEventRepository.findByBookIdOrderByEventOrderAsc(bookId);
    }

    public List<BookTimelineEvent> getSpoilerTimelineEvents() {
        return bookTimelineEventRepository.findBySpoilerWarningTrue();
    }
}