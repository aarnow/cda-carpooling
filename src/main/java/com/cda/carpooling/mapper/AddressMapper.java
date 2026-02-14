package com.cda.carpooling.mapper;

import com.cda.carpooling.dto.request.AddressRequest;
import com.cda.carpooling.dto.response.AddressResponse;
import com.cda.carpooling.dto.response.CityResponse;
import com.cda.carpooling.entity.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {
    public AddressResponse toResponse(Address address) {
        if(address == null){
            return null;
        }

        return AddressResponse.builder()
                .id(address.getId())
                .streetNumber(address.getStreetNumber())
                .streetName(address.getStreetName())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .city(CityResponse.builder()
                        .id(address.getCity().getId())
                        .name(address.getCity().getName())
                        .postalCode(address.getCity().getPostalCode())
                        .build())
                .build();
    }

    public AddressRequest toRequest(Address address) {
        if(address == null){
            return null;
        }

        return AddressRequest.builder()
                .streetName(address.getStreetName())
                .streetNumber(address.getStreetNumber())
                .cityId(address.getCity().getId())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .build();
    }
}
