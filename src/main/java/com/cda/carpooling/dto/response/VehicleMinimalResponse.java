package com.cda.carpooling.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleMinimalResponse {

    private Long id;
    private String model;
    private int seats;
    private String plate;
    private String description;

    private BrandResponse brand;
}