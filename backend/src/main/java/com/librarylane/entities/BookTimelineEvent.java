package com.librarylane.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_timeline_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookTimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventTitle;

    private Integer eventOrder;

    private String chapterName;

    private Integer pageNumber;

    @Column(length = 5000)
    private String eventDescription;

    @Column(length = 3000)
    private String notes;

    private Boolean spoilerWarning;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @CreationTimestamp
    private LocalDateTime createdAt;
}