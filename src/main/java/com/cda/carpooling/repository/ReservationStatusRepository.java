package com.cda.carpooling.repository;

import com.cda.carpooling.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReservationStatusRepository extends JpaRepository<ReservationStatus, Long> {

    /**
     * Recherche un statut par son label.
     */
    Optional<ReservationStatus> findByLabel(String label);
}
