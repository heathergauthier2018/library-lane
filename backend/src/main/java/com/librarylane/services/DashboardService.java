package com.librarylane.services;

import com.librarylane.dto.DashboardResponse;
import com.librarylane.dto.DashboardStats;
import com.librarylane.entities.Book;
import com.librarylane.entities.ReadingExperience;
import com.librarylane.enums.ReadingStatus;
import com.librarylane.repositories.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class DashboardService {

    private final BookRepository bookRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final ReadingGoalRepository readingGoalRepository;
    private final GenreRepository genreRepository;
    private final QuoteRepository quoteRepository;
    private final BookCharacterRepository bookCharacterRepository;
    private final BookLocationRepository bookLocationRepository;
    private final BookTimelineEventRepository bookTimelineEventRepository;
    private final MusicAssociationRepository musicAssociationRepository;
    private final ReadingExperienceRepository readingExperienceRepository;

    public DashboardService(
            BookRepository bookRepository,
            JournalEntryRepository journalEntryRepository,
            ReadingGoalRepository readingGoalRepository,
            GenreRepository genreRepository,
            QuoteRepository quoteRepository,
            BookCharacterRepository bookCharacterRepository,
            BookLocationRepository bookLocationRepository,
            BookTimelineEventRepository bookTimelineEventRepository,
            MusicAssociationRepository musicAssociationRepository,
            ReadingExperienceRepository readingExperienceRepository) {

        this.bookRepository = bookRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.readingGoalRepository = readingGoalRepository;
        this.genreRepository = genreRepository;
        this.quoteRepository = quoteRepository;
        this.bookCharacterRepository = bookCharacterRepository;
        this.bookLocationRepository = bookLocationRepository;
        this.bookTimelineEventRepository = bookTimelineEventRepository;
        this.musicAssociationRepository = musicAssociationRepository;
        this.readingExperienceRepository = readingExperienceRepository;
    }

    public DashboardResponse getDashboard() {
        DashboardStats stats = getDashboardStats();
        DashboardResponse.CurrentBook currentBook = getCurrentBook();

        List<DashboardResponse.GoalSummary> activeGoals =
                readingGoalRepository.findByCompletedFalse()
                        .stream()
                        .map(goal -> DashboardResponse.GoalSummary.builder()
                                .id(goal.getId())
                                .goalName(goal.getGoalName())
                                .goalType(goal.getGoalType())
                                .targetValue(goal.getTargetValue())
                                .currentValue(goal.getCurrentValue())
                                .completed(goal.getCompleted())
                                .build())
                        .toList();

        List<DashboardResponse.JournalSummary> recentJournalEntries =
                journalEntryRepository.findTop5ByOrderByCreatedAtDesc()
                        .stream()
                        .map(entry -> DashboardResponse.JournalSummary.builder()
                                .id(entry.getId())
                                .title(entry.getTitle())
                                .mood(entry.getMood())
                                .pageNumber(entry.getPageNumber())
                                .favorite(entry.getFavorite())
                                .createdAt(entry.getCreatedAt())
                                .build())
                        .toList();

        List<DashboardResponse.QuoteSummary> recentQuotes =
                quoteRepository.findTop5ByOrderByCreatedAtDesc()
                        .stream()
                        .map(quote -> DashboardResponse.QuoteSummary.builder()
                                .id(quote.getId())
                                .quoteText(quote.getQuoteText())
                                .characterName(quote.getCharacterName())
                                .pageNumber(quote.getPageNumber())
                                .favorite(quote.getFavorite())
                                .createdAt(quote.getCreatedAt())
                                .build())
                        .toList();

        return DashboardResponse.builder()
                .stats(stats)
                .currentBook(currentBook)
                .activeGoals(activeGoals)
                .recentJournalEntries(recentJournalEntries)
                .recentQuotes(recentQuotes)
                .recentActivity(getRecentActivity())
                .reflections(getReflections(stats, currentBook, activeGoals))
                .build();
    }

    public DashboardStats getDashboardStats() {
        return DashboardStats.builder()
                .totalBooks(bookRepository.count())
                .currentlyReading(bookRepository.findByCurrentStatus(ReadingStatus.CURRENTLY_READING).size())
                .completedBooks(bookRepository.findByCurrentStatus(ReadingStatus.COMPLETED).size())
                .favoriteBooks(bookRepository.findByFavoriteTrue().size())
                .wishlistBooks(bookRepository.findByWishlistTrue().size())
                .tbrBooks(bookRepository.findByOwnedTrueAndCurrentStatus(ReadingStatus.TBR).size())
                .dnfBooks(bookRepository.findByDnfTrue().size())

                .totalJournalEntries(journalEntryRepository.count())
                .favoriteJournalEntries(journalEntryRepository.findByFavoriteTrue().size())

                .totalReadingGoals(readingGoalRepository.count())
                .activeReadingGoals(readingGoalRepository.findByCompletedFalse().size())
                .completedReadingGoals(readingGoalRepository.findByCompletedTrue().size())

                .totalGenres(genreRepository.count())
                .totalQuotes(quoteRepository.count())
                .totalCharacters(bookCharacterRepository.count())
                .totalLocations(bookLocationRepository.count())
                .totalTimelineEvents(bookTimelineEventRepository.count())
                .totalMusicAssociations(musicAssociationRepository.count())
                .build();
    }

    private DashboardResponse.CurrentBook getCurrentBook() {
        List<Book> currentBooks =
                bookRepository.findByCurrentStatus(ReadingStatus.CURRENTLY_READING);

        if (currentBooks.isEmpty()) {
            return null;
        }

        Book book = currentBooks.get(0);

        Optional<ReadingExperience> currentExperience =
                readingExperienceRepository.findByBookIdAndCurrentExperienceTrue(book.getId());

        return DashboardResponse.CurrentBook.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .currentPage(currentExperience.map(ReadingExperience::getCurrentPage).orElse(null))
                .totalPages(currentExperience.map(ReadingExperience::getTotalPages).orElse(book.getPageCount()))
                .percentComplete(currentExperience.map(ReadingExperience::getPercentComplete).orElse(null))
                .startDate(currentExperience.map(ReadingExperience::getStartDate).orElse(null))
                .build();
    }

    private List<DashboardResponse.ActivitySummary> getRecentActivity() {
        List<DashboardResponse.ActivitySummary> activities = new ArrayList<>();

        journalEntryRepository.findTop5ByOrderByCreatedAtDesc()
                .forEach(entry -> activities.add(
                        DashboardResponse.ActivitySummary.builder()
                                .activityType("JOURNAL_ENTRY")
                                .title(entry.getTitle())
                                .description("New journal entry added")
                                .createdAt(entry.getCreatedAt())
                                .build()
                ));

        quoteRepository.findTop5ByOrderByCreatedAtDesc()
                .forEach(quote -> activities.add(
                        DashboardResponse.ActivitySummary.builder()
                                .activityType("QUOTE")
                                .title("Quote saved")
                                .description(quote.getQuoteText())
                                .createdAt(quote.getCreatedAt())
                                .build()
                ));

        bookCharacterRepository.findTop5ByOrderByCreatedAtDesc()
                .forEach(character -> activities.add(
                        DashboardResponse.ActivitySummary.builder()
                                .activityType("CHARACTER")
                                .title(character.getCharacterName())
                                .description("Character added")
                                .createdAt(character.getCreatedAt())
                                .build()
                ));

        bookLocationRepository.findTop5ByOrderByCreatedAtDesc()
                .forEach(location -> activities.add(
                        DashboardResponse.ActivitySummary.builder()
                                .activityType("LOCATION")
                                .title(location.getLocationName())
                                .description("Location added")
                                .createdAt(location.getCreatedAt())
                                .build()
                ));

        bookTimelineEventRepository.findTop5ByOrderByCreatedAtDesc()
                .forEach(event -> activities.add(
                        DashboardResponse.ActivitySummary.builder()
                                .activityType("TIMELINE_EVENT")
                                .title(event.getEventTitle())
                                .description("Timeline event added")
                                .createdAt(event.getCreatedAt())
                                .build()
                ));

        musicAssociationRepository.findTop5ByOrderByCreatedAtDesc()
                .forEach(music -> activities.add(
                        DashboardResponse.ActivitySummary.builder()
                                .activityType("MUSIC")
                                .title(music.getTitle())
                                .description("Music association added")
                                .createdAt(music.getCreatedAt())
                                .build()
                ));

        return activities.stream()
                .sorted(Comparator
                        .comparing(DashboardResponse.ActivitySummary::getCreatedAt)
                        .reversed())
                .limit(10)
                .toList();
    }

    private List<String> getReflections(
            DashboardStats stats,
            DashboardResponse.CurrentBook currentBook,
            List<DashboardResponse.GoalSummary> activeGoals) {

        List<String> reflections = new ArrayList<>();

        if (currentBook != null && currentBook.getPercentComplete() != null) {
            reflections.add("You are " + currentBook.getPercentComplete()
                    + "% through " + currentBook.getTitle() + ".");
        }

        if (stats.getTotalQuotes() > 0) {
            reflections.add("You have saved " + stats.getTotalQuotes()
                    + " quote" + (stats.getTotalQuotes() == 1 ? "." : "s."));
        }

        if (stats.getTotalJournalEntries() > 0) {
            reflections.add("You have written " + stats.getTotalJournalEntries()
                    + " journal entr" + (stats.getTotalJournalEntries() == 1 ? "y." : "ies."));
        }

        if (stats.getTotalCharacters() > 0
                || stats.getTotalLocations() > 0
                || stats.getTotalTimelineEvents() > 0
                || stats.getTotalMusicAssociations() > 0) {

            reflections.add("You are building a rich reading memory with characters, locations, timeline events, and music connections.");
        }

        if (!activeGoals.isEmpty()) {
            DashboardResponse.GoalSummary goal = activeGoals.get(0);

            reflections.add("Your current goal is "
                    + goal.getCurrentValue()
                    + " of "
                    + goal.getTargetValue()
                    + " "
                    + goal.getGoalType().toLowerCase()
                    + ".");
        }

        if (reflections.isEmpty()) {
            reflections.add("Start adding books, quotes, journal entries, and goals to begin building your reading journey.");
        }

        return reflections;
    }
}