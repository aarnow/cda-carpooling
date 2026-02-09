package com.cda.carpooling.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Entité représentant le statut d'un utilisateur (actif, suspendu, etc.).
 * Table de référence.
 */
@Entity
@Table(name = "person_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_person_status")
    private Long id;

    @Column(name = "label", nullable = false, unique = true, length = 50)
    private String label;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "status")
    @Builder.Default
    private Set<Person> people = new HashSet<>();

    // Constantes pour les statuts standards
    public static final String ACTIVE = "ACTIVE";
    public static final String PENDING = "PENDING";
    public static final String SUSPENDED = "SUSPENDED";
    public static final String DELETED = "DELETED";
}