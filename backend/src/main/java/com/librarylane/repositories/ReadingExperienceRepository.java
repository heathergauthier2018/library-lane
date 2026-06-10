package com.librarylane.repositories;

import com.librarylane.entities.Book;
import com.librarylane.entities.ReadingExperience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingExperienceRepository extends JpaRepository<ReadingExperience, Long> {

    List<ReadingExperience> findByBook(Book book);

    List<ReadingExperience> findByBookId(Long bookId);

    Optional<ReadingExperience> findByBookIdAndCurrentExperienceTrue(Long bookId);
}
