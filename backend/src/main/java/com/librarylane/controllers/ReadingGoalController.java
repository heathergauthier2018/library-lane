package com.librarylane.controllers;

import com.librarylane.entities.ReadingGoal;
import com.librarylane.services.ReadingGoalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reading-goals")
@CrossOrigin(origins = "*")
public class ReadingGoalController {

    private final ReadingGoalService readingGoalService;

    public ReadingGoalController(ReadingGoalService readingGoalService) {
        this.readingGoalService = readingGoalService;
    }

    @GetMapping
    public List<ReadingGoal> getAllReadingGoals() {
        return readingGoalService.getAllReadingGoals();
    }

    @GetMapping("/{id}")
    public ReadingGoal getReadingGoalById(@PathVariable Long id) {
        return readingGoalService.getReadingGoalById(id);
    }

    @PostMapping
    public ReadingGoal createReadingGoal(@RequestBody ReadingGoal readingGoal) {
        return readingGoalService.createReadingGoal(readingGoal);
    }

    @PutMapping("/{id}")
    public ReadingGoal updateReadingGoal(
            @PathVariable Long id,
            @RequestBody ReadingGoal readingGoal) {
        return readingGoalService.updateReadingGoal(id, readingGoal);
    }

    @DeleteMapping("/{id}")
    public void deleteReadingGoal(@PathVariable Long id) {
        readingGoalService.deleteReadingGoal(id);
    }
}