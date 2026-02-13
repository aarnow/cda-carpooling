package com.cda.carpooling.repository;

import com.cda.carpooling.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}