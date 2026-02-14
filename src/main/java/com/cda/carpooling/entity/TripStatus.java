package com.cda.carpooling.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "trip_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "trips")
public class TripStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_trip_status")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "label", nullable = false, unique = true, length = 50)
    private String label;

    //relations
    @OneToMany(mappedBy = "tripStatus")
    @Builder.Default
    private Set<Trip> trips = new HashSet<>();

    //constantes
    public static final String PLANNED = "PLANNED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";
}
