package com.librarylane.repositories;

import com.librarylane.entities.Book;
import com.librarylane.entities.ReadingExperience;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingExperienceRepository extends JpaRepository<ReadingExperience, Long> {

    @Override
    @EntityGraph(attributePaths = "journalEntries")
    List<ReadingExperience> findAll();

    @Override
    @EntityGraph(attributePaths = "journalEntries")
    Optional<ReadingExperience> findById(Long id);

    @EntityGraph(attributePaths = "journalEntries")
    List<ReadingExperience> findByBook(Book book);

    @EntityGraph(attributePaths = "journalEntries")
    List<ReadingExperience> findByBookId(Long bookId);

    @EntityGraph(attributePaths = "journalEntries")
    Optional<ReadingExperience> findByBookIdAndCurrentExperienceTrue(Long bookId);
}
