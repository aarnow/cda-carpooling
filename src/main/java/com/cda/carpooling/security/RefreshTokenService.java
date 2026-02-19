package com.cda.carpooling.security;

import com.cda.carpooling.entity.Person;
import com.cda.carpooling.entity.RefreshToken;
import com.cda.carpooling.repository.PersonRepository;
import com.cda.carpooling.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service de gestion des refresh tokens avec ReUse Detection.
 *
 * - Rotation des tokens : chaque refresh génère un nouveau token
 * - ReUse Detection : si un token déjà utilisé est rejoué, tous les tokens sont révoqués
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final PersonRepository personRepository;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /**
     * Crée un refresh token pour un utilisateur.
     */
    @Transactional
    public String createRefreshToken(Person person, String deviceFingerprint, String userAgent) {
        refreshTokenRepository.findByPersonIdAndDeviceFingerprintAndRevokedFalse(
                person.getId(),
                deviceFingerprint
        ).ifPresent(existingToken -> {
            existingToken.setRevoked(true);
            refreshTokenRepository.save(existingToken);
            log.debug("Ancien token révoqué pour ce device (userId={})", person.getId());
        });

        String token = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .person(person)
                .deviceFingerprint(deviceFingerprint)
                .userAgent(userAgent)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .revoked(false)
                .used(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        long activeTokens = countActiveTokens(person.getId());
        log.debug("Refresh token créé pour userId={} ({} devices actifs)", person.getId(), activeTokens);

        return token;
    }

    /**
     * Valide un refresh token avec ReUse Detection.
     *
     * @param token Token de rafraîchissement
     * @return Utilisateur associé au token
     * @throws IllegalArgumentException Si le token est invalide, révoqué ou expiré
     * @throws SecurityException Si tentative de réutilisation détectée
     */
    @Transactional
    public Person validateAndConsumeRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> {
                    log.warn("Tentative d'utilisation d'un refresh token invalide ou révoqué");
                    return new IllegalArgumentException("Refresh token invalide ou révoqué");
                });

        //REUSE DETECTION
        if (refreshToken.getUsed()) {
            log.error("ALERTE SÉCURITÉ : Tentative de réutilisation d'un refresh token (userId={}, token utilisé le {})",
                    refreshToken.getPerson().getId(), refreshToken.getUsedAt());

            revokeAllUserTokens(refreshToken.getPerson().getId());

            throw new SecurityException(
                    "Token déjà utilisé. Tous vos tokens ont été révoqués par sécurité. " +
                            "Veuillez vous reconnecter.");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Tentative d'utilisation d'un refresh token expiré (userId={})",
                    refreshToken.getPerson().getId());
            throw new IllegalArgumentException("Refresh token expiré");
        }

        refreshToken.setUsed(true);
        refreshToken.setUsedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        log.debug("Refresh token consommé (userId={})", refreshToken.getPerson().getId());

        return personRepository.findByIdWithProfileAndRoles(refreshToken.getPerson().getId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
    }

    /**
     * Révoque un refresh token spécifique.
     */
    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                    log.info("Refresh token révoqué pour userId={}", rt.getPerson().getId());
                });
    }

    /**
     * Révoque tous les refresh tokens d'un utilisateur.
     * Utilisé lors du retrait de rôle, suppression de compte, ou détection de réutilisation.
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        int revoked = refreshTokenRepository.revokeAllByPersonId(userId);
        if (revoked > 0) {
            log.warn("{} refresh token(s) révoqué(s) pour userId={}", revoked, userId);
        }
    }

    /**
     * Supprime les refresh tokens expirés (nettoyage périodique).
     */
    @Transactional
    public int cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        if (deleted > 0) {
            log.info("{} refresh token(s) expiré(s) supprimé(s)", deleted);
        }
        return deleted;
    }

    /**
     * Compte les refresh tokens actifs d'un utilisateur.
     */
    @Transactional(readOnly = true)
    public long countActiveTokens(Long userId) {
        return refreshTokenRepository.countActiveTokensByPersonId(userId, LocalDateTime.now());
    }
}