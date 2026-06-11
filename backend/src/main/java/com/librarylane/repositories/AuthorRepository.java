package com.librarylane.repositories;

import com.librarylane.entities.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    Optional<Author> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);

    Optional<Author> findByPenNameIgnoreCase(String penName);

    List<Author> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrPenNameContainingIgnoreCase(
            String firstName,
            String lastName,
            String penName
    );
}