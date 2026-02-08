package com.cda.carpooling.mapper;

import com.cda.carpooling.dto.request.CreateUserRequest;
import com.cda.carpooling.dto.request.UpdateUserRequest;
import com.cda.carpooling.dto.response.UserMinimalResponse;
import com.cda.carpooling.dto.response.UserProfileResponse;
import com.cda.carpooling.dto.response.UserResponse;
import com.cda.carpooling.entity.Role;
import com.cda.carpooling.entity.User;
import com.cda.carpooling.entity.UserProfile;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper pour convertir entre User et ses DTOs (request/response).
 * @see User
 * @see CreateUserRequest
 * @see UpdateUserRequest
 * @see UserResponse
 */
@Component
public class UserMapper {

    /**
     * Convertit CreateUserRequest → User (entité).
     * Crée une nouvelle entité User avec uniquement email et password.
     *
     * @param request DTO contenant les données de création
     * @return User
     */
    public User toEntity(CreateUserRequest request) {
        return User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();
    }

    /**
     * Met à jour une entité User existante avec les données d'UpdateUserRequest.
     * Applique uniquement les champs non-null.
     *
     * @param user Entité User existante à modifier
     * @param request DTO contenant les nouvelles valeurs
     */
    public void updateEntity(User user, UpdateUserRequest request) {
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null) {
            user.setPassword(request.getPassword());
        }
    }

    /**
     * Convertit User → UserResponse.
     * Inclut tous les détails : profil, rôles.
     *
     * @param user Entité User à convertir
     * @return UserResponse DTO avec toutes les informations
     */
    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .status(user.getStatus() != null ? user.getStatus().getLabel() : null)
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .profile(toProfileResponse(user.getProfile()))
                .roles(mapRolesSafely(user.getRoles()))
                .build();
    }

    /**
     * Convertit UserProfile → UserProfileResponse.
     * Retourne null si le profil n'existe pas.
     *
     * @param profile Entité UserProfile
     * @return UserProfileResponse DTO ou null si profile inexistant
     */
    private UserProfileResponse toProfileResponse(UserProfile profile) {
        if (profile == null) {
            return null;
        }

        return UserProfileResponse.builder()
                .firstname(profile.getFirstname())
                .lastname(profile.getLastname())
                .phone(profile.getPhone())
                .birthday(profile.getBirthday())
                .avatarUrl(profile.getAvatarUrl())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    /**
     * Convertit User → UserMinimalResponse.
     * N'inclut PAS les relations (profil, rôles) pour optimiser les performances.
     *
     * @param user Entité User à convertir
     * @return UserMinimalResponse DTO avec uniquement les infos essentielles
     */
    public UserMinimalResponse toMinimalResponse(User user) {
        return UserMinimalResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .status(user.getStatus() != null ? user.getStatus().getLabel() : null)
                .createdAt(user.getCreatedAt())
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