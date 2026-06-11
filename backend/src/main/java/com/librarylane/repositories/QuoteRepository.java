package com.librarylane.repositories;

import com.librarylane.entities.Quote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    List<Quote> findByBookId(Long bookId);

    List<Quote> findByFavoriteTrue();
}