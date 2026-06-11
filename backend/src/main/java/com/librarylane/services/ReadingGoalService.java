package com.librarylane.services;

import com.librarylane.entities.ReadingGoal;
import com.librarylane.entities.User;
import com.librarylane.repositories.ReadingGoalRepository;
import com.librarylane.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReadingGoalService {

    private final ReadingGoalRepository readingGoalRepository;
    private final UserRepository userRepository;

    public ReadingGoalService(
            ReadingGoalRepository readingGoalRepository,
            UserRepository userRepository) {
        this.readingGoalRepository = readingGoalRepository;
        this.userRepository = userRepository;
    }

    public List<ReadingGoal> getAllReadingGoals() {
        return readingGoalRepository.findAll();
    }

    public ReadingGoal getReadingGoalById(Long id) {
        return readingGoalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reading goal not found with id: " + id));
    }

    public ReadingGoal createReadingGoal(ReadingGoal readingGoal) {
        Long userId = readingGoal.getUser().getId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        readingGoal.setUser(user);

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

    public List<ReadingGoal> getReadingGoalsByUser(Long userId) {
        return readingGoalRepository.findByUserId(userId);
    }

    public List<ReadingGoal> getActiveReadingGoals() {
        return readingGoalRepository.findByCompletedFalse();
    }

    public List<ReadingGoal> getCompletedReadingGoals() {
        return readingGoalRepository.findByCompletedTrue();
    }

    public List<ReadingGoal> getReadingGoalsByType(String goalType) {
        return readingGoalRepository.findByGoalTypeIgnoreCase(goalType);
    }
}