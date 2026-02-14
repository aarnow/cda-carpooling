package com.cda.carpooling.mapper;

import com.cda.carpooling.dto.response.CityResponse;
import com.cda.carpooling.entity.City;
import org.springframework.stereotype.Component;

@Component
public class CityMapper {

    public CityResponse toResponse(City city) {
        if (city == null) {
            return null;
        }

        return CityResponse.builder()
                .id(city.getId())
                .name(city.getName())
                .postalCode(city.getPostalCode())
                .build();
    }
}
