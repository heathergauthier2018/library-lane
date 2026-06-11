package com.librarylane.repositories;

import com.librarylane.entities.BookTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookTimelineEventRepository extends JpaRepository<BookTimelineEvent, Long> {

    List<BookTimelineEvent> findByBookId(Long bookId);

    List<BookTimelineEvent> findByBookIdOrderByEventOrderAsc(Long bookId);

    List<BookTimelineEvent> findBySpoilerWarningTrue();

    List<BookTimelineEvent> findTop5ByOrderByCreatedAtDesc();
}