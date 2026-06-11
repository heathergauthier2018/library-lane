package com.librarylane.services;

import com.librarylane.entities.Book;
import com.librarylane.entities.BookCharacter;
import com.librarylane.repositories.BookCharacterRepository;
import com.librarylane.repositories.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookCharacterService {

    private final BookCharacterRepository bookCharacterRepository;
    private final BookRepository bookRepository;

    public BookCharacterService(
            BookCharacterRepository bookCharacterRepository,
            BookRepository bookRepository) {

        this.bookCharacterRepository = bookCharacterRepository;
        this.bookRepository = bookRepository;
    }

    public List<BookCharacter> getAllCharacters() {
        return bookCharacterRepository.findAll();
    }

    public BookCharacter getCharacterById(Long id) {
        return bookCharacterRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Character not found with id: " + id));
    }

    public BookCharacter createCharacter(BookCharacter character) {

        Long bookId = character.getBook().getId();

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() ->
                        new RuntimeException("Book not found with id: " + bookId));

        character.setBook(book);

        return bookCharacterRepository.save(character);
    }

    public BookCharacter updateCharacter(
            Long id,
            BookCharacter updatedCharacter) {

        BookCharacter existingCharacter = getCharacterById(id);

        existingCharacter.setCharacterName(updatedCharacter.getCharacterName());
        existingCharacter.setRole(updatedCharacter.getRole());
        existingCharacter.setDescription(updatedCharacter.getDescription());
        existingCharacter.setNotes(updatedCharacter.getNotes());
        existingCharacter.setFavorite(updatedCharacter.getFavorite());

        return bookCharacterRepository.save(existingCharacter);
    }

    public void deleteCharacter(Long id) {
        bookCharacterRepository.deleteById(id);
    }

    public List<BookCharacter> getCharactersByBookId(Long bookId) {
        return bookCharacterRepository.findByBookId(bookId);
    }

    public List<BookCharacter> getFavoriteCharacters() {
        return bookCharacterRepository.findByFavoriteTrue();
    }
}