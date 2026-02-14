package com.cda.carpooling.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @Size(max = 10, message = "Le numéro de rue ne peut pas dépasser 10 caractères")
    @NotNull(message = "La numéro de la rue est obligatoire")
    private String streetNumber;

    @NotBlank(message = "Le nom de la rue est obligatoire")
    @Size(max = 150, message = "Le nom de la rue ne peut pas dépasser 150 caractères")
    @NotNull(message = "La nom de la rue est obligatoire")
    private String streetName;

    @DecimalMin(value = "-90.0", message = "La latitude doit être comprise entre -90 et 90")
    @DecimalMax(value = "90.0", message = "La latitude doit être comprise entre -90 et 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "La longitude doit être comprise entre -180 et 180")
    @DecimalMax(value = "180.0", message = "La longitude doit être comprise entre -180 et 180")
    private Double longitude;

    @NotNull(message = "La ville est obligatoire")
    private Long cityId;
}