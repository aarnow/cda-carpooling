package com.cda.carpooling.repository;

import com.cda.carpooling.entity.Person;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    /**
     * Vérifie si un utilisateur existe avec l'email donné.
     */
    boolean existsByEmail(String email);

    /**
     * Trouve un utilisateur par email avec son profil et ses rôles (EAGER).
     */
    @EntityGraph(attributePaths = {"profile", "roles", "status"})
    @Query("SELECT u FROM Person u WHERE u.email = :email")
    Optional<Person> findByEmailWithProfileAndRoles(@Param("email") String email);

    /**
     * Trouve un utilisateur par ID avec son profil et ses rôles (EAGER).
     */
    @EntityGraph(attributePaths = {"profile", "roles", "status"})
    @Query("SELECT u FROM Person u WHERE u.id = :id")
    Optional<Person> findByIdWithProfileAndRoles(@Param("id") Long id);

    /**
     * Récupère tous les utilisateurs avec leurs relations (EAGER).
     */
    @EntityGraph(attributePaths = {"profile", "roles", "status"})
    @Query("SELECT u FROM Person u")
    List<Person> findAllWithProfileAndRoles();
}