package com.cda.carpooling.security;

import com.cda.carpooling.entity.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Utilitaires pour la sécurité et la gestion des permissions.
 */
@Component
public class SecurityUtils {

    /**
     * Détermine l'ID de la personne cible en fonction des permissions.
     *
     * Logique :
     * - Si targetPersonId fourni ET user est ADMIN → utilise targetPersonId
     * - Sinon → utilise currentUserId (l'utilisateur connecté)
     *
     * @param targetPersonId ID de la personne cible
     * @param jwt JWT de l'utilisateur connecté
     * @return L'ID de la personne à utiliser
     * @throws AccessDeniedException Si non-admin tente d'agir sur un autre utilisateur
     */
    public Long resolveTargetPersonId(Long targetPersonId, Jwt jwt) {
        Long currentUserId = _extractUserId(jwt);

        if (targetPersonId == null) {
            return currentUserId;
        }

        if (!isAdmin(jwt)) {
            throw new AccessDeniedException(
                    "Seuls les administrateurs peuvent effectuer cette action pour un autre utilisateur"
            );
        }

        return targetPersonId;
    }

    /**
     * Vérifie si l'utilisateur a le rôle ADMIN.
     */
    public boolean isAdmin(Jwt jwt) {
        List<String> roles = _extractRoles(jwt);
        return roles != null && roles.contains(Role.ROLE_ADMIN);
    }

    /**
     * Vérifie si l'utilisateur est propriétaire de la ressource OU admin.
     */
    public boolean isOwnerOrAdmin(Long resourceOwnerId, Jwt jwt) {
        Long currentUserId = _extractUserId(jwt);
        return resourceOwnerId.equals(currentUserId) || isAdmin(jwt);
    }

    /**
     * Extrait l'ID utilisateur du JWT.
     */
    private Long _extractUserId(Jwt jwt) {
        return jwt.getClaim("userId");
    }

    /**
     * Extrait les rôles du JWT.
     */
    private List<String> _extractRoles(Jwt jwt) {
        return jwt.getClaim("roles");
    }
}