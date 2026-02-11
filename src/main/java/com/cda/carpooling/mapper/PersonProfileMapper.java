package com.cda.carpooling.mapper;

import com.cda.carpooling.dto.response.PersonProfileResponse;
import com.cda.carpooling.entity.PersonProfile;
import org.springframework.stereotype.Component;

@Component
public class PersonProfileMapper {

    public PersonProfileResponse toResponse(PersonProfile profile) {
        if (profile == null) {
            return null;
        }

        return PersonProfileResponse.builder()
                .lastname(profile.getLastname())
                .firstname(profile.getFirstname())
                .birthday(profile.getBirthday())
                .phone(profile.getPhone())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}