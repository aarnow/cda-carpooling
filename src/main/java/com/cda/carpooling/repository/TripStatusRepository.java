package com.cda.carpooling.repository;

import com.cda.carpooling.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TripStatusRepository extends JpaRepository<TripStatus, Long> {

    /**
     * Recherche un statut par son label.
     */
    Optional<TripStatus> findByLabel(String label);
}