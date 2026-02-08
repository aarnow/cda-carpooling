package com.cda.carpooling.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO minimal : seulement l'essentiel (id, email, status).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMinimalResponse {

    private Long id;
    private String email;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}