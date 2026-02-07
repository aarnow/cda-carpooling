package com.cda.carpooling.dto.request;

import com.cda.carpooling.validation.ValidAge;
import com.cda.carpooling.validation.ValidPhone;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO pour la mise à jour d'un utilisateur.
 * Tous les champs sont optionnels (patch update).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @Email(message = "L'email doit être valide")
    @Size(max = 100, message = "L'email ne peut pas dépasser 100 caractères")
    private String email;

    @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial"
    )
    private String password;

    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s'-]+$", message = "Le nom ne peut contenir que des lettres, espaces, apostrophes et tirets")
    private String lastname;

    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s'-]+$", message = "Le prénom ne peut contenir que des lettres, espaces, apostrophes et tirets")
    private String firstname;

    @Past(message = "La date de naissance doit être dans le passé")
    @ValidAge(min = 16, message = "L'utilisateur doit avoir au moins 16 ans")
    private LocalDate birthday;

    @ValidPhone(message = "Le numéro de téléphone n'est pas valide")
    private String phone;

    @Size(max = 500, message = "L'URL de l'avatar ne peut pas dépasser 500 caractères")
    @Pattern(
            regexp = "^(https?://.*\\.(jpg|jpeg|png|gif|webp))$",
            message = "L'URL de l'avatar doit être une URL valide pointant vers une image (jpg, jpeg, png, gif, webp)"
    )
    private String avatarUrl;
}