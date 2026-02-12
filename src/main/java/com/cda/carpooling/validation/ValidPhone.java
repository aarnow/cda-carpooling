package com.cda.carpooling.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Annotation de validation pour les numéros de téléphone.
 * Supporte les formats français et internationaux.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
@Documented
public @interface ValidPhone {

    String message() default "Le numéro de téléphone n'est pas valide";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    boolean required() default false;
}