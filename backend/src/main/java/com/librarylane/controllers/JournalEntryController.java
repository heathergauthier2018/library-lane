package com.librarylane.controllers;

import com.librarylane.entities.JournalEntry;
import com.librarylane.services.JournalEntryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/journal-entries")
@CrossOrigin(origins = "*")
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    public JournalEntryController(JournalEntryService journalEntryService) {
        this.journalEntryService = journalEntryService;
    }

    @GetMapping
    public List<JournalEntry> getAllJournalEntries() {
        return journalEntryService.getAllJournalEntries();
    }

    @GetMapping("/{id}")
    public JournalEntry getJournalEntryById(@PathVariable Long id) {
        return journalEntryService.getJournalEntryById(id);
    }

    @PostMapping
    public JournalEntry createJournalEntry(@RequestBody JournalEntry journalEntry) {
        return journalEntryService.createJournalEntry(journalEntry);
    }

    @PutMapping("/{id}")
    public JournalEntry updateJournalEntry(
            @PathVariable Long id,
            @RequestBody JournalEntry journalEntry) {
        return journalEntryService.updateJournalEntry(id, journalEntry);
    }

    @DeleteMapping("/{id}")
    public void deleteJournalEntry(@PathVariable Long id) {
        journalEntryService.deleteJournalEntry(id);
    }

    @GetMapping("/reading-experience/{id}")
    public List<JournalEntry> getJournalEntriesByReadingExperience(@PathVariable Long id) {
        return journalEntryService.getJournalEntriesByReadingExperience(id);
    }

    @GetMapping("/favorites")
    public List<JournalEntry> getFavoriteJournalEntries() {
        return journalEntryService.getFavoriteJournalEntries();
    }

    @GetMapping("/mood/{mood}")
    public List<JournalEntry> getJournalEntriesByMood(@PathVariable String mood) {
        return journalEntryService.getJournalEntriesByMood(mood);
    }
}