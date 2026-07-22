package com.librarylane.services;

import com.librarylane.entities.Author;
import com.librarylane.entities.Book;
import com.librarylane.entities.Genre;
import com.librarylane.enums.ReadingStatus;
import com.librarylane.repositories.AuthorRepository;
import com.librarylane.repositories.BookRepository;
import com.librarylane.repositories.GenreRepository;
import com.librarylane.repositories.ReadingExperienceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookService {

    @PersistenceContext
    private EntityManager entityManager;

    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final ReadingExperienceRepository readingExperienceRepository;
    private final AuthorRepository authorRepository;

    public BookService(
            BookRepository bookRepository,
            GenreRepository genreRepository,
            ReadingExperienceRepository readingExperienceRepository,
            AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.genreRepository = genreRepository;
        this.readingExperienceRepository = readingExperienceRepository;
        this.authorRepository = authorRepository;
    }

    @Transactional(readOnly = true)
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Book getBookById(Long id) {
        return findBookRowById(id);
    }

    @Transactional
    public Book createBook(Book book) {
        if (book.getTitle() == null || book.getTitle().isBlank()) {
            throw new RuntimeException("Book title is required.");
        }

        if (book.getIsbn13() != null && !book.getIsbn13().isBlank() &&
                bookRepository.existsByIsbn13IgnoreCase(book.getIsbn13().trim())) {
            throw new RuntimeException(
                    "This exact ISBN edition already exists in the library.");
        }

        book.setTitle(book.getTitle().trim());
        book.setGenres(resolveGenres(book.getGenres()));
        book.setAuthors(resolveAuthors(book.getAuthors()));

        return bookRepository.save(book);
    }

    @Transactional
    public Book updateBook(Long id, Book updatedBook) {
        /*
         * Use the actual books-table row. Repository findById() generates an
         * inner join through Library/User for this entity; older rows without
         * that association are consequently (and incorrectly) reported as
         * missing even though they are returned by findAll().
         */
        Book existingBook = findBookRowById(id);

        existingBook.setTitle(updatedBook.getTitle());
        existingBook.setSubtitle(updatedBook.getSubtitle());
        existingBook.setDescription(updatedBook.getDescription());
        existingBook.setIsbn10(updatedBook.getIsbn10());
        existingBook.setIsbn13(updatedBook.getIsbn13());
        existingBook.setPublisher(updatedBook.getPublisher());
        existingBook.setPublicationDate(updatedBook.getPublicationDate());
        existingBook.setPageCount(updatedBook.getPageCount());
        existingBook.setAudiobookLengthSeconds(updatedBook.getAudiobookLengthSeconds());
        existingBook.setCoverImageUrl(updatedBook.getCoverImageUrl());
        existingBook.setLanguage(updatedBook.getLanguage());
        existingBook.setSeriesName(updatedBook.getSeriesName());
        existingBook.setSeriesNumber(updatedBook.getSeriesNumber());
        existingBook.setEditionFormat(updatedBook.getEditionFormat());
        existingBook.setNarrator(updatedBook.getNarrator());
        existingBook.setCatalogProvider(updatedBook.getCatalogProvider());
        existingBook.setCatalogProviderId(updatedBook.getCatalogProviderId());
        existingBook.setPersonalNotes(updatedBook.getPersonalNotes());
        existingBook.setPrimaryFormat(updatedBook.getPrimaryFormat());
        existingBook.setCurrentStatus(updatedBook.getCurrentStatus());
        existingBook.setFavorite(updatedBook.getFavorite());
        existingBook.setOwned(updatedBook.getOwned());
        existingBook.setWishlist(updatedBook.getWishlist());
        existingBook.setDnf(updatedBook.getDnf());

        existingBook.setGenres(resolveGenres(updatedBook.getGenres()));
        existingBook.setAuthors(resolveAuthors(updatedBook.getAuthors()));

        return bookRepository.save(existingBook);
    }

    @Transactional
    public void deleteBook(Long id) {
        entityManager.createNativeQuery(
                "DELETE FROM reading_experiences WHERE book_id = ?1"
        ).setParameter(1, id).executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM book_genres WHERE book_id = ?1"
        ).setParameter(1, id).executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM book_authors WHERE book_id = ?1"
        ).setParameter(1, id).executeUpdate();

        int deletedRows = entityManager.createNativeQuery(
                "DELETE FROM books WHERE id = ?1"
        ).setParameter(1, id).executeUpdate();

        System.out.println(
                "Deleted book rows: " + deletedRows + " for book " + id);
    }

    @Transactional(readOnly = true)
    public List<Book> searchBooksByTitle(String title) {
        return bookRepository.findByTitleContainingIgnoreCase(title);
    }

    @Transactional(readOnly = true)
    public List<Book> getWishlistBooks() {
        return bookRepository.findByWishlistTrue();
    }

    @Transactional(readOnly = true)
    public List<Book> getTbrBooks() {
        return bookRepository.findByOwnedTrueAndCurrentStatus(ReadingStatus.TBR);
    }

    @Transactional(readOnly = true)
    public List<Book> getCurrentlyReadingBooks() {
        return bookRepository.findByCurrentStatus(ReadingStatus.CURRENTLY_READING);
    }

    @Transactional(readOnly = true)
    public List<Book> getCompletedBooks() {
        return bookRepository.findByCurrentStatus(ReadingStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public List<Book> getDnfBooks() {
        return bookRepository.findByDnfTrue();
    }

    @Transactional(readOnly = true)
    public List<Book> getPausedBooks() {
        return bookRepository.findByCurrentStatus(ReadingStatus.PAUSED);
    }

    @Transactional(readOnly = true)
    public List<Book> getFavoriteBooks() {
        return bookRepository.findByFavoriteTrue();
    }

    @Transactional(readOnly = true)
    public List<Book> getOwnedBooks() {
        return bookRepository.findByOwnedTrue();
    }

    @Transactional(readOnly = true)
    public List<Book> getBooksByGenre(String genreName) {
        return bookRepository.findByGenresNameIgnoreCase(genreName);
    }

    @SuppressWarnings("unchecked")
    private Book findBookRowById(Long id) {
        List<Book> matches = entityManager.createNativeQuery(
                        "SELECT * FROM books WHERE id = ?1",
                        Book.class
                )
                .setParameter(1, id)
                .getResultList();

        if (matches.isEmpty()) {
            throw new RuntimeException("Book not found with id: " + id);
        }

        return matches.get(0);
    }

    private Set<Author> resolveAuthors(Set<Author> incomingAuthors) {
        if (incomingAuthors == null || incomingAuthors.isEmpty()) {
            return new HashSet<>();
        }

        return incomingAuthors.stream()
                .filter(author ->
                        author != null &&
                                (author.getId() != null ||
                                        (author.getPenName() != null &&
                                                !author.getPenName().isBlank())))
                .map(author -> {
                    if (author.getId() != null) {
                        return authorRepository.findById(author.getId())
                                .orElseThrow(() -> new RuntimeException(
                                        "Author not found with id: " + author.getId()));
                    }

                    String penName = author.getPenName().trim();

                    return authorRepository.findByPenNameIgnoreCase(penName)
                            .orElseGet(() -> {
                                author.setPenName(penName);

                                if (author.getFirstName() == null ||
                                        author.getFirstName().isBlank()) {
                                    author.setFirstName(penName.split(" ")[0]);
                                }

                                if (author.getLastName() == null) {
                                    author.setLastName("");
                                }

                                return authorRepository.save(author);
                            });
                })
                .collect(Collectors.toSet());
    }

    private Set<Genre> resolveGenres(Set<Genre> incomingGenres) {
        if (incomingGenres == null || incomingGenres.isEmpty()) {
            return new HashSet<>();
        }

        return incomingGenres.stream()
                .filter(genre ->
                        genre != null &&
                                (genre.getId() != null ||
                                        (genre.getName() != null &&
                                                !genre.getName().isBlank())))
                .map(genre -> {
                    if (genre.getId() != null) {
                        return genreRepository.findById(genre.getId())
                                .orElseThrow(() -> new RuntimeException(
                                        "Genre not found with id: " + genre.getId()));
                    }

                    String genreName = genre.getName().trim();

                    return genreRepository.findByNameIgnoreCase(genreName)
                            .orElseGet(() -> {
                                genre.setName(genreName);
                                return genreRepository.save(genre);
                            });
                })
                .collect(Collectors.toSet());
    }
}
