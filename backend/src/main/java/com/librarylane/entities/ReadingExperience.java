package com.librarylane.entities;

import com.librarylane.enums.BookFormat;
import com.librarylane.enums.ReadingExperienceType;
import com.librarylane.enums.ReadingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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

    @Column(length = 5000)
    private String reviewText;

    @Column(length = 2000)
    private String dnfReason;

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