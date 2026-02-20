package com.cda.carpooling.repository;

import com.cda.carpooling.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    /**
     * Vérifie si une marque existe par son nom.
     */
    boolean existsByName(String name);

    /**
     * Retourne les marques par ordre alphabétique
     */
    List<Brand> findAllByOrderByNameAsc();

    /**
     * Retourne une marque selon son nom
     */
    Optional<Brand> findByName(String name);
}