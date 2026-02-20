package com.cda.carpooling.repository;

import com.cda.carpooling.entity.PersonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonStatusRepository extends JpaRepository<PersonStatus, Long> {

    /**
     * Recherche un statut par son label.
     */
    Optional<PersonStatus> findByLabel(String label);
}