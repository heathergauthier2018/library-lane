package com.librarylane.services;

import com.librarylane.entities.ReadingGoal;
import com.librarylane.repositories.ReadingGoalRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReadingGoalService {

    private final ReadingGoalRepository readingGoalRepository;

    public ReadingGoalService(ReadingGoalRepository readingGoalRepository) {
        this.readingGoalRepository = readingGoalRepository;
    }

    public List<ReadingGoal> getAllReadingGoals() {
        return readingGoalRepository.findAll();
    }

    public ReadingGoal getReadingGoalById(Long id) {
        return readingGoalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reading goal not found with id: " + id));
    }

    public ReadingGoal createReadingGoal(ReadingGoal readingGoal) {
        return readingGoalRepository.save(readingGoal);
    }

    public ReadingGoal updateReadingGoal(Long id, ReadingGoal updatedGoal) {
        ReadingGoal existingGoal = getReadingGoalById(id);

        existingGoal.setGoalName(updatedGoal.getGoalName());
        existingGoal.setGoalType(updatedGoal.getGoalType());
        existingGoal.setTargetValue(updatedGoal.getTargetValue());
        existingGoal.setCurrentValue(updatedGoal.getCurrentValue());
        existingGoal.setStartDate(updatedGoal.getStartDate());
        existingGoal.setEndDate(updatedGoal.getEndDate());
        existingGoal.setCompleted(updatedGoal.getCompleted());

        return readingGoalRepository.save(existingGoal);
    }

    public void deleteReadingGoal(Long id) {
        readingGoalRepository.deleteById(id);
    }
}