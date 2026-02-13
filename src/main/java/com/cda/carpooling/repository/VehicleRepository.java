package com.cda.carpooling.repository;

import com.cda.carpooling.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    /**
     * Vérifie si une personne possède déjà un véhicule.
     */
    boolean existsByPersonId(Long id);
}
