package com.cda.carpooling.dto.request;

import com.cda.carpooling.validation.ValidAge;
import com.cda.carpooling.validation.ValidPhone;
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
public class UpdatePersonProfileRequest {
    @Size(max = 50, message = "Le nom ne peut pas dépasser 50 caractères")
    private String lastname;

    @Size(max = 50, message = "Le prénom ne peut pas dépasser 50 caractères")
    private String firstname;

    @ValidAge
    private LocalDate birthday;

    @ValidPhone
    private String phone;
}