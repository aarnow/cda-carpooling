package com.cda.carpooling.repository;

import com.cda.carpooling.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long>, JpaSpecificationExecutor<Trip> {

    /**
     * Retourne tous les trajets d'un conducteur.
     */
    List<Trip> findAllByDriverId(Long driverId);

    /**
     * Retourne tous les trajets ayant encore des places disponibles
     * et dont la date est dans le futur.
     */
    @Query("SELECT t FROM Trip t WHERE t.availableSeats > 0 AND t.tripDatetime > :now")
    List<Trip> findAvailableTrips(@Param("now") LocalDateTime now);

    /**
     * Retourne tous les trajets d'un conducteur selon un statut donné.
     */
    List<Trip> findAllByDriverIdAndTripStatusLabel(Long driverId, String statusLabel);

    /**
     * Vérifie si un conducteur a un trajet actif.
     */
    @Query("SELECT COUNT(t) > 0 FROM Trip t WHERE t.driver.id = :driverId " +
            "AND t.tripStatus.label IN (:status)")
    boolean hasActiveTrip(@Param("driverId") Long driverId, @Param("status") List<String> status);
}
