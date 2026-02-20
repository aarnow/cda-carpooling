package com.cda.carpooling.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCityRequest {

    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String name;

    @Pattern(
            regexp = "^\\d{5}$",
            message = "Le code postal doit contenir exactement 5 chiffres"
    )
    private String postalCode;
}