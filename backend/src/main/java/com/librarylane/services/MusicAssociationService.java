package com.librarylane.services;

import com.librarylane.entities.Book;
import com.librarylane.entities.MusicAssociation;
import com.librarylane.entities.ReadingExperience;
import com.librarylane.enums.MusicAssociationType;
import com.librarylane.enums.MusicPlatform;
import com.librarylane.enums.MusicType;
import com.librarylane.repositories.BookRepository;
import com.librarylane.repositories.MusicAssociationRepository;
import com.librarylane.repositories.ReadingExperienceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MusicAssociationService {

    private final MusicAssociationRepository musicAssociationRepository;
    private final BookRepository bookRepository;
    private final ReadingExperienceRepository readingExperienceRepository;

    public MusicAssociationService(
            MusicAssociationRepository musicAssociationRepository,
            BookRepository bookRepository,
            ReadingExperienceRepository readingExperienceRepository) {

        this.musicAssociationRepository = musicAssociationRepository;
        this.bookRepository = bookRepository;
        this.readingExperienceRepository = readingExperienceRepository;
    }

    public List<MusicAssociation> getAllMusicAssociations() {
        return musicAssociationRepository.findAll();
    }

    public MusicAssociation getMusicAssociationById(Long id) {
        return musicAssociationRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Music association not found with id: " + id));
    }

    public MusicAssociation createMusicAssociation(MusicAssociation musicAssociation) {

        if (musicAssociation.getBook() != null) {

            Long bookId = musicAssociation.getBook().getId();

            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() ->
                            new RuntimeException("Book not found with id: " + bookId));

            musicAssociation.setBook(book);
        }

        if (musicAssociation.getReadingExperience() != null) {

            Long experienceId =
                    musicAssociation.getReadingExperience().getId();

            ReadingExperience experience =
                    readingExperienceRepository.findById(experienceId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Reading Experience not found with id: "
                                                    + experienceId));

            musicAssociation.setReadingExperience(experience);
        }

        return musicAssociationRepository.save(musicAssociation);
    }

    public MusicAssociation updateMusicAssociation(
            Long id,
            MusicAssociation updatedMusicAssociation) {

        MusicAssociation existing =
                getMusicAssociationById(id);

        existing.setTitle(updatedMusicAssociation.getTitle());
        existing.setArtist(updatedMusicAssociation.getArtist());
        existing.setPlaylistName(updatedMusicAssociation.getPlaylistName());
        existing.setPlatform(updatedMusicAssociation.getPlatform());
        existing.setMusicType(updatedMusicAssociation.getMusicType());
        existing.setAssociationType(updatedMusicAssociation.getAssociationType());
        existing.setExternalUrl(updatedMusicAssociation.getExternalUrl());
        existing.setEmbedUrl(updatedMusicAssociation.getEmbedUrl());
        existing.setAssociationDescription(
                updatedMusicAssociation.getAssociationDescription());
        existing.setNotes(updatedMusicAssociation.getNotes());

        return musicAssociationRepository.save(existing);
    }

    public void deleteMusicAssociation(Long id) {
        musicAssociationRepository.deleteById(id);
    }

    public List<MusicAssociation> getByBookId(Long bookId) {
        return musicAssociationRepository.findByBookId(bookId);
    }

    public List<MusicAssociation> getByReadingExperienceId(
            Long readingExperienceId) {

        return musicAssociationRepository
                .findByReadingExperienceId(readingExperienceId);
    }

    public List<MusicAssociation> getByPlatform(
            MusicPlatform platform) {

        return musicAssociationRepository.findByPlatform(platform);
    }

    public List<MusicAssociation> getByMusicType(
            MusicType musicType) {

        return musicAssociationRepository.findByMusicType(musicType);
    }

    public List<MusicAssociation> getByAssociationType(
            MusicAssociationType associationType) {

        return musicAssociationRepository
                .findByAssociationType(associationType);
    }
}