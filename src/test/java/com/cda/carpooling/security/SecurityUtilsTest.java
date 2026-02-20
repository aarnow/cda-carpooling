package com.cda.carpooling.security;

import com.cda.carpooling.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires pour SecurityUtils.
 */
@DisplayName("SecurityUtils - Tests Unitaires")
class SecurityUtilsTest {

    private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        securityUtils = new SecurityUtils();
    }

    private Jwt buildJwt(Long userId, List<String> roles) {
        return Jwt.withTokenValue("fake.jwt.token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .claim("roles", roles)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private Jwt adminJwt() {
        return buildJwt(1L, List.of(Role.ROLE_ADMIN));
    }

    private Jwt studentJwt() {
        return buildJwt(2L, List.of("ROLE_STUDENT"));
    }

    private Jwt multiRoleJwt() {
        return buildJwt(3L, List.of("ROLE_STUDENT", Role.ROLE_ADMIN));
    }

    private Jwt noRoleJwt() {
        return buildJwt(4L, List.of());
    }

    //region isAdmin()
    @Nested
    @DisplayName("isAdmin()")
    class IsAdminTests {

        @Test
        @DisplayName("Devrait retourner true si l'utilisateur a le rôle ADMIN")
        void shouldReturnTrueWhenUserIsAdmin() {
            assertThat(securityUtils.isAdmin(adminJwt())).isTrue();
        }

        @Test
        @DisplayName("Devrait retourner false si l'utilisateur n'a pas le rôle ADMIN")
        void shouldReturnFalseWhenUserIsNotAdmin() {
            assertThat(securityUtils.isAdmin(studentJwt())).isFalse();
        }

        @Test
        @DisplayName("Devrait retourner true si l'utilisateur a ADMIN parmi plusieurs rôles")
        void shouldReturnTrueWhenUserHasAdminAmongMultipleRoles() {
            assertThat(securityUtils.isAdmin(multiRoleJwt())).isTrue();
        }

        @Test
        @DisplayName("Devrait retourner false si l'utilisateur n'a aucun rôle")
        void shouldReturnFalseWhenUserHasNoRoles() {
            assertThat(securityUtils.isAdmin(noRoleJwt())).isFalse();
        }

        @Test
        @DisplayName("Devrait retourner false si le claim roles est null")
        void shouldReturnFalseWhenRolesClaimIsNull() {
            Jwt jwtWithoutRoles = Jwt.withTokenValue("fake.jwt.token")
                    .header("alg", "HS256")
                    .subject("1")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            assertThat(securityUtils.isAdmin(jwtWithoutRoles)).isFalse();
        }
    }
    //endregion

    //region isOwnerOrAdmin()
    @Nested
    @DisplayName("isOwnerOrAdmin()")
    class IsOwnerOrAdminTests {

        @Test
        @DisplayName("Devrait retourner true si l'utilisateur est le propriétaire")
        void shouldReturnTrueWhenUserIsOwner() {
            // userId = 2L dans le JWT, resourceOwnerId = 2L
            assertThat(securityUtils.isOwnerOrAdmin(2L, studentJwt())).isTrue();
        }

        @Test
        @DisplayName("Devrait retourner true si l'utilisateur est ADMIN (même sans être owner)")
        void shouldReturnTrueWhenUserIsAdmin() {
            // adminJwt userId = 1L, resourceOwnerId = 99L → pas owner, mais admin
            assertThat(securityUtils.isOwnerOrAdmin(99L, adminJwt())).isTrue();
        }

        @Test
        @DisplayName("Devrait retourner true si l'utilisateur est owner ET admin")
        void shouldReturnTrueWhenUserIsOwnerAndAdmin() {
            // adminJwt userId = 1L, resourceOwnerId = 1L → owner ET admin
            assertThat(securityUtils.isOwnerOrAdmin(1L, adminJwt())).isTrue();
        }

        @Test
        @DisplayName("Devrait retourner false si ni propriétaire ni admin")
        void shouldReturnFalseWhenNeitherOwnerNorAdmin() {
            // studentJwt userId = 2L, resourceOwnerId = 99L → ni owner ni admin
            assertThat(securityUtils.isOwnerOrAdmin(99L, studentJwt())).isFalse();
        }
    }
    //endregion

    //region resolveTargetPersonId()
    @Nested
    @DisplayName("resolveTargetPersonId()")
    class ResolveTargetPersonIdTests {

        @Test
        @DisplayName("Devrait retourner l'ID courant si targetPersonId est null")
        void shouldReturnCurrentUserIdWhenTargetIsNull() {
            // studentJwt userId = 2L
            Long result = securityUtils.resolveTargetPersonId(null, studentJwt());

            assertThat(result).isEqualTo(2L);
        }

        @Test
        @DisplayName("Devrait retourner targetPersonId si l'utilisateur est ADMIN")
        void shouldReturnTargetIdWhenUserIsAdmin() {
            // adminJwt userId = 1L, on cible l'utilisateur 99L
            Long result = securityUtils.resolveTargetPersonId(99L, adminJwt());

            assertThat(result).isEqualTo(99L);
        }

        @Test
        @DisplayName("Devrait lancer AccessDeniedException si non-admin cible un autre utilisateur")
        void shouldThrowAccessDeniedWhenNonAdminTargetsOtherUser() {
            // studentJwt userId = 2L, tente de cibler 99L
            assertThatThrownBy(() ->
                    securityUtils.resolveTargetPersonId(99L, studentJwt())
            )
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("administrateurs");
        }

        @Test
        @DisplayName("Devrait retourner l'ID courant si admin cible null")
        void shouldReturnCurrentUserIdWhenAdminTargetsNull() {
            // adminJwt userId = 1L, targetPersonId = null
            Long result = securityUtils.resolveTargetPersonId(null, adminJwt());

            assertThat(result).isEqualTo(1L);
        }

        @Test
        @DisplayName("Devrait retourner son propre ID si non-admin se cible lui-même via null")
        void shouldReturnOwnIdWhenNonAdminTargetsNull() {
            // studentJwt userId = 2L, targetPersonId = null → retourne son propre ID
            Long result = securityUtils.resolveTargetPersonId(null, studentJwt());

            assertThat(result).isEqualTo(2L);
        }
    }
    //endregion
}