package com.cda.carpooling.security;

import com.cda.carpooling.entity.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Utilitaires pour la gestion des permissions et l'extraction d'informations depuis les tokens JWT.
 */
@Component
@Slf4j
public class SecurityUtils {

    /**
     * Détermine l'ID de la personne cible selon les permissions.
     *
     * @param targetPersonId ID de la personne cible (optionnel)
     * @param jwt JWT de l'utilisateur connecté
     * @return L'ID de la personne à utiliser
     * @throws AccessDeniedException Si non-admin tente d'agir pour autrui
     */
    public Long resolveTargetPersonId(Long targetPersonId, Jwt jwt) {
        Long currentUserId = extractUserId(jwt);

        if (targetPersonId == null) {
            log.debug("Target résolu : userId={}", currentUserId);
            return currentUserId;
        }

        if (!isAdmin(jwt)) {
            log.warn("Accès refusé : userId={} tente d'agir pour userId={} sans rôle ADMIN",
                    currentUserId, targetPersonId);
            throw new AccessDeniedException(
                    "Seuls les administrateurs peuvent effectuer cette action pour un autre utilisateur"
            );
        }

        log.debug("Target résolu : userId={} (action admin pour userId={})",
                currentUserId, targetPersonId);
        return targetPersonId;
    }

    /**
     * Vérifie si l'utilisateur possède le rôle ADMIN.
     *
     * @param jwt Token JWT de l'utilisateur
     * @return true si ADMIN, false sinon
     */
    public boolean isAdmin(Jwt jwt) {
        List<String> roles = extractRoles(jwt);
        return roles != null && roles.contains(Role.ROLE_ADMIN);
    }

    /**
     * Vérifie si l'utilisateur est propriétaire de la ressource OU admin.
     *
     * @param resourceOwnerId ID du propriétaire de la ressource
     * @param jwt Token JWT de l'utilisateur
     * @return true si owner ou admin, false sinon
     */
    public boolean isOwnerOrAdmin(Long resourceOwnerId, Jwt jwt) {
        Long currentUserId = extractUserId(jwt);
        return resourceOwnerId.equals(currentUserId) || isAdmin(jwt);
    }

    /**
     * Extrait l'ID utilisateur du JWT.
     *
     * @param jwt Token JWT
     * @return ID de l'utilisateur
     */
    public Long extractUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    /**
     * Extrait les rôles du JWT.
     *
     * @param jwt Token JWT
     * @return Liste des rôles
     */
    private List<String> extractRoles(Jwt jwt) {
        return jwt.getClaim("roles");
    }
}