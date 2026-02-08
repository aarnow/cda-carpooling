package com.cda.carpooling.repository;

import com.cda.carpooling.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Vérifie si un utilisateur existe avec l'email donné.
     */
    boolean existsByEmail(String email);

    /**
     * Trouve un utilisateur par email avec son profil et ses rôles (EAGER).
     */
    @EntityGraph(attributePaths = {"profile", "roles", "status"})
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailWithProfileAndRoles(@Param("email") String email);

    /**
     * Trouve un utilisateur par ID avec son profil et ses rôles (EAGER).
     */
    @EntityGraph(attributePaths = {"profile", "roles", "status"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithProfileAndRoles(@Param("id") Long id);

    /**
     * Récupère tous les utilisateurs avec leurs relations (EAGER).
     */
    @EntityGraph(attributePaths = {"profile", "roles", "status"})
    @Query("SELECT u FROM User u")
    List<User> findAllWithProfileAndRoles();
}