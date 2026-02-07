package com.cda.carpooling.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

/**
 * Ce validateur sait valider une LocalDate annotée avec @ValidAge.
 * Calcule l'âge à partir de la date de naissance.
 */
public class AgeValidator implements ConstraintValidator<ValidAge, LocalDate> {

    private int minAge;
    private int maxAge;

    @Override
    public void initialize(ValidAge constraintAnnotation) {
        this.minAge = constraintAnnotation.min();
        this.maxAge = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(LocalDate birthdate, ConstraintValidatorContext context) {
        if (birthdate == null) {
            return true;
        }

        LocalDate now = LocalDate.now();
        int age = Period.between(birthdate, now).getYears();

        return age >= minAge && age <= maxAge;
    }
}