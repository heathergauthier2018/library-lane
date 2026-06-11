package com.librarylane.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStats {

    private long totalBooks;
    private long currentlyReading;
    private long completedBooks;
    private long favoriteBooks;
    private long wishlistBooks;
    private long tbrBooks;
    private long dnfBooks;

    private long totalJournalEntries;
    private long favoriteJournalEntries;

    private long totalReadingGoals;
    private long activeReadingGoals;
    private long completedReadingGoals;

    private long totalGenres;
    private long totalQuotes;
    private long totalCharacters;
    private long totalLocations;
    private long totalTimelineEvents;
    private long totalMusicAssociations;
}