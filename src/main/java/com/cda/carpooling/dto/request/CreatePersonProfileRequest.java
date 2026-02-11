package com.cda.carpooling.dto.request;

import com.cda.carpooling.validation.ValidAge;
import com.cda.carpooling.validation.ValidPhone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePersonProfileRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 50, message = "Le nom ne peut pas dépasser 50 caractères")
    private String lastname;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 50, message = "Le prénom ne peut pas dépasser 50 caractères")
    private String firstname;

    @ValidAge
    private LocalDate birthday;

    @ValidPhone
    private String phone;

    /**
     * ID de la personne (optionnel pour permettre aux admins de cibler un utilisateur).
     * Si absent, utilise l'utilisateur connecté.
     * TODO : 🦥 Idéalement, il faudrait l'id de l'utilisateur dans un endpoint [POST - /persons/{id}/profiles]
     */
    private Long personId;
}