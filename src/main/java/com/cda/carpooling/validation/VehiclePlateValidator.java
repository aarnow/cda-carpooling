package com.cda.carpooling.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validateur pour les plaques d'immatriculation françaises.
 * Accepte différents formats et les normalise automatiquement.
 */
public class VehiclePlateValidator implements ConstraintValidator<ValidVehiclePlate, String> {

    private static final Pattern NORMALIZED_PATTERN = Pattern.compile("^[A-Z]{2}-\\d{3}-[A-Z]{2}$");

    @Override
    public boolean isValid(String plate, ConstraintValidatorContext context) {
        if (plate == null || plate.trim().isEmpty()) {
            return true;
        }

        String normalized = normalize(plate);
        return normalized != null && NORMALIZED_PATTERN.matcher(normalized).matches();
    }

    /**
     * Normalise une plaque d'immatriculation au format standard AB-123-CD.
     *
     * @param plate Plaque brute (ex: "ab 123 cd", "AB123CD")
     * @return Plaque normalisée (ex: "AB-123-CD") ou null si format invalide
     */
    public static String normalize(String plate) {
        if (plate == null) return null;

        String cleaned = plate.replaceAll("[\\s.-]", "").toUpperCase();

        if (cleaned.matches("^[A-Z]{2}\\d{3}[A-Z]{2}$")) {
            // Reformater avec tirets
            return cleaned.substring(0, 2) + "-" +
                    cleaned.substring(2, 5) + "-" +
                    cleaned.substring(5, 7);
        }

        return null;
    }
}