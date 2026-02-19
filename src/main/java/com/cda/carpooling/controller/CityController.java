package com.cda.carpooling.controller;

import com.cda.carpooling.dto.request.CreateCityRequest;
import com.cda.carpooling.dto.request.UpdateCityRequest;
import com.cda.carpooling.dto.response.CityResponse;
import com.cda.carpooling.service.CityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Villes", description = "Gestion des villes")
@RestController
@RequestMapping("/cities")
@RequiredArgsConstructor
@Slf4j
public class CityController {

    private final CityService cityService;

    /**
     * GET /cities
     */
    @GetMapping
    public ResponseEntity<List<CityResponse>> getAllCities() {
        return ResponseEntity.ok(cityService.getAllCities());
    }

    /**
     * GET /cities/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CityResponse> getCityById(@PathVariable Long id) {
        return ResponseEntity.ok(cityService.getCityById(id));
    }

    /**
     * GET /cities/search?name=Nantes
     * Recherche une ville par son nom
     */
    @GetMapping("/search")
    public ResponseEntity<List<CityResponse>> searchCities(
            @RequestParam String name) {
        log.info("Recherche ville : '{}'", name);
        return ResponseEntity.ok(cityService.searchCities(name));
    }

    /**
     * POST /cities
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CityResponse> createCity(@Valid @RequestBody CreateCityRequest request) {
        log.info("Création ville : {}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(cityService.createCity(request));
    }

    /**
     * PATCH /cities/{id}
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CityResponse> updateCity(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCityRequest request) {
        log.info("Modification ville {}", id);
        return ResponseEntity.ok(cityService.updateCity(id, request));
    }

    /**
     * DELETE /cities/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCity(@PathVariable Long id) {
        cityService.deleteCity(id);
        log.warn("Suppression ville {}", id);
        return ResponseEntity.noContent().build();
    }
}