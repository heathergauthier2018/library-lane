package com.librarylane.repositories;

import com.librarylane.entities.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    List<JournalEntry> findByReadingExperienceId(Long readingExperienceId);

    List<JournalEntry> findByFavoriteTrue();

    List<JournalEntry> findByMoodIgnoreCase(String mood);

    List<JournalEntry> findTop5ByOrderByCreatedAtDesc();
}