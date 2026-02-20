package com.cda.carpooling.dto.request;

import com.cda.carpooling.validation.ValidVehiclePlate;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVehicleRequest {

    private Long brandId;

    @Size(max = 60, message = "Le modèle ne peut pas dépasser 60 caractères")
    private String model;

    @Min(value = 1, message = "Le nombre de places doit être au moins 1")
    @Max(value = 9, message = "Le nombre de places ne peut pas dépasser 9")
    private Integer seats;

    @ValidVehiclePlate
    private String plate;

    @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères")
    private String description;
}