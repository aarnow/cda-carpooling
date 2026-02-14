package com.cda.carpooling.repository;

import com.cda.carpooling.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    /**
     * Vérifie si une ville existe par son nom.
     */
    boolean existsByName(String name);

    /**
     * Retourne la liste des villes
     */
    List<City> findAllByOrderByNameAsc();

    /**
     * Retourne une ville selon son nom
     */
    Optional<City> findByName(String name);

    /**
     * Retourne les villes selon l'occurence d'un texte dans leurs noms
     */
    List<City> findAllByNameContainingIgnoreCase(String name);
}
