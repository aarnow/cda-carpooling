package com.cda.carpooling.controller;

import com.cda.carpooling.dto.request.CreatePersonProfileRequest;
import com.cda.carpooling.dto.request.UpdatePersonProfileRequest;
import com.cda.carpooling.dto.response.PersonMinimalResponse;
import com.cda.carpooling.dto.response.PersonProfileResponse;
import com.cda.carpooling.dto.response.PersonResponse;
import com.cda.carpooling.security.SecurityUtils;
import com.cda.carpooling.service.PersonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class PersonController {

    private final PersonService personService;
    private final SecurityUtils securityUtils;

    /**
     * GET /api/persons
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

        if (minimal != null && minimal) {
            List<PersonMinimalResponse> persons = personService.getAllPersonsMinimal();
            return ResponseEntity.ok(persons);
        }

        List<PersonResponse> persons = personService.getAllPersons();
        return ResponseEntity.ok(persons);
    }

    /**
     * GET /api/persons/{id}
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
     * POST /persons
     * Crée un profil pour l'utilisateur connecté.
     * Un admin peut créer le profil d'un autre utilisateur.
     */
    @PostMapping
    public ResponseEntity<PersonProfileResponse> createPersonProfile(
            @Valid @RequestBody CreatePersonProfileRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        // Utiliser SecurityUtils
        Long targetPersonId = securityUtils.resolveTargetPersonId(
                request.getPersonId(),
                jwt
        );

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

        return ResponseEntity.ok(personService.updatePersonProfile(id, request));
    }

    /**
     * TODO : 🦥 Si nous voulons vraiment respecter les standards REST, faudrait utiliser la méthode DELETE
     * PATCH /api/persons/{id}/soft-delete
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
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/persons/{personId}/roles/{roleLabel}
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
        PersonResponse person = personService.assignRole(personId, roleLabel);
        return ResponseEntity.ok(person);
    }

    /**
     * DELETE /api/persons/{personId}/roles/{roleLabel}
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
        PersonResponse person = personService.removeRole(personId, roleLabel);
        return ResponseEntity.ok(person);
    }
}