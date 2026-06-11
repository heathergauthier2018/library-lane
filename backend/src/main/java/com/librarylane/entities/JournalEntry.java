package com.librarylane.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "journal_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 10000)
    private String content;

    private Integer pageNumber;

    private String chapterName;

    private String mood;

    private Boolean spoilerWarning;

    @ManyToOne
    @JoinColumn(name = "reading_experience_id", nullable = false)
    @JsonBackReference
    private ReadingExperience readingExperience;

    @CreationTimestamp
    private LocalDateTime createdAt;
}