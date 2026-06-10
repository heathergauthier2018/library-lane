package com.librarylane.repositories;

import com.librarylane.entities.MusicAssociation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MusicAssociationRepository extends JpaRepository<MusicAssociation, Long> {
}