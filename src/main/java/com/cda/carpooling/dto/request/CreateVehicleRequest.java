package com.cda.carpooling.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVehicleRequest {

    @NotNull(message = "La marque est obligatoire")
    private Long brandId;

    @NotBlank(message = "Le modèle est obligatoire")
    @Size(max = 60, message = "Le modèle ne peut pas dépasser 60 caractères")
    private String model;

    @Min(value = 1, message = "Le nombre de places doit être au moins 1")
    @Max(value = 9, message = "Le nombre de places ne peut pas dépasser 9")
    private int seats;

    @NotBlank(message = "La plaque d'immatriculation est obligatoire")
    @Pattern(
            regexp = "^[A-Z]{2}-\\d{3}-[A-Z]{2}$",
            message = "La plaque doit respecter le format français : AB-123-CD"
    )
    private String plate;

    @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères")
    private String description;

    /**
     * ID de la personne (optionnel pour permettre aux admins de cibler un utilisateur).
     * Si absent, utilise l'utilisateur connecté.
     * TODO : 🦥 Idéalement, il faudrait l'id de l'utilisateur dans un endpoint [POST - /persons/{id}/vehicles]
     */
    private Long personId;
}