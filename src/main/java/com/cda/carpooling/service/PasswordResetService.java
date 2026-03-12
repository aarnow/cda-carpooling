package com.cda.carpooling.service;

import com.cda.carpooling.entity.PasswordResetToken;
import com.cda.carpooling.entity.Person;
import com.cda.carpooling.exception.business.InvalidTokenException;
import com.cda.carpooling.repository.PasswordResetTokenRepository;
import com.cda.carpooling.repository.PersonRepository;
import com.cda.carpooling.repository.RefreshTokenRepository;
import com.cda.carpooling.integration.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final PersonRepository personRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.password-reset.token-validity-minutes:15}")
    private int tokenValidityMinutes;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Initie le processus de réinitialisation de mot de passe.
     * Envoie un email si le compte existe, sinon fait semblant (sécurité).
     */
    @Transactional
    public void initiateForgotPassword(String email) {
        log.info("🔐 Demande de réinitialisation pour : {}", email);

        // Chercher l'utilisateur
        Person person = personRepository.findByEmail(email).orElse(null);

        if (person == null) {
            log.warn("Tentative de reset pour email inexistant : {}", email);
            simulateProcessingDelay();
            return;
        }

        // Générer token en clair
        String plainToken = PasswordResetToken.generateToken();
        String tokenHash = DigestUtils.sha256Hex(plainToken);

        // Révoquer les anciens tokens de cet utilisateur
        tokenRepository.revokeAllUserTokens(person.getId());

        // Créer nouveau token
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .tokenHash(tokenHash)
                .person(person)
                .expiresAt(LocalDateTime.now().plusMinutes(tokenValidityMinutes))
                .build();

        tokenRepository.save(resetToken);

        // Envoyer l'email avec le token en clair
        sendResetEmail(person, plainToken);

        log.info("Email de réinitialisation envoyé à : {}", email);
    }

    /**
     * Réinitialise le mot de passe avec validation du token.
     */
    @Transactional
    public void resetPassword(String plainToken, String newPassword) {
        log.info("🔐 Tentative de réinitialisation avec token");

        String tokenHash = DigestUtils.sha256Hex(plainToken);

        // Trouver token valide
        PasswordResetToken resetToken = tokenRepository
                .findByTokenHashAndUsedFalseAndExpiresAtAfter(tokenHash, LocalDateTime.now())
                .orElseThrow(() -> {
                    log.warn("Token invalide ou expiré");
                    return new InvalidTokenException("Le lien de réinitialisation est invalide ou a expiré");
                });

        Person person = resetToken.getPerson();

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        person.setPassword(passwordEncoder.encode(newPassword));
        personRepository.save(person);

        refreshTokenRepository.revokeAllByPersonId(person.getId());

        log.info("Mot de passe réinitialisé pour : {}", person.getEmail());
    }

    /**
     * Envoie l'email de réinitialisation via EmailService.
     */
    private void sendResetEmail(Person person, String plainToken) {
        emailService.sendPasswordResetEmail(person, plainToken, tokenValidityMinutes);
    }

    /**
     * Simule un délai de traitement pour ne pas révéler si l'email existe.
     */
    private void simulateProcessingDelay() {
        try {
            Thread.sleep(100 + (long) (Math.random() * 200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}