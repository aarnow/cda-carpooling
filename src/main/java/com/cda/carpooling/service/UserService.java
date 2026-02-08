package com.cda.carpooling.service;

import com.cda.carpooling.dto.request.CreateUserRequest;
import com.cda.carpooling.dto.request.UpdateUserRequest;
import com.cda.carpooling.dto.response.UserMinimalResponse;
import com.cda.carpooling.dto.response.UserResponse;
import com.cda.carpooling.entity.Role;
import com.cda.carpooling.entity.User;
import com.cda.carpooling.entity.UserStatus;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.exception.DuplicateResourceException;
import com.cda.carpooling.mapper.UserMapper;
import com.cda.carpooling.repository.RoleRepository;
import com.cda.carpooling.repository.UserRepository;
import com.cda.carpooling.repository.UserStatusRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Crée un nouvel utilisateur.
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Utilisateur", "email", request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        UserStatus defaultStatus = userStatusRepository.findByLabel(UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Le statut par défaut n'existe pas"));
        user.setStatus(defaultStatus);

        Role defaultRole = roleRepository.findByLabel(Role.ROLE_STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Le rôle par défaut n'existe pas"));
        user.addRole(defaultRole);

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    /**
     * Récupère tous les utilisateurs.
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAllWithProfileAndRoles().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère tous les utilisateurs (version minimale).
     */
    public List<UserMinimalResponse> getAllUsersMinimal() {
        return userRepository.findAll().stream()
                .map(userMapper::toMinimalResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un utilisateur par son ID.
     */
    @NonNull
    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdWithProfileAndRoles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
        return userMapper.toResponse(user);
    }

    /**
     * Récupère un utilisateur par son email.
     */
    @NonNull
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmailWithProfileAndRoles(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "email", email));
        return userMapper.toResponse(user);
    }

    /**
     * Met à jour un utilisateur existant.
     */
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findByIdWithProfileAndRoles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Utilisateur", "email", request.getEmail());
            }
        }

        if (request.getPassword() != null) {
            request.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userMapper.updateEntity(user, request);
        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    /**
     * Supprime un utilisateur.
     *
     * Anonymise toutes les données personnelles.
     *
     * Conserve :
     * - Les relations (réservations, trajets) pour obligations légales
     * - L'ID pour intégrité référentielle
     *
     * @param id L'ID de l'utilisateur
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        anonymizePersonalData(user);
        user.setDeletedAt(LocalDateTime.now());
        UserStatus deletedStatus = userStatusRepository.findByLabel(UserStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Le statut DELETED n'existe pas"));
        user.setStatus(deletedStatus);

        userRepository.save(user);
    }

    /**
     * Anonymise les données personnelles d'un utilisateur.
     * Appelé lors d'une demande de soft delete.
     */
    private void anonymizePersonalData(User user) {
        user.setEmail("deleted-" + UUID.randomUUID() + "@anonymized.local");
        user.setPassword("DELETED");

        if (user.getProfile() != null) {
            user.getProfile().setFirstname("Utilisateur");
            user.getProfile().setLastname("Supprimé");
            user.getProfile().setPhone(null);
            user.getProfile().setBirthday(null);
            user.getProfile().setAvatarUrl(null);
        }
    }

    /**
     * Supprime définitivement un utilisateur.
     */
    @Transactional
    public void hardDeleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur", "id", id);
        }
        userRepository.deleteById(id);
    }

    /**
     * Assigne un rôle à un utilisateur.
     */
    @Transactional
    public UserResponse assignRole(Long userId, String roleLabel) {
        User user = userRepository.findByIdWithProfileAndRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", userId));

        Role role = roleRepository.findByLabel(roleLabel)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle", "label", roleLabel));

        user.addRole(role);
        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    /**
     * Retire un rôle d'un utilisateur.
     */
    @Transactional
    public UserResponse removeRole(Long userId, String roleLabel) {
        User user = userRepository.findByIdWithProfileAndRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", userId));

        Role role = roleRepository.findByLabel(roleLabel)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle", "label", roleLabel));

        user.removeRole(role);
        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }
}