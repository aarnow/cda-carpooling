package com.cda.carpooling.controller;

import com.cda.carpooling.dto.request.BrandRequest;
import com.cda.carpooling.dto.response.BrandResponse;
import com.cda.carpooling.service.BrandService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des marques de véhicules.
 */
@Tag(name = "Marques", description = "Gestion des marques de véhicules")
@RestController
@RequestMapping("/brands")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BrandController {

    private final BrandService brandService;

    /**
     * GET /brands
     * Récupère toutes les marques triées par nom.
     *
     * @return 200 OK avec la liste des marques
     */
    @GetMapping
    public ResponseEntity<List<BrandResponse>> getAllBrands() {
        return ResponseEntity.ok(brandService.getAllBrands());
    }

    /**
     * GET /brands/{id}
     * Récupère une marque par son ID.
     *
     * @param id L'ID de la marque
     * @return 200 OK avec la marque
     */
    @GetMapping("/{id}")
    public ResponseEntity<BrandResponse> getBrandById(@PathVariable Long id) {
        return ResponseEntity.ok(brandService.getBrandById(id));
    }

    /**
     * POST /brands
     * Crée une nouvelle marque.
     *
     * @param request Les données de la marque
     * @return 201 CREATED avec la marque créée
     */
    @PostMapping
    public ResponseEntity<BrandResponse> createBrand(@Valid @RequestBody BrandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(brandService.createBrand(request));
    }

    /**
     * PUT /brands/{id}
     * Met à jour une marque existante (remplacement complet).
     *
     * @param id      L'ID de la marque
     * @param request Les nouvelles données
     * @return 200 OK avec la marque mise à jour
     */
    @PutMapping("/{id}")
    public ResponseEntity<BrandResponse> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody BrandRequest request) {
        return ResponseEntity.ok(brandService.updateBrand(id, request));
    }

    /**
     * DELETE /brands/{id}
     * Supprime une marque.
     * Échoue si des véhicules sont associés à cette marque.
     *
     * @param id L'ID de la marque
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }
}