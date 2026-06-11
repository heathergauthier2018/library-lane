package com.librarylane.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.librarylane.enums.BookFormat;
import com.librarylane.enums.ReadingStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    private String subtitle;

    @Column(length = 5000)
    private String description;

    private String isbn10;

    private String isbn13;

    private String publisher;

    private LocalDate publicationDate;

    private Integer pageCount;

    private Integer audiobookLengthSeconds;

    private String coverImageUrl;

    private String language;

    private String seriesName;

    private Double seriesNumber;

    @Column(length = 5000)
    private String personalNotes;

    private Boolean favorite;

    private Boolean owned;

    private Boolean wishlist;

    private Boolean dnf;

    @Enumerated(EnumType.STRING)
    private BookFormat primaryFormat;

    @Enumerated(EnumType.STRING)
    private ReadingStatus currentStatus;

    private LocalDate dateAdded;

    @ManyToOne
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;

    @ManyToMany
    @JoinTable(
            name = "book_authors",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    @Builder.Default
    private Set<Author> authors = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "book_genres",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    @Builder.Default
    private Set<Genre> genres = new HashSet<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private Set<ReadingExperience> readingExperiences = new HashSet<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private Set<Quote> quotes = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (dateAdded == null) {
            dateAdded = LocalDate.now();
        }

        if (favorite == null) {
            favorite = false;
        }

        if (owned == null) {
            owned = true;
        }

        if (wishlist == null) {
            wishlist = false;
        }

        if (dnf == null) {
            dnf = false;
        }
    }
}