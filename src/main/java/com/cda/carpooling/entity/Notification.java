package com.cda.carpooling.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Stub pour l'entité Notification.
 */
@Entity
@Table(name = "notification")
@Data
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notification")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_user")
    private User user;
}