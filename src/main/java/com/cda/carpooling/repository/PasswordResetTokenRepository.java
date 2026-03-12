package com.cda.carpooling.repository;

import com.cda.carpooling.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Trouve un token valide (non utilisé, non expiré) par son hash.
     */
    Optional<PasswordResetToken> findByTokenHashAndUsedFalseAndExpiresAtAfter(
            String tokenHash,
            LocalDateTime now
    );

    /**
     * Supprime tous les tokens expirés.
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Révoque tous les tokens d'un utilisateur.
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.person.id = :personId")
    void revokeAllUserTokens(@Param("personId") Long personId);
}