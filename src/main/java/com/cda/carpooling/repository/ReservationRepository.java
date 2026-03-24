package com.cda.carpooling.repository;

import com.cda.carpooling.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Retourne une réservation selon l'ID de la personne et du voyage
     */
    Optional<Reservation> findByPersonIdAndTripId(Long personId, Long tripId);

    /**
     * Retourne une réservation active selon l'id d'un personne et d'un trajet
     */
    @Query("SELECT r FROM Reservation r WHERE r.trip.id = :tripId AND r.person.id = :personId AND r.reservationStatus.label != 'CANCELLED'")
    Optional<Reservation> findByTripIdAndPersonIdAndStatusNotCancelled(
            @Param("tripId") Long tripId,
            @Param("personId") Long personId
    );
}