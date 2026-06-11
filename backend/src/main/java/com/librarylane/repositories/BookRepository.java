package com.librarylane.repositories;

import com.librarylane.entities.Book;
import com.librarylane.enums.ReadingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByTitleContainingIgnoreCase(String title);

    List<Book> findByCurrentStatus(ReadingStatus currentStatus);

    List<Book> findByWishlistTrue();

    List<Book> findByFavoriteTrue();

    List<Book> findByDnfTrue();

    List<Book> findByOwnedTrue();

    List<Book> findByOwnedTrueAndCurrentStatus(ReadingStatus currentStatus);
}