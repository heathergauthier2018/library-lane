package com.librarylane.services;

import com.librarylane.entities.Genre;
import com.librarylane.repositories.GenreRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GenreService {

    private final GenreRepository genreRepository;

    public GenreService(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    public List<Genre> getAllGenres() {
        return genreRepository.findAll();
    }

    public Genre getGenreById(Long id) {
        return genreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Genre not found with id: " + id));
    }

    public Genre createGenre(Genre genre) {
        genreRepository.findByNameIgnoreCase(genre.getName())
                .ifPresent(existing -> {
                    throw new RuntimeException("Genre already exists: " + genre.getName());
                });

        return genreRepository.save(genre);
    }

    public Genre updateGenre(Long id, Genre updatedGenre) {
        Genre existingGenre = getGenreById(id);

        existingGenre.setName(updatedGenre.getName());
        existingGenre.setDescription(updatedGenre.getDescription());

        return genreRepository.save(existingGenre);
    }

    public void deleteGenre(Long id) {
        genreRepository.deleteById(id);
    }

    public List<Genre> searchGenres(String name) {
        return genreRepository.findByNameContainingIgnoreCase(name);
    }
}