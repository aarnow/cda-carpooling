package com.cda.carpooling.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private Long id;
    private String streetNumber;
    private String streetName;

    private Double latitude;
    private Double longitude;
    private boolean validated;

    private CityResponse city;
}