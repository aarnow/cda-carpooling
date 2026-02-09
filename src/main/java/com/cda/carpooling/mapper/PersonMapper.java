package com.cda.carpooling.mapper;

import com.cda.carpooling.dto.request.CreatePersonRequest;
import com.cda.carpooling.dto.request.UpdatePersonRequest;
import com.cda.carpooling.dto.response.PersonMinimalResponse;
import com.cda.carpooling.dto.response.PersonProfileResponse;
import com.cda.carpooling.dto.response.PersonResponse;
import com.cda.carpooling.entity.Role;
import com.cda.carpooling.entity.Person;
import com.cda.carpooling.entity.PersonProfile;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper pour convertir entre Person et ses DTOs (request/response).
 * @see Person
 * @see CreatePersonRequest
 * @see UpdatePersonRequest
 * @see PersonResponse
 */
@Component
public class PersonMapper {

    /**
     * Convertit CreatePersonRequest → Person (entité).
     * Crée une nouvelle entité Person avec uniquement email et password.
     *
     * @param request DTO contenant les données de création
     * @return Person
     */
    public Person toEntity(CreatePersonRequest request) {
        return Person.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();
    }

    /**
     * Met à jour une entité Person existante avec les données d'UpdatePersonRequest.
     * Applique uniquement les champs non-null.
     *
     * @param person Entité Person existante à modifier
     * @param request DTO contenant les nouvelles valeurs
     */
    public void updateEntity(Person person, UpdatePersonRequest request) {
        if (request.getEmail() != null) {
            person.setEmail(request.getEmail());
        }
        if (request.getPassword() != null) {
            person.setPassword(request.getPassword());
        }
    }

    /**
     * Convertit Person → PersonResponse.
     * Inclut tous les détails : profil, rôles.
     *
     * @param person Entité Person à convertir
     * @return PersonResponse DTO avec toutes les informations
     */
    public PersonResponse toResponse(Person person) {
        return PersonResponse.builder()
                .id(person.getId())
                .email(person.getEmail())
                .status(person.getStatus() != null ? person.getStatus().getLabel() : null)
                .createdAt(person.getCreatedAt())
                .lastLogin(person.getLastLogin())
                .profile(toProfileResponse(person.getProfile()))
                .roles(mapRolesSafely(person.getRoles()))
                .build();
    }

    /**
     * Convertit PersonProfile → PersonProfileResponse.
     * Retourne null si le profil n'existe pas.
     *
     * @param profile Entité PersonProfile
     * @return PersonProfileResponse DTO ou null si profile inexistant
     */
    private PersonProfileResponse toProfileResponse(PersonProfile profile) {
        if (profile == null) {
            return null;
        }

        return PersonProfileResponse.builder()
                .firstname(profile.getFirstname())
                .lastname(profile.getLastname())
                .phone(profile.getPhone())
                .birthday(profile.getBirthday())
                .avatarUrl(profile.getAvatarUrl())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    /**
     * Convertit Person → PersonMinimalResponse.
     * N'inclut PAS les relations (profil, rôles) pour optimiser les performances.
     *
     * @param person Entité Person à convertir
     * @return PersonMinimalResponse DTO avec uniquement les infos essentielles
     */
    public PersonMinimalResponse toMinimalResponse(Person person) {
        return PersonMinimalResponse.builder()
                .id(person.getId())
                .email(person.getEmail())
                .status(person.getStatus() != null ? person.getStatus().getLabel() : null)
                .createdAt(person.getCreatedAt())
                .build();
    }

    /**
     * Convertit Set<Role> → Set<String> de manière sécurisée.
     * Crée une copie défensive du Set pour éviter ConcurrentModificationException
     * si la collection Hibernate est lazy-loadée et modifiée pendant l'itération.
     * Extrait uniquement les labels des rôles (ex: "ROLE_STUDENT", "ROLE_DRIVER").
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