package com.cda.carpooling.controller;

import com.cda.carpooling.dto.request.CreateUserRequest;
import com.cda.carpooling.dto.request.UpdateUserRequest;
import com.cda.carpooling.dto.response.UserMinimalResponse;
import com.cda.carpooling.dto.response.UserResponse;
import com.cda.carpooling.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des utilisateurs.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users
     * Récupère tous les utilisateurs.
     *
     * Query parameters :
     * - minimal: true pour version minimale (status uniquement)
     *
     * @param minimal Si true, retourne version minimale
     * @return 200 OK avec la liste des utilisateurs
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(required = false, defaultValue = "false") Boolean minimal) {

        if (minimal != null && minimal) {
            List<UserMinimalResponse> users = userService.getAllUsersMinimal();
            return ResponseEntity.ok(users);
        }

        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/users/{id}
     * Récupère un utilisateur par son ID.
     *
     * @param id L'ID de l'utilisateur
     * @return 200 OK avec l'utilisateur
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * GET /api/users/email/{email}
     * Récupère un utilisateur par son email.
     *
     * @param email L'email de l'utilisateur
     * @return 200 OK avec l'utilisateur
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    /**
     * POST /api/users
     * Crée un nouvel utilisateur.
     *
     * @param request Les données de l'utilisateur
     * @return 201 CREATED avec l'utilisateur créé
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * PATCH /api/users/{id}
     * Met à jour un utilisateur existant.
     *
     * @param id L'ID de l'utilisateur
     * @param request Les données à mettre à jour
     * @return 200 OK avec l'utilisateur mis à jour
     */
    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse user = userService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }

    /**
     * DELETE /api/users/{id}
     * Supprime un utilisateur (soft delete).
     *
     * @param id L'ID de l'utilisateur
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/users/{id}/hard
     * Supprime définitivement un utilisateur.
     *
     * @param id L'ID de l'utilisateur
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteUser(@PathVariable Long id) {
        userService.hardDeleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/users/{userId}/roles/{roleLabel}
     * Assigne un rôle à un utilisateur.
     *
     * @param userId L'ID de l'utilisateur
     * @param roleLabel Le label du rôle
     * @return 200 OK avec l'utilisateur mis à jour
     */
    @PostMapping("/{userId}/roles/{roleLabel}")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable Long userId,
            @PathVariable String roleLabel) {
        UserResponse user = userService.assignRole(userId, roleLabel);
        return ResponseEntity.ok(user);
    }

    /**
     * DELETE /api/users/{userId}/roles/{roleLabel}
     * Retire un rôle d'un utilisateur.
     *
     * @param userId L'ID de l'utilisateur
     * @param roleLabel Le label du rôle
     * @return 200 OK avec l'utilisateur mis à jour
     */
    @DeleteMapping("/{userId}/roles/{roleLabel}")
    public ResponseEntity<UserResponse> removeRole(
            @PathVariable Long userId,
            @PathVariable String roleLabel) {
        UserResponse user = userService.removeRole(userId, roleLabel);
        return ResponseEntity.ok(user);
    }
}