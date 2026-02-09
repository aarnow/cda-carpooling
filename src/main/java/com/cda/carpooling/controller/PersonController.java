package com.cda.carpooling.controller;

import com.cda.carpooling.dto.request.CreatePersonRequest;
import com.cda.carpooling.dto.request.UpdatePersonRequest;
import com.cda.carpooling.dto.response.PersonMinimalResponse;
import com.cda.carpooling.dto.response.PersonResponse;
import com.cda.carpooling.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des personnes.
 */
@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

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
     * GET /api/persons/email/{email}
     * Récupère une personne par son email.
     *
     * @param email L'email de la personne.
     * @return 200 OK avec PersonResponse
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<@NonNull PersonResponse> getPersonByEmail(@PathVariable String email) {
        PersonResponse person = personService.getPersonByEmail(email);
        return ResponseEntity.ok(person);
    }

    /**
     * POST /api/persons
     * Crée une nouvelle personne.
     *
     * @param request Les données de la personne
     * @return 201 CREATED avec PersonResponse contenan la personne créée
     */
    @PostMapping
    public ResponseEntity<@NonNull PersonResponse> createPerson(@Valid @RequestBody CreatePersonRequest request) {
        PersonResponse person = personService.createPerson(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(person);
    }

    /**
     * PATCH /api/persons/{id}
     * Met à jour d'une personne existante.
     *
     * @param id L'ID de la personne
     * @param request Les données à mettre à jour
     * @return 200 OK avec PersonResponse contenant la personne mise à jour
     */
    @PatchMapping("/{id}")
    public ResponseEntity<@NonNull PersonResponse> updatePerson(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePersonRequest request) {
        PersonResponse person = personService.updatePerson(id, request);
        return ResponseEntity.ok(person);
    }

    /**
     * DELETE /api/persons/{id}
     * Supprime une personne (soft delete).
     *
     * @param id L'ID de la personne
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<@NonNull Void> deletePerson(@PathVariable Long id) {
        personService.deletePerson(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/persons/{id}/hard
     * Supprime définitivement une personne.
     *
     * @param id L'ID de la personne
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<@NonNull Void> hardDeletePerson(@PathVariable Long id) {
        personService.hardDeletePerson(id);
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
    public ResponseEntity<@NonNull PersonResponse> removeRole(
            @PathVariable Long personId,
            @PathVariable String roleLabel) {
        PersonResponse person = personService.removeRole(personId, roleLabel);
        return ResponseEntity.ok(person);
    }
}