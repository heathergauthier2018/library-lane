package com.librarylane.repositories;

import com.librarylane.entities.MusicAssociation;
import com.librarylane.enums.MusicAssociationType;
import com.librarylane.enums.MusicPlatform;
import com.librarylane.enums.MusicType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MusicAssociationRepository extends JpaRepository<MusicAssociation, Long> {

    List<MusicAssociation> findByBookId(Long bookId);

    List<MusicAssociation> findByReadingExperienceId(Long readingExperienceId);

    List<MusicAssociation> findByPlatform(MusicPlatform platform);

    List<MusicAssociation> findByMusicType(MusicType musicType);

    List<MusicAssociation> findByAssociationType(MusicAssociationType associationType);

    List<MusicAssociation> findTop5ByOrderByCreatedAtDesc();
}