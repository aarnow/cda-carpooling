package com.cda.carpooling.controller;

import com.cda.carpooling.dto.request.ContactRequest;
import com.cda.carpooling.dto.request.CreatePersonProfileRequest;
import com.cda.carpooling.dto.request.UpdatePersonProfileRequest;
import com.cda.carpooling.dto.response.*;
import com.cda.carpooling.security.SecurityUtils;
import com.cda.carpooling.service.PersonService;
import com.cda.carpooling.service.ReservationService;
import com.cda.carpooling.service.TripService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des personnes.
 */
@Tag(name = "Personnes", description = "Gestion des personnes")
@RestController
@RequestMapping("/persons")
@RequiredArgsConstructor
@Slf4j
public class PersonController {

    private final PersonService personService;
    private final TripService tripService;
    private final SecurityUtils securityUtils;
    private final ReservationService reservationService;


    @PostMapping("/{personId}/contact")
    public ResponseEntity<Void> contactPerson(
            @PathVariable Long personId,
            @Valid @RequestBody ContactRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Long senderId = securityUtils.extractUserId(jwt);
        log.info("Contact de la personne {} par {}", personId, senderId);

        personService.contactPerson(senderId, personId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /persons
     * Récupère toutes les personnes.
     *
     * Query parameters :
     * - minimal: true pour version minimale (status uniquement)
     *
     * @param minimal Si true, retourne version minimale
     * @return 200 OK avec la liste des personnes
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllPersons(
            @RequestParam(required = false, defaultValue = "false") Boolean minimal) {
        log.debug("GET /persons (minimal={})", minimal);

        if (minimal != null && minimal) {
            List<PersonMinimalResponse> persons = personService.getAllPersonsMinimal();
            return ResponseEntity.ok(persons);
        }

        List<PersonResponse> persons = personService.getAllPersons();
        return ResponseEntity.ok(persons);
    }

    /**
     * GET /persons/{id}
     * Récupère une personne par son ID.
     *
     * @param id L'ID de la personne
     * @return 200 OK avec PersonResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<@NonNull PersonResponse> getPersonById(@PathVariable Long id) {
        PersonResponse person = personService.getPersonById(id);
        return ResponseEntity.ok(person);
    }

    /**
     * GET /persons/{id}/trips-driver
     * Retourne les trajets d'une personne en tant que conducteur.
     * Réservé au propriétaire ou à un admin.
     */
    @GetMapping("/{id}/trips-driver")
    public ResponseEntity<List<TripMinimalResponse>> getTripsByDriver(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        if (!securityUtils.isOwnerOrAdmin(id, jwt)) {
            throw new AccessDeniedException(
                    "Vous n'avez pas accès aux trajets de cet utilisateur");
        }

        return ResponseEntity.ok(tripService.getTripsByDriver(id));
    }

    /**
     * GET /persons/{id}/trips-passenger
     * Retourne les réservations d'une personne en tant que passager.
     * Réservé au propriétaire ou à un admin.
     */
    @GetMapping("/{id}/trips-passenger")
    public ResponseEntity<List<ReservationResponse>> getTripsByPassenger(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        if (!securityUtils.isOwnerOrAdmin(id, jwt)) {
            throw new AccessDeniedException(
                    "Vous n'avez pas accès aux trajets de cet utilisateur");
        }

        return ResponseEntity.ok(reservationService.getTripsByPassenger(id));
    }

    /**
     * POST /persons
     * Crée un profil pour l'utilisateur connecté.
     * Un admin peut créer le profil d'un autre utilisateur.
     */
    @PostMapping
    public ResponseEntity<PersonProfileResponse> createPersonProfile(
            @Valid @RequestBody CreatePersonProfileRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Long targetPersonId = securityUtils.resolveTargetPersonId(
                request.getPersonId(),
                jwt
        );

        log.info("Création profil pour la personne {}", targetPersonId);
        PersonProfileResponse profile = personService.createPersonProfile(targetPersonId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    /**
     * PATCH /persons/{id}
     * Modifie son propre profil ou celui d'un autre si admin.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<PersonProfileResponse> updatePerson(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePersonProfileRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        if (!securityUtils.isOwnerOrAdmin(id, jwt)) {
            throw new AccessDeniedException("Vous n'avez pas la permission de modifier ce profil");
        }

        log.info("Modification profil personne {}", id);
        return ResponseEntity.ok(personService.updatePersonProfile(id, request));
    }

    /**
     * 🦥 Si nous voulons vraiment respecter les standards REST, faudrait utiliser la méthode DELETE
     * PATCH /persons/{id}/soft-delete
     * Anonymise une personne (soft delete).
     *
     * @param id L'ID de la personne
     * @return 204 NO CONTENT
     */
    @PatchMapping("/{id}/soft-delete")
    public ResponseEntity<PersonResponse> softDeletePerson(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (!securityUtils.isOwnerOrAdmin(id, jwt)) {
            throw new AccessDeniedException("Vous n'avez pas la permission");
        }

        log.warn("Soft delete personne {} par {}", id, securityUtils.extractUserId(jwt));
        return ResponseEntity.ok(personService.softDeletePerson(id));
    }

    /**
     * DELETE /persons/{id}
     * Supprime définitivement une personne.
     *
     * @param id L'ID de la personne
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<@NonNull Void> deletePerson(@PathVariable Long id) {
        personService.deletePerson(id);
        log.warn("SUPPRESSION DÉFINITIVE personne {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /persons/{personId}/roles/{roleLabel}
     * Assigne un rôle à une personne.
     *
     * @param personId L'ID de la personne
     * @param roleLabel Le label du rôle
     * @return 200 OK avec la personne mise à jour
     */
    @PostMapping("/{personId}/roles/{roleLabel}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<@NonNull PersonResponse> assignRole(
            @PathVariable Long personId,
            @PathVariable String roleLabel) {
        log.info("Attribution rôle {} à personne {}", roleLabel, personId);
        PersonResponse person = personService.assignRole(personId, roleLabel);
        return ResponseEntity.ok(person);
    }

    /**
     * DELETE /persons/{personId}/roles/{roleLabel}
     * Retire un rôle d'une personne.
     *
     * @param personId L'ID de la personne
     * @param roleLabel Le label du rôle
     * @return 200 OK avec la personne mise à jour
     */
    @DeleteMapping("/{personId}/roles/{roleLabel}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<@NonNull PersonResponse> removeRole(
            @PathVariable Long personId,
            @PathVariable String roleLabel) {
        log.info("Retrait rôle {} à personne {}", roleLabel, personId);
        PersonResponse person = personService.removeRole(personId, roleLabel);
        return ResponseEntity.ok(person);
    }
}