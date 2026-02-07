package com.cda.carpooling.mapper;

import com.cda.carpooling.dto.request.CreateUserRequest;
import com.cda.carpooling.dto.request.UpdateUserRequest;
import com.cda.carpooling.dto.response.UserResponse;
import com.cda.carpooling.entity.Role;
import com.cda.carpooling.entity.User;
import com.cda.carpooling.entity.UserProfile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public User toEntity(CreateUserRequest request) {
        return User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();
    }

    public void updateEntity(User user, UpdateUserRequest request) {
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null) {
            user.setPassword(request.getPassword());
        }

        UserProfile profile = user.getProfile();
        if (profile != null) {
            if (request.getLastname() != null) {
                profile.setLastname(request.getLastname());
            }
            if (request.getFirstname() != null) {
                profile.setFirstname(request.getFirstname());
            }
            if (request.getBirthday() != null) {
                profile.setBirthday(request.getBirthday());
            }
            if (request.getPhone() != null) {
                profile.setPhone(request.getPhone());
            }
            if (request.getAvatarUrl() != null) {
                profile.setAvatarUrl(request.getAvatarUrl());
            }
        }
    }

    public UserResponse toResponse(User user) {
        UserProfile profile = user.getProfile();

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .lastname(profile != null ? profile.getLastname() : null)
                .firstname(profile != null ? profile.getFirstname() : null)
                .birthday(profile != null ? profile.getBirthday() : null)
                .phone(profile != null ? profile.getPhone() : null)
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .status(user.getStatus() != null ? user.getStatus().getLabel() : null)
                .roles(mapRoles(user.getRoles()))
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .updatedAt(profile != null ? profile.getUpdatedAt() : null)
                .build();
    }

    private Set<String> mapRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        return roles.stream()
                .map(Role::getLabel)
                .collect(Collectors.toSet());
    }
}