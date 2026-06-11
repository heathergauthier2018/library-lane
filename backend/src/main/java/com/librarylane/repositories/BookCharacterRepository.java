package com.librarylane.repositories;

import com.librarylane.entities.BookCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookCharacterRepository extends JpaRepository<BookCharacter, Long> {

    List<BookCharacter> findByBookId(Long bookId);

    List<BookCharacter> findByFavoriteTrue();

    List<BookCharacter> findTop5ByOrderByCreatedAtDesc();
}