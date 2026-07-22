package com.librarylane.controllers;

import com.librarylane.entities.ReadingExperience;
import com.librarylane.services.ReadingExperienceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reading-experiences")
@CrossOrigin(origins = "*")
public class ReadingExperienceController {

    private final ReadingExperienceService readingExperienceService;

    public ReadingExperienceController(
            ReadingExperienceService readingExperienceService) {
        this.readingExperienceService = readingExperienceService;
    }

    @GetMapping
    public List<ReadingExperience> getAllReadingExperiences() {
        return readingExperienceService.getAllReadingExperiences();
    }

    @GetMapping("/{id}")
    public ReadingExperience getReadingExperienceById(@PathVariable Long id) {
        return readingExperienceService.getReadingExperienceById(id);
    }

    @GetMapping("/book/{bookId}")
    public List<ReadingExperience> getReadingExperiencesByBookId(
            @PathVariable Long bookId) {
        return readingExperienceService.getReadingExperiencesByBookId(bookId);
    }

    @GetMapping("/book/{bookId}/current")
    public ReadingExperience getCurrentReadingExperienceForBook(
            @PathVariable Long bookId) {
        return readingExperienceService.getCurrentReadingExperienceForBook(bookId);
    }

    @PostMapping
    public ResponseEntity<ReadingExperience> createReadingExperience(
            @RequestBody ReadingExperience readingExperience) {
        ReadingExperience saved =
                readingExperienceService.createReadingExperience(readingExperience);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ReadingExperience updateReadingExperience(
            @PathVariable Long id,
            @RequestBody ReadingExperience readingExperience) {
        return readingExperienceService.updateReadingExperience(id, readingExperience);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReadingExperience(@PathVariable Long id) {
        readingExperienceService.deleteReadingExperience(id);
        return ResponseEntity.noContent().build();
    }
}