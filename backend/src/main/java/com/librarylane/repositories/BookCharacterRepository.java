package com.librarylane.repositories;

import com.librarylane.entities.BookCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookCharacterRepository extends JpaRepository<BookCharacter, Long> {
}