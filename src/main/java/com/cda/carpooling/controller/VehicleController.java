package com.cda.carpooling.controller;

import com.cda.carpooling.dto.request.CreateVehicleRequest;
import com.cda.carpooling.dto.request.UpdateVehicleRequest;
import com.cda.carpooling.dto.response.VehicleResponse;
import com.cda.carpooling.security.SecurityUtils;
import com.cda.carpooling.service.VehicleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des véhicules.
 */
@Tag(name = "Véhicules", description = "Gestion des véhicules")
@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Slf4j
public class VehicleController {

    private final VehicleService vehicleService;
    private final SecurityUtils securityUtils;

    /**
     * GET /vehicles
     * Récupère tous les véhicules.
     *
     * @return 200 OK avec la liste des véhicules
     */
    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    /**
     * GET /vehicles/{id}
     * Récupère un véhicule par son ID.
     *
     * @param id L'ID du véhicule
     * @return 200 OK avec le véhicule
     */
    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    /**
     * POST /vehicles
     * Crée un véhicule.
     *
     * @param request Données du véhicule
     * @param jwt     JWT de l'utilisateur connecté
     * @return 201 CREATED avec le véhicule créé
     */
    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(
            @Valid @RequestBody CreateVehicleRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Long targetPersonId = securityUtils.resolveTargetPersonId(request.getPersonId(), jwt);
        log.info("Création véhicule pour personne {}", targetPersonId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleService.createVehicle(targetPersonId, request));
    }

    /**
     * PUT /vehicles/{id}
     * Met à jour un véhicule.
     *
     * @param id      L'ID du véhicule
     * @param request Les nouvelles données
     * @param jwt     JWT de l'utilisateur connecté
     * @return 200 OK avec le véhicule mis à jour
     */
    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Modification véhicule {} par {}", id, securityUtils.extractUserId(jwt));
        checkOwnerOrAdmin(id, jwt);
        return ResponseEntity.ok(vehicleService.updateVehicle(id, request));
    }

    /**
     * DELETE /vehicles/{id}
     * Supprime un véhicule.
     *
     * @param id  L'ID du véhicule
     * @param jwt JWT de l'utilisateur connecté
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        log.warn("Suppression véhicule {} par {}", id, securityUtils.extractUserId(jwt));
        checkOwnerOrAdmin(id, jwt);
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    //region Utils
    /**
     * Vérifie que l'utilisateur est propriétaire du véhicule ou admin.
     * Lève AccessDeniedException si la condition n'est pas remplie.
     */
    private void checkOwnerOrAdmin(Long vehicleId, Jwt jwt) {
        Long ownerId = vehicleService.getVehicleOwnerId(vehicleId);
        if (!securityUtils.isOwnerOrAdmin(ownerId, jwt)) {
            throw new AccessDeniedException(
                    "Vous n'avez pas la permission de modifier ce véhicule"
            );
        }
    }
    //endregion
}