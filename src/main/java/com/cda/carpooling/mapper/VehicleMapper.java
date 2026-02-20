package com.cda.carpooling.mapper;

import com.cda.carpooling.dto.response.BrandResponse;
import com.cda.carpooling.dto.response.VehicleResponse;
import com.cda.carpooling.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VehicleMapper {
    private final PersonMapper personMapper;

    public VehicleResponse toResponse(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }

        return VehicleResponse.builder()
                .id(vehicle.getId())
                .model(vehicle.getModel())
                .seats(vehicle.getSeats())
                .plate(vehicle.getPlate())
                .description(vehicle.getDescription())
                .brand(BrandResponse.builder()
                        .id(vehicle.getBrand().getId())
                        .name(vehicle.getBrand().getName())
                        .build())
                .person(personMapper.toResponse(vehicle.getPerson()))
                .build();
    }
}
