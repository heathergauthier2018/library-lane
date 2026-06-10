package com.librarylane.repositories;

import com.librarylane.entities.ReadingGoal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingGoalRepository extends JpaRepository<ReadingGoal, Long> {
}