package com.cda.carpooling.controller;

import com.cda.carpooling.dto.request.CreatePersonRequest;
import com.cda.carpooling.dto.request.AuthRequest;
import com.cda.carpooling.dto.response.AuthResponse;
import com.cda.carpooling.security.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur pour l'authentification.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * POST /login
     * Connexion d'un utilisateur.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("Tentative de connexion : {}", request.getEmail());
        AuthResponse response = authService.login(request);
        log.info("Connexion réussie : {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/register
     * Inscription d'un nouvel utilisateur.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody CreatePersonRequest request) {
        log.info("Tentative d'inscription : {}", request.getEmail());
        AuthResponse response = authService.register(request);
        log.info("Inscription réussie : {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}