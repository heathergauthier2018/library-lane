package com.librarylane.services;

import com.librarylane.entities.JournalEntry;
import com.librarylane.repositories.JournalEntryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;

    public JournalEntryService(JournalEntryRepository journalEntryRepository) {
        this.journalEntryRepository = journalEntryRepository;
    }

    public List<JournalEntry> getAllJournalEntries() {
        return journalEntryRepository.findAll();
    }

    public JournalEntry getJournalEntryById(Long id) {
        return journalEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Journal entry not found with id: " + id));
    }

    public JournalEntry createJournalEntry(JournalEntry journalEntry) {
        return journalEntryRepository.save(journalEntry);
    }

    public JournalEntry updateJournalEntry(Long id, JournalEntry updatedJournalEntry) {
        JournalEntry existingJournalEntry = getJournalEntryById(id);

        existingJournalEntry.setTitle(updatedJournalEntry.getTitle());
        existingJournalEntry.setContent(updatedJournalEntry.getContent());
        existingJournalEntry.setPageNumber(updatedJournalEntry.getPageNumber());
        existingJournalEntry.setChapterName(updatedJournalEntry.getChapterName());
        existingJournalEntry.setMood(updatedJournalEntry.getMood());
        existingJournalEntry.setSpoilerWarning(updatedJournalEntry.getSpoilerWarning());

        return journalEntryRepository.save(existingJournalEntry);
    }

    public void deleteJournalEntry(Long id) {
        journalEntryRepository.deleteById(id);
    }
}