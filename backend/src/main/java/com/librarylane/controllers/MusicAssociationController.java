package com.librarylane.controllers;

import com.librarylane.entities.MusicAssociation;
import com.librarylane.enums.MusicAssociationType;
import com.librarylane.enums.MusicPlatform;
import com.librarylane.enums.MusicType;
import com.librarylane.services.MusicAssociationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/music-associations")
@CrossOrigin(origins = "*")
public class MusicAssociationController {

    private final MusicAssociationService musicAssociationService;

    public MusicAssociationController(
            MusicAssociationService musicAssociationService) {

        this.musicAssociationService = musicAssociationService;
    }

    @GetMapping
    public List<MusicAssociation> getAllMusicAssociations() {
        return musicAssociationService.getAllMusicAssociations();
    }

    @GetMapping("/{id}")
    public MusicAssociation getMusicAssociationById(
            @PathVariable Long id) {

        return musicAssociationService.getMusicAssociationById(id);
    }

    @PostMapping
    public MusicAssociation createMusicAssociation(
            @RequestBody MusicAssociation musicAssociation) {

        return musicAssociationService
                .createMusicAssociation(musicAssociation);
    }

    @PutMapping("/{id}")
    public MusicAssociation updateMusicAssociation(
            @PathVariable Long id,
            @RequestBody MusicAssociation musicAssociation) {

        return musicAssociationService
                .updateMusicAssociation(id, musicAssociation);
    }

    @DeleteMapping("/{id}")
    public void deleteMusicAssociation(
            @PathVariable Long id) {

        musicAssociationService.deleteMusicAssociation(id);
    }

    @GetMapping("/book/{bookId}")
    public List<MusicAssociation> getByBookId(
            @PathVariable Long bookId) {

        return musicAssociationService.getByBookId(bookId);
    }

    @GetMapping("/reading-experience/{readingExperienceId}")
    public List<MusicAssociation> getByReadingExperienceId(
            @PathVariable Long readingExperienceId) {

        return musicAssociationService
                .getByReadingExperienceId(readingExperienceId);
    }

    @GetMapping("/platform/{platform}")
    public List<MusicAssociation> getByPlatform(
            @PathVariable MusicPlatform platform) {

        return musicAssociationService.getByPlatform(platform);
    }

    @GetMapping("/type/{musicType}")
    public List<MusicAssociation> getByMusicType(
            @PathVariable MusicType musicType) {

        return musicAssociationService.getByMusicType(musicType);
    }

    @GetMapping("/association/{associationType}")
    public List<MusicAssociation> getByAssociationType(
            @PathVariable MusicAssociationType associationType) {

        return musicAssociationService
                .getByAssociationType(associationType);
    }
}