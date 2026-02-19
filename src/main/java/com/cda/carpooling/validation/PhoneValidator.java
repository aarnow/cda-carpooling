package com.cda.carpooling.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validateur pour l'annotation @ValidPhone.
 * Accepte les formats français (06, 07, +33) et internationaux.
 */
public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private boolean required;

    // Formats acceptés :
    // - 06 12 34 56 78
    // - 0612345678
    // - +33 6 12 34 56 78
    // - +33612345678
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(?:(?:\\+|00)33|0)[1-9](?:[\\s.-]?\\d{2}){4}$"
    );

    @Override
    public void initialize(ValidPhone constraintAnnotation) {
        this.required = constraintAnnotation.required();
    }

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        if (phone == null || phone.trim().isEmpty()) {
            return !required;
        }

        String cleanPhone = phone.replaceAll("[\\s.-]", "");

        return PHONE_PATTERN.matcher(cleanPhone).matches();
    }
}