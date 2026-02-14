package com.cda.carpooling.controller;

import com.cda.carpooling.dto.request.CreateTripRequest;
import com.cda.carpooling.dto.request.UpdateTripRequest;
import com.cda.carpooling.dto.response.PersonMinimalResponse;
import com.cda.carpooling.dto.response.ReservationResponse;
import com.cda.carpooling.dto.response.TripResponse;
import com.cda.carpooling.dto.response.TripMinimalResponse;
import com.cda.carpooling.security.SecurityUtils;
import com.cda.carpooling.service.TripService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Trajets", description = "Gestion des trajets")
@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final SecurityUtils securityUtils;

    /**
     * GET /trips
     * Retourne la liste résumée de tous les trajets.
     * Accessible à tous les utilisateurs authentifiés.
     */
    @GetMapping
    public ResponseEntity<List<TripMinimalResponse>> getAllTrips() {
        return ResponseEntity.ok(tripService.getAllTrips());
    }

    /**
     * GET /trips/{id}
     * Retourne le détail complet d'un trajet avec ses réservations.
     * Accessible à tous les utilisateurs authentifiés.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> getTripById(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.getTripById(id));
    }

    /**
     * GET /trips/{id}/persons
     * Retourne la liste des passagers d'un trajet (réservations non annulées).
     * Accessible à tous les utilisateurs authentifiés.
     */
    @GetMapping("/{id}/persons")
    public ResponseEntity<List<PersonMinimalResponse>> getTripPassengers(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.getTripPassengers(id));
    }

    /**
     * POST /trips
     * Crée un trajet. Réservé aux conducteurs (ROLE_DRIVER).
     * L'ID du conducteur est extrait du JWT.
     */
    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<TripResponse> createTrip(
            @Valid @RequestBody CreateTripRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Long driverId = securityUtils.extractUserId(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tripService.createTrip(driverId, request));
    }

    /**
     * PATCH /trips/{id}
     * Met à jour un trajet.
     * Réservé au conducteur propriétaire ou à un admin.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<TripResponse> updateTrip(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTripRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        checkDriverOrAdmin(id, jwt);
        return ResponseEntity.ok(tripService.updateTrip(id, request));
    }

    /**
     * DELETE /trips/{id}
     * Supprime un trajet.
     * Réservé au conducteur propriétaire ou à un admin.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        checkDriverOrAdmin(id, jwt);
        tripService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /trips/{id}/person
     * Réserve une place sur un trajet OU annule la réservation existante (toggle).
     * TODO : seul les personnes concernés par ce trajet devrait y avoir accès (et les admins)
     */
    @PostMapping("/{id}/person")
    public ResponseEntity<ReservationResponse> toggleReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        Long personId = securityUtils.extractUserId(jwt);
        return ResponseEntity.ok(tripService.toggleReservation(id, personId));
    }

    /**
     * Vérifie que l'utilisateur est le conducteur du trajet ou un admin.
     */
    private void checkDriverOrAdmin(Long tripId, Jwt jwt) {
        Long driverId = tripService.getTripDriverId(tripId);
        if (!securityUtils.isOwnerOrAdmin(driverId, jwt)) {
            throw new AccessDeniedException(
                    "Vous n'avez pas la permission de modifier ce trajet"
            );
        }
    }
}