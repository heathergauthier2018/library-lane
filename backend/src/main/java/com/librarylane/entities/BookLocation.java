package com.librarylane.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Book book;

    @CreationTimestamp
    private LocalDateTime createdAt;
}