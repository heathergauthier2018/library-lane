package com.librarylane.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private DashboardStats stats;
    private CurrentBook currentBook;
    private List<GoalSummary> activeGoals;
    private List<JournalSummary> recentJournalEntries;
    private List<QuoteSummary> recentQuotes;
    private List<ActivitySummary> recentActivity;
    private List<String> reflections;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CurrentBook {
        private Long bookId;
        private String title;
        private Integer currentPage;
        private Integer totalPages;
        private Double percentComplete;
        private LocalDate startDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GoalSummary {
        private Long id;
        private String goalName;
        private String goalType;
        private Integer targetValue;
        private Integer currentValue;
        private Boolean completed;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JournalSummary {
        private Long id;
        private String title;
        private String mood;
        private Integer pageNumber;
        private Boolean favorite;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuoteSummary {
        private Long id;
        private String quoteText;
        private String characterName;
        private Integer pageNumber;
        private Boolean favorite;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivitySummary {
        private String activityType;
        private String title;
        private String description;
        private LocalDateTime createdAt;
    }
}