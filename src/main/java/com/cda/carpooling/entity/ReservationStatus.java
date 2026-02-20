package com.cda.carpooling.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "reservation_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "reservations")
public class ReservationStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reservation_status")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "label", nullable = false, unique = true, length = 50)
    private String label;

    //relations
    @OneToMany(mappedBy = "reservationStatus")
    @Builder.Default
    private Set<Reservation> reservations = new HashSet<>();

    //constantes
    public static final String CONFIRMED = "CONFIRMED";
    public static final String CANCELLED = "CANCELLED";
}
