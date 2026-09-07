package com.librarylane.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "authors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;

    private String lastName;

    private String penName;

    @Column(length = 3000)
    private String biography;

    private LocalDate birthDate;

    private LocalDate deathDate;

    private String nationality;

    private String website;

    private String photoUrl;

    @JsonIgnore
    @ManyToMany(mappedBy = "authors")
    @Builder.Default
    private Set<Book> books = new HashSet<>();
}
