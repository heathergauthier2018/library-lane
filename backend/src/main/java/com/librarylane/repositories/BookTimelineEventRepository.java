package com.librarylane.repositories;

import com.librarylane.entities.BookTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookTimelineEventRepository extends JpaRepository<BookTimelineEvent, Long> {
}