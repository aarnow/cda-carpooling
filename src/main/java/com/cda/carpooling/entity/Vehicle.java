package com.cda.carpooling.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Stub pour l'entité Vehicle.
 */
@Entity
@Table(name = "vehicle")
@Data
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vehicle")
    private Long id;

    @OneToOne
    @JoinColumn(name = "id_user")
    private Person person;
}