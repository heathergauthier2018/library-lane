package com.librarylane.repositories;

import com.librarylane.entities.ReadingGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReadingGoalRepository extends JpaRepository<ReadingGoal, Long> {

    List<ReadingGoal> findByUserId(Long userId);

    List<ReadingGoal> findByCompletedFalse();

    List<ReadingGoal> findByCompletedTrue();

    List<ReadingGoal> findByGoalTypeIgnoreCase(String goalType);
}