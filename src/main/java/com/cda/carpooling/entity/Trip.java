package com.cda.carpooling.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Stub pour l'entité Reservation.
 */
@Entity
@Table(name = "trip")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"driver", "tripStatus", "departureAddress", "arrivingAddress", "reservations"})
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_trip")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "trip_datetime", nullable = false)
    private LocalDateTime tripDatetime;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Column(name = "smoking_allowed", nullable = false)
    private boolean smokingAllowed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    //relations
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn (name = "id_driver", nullable = false)
    private Person driver;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_trip_status", nullable = false)
    private TripStatus tripStatus;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn (name = "id_departure_address", nullable = false)
    private Address departureAddress;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_arriving_address", nullable = false)
    private Address arrivingAddress;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Reservation> reservations = new HashSet<>();
}
