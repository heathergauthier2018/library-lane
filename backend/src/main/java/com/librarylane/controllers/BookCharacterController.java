package com.librarylane.controllers;

import com.librarylane.entities.BookCharacter;
import com.librarylane.services.BookCharacterService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/book-characters")
@CrossOrigin(origins = "*")
public class BookCharacterController {

    private final BookCharacterService bookCharacterService;

    public BookCharacterController(
            BookCharacterService bookCharacterService) {

        this.bookCharacterService = bookCharacterService;
    }

    @GetMapping
    public List<BookCharacter> getAllCharacters() {
        return bookCharacterService.getAllCharacters();
    }

    @GetMapping("/{id}")
    public BookCharacter getCharacterById(@PathVariable Long id) {
        return bookCharacterService.getCharacterById(id);
    }

    @PostMapping
    public BookCharacter createCharacter(
            @RequestBody BookCharacter character) {

        return bookCharacterService.createCharacter(character);
    }

    @PutMapping("/{id}")
    public BookCharacter updateCharacter(
            @PathVariable Long id,
            @RequestBody BookCharacter character) {

        return bookCharacterService.updateCharacter(id, character);
    }

    @DeleteMapping("/{id}")
    public void deleteCharacter(@PathVariable Long id) {
        bookCharacterService.deleteCharacter(id);
    }

    @GetMapping("/book/{bookId}")
    public List<BookCharacter> getCharactersByBookId(
            @PathVariable Long bookId) {

        return bookCharacterService.getCharactersByBookId(bookId);
    }

    @GetMapping("/favorites")
    public List<BookCharacter> getFavoriteCharacters() {
        return bookCharacterService.getFavoriteCharacters();
    }
}