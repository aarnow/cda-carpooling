package com.cda.carpooling.dto.response;

import com.cda.carpooling.entity.PersonProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Version allégée pour les listes de trajets.
 * Expose uniquement les informations essentielles pour la recherche.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripMinimalResponse {

    private Long id;
    private LocalDateTime tripDatetime;
    private int availableSeats;
    private boolean smokingAllowed;
    private String tripStatus;

    // Adresses
    private CityResponse departureCityName;
    private CityResponse arrivingCityName;

    // Conducteur
    private PersonMinimalResponse driver;
}