package com.cda.carpooling.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Annotation de validation pour les plaques d'immatriculation françaises.
 * Accepte différents formats et les normalise automatiquement vers AB-123-CD.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = VehiclePlateValidator.class)
@Documented
public @interface ValidVehiclePlate {

    String message() default "Format de plaque d'immatriculation invalide (attendu: AB-123-CD)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}