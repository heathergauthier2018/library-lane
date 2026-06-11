package com.librarylane.services;

import com.librarylane.entities.Author;
import com.librarylane.repositories.AuthorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public List<Author> getAllAuthors() {
        return authorRepository.findAll();
    }

    public Author getAuthorById(Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Author not found with id: " + id));
    }

    public Author createAuthor(Author author) {
        checkForDuplicateAuthor(author);
        return authorRepository.save(author);
    }

    public Author updateAuthor(Long id, Author updatedAuthor) {
        Author existingAuthor = getAuthorById(id);

        existingAuthor.setFirstName(updatedAuthor.getFirstName());
        existingAuthor.setLastName(updatedAuthor.getLastName());
        existingAuthor.setPenName(updatedAuthor.getPenName());
        existingAuthor.setBiography(updatedAuthor.getBiography());
        existingAuthor.setBirthDate(updatedAuthor.getBirthDate());
        existingAuthor.setDeathDate(updatedAuthor.getDeathDate());
        existingAuthor.setNationality(updatedAuthor.getNationality());
        existingAuthor.setWebsite(updatedAuthor.getWebsite());
        existingAuthor.setPhotoUrl(updatedAuthor.getPhotoUrl());

        return authorRepository.save(existingAuthor);
    }

    public void deleteAuthor(Long id) {
        authorRepository.deleteById(id);
    }

    public List<Author> searchAuthors(String keyword) {
        return authorRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrPenNameContainingIgnoreCase(
                        keyword,
                        keyword,
                        keyword
                );
    }

    private void checkForDuplicateAuthor(Author author) {
        if (author.getFirstName() != null && author.getLastName() != null) {
            authorRepository
                    .findByFirstNameIgnoreCaseAndLastNameIgnoreCase(
                            author.getFirstName(),
                            author.getLastName()
                    )
                    .ifPresent(existing -> {
                        throw new RuntimeException("Author already exists: "
                                + author.getFirstName() + " " + author.getLastName());
                    });
        }

        if (author.getPenName() != null && !author.getPenName().isBlank()) {
            authorRepository.findByPenNameIgnoreCase(author.getPenName())
                    .ifPresent(existing -> {
                        throw new RuntimeException("Author already exists with pen name: "
                                + author.getPenName());
                    });
        }
    }
}