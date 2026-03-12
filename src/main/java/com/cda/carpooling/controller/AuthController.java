package com.cda.carpooling.controller;

import com.cda.carpooling.dto.request.*;
import com.cda.carpooling.dto.response.AuthResponse;
import com.cda.carpooling.dto.response.MessageResponse;
import com.cda.carpooling.entity.Person;
import com.cda.carpooling.entity.Role;
import com.cda.carpooling.security.AuthService;
import com.cda.carpooling.security.JwtService;
import com.cda.carpooling.security.RefreshTokenService;
import com.cda.carpooling.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contrôleur pour l'authentification.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;

    /**
     * POST /login
     * Connexion d'un utilisateur.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest httpRequest) {

        String deviceFingerprint = generateDeviceFingerprint(httpRequest);
        AuthResponse response = authService.login(request, deviceFingerprint);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/register
     * Inscription d'un nouvel utilisateur.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody CreatePersonRequest request,
            HttpServletRequest httpRequest) {

        String deviceFingerprint = generateDeviceFingerprint(httpRequest);
        AuthResponse response = authService.register(request, deviceFingerprint);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /refresh
     * Échange un refresh token contre un nouvel access token + nouveau refresh token.
     * Implémente la rotation des tokens et la détection de réutilisation.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest httpRequest) {

        log.debug("Tentative de refresh token");

        try {
            Person person = refreshTokenService.validateAndConsumeRefreshToken(request.getRefreshToken());

            // access token
            String newAccessToken = jwtService.generateToken(person);

            // refresh token
            String deviceFingerprint = generateDeviceFingerprint(httpRequest);
            String newRefreshToken = refreshTokenService.createRefreshToken(
                    person,
                    deviceFingerprint,
                    null
            );

            String[] roles = person.getRoles().stream()
                    .map(Role::getLabel)
                    .toArray(String[]::new);

            log.info("Token rafraîchi pour userId={} avec rôles: {} (rotation effectuée)",
                    person.getId(), String.join(", ", roles));

            return ResponseEntity.ok(AuthResponse.builder()
                    .token(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .type("Bearer")
                    .userId(person.getId())
                    .roles(roles)
                    .build());

        } catch (SecurityException e) {
            // ReUse détecté
            log.error("ReUse detection : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error", "token_reuse_detected",
                            "message", e.getMessage()
                    ));

        } catch (IllegalArgumentException e) {
            log.warn("Refresh échoué : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_token", "message", e.getMessage()));
        }
    }

    /**
     * POST /logout
     * Révoque le refresh token de l'appareil actuel.
     * L'access token reste valide jusqu'à expiration.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        log.debug("Déconnexion (révocation refresh token)");

        refreshTokenService.revokeToken(request.getRefreshToken());

        log.info("Refresh token révoqué");
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /logout-all
     * Révoque TOUS les refresh tokens de l'utilisateur (tous les appareils).
     * Nécessite un access token valide pour identifier l'utilisateur.
     */
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAllDevices(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());

        log.warn("Déconnexion de TOUS les appareils (userId={})", userId);
        refreshTokenService.revokeAllUserTokens(userId);

        log.info("Tous les refresh tokens révoqués pour userId={}", userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /auth/forgot-password
     * Initie le processus de réinitialisation de mot de passe.
     * Envoie un email si le compte existe.
     * Retourne toujours 200 pour ne pas révéler si l'email existe.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        log.info("📧 Demande de réinitialisation pour : {}", request.getEmail());

        passwordResetService.initiateForgotPassword(request.getEmail());

        return ResponseEntity.ok(new MessageResponse(
                "Si un compte existe avec cet email, vous recevrez un lien de réinitialisation."
        ));
    }

    /**
     * POST /auth/reset-password
     * Valide le token et change le mot de passe.
     * Révoque tous les refresh tokens de l'utilisateur.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        log.info("🔐 Tentative de réinitialisation de mot de passe");

        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());

        return ResponseEntity.ok(new MessageResponse(
                "Votre mot de passe a été réinitialisé avec succès. Vous pouvez maintenant vous connecter."
        ));
    }

    /**
     * Génère un fingerprint unique du device basé sur User-Agent et IP.
     * Utilise SHA-256 pour un hash sécurisé et irréversible.
     */
    private String generateDeviceFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }

        return DigestUtils.sha256Hex(userAgent + ip);
    }
}