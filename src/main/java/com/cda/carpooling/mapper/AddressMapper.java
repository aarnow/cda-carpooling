package com.cda.carpooling.mapper;

import com.cda.carpooling.dto.request.CreateAddressRequest;
import com.cda.carpooling.dto.response.AddressResponse;
import com.cda.carpooling.entity.Address;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AddressMapper {
    private final CityMapper cityMapper;

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
                .city(cityMapper.toResponse(address.getCity()))
                .validated(address.isValidated())
                .build();
    }

    public CreateAddressRequest toRequest(Address address) {
        if(address == null){
            return null;
        }

        return CreateAddressRequest.builder()
                .streetName(address.getStreetName())
                .streetNumber(address.getStreetNumber())
                .cityId(address.getCity().getId())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .build();
    }
}
