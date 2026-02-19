package com.cda.carpooling.repository;

import com.cda.carpooling.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Trouve un refresh token valide.
     */
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    /**
     * Révoque tous les refresh tokens d'un utilisateur.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.person.id = :personId AND rt.revoked = false")
    int revokeAllByPersonId(@Param("personId") Long personId);

    /**
     * Supprime les refresh tokens expirés.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Nombre de refresh tokens actifs d'un utilisateur.
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.person.id = :personId AND rt.revoked = false AND rt.expiresAt > :now")
    long countActiveTokensByPersonId(@Param("personId") Long personId, @Param("now") LocalDateTime now);

    Optional<RefreshToken> findByPersonIdAndDeviceFingerprintAndRevokedFalse(
            Long personId,
            String deviceFingerprint
    );
}