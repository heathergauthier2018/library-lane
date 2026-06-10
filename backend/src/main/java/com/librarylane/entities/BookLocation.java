package com.librarylane.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String locationName;

    private String locationType;

    @Column(length = 3000)
    private String description;

    @Column(length = 3000)
    private String notes;

    private Boolean favorite;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @CreationTimestamp
    private LocalDateTime createdAt;
}