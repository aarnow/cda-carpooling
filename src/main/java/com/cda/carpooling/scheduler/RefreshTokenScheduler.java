package com.cda.carpooling.scheduler;

import com.cda.carpooling.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tâche planifiée pour nettoyer les refresh tokens expirés.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenScheduler {

    private final RefreshTokenService refreshTokenService;

    /**
     * Nettoie les refresh tokens expirés
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredTokens() {
        log.debug("Nettoyage des refresh tokens expirés...");
        int deleted = refreshTokenService.cleanupExpiredTokens();
        if (deleted > 0) {
            log.info("{} refresh token(s) expiré(s) supprimé(s)", deleted);
        }
    }
}