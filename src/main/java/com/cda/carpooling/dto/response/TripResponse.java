package com.cda.carpooling.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Version complète pour le détail d'un trajet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {

    private Long id;
    private LocalDateTime tripDatetime;
    private int availableSeats;
    private boolean smokingAllowed;
    private String tripStatus;
    private Double distanceKm;
    private Integer durationMinutes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Adresses
    private AddressResponse departureAddress;
    private AddressResponse arrivingAddress;

    // Conducteur
    private PersonResponse driver;

    // TODO
    private VehicleMinimalResponse vehicle;

    // Réservations
    private List<ReservationMinimalResponse> reservations;
}