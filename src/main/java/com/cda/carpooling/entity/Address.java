package com.cda.carpooling.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"city", "departureTrips", "arrivingTrips"})
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_address")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "street_number", length = 10)
    private String streetNumber;

    @Column(name = "street_name", nullable = false, length = 150)
    private String streetName;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "validated", nullable = false)
    private boolean validated = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    //relation
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_city", nullable = false)
    private City city;

    @OneToMany(mappedBy = "departureAddress")
    @Builder.Default
    private Set<Trip> departureTrips =  new HashSet<>();

    @OneToMany(mappedBy = "arrivingAddress")
    @Builder.Default
    private Set<Trip> arrivingTrips = new HashSet<>();
}
