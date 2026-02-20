package com.cda.carpooling.controller;

import com.cda.carpooling.dto.request.CreateAddressRequest;
import com.cda.carpooling.dto.request.UpdateAddressRequest;
import com.cda.carpooling.dto.response.AddressResponse;
import com.cda.carpooling.service.AddressService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Adresses", description = "Gestion des adresses")
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@Slf4j
public class AddressController {

    private final AddressService addressService;

    /**
     * GET /addresses
     */
    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAllAddresses() {
        return ResponseEntity.ok(addressService.getAllAddresses());
    }

    /**
     * GET /addresses/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getAddressById(@PathVariable Long id) {
        return ResponseEntity.ok(addressService.getAddressById(id));
    }

    /**
     * GET /addresses/search?q=5 rue de Pr&city=Séné
     * Le paramètre city est optionnel.
     */
    @GetMapping("/search")
    public ResponseEntity<List<AddressResponse>> searchAddresses(
            @RequestParam String q,
            @RequestParam(required = false) String city) {

        log.info("Recherche d'adresses : query='{}', city='{}'", q, city);
        return ResponseEntity.ok(addressService.searchAddresses(q, city));
    }

    /**
     * POST /addresses
     * Réservé aux administrateurs.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AddressResponse> createAddress(@Valid @RequestBody CreateAddressRequest request) {
        log.info("Création adresse : {}, {}", request.getStreetName(), request.getCityId());
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.createAddress(request));
    }

    /**
     * PATCH /addresses/{id}
     * Réservé aux administrateurs.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAddressRequest request) {
        log.info("Modification adresse {}", id);
        return ResponseEntity.ok(addressService.updateAddress(id, request));
    }

    /**
     * DELETE /addresses/{id}
     * Réservé aux administrateurs.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        log.warn("Suppression adresse {}", id);
        addressService.deleteAddress(id);
        return ResponseEntity.noContent().build();
    }
}