package com.cda.carpooling.controller;

import com.cda.carpooling.dto.request.CreateUserRequest;
import com.cda.carpooling.dto.request.UpdateUserRequest;
import com.cda.carpooling.dto.response.UserResponse;
import com.cda.carpooling.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion des utilisateurs.
 * Expose les endpoints CRUD selon les bonnes pratiques REST.
 *
 * Routes :
 * - POST   /api/users              : Créer un utilisateur
 * - GET    /api/users              : Liste tous les utilisateurs
 * - GET    /api/users/{id}         : Récupère un utilisateur par ID
 * - GET    /api/users/email/{email}: Récupère un utilisateur par email
 * - PATCH  /api/users/{id}         : Met à jour un utilisateur
 * - DELETE /api/users/{id}         : Supprime un utilisateur (soft delete)
 * - DELETE /api/users/{id}/hard    : Supprime définitivement un utilisateur
 * - POST   /api/users/{id}/roles   : Assigne un rôle
 * - DELETE /api/users/{id}/roles   : Retire un rôle
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * POST /api/users
     * Crée un nouvel utilisateur.
     *
     * @param request Les données de l'utilisateur à créer
     * @return 201 CREATED avec l'utilisateur créé
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    /**
     * GET /api/users
     * Récupère tous les utilisateurs.
     *
     * @return 200 OK avec la liste des utilisateurs
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/users/{id}
     * Récupère un utilisateur par son ID.
     *
     * @param id L'ID de l'utilisateur
     * @return 200 OK avec l'utilisateur trouvé
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
     * @return 200 OK avec l'utilisateur trouvé
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    /**
     * PATCH /api/users/{id}
     * Met à jour un utilisateur existant (patch update).
     *
     * @param id L'ID de l'utilisateur à mettre à jour
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
     * Supprime un utilisateur (soft delete : change le statut en DELETED).
     *
     * @param id L'ID de l'utilisateur à supprimer
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/users/{id}/hard
     * Supprime définitivement un utilisateur de la base de données.
     * ⚠️ ATTENTION : Opération irréversible !
     *
     * @param id L'ID de l'utilisateur à supprimer définitivement
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteUser(@PathVariable Long id) {
        userService.hardDeleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/users/{id}/roles
     * Assigne un rôle à un utilisateur.
     *
     * @param id L'ID de l'utilisateur
     * @param body Le corps de la requête contenant le label du rôle
     * @return 200 OK avec l'utilisateur mis à jour
     */
    @PostMapping("/{id}/roles")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String roleLabel = body.get("roleLabel");
        UserResponse user = userService.assignRole(id, roleLabel);
        return ResponseEntity.ok(user);
    }

    /**
     * DELETE /api/users/{id}/roles
     * Retire un rôle d'un utilisateur.
     *
     * @param id L'ID de l'utilisateur
     * @param body Le corps de la requête contenant le label du rôle
     * @return 200 OK avec l'utilisateur mis à jour
     */
    @DeleteMapping("/{id}/roles")
    public ResponseEntity<UserResponse> removeRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String roleLabel = body.get("roleLabel");
        UserResponse user = userService.removeRole(id, roleLabel);
        return ResponseEntity.ok(user);
    }
}