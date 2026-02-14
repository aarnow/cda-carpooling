package com.cda.carpooling.mapper;

import com.cda.carpooling.dto.request.CreatePersonRequest;
import com.cda.carpooling.dto.request.UpdatePersonRequest;
import com.cda.carpooling.dto.response.PersonMinimalResponse;
import com.cda.carpooling.dto.response.PersonProfileResponse;
import com.cda.carpooling.dto.response.PersonResponse;
import com.cda.carpooling.entity.Role;
import com.cda.carpooling.entity.Person;
import com.cda.carpooling.entity.PersonProfile;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class PersonMapper {
    private final PersonProfileMapper personProfileMapper;


    public Person toEntity(CreatePersonRequest request) {
        return Person.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();
    }

    public PersonResponse toResponse(Person person) {
        return PersonResponse.builder()
                .id(person.getId())
                .email(person.getEmail())
                .status(person.getStatus() != null ? person.getStatus().getLabel() : null)
                .createdAt(person.getCreatedAt())
                .lastLogin(person.getLastLogin())
                .profile(personProfileMapper.toResponse(person.getProfile()))
                .roles(mapRolesSafely(person.getRoles()))
                .build();
    }

    public PersonMinimalResponse toMinimalResponse(Person person) {
        return PersonMinimalResponse.builder()
                .id(person.getId())
                .email(person.getEmail())
                .status(person.getStatus() != null ? person.getStatus().getLabel() : null)
                .createdAt(person.getCreatedAt())
                .build();
    }

    /**
     * Convertit Set<Role> en Set<String> de manière sécurisée.
     * Crée une copie défensive du Set pour éviter ConcurrentModificationException
     *
     * @param roles Collection de rôles
     * @return Set immutable de labels de rôles, ou Set.of() si vide/null
     */
    private Set<String> mapRolesSafely(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(roles).stream()
                .map(Role::getLabel)
                .collect(Collectors.toSet());
    }
}