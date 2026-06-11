package com.librarylane.entities;

import com.librarylane.enums.MusicAssociationType;
import com.librarylane.enums.MusicPlatform;
import com.librarylane.enums.MusicType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "music_associations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MusicAssociation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String artist;

    private String playlistName;

    @Enumerated(EnumType.STRING)
    private MusicPlatform platform;

    @Enumerated(EnumType.STRING)
    private MusicType musicType;

    @Enumerated(EnumType.STRING)
    private MusicAssociationType associationType;

    private String externalUrl;

    private String embedUrl;

    @Column(length = 2000)
    private String associationDescription;

    @Column(length = 3000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reading_experience_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private ReadingExperience readingExperience;

    @CreationTimestamp
    private LocalDateTime createdAt;
}