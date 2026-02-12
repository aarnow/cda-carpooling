package com.cda.carpooling.repository;

import com.cda.carpooling.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour l'entité Role.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Recherche un rôle par son label.
     */
    Optional<Role> findByLabel(String label);
}