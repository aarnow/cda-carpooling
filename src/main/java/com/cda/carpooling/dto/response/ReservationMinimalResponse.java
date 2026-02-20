package com.cda.carpooling.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationMinimalResponse {

    private Long id;
    private String reservationStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Personne
    private PersonMinimalResponse passenger;

    // Trajet
    private Long tripId;
    private LocalDateTime tripDatetime;
    private String departureCityName;
    private String arrivingCityName;
}