package com.librarylane.controllers;

import com.librarylane.entities.BookTimelineEvent;
import com.librarylane.services.BookTimelineEventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/book-timeline-events")
@CrossOrigin(origins = "*")
public class BookTimelineEventController {

    private final BookTimelineEventService bookTimelineEventService;

    public BookTimelineEventController(BookTimelineEventService bookTimelineEventService) {
        this.bookTimelineEventService = bookTimelineEventService;
    }

    @GetMapping
    public List<BookTimelineEvent> getAllTimelineEvents() {
        return bookTimelineEventService.getAllTimelineEvents();
    }

    @GetMapping("/{id}")
    public BookTimelineEvent getTimelineEventById(@PathVariable Long id) {
        return bookTimelineEventService.getTimelineEventById(id);
    }

    @PostMapping
    public BookTimelineEvent createTimelineEvent(@RequestBody BookTimelineEvent timelineEvent) {
        return bookTimelineEventService.createTimelineEvent(timelineEvent);
    }

    @PutMapping("/{id}")
    public BookTimelineEvent updateTimelineEvent(
            @PathVariable Long id,
            @RequestBody BookTimelineEvent timelineEvent) {
        return bookTimelineEventService.updateTimelineEvent(id, timelineEvent);
    }

    @DeleteMapping("/{id}")
    public void deleteTimelineEvent(@PathVariable Long id) {
        bookTimelineEventService.deleteTimelineEvent(id);
    }

    @GetMapping("/book/{bookId}")
    public List<BookTimelineEvent> getTimelineEventsByBookId(@PathVariable Long bookId) {
        return bookTimelineEventService.getTimelineEventsByBookId(bookId);
    }

    @GetMapping("/book/{bookId}/ordered")
    public List<BookTimelineEvent> getTimelineEventsByBookIdOrdered(@PathVariable Long bookId) {
        return bookTimelineEventService.getTimelineEventsByBookIdOrdered(bookId);
    }

    @GetMapping("/spoilers")
    public List<BookTimelineEvent> getSpoilerTimelineEvents() {
        return bookTimelineEventService.getSpoilerTimelineEvents();
    }
}