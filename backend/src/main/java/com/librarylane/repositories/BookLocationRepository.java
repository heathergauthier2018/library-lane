package com.librarylane.repositories;

import com.librarylane.entities.BookLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookLocationRepository extends JpaRepository<BookLocation, Long> {

    List<BookLocation> findByBookId(Long bookId);

    List<BookLocation> findByFavoriteTrue();

    List<BookLocation> findTop5ByOrderByCreatedAtDesc();
}