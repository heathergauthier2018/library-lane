package com.librarylane.controllers;

import com.librarylane.entities.ReadingExperience;
import com.librarylane.services.ReadingExperienceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reading-experiences")
@CrossOrigin(origins = "*")
public class ReadingExperienceController {

    private final ReadingExperienceService readingExperienceService;

    public ReadingExperienceController(ReadingExperienceService readingExperienceService) {
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

    @PostMapping
    public ReadingExperience createReadingExperience(@RequestBody ReadingExperience readingExperience) {
        return readingExperienceService.createReadingExperience(readingExperience);
    }

    @PutMapping("/{id}")
    public ReadingExperience updateReadingExperience(
            @PathVariable Long id,
            @RequestBody ReadingExperience readingExperience) {
        return readingExperienceService.updateReadingExperience(id, readingExperience);
    }

    @DeleteMapping("/{id}")
    public void deleteReadingExperience(@PathVariable Long id) {
        readingExperienceService.deleteReadingExperience(id);
    }
}