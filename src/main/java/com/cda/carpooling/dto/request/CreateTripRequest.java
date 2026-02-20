package com.cda.carpooling.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripRequest {

    @NotNull(message = "La date et l'heure du trajet sont obligatoires")
    @Future(message = "La date du trajet doit être dans le futur")
    private LocalDateTime tripDatetime;

    @Min(value = 1, message = "Le nombre de places disponibles doit être au moins 1")
    @Max(value = 8, message = "Le nombre de places disponibles ne peut pas dépasser 8")
    private int availableSeats;

    private boolean smokingAllowed = false;

    @NotNull(message = "L'adresse de départ est obligatoire")
    private Long departureAddressId;

    @NotNull(message = "L'adresse d'arrivée est obligatoire")
    private Long arrivingAddressId;
}
