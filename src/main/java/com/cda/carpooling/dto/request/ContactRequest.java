package com.cda.carpooling.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ContactRequest {

    @NotBlank(message = "Le sujet est obligatoire")
    @Size(max = 150, message = "Le sujet ne peut pas dépasser 150 caractères")
    private String subject;

    @NotBlank(message = "Le message est obligatoire")
    @Size(max = 2000, message = "Le message ne peut pas dépasser 2000 caractères")
    private String message;
}