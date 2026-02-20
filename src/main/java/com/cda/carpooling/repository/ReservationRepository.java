package com.cda.carpooling.repository;

import com.cda.carpooling.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Retourne une réservation selon l'ID de la personne et du voyage
     */
    Optional<Reservation> findByPersonIdAndTripId(Long personId, Long tripId);
}