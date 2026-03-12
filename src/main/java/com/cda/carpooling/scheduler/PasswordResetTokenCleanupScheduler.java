package com.cda.carpooling.scheduler;

import com.cda.carpooling.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetTokenCleanupScheduler {

    private final PasswordResetTokenRepository tokenRepository;

    /**
     * Supprime les tokens expirés tous les jours à 3h du matin.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("🧹 Nettoyage des tokens de réinitialisation expirés...");
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("✅ Nettoyage terminé");
    }
}