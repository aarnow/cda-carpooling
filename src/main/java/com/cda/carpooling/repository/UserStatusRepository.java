package com.cda.carpooling.repository;

import com.cda.carpooling.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour l'entité UserStatus.
 */
@Repository
public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {

    /**
     * Recherche un statut par son label.
     */
    Optional<UserStatus> findByLabel(String label);
}