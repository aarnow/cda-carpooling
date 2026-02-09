package com.cda.carpooling.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Stub pour l'entité Booking.
 */
@Entity
@Table(name = "booking")
@Data
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_booking")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_user")
    private Person person;
}