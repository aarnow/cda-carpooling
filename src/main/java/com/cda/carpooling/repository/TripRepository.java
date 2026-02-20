package com.cda.carpooling.repository;

import com.cda.carpooling.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long>, JpaSpecificationExecutor<Trip> {

    /**
     * Retourne tous les trajets d'un conducteur.
     */
    List<Trip> findAllByDriverId(Long driverId);

    /**
     * Retourne tous les trajets d'un passagé
     */
    @Query("SELECT t FROM Trip t JOIN t.reservations r " +
            "WHERE r.person.id = :personId")
    List<Trip> findAllByPassengerId(
            @Param("personId") Long personId);

    /**
     * Récupère tous les trajets par statut.
     */
    List<Trip> findAllByTripStatusLabel(String statusLabel);

    /**
     * Retourne tous les trajets d'un conducteur selon un statut donné.
     */
    List<Trip> findAllByDriverIdAndTripStatusLabel(Long driverId, String statusLabel);
}
