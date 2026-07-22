package com.librarylane.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.librarylane.enums.BookFormat;
import com.librarylane.enums.ReadingExperienceType;
import com.librarylane.enums.ReadingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "reading_experiences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer experienceNumber;

    private String label;

    @Enumerated(EnumType.STRING)
    private ReadingExperienceType experienceType;

    @Enumerated(EnumType.STRING)
    private BookFormat format;

    @Enumerated(EnumType.STRING)
    private ReadingStatus status;

    private LocalDate startDate;

    private LocalDate finishDate;

    private Integer currentPage;

    private Integer totalPages;

    private Integer currentListeningSeconds;

    private Integer totalListeningSeconds;

    private Double listeningSpeed;

    private Double percentComplete;

    private Double rating;

    private String predictedRating;
    private String currentRating;
    private String initialRating;
    private String finalRating;
    private String rereadRating;
    private String emotionalDevastationRating;

    private String excitementRating;
    private String currentExcitementRating;
    private String excitementWhileReading;

    private String romancePresence;
    private String romanceImportance;
    private String romanceRating;
    private String spiceRating;

    private String horrorRating;

    @Column(length = 5000)
    private String romanceNotes;

    @Column(length = 5000)
    private String reviewText;

    @Column(length = 2000)
    private String dnfReason;

    /**
     * Stores every expandable prompt answer in JSON.
     * This allows Library Lane to add or remove prompts
     * without changing the database schema.
     */
    @Column(columnDefinition = "TEXT")
    private String promptResponsesJson;

    private Boolean currentExperience = false;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Book book;

    @OneToMany(
            mappedBy = "readingExperience",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonManagedReference
    @Builder.Default
    private Set<JournalEntry> journalEntries = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (currentExperience == null) {
            currentExperience = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}