package com.cda.carpooling.service;

import com.cda.carpooling.dto.request.CreateUserRequest;
import com.cda.carpooling.dto.request.UpdateUserRequest;
import com.cda.carpooling.dto.response.UserResponse;
import com.cda.carpooling.entity.Role;
import com.cda.carpooling.entity.User;
import com.cda.carpooling.entity.UserStatus;
import com.cda.carpooling.exception.DuplicateResourceException;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.mapper.UserMapper;
import com.cda.carpooling.repository.RoleRepository;
import com.cda.carpooling.repository.UserRepository;
import com.cda.carpooling.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service gérant la logique métier des utilisateurs.
 * Respecte les principes SOLID :
 * - Single Responsibility : gestion uniquement des utilisateurs
 * - Open/Closed : extensible via injection de dépendances
 * - Dependency Inversion : dépend des abstractions (repositories)
 */
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
     *
     * @param request Les données de l'utilisateur à créer
     * @return L'utilisateur créé
     * @throws DuplicateResourceException si l'email existe déjà
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Utilisateur", "email", request.getEmail());
        }

        // Convertir DTO vers entity
        User user = userMapper.toEntity(request);

        // Hasher le mot de passe
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Définir le statut par défaut (PENDING ou ACTIVE)
        UserStatus defaultStatus = userStatusRepository.findByLabel(UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Le statut par défaut n'existe pas"));
        user.setStatus(defaultStatus);

        // Assigner le rôle par défaut (STUDENT)
        Role defaultRole = roleRepository.findByLabel(Role.ROLE_STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Le rôle par défaut n'existe pas"));
        user.addRole(defaultRole);

        // Sauvegarder
        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    /**
     * Récupère tous les utilisateurs.
     *
     * @return Liste de tous les utilisateurs
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAllWithProfileAndRoles().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un utilisateur par son ID.
     *
     * @param id L'ID de l'utilisateur
     * @return L'utilisateur trouvé
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
     */
    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdWithProfileAndRoles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        return userMapper.toResponse(user);
    }

    /**
     * Récupère un utilisateur par son email.
     *
     * @param email L'email de l'utilisateur
     * @return L'utilisateur trouvé
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
     */
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmailWithProfileAndRoles(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "email", email));

        return userMapper.toResponse(user);
    }

    /**
     * Met à jour un utilisateur existant.
     * Applique un patch update : seuls les champs non null sont mis à jour.
     *
     * @param id L'ID de l'utilisateur à mettre à jour
     * @param request Les données à mettre à jour
     * @return L'utilisateur mis à jour
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
     * @throws DuplicateResourceException si le nouvel email existe déjà
     */
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        // Vérifier que l'utilisateur existe
        User user = userRepository.findByIdWithProfileAndRoles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        // Vérifier l'unicité de l'email si modifié
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Utilisateur", "email", request.getEmail());
            }
        }

        // Hasher le nouveau mot de passe si fourni
        if (request.getPassword() != null) {
            request.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Appliquer les modifications
        userMapper.updateEntity(user, request);

        // Sauvegarder
        User updatedUser = userRepository.save(user);

        return userMapper.toResponse(updatedUser);
    }

    /**
     * Supprime un utilisateur (soft delete : change le statut).
     *
     * @param id L'ID de l'utilisateur à supprimer
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        // Soft delete : changer le statut en DELETED
        UserStatus deletedStatus = userStatusRepository.findByLabel(UserStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Le statut DELETED n'existe pas"));

        user.setStatus(deletedStatus);
        userRepository.save(user);
    }

    /**
     * Supprime définitivement un utilisateur (hard delete).
     * À utiliser avec précaution !
     *
     * @param id L'ID de l'utilisateur à supprimer définitivement
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
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
     *
     * @param userId L'ID de l'utilisateur
     * @param roleLabel Le label du rôle à assigner
     * @return L'utilisateur mis à jour
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
     *
     * @param userId L'ID de l'utilisateur
     * @param roleLabel Le label du rôle à retirer
     * @return L'utilisateur mis à jour
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