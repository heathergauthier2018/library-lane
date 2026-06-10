package com.librarylane.repositories;

import com.librarylane.entities.BookLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookLocationRepository extends JpaRepository<BookLocation, Long> {
}