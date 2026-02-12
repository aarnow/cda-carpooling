package com.cda.carpooling.security;

import com.cda.carpooling.entity.Person;
import com.cda.carpooling.entity.PersonStatus;
import com.cda.carpooling.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires pour JwtService.
 */
@DisplayName("JwtService - Tests Unitaires")
class JwtServiceTest {

    private static final String SECRET_KEY = "TestSecretKeyForTestingPurposesOnly1234567890ABCDEF";
    private static final String ISSUER = "carpooling-test";
    private static final long EXPIRATION_MS = 900000;

    private JwtService jwtService;
    private Person testPerson;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET_KEY, ISSUER, EXPIRATION_MS);

        Role studentRole = Role.builder()
                .id(1L)
                .label("ROLE_STUDENT")
                .build();

        Role driverRole = Role.builder()
                .id(2L)
                .label("ROLE_DRIVER")
                .build();

        testPerson = Person.builder()
                .id(42L)
                .email("test@test.fr")
                .status(PersonStatus.builder().label(PersonStatus.ACTIVE).build())
                .roles(Set.of(studentRole, driverRole))
                .build();
    }

    //region generateToken()
    @Nested
    @DisplayName("generateToken()")
    class GenerateTokenTests {

        @Test
        @DisplayName("Le subject doit être l'ID de l'utilisateur (pas l'email)")
        void subjectShouldBeUserId() {
            // When
            String token = jwtService.generateToken(testPerson);
            Jwt decoded = jwtService.validateToken(token);

            // Then
            assertThat(decoded.getSubject()).isEqualTo("42");
            assertThat(decoded.getSubject()).isNotEqualTo("test@test.fr");
        }

        @Test
        @DisplayName("Le token doit contenir tous les rôles de l'utilisateur")
        void tokenShouldContainAllRoles() {
            // When
            String token = jwtService.generateToken(testPerson);
            Jwt decoded = jwtService.validateToken(token);

            // Then
            List<String> roles = decoded.getClaim("roles");
            assertThat(roles)
                    .hasSize(2)
                    .contains("ROLE_STUDENT", "ROLE_DRIVER");
        }

        @Test
        @DisplayName("Le token ne doit pas contenir l'email")
        void tokenShouldNotContainEmail() {
            // When
            String token = jwtService.generateToken(testPerson);
            Jwt decoded = jwtService.validateToken(token);

            // Then
            assertThat(decoded.getClaims()).doesNotContainKey("email");
        }

        @Test
        @DisplayName("Le token doit avoir le bon issuer")
        void tokenShouldHaveCorrectIssuer() {
            // When
            String token = jwtService.generateToken(testPerson);
            Jwt decoded = jwtService.validateToken(token);

            // Then
            assertThat(decoded.<String>getClaim("iss")).isEqualTo(ISSUER);
        }

        @Test
        @DisplayName("Le token doit avoir une date d'expiration dans le futur")
        void tokenShouldHaveFutureExpiration() {
            // When
            String token = jwtService.generateToken(testPerson);
            Jwt decoded = jwtService.validateToken(token);

            // Then
            assertThat(decoded.getExpiresAt()).isAfter(decoded.getIssuedAt());
        }

        @Test
        @DisplayName("Le token d'un utilisateur sans rôle doit avoir une liste vide")
        void tokenShouldHaveEmptyRolesWhenUserHasNone() {
            // Given
            Person personWithoutRoles = Person.builder()
                    .id(99L)
                    .email("norole@test.fr")
                    .status(PersonStatus.builder().label(PersonStatus.ACTIVE).build())
                    .roles(Set.of())
                    .build();

            // When
            String token = jwtService.generateToken(personWithoutRoles);
            Jwt decoded = jwtService.validateToken(token);

            // Then
            List<String> roles = decoded.getClaim("roles");
            assertThat(roles).isEmpty();
        }
    }
    //endregion

    //region validateToken()
    @Nested
    @DisplayName("validateToken()")
    class ValidateTokenTests {

        @Test
        @DisplayName("Devrait valider et décoder un token valide")
        void shouldDecodeValidToken() {
            // Given
            String token = jwtService.generateToken(testPerson);

            // When
            Jwt decoded = jwtService.validateToken(token);

            // Then
            assertThat(decoded).isNotNull();
            assertThat(decoded.getSubject()).isEqualTo("42");
        }

        @Test
        @DisplayName("Devrait rejeter un token expiré")
        void shouldRejectExpiredToken() {
            // Given
            JwtService shortLivedJwtService = new JwtService(SECRET_KEY, ISSUER, 100L);
            String expiredToken = shortLivedJwtService.generateToken(testPerson);

            try { Thread.sleep(200); } catch (InterruptedException ignored) {}

            // When & Then
            assertThatThrownBy(() -> jwtService.validateToken(expiredToken))
                    .isInstanceOf(BadJwtException.class);
        }

        @Test
        @DisplayName("Devrait rejeter un token avec une signature invalide")
        void shouldRejectTokenWithInvalidSignature() {
            // Given
            JwtService otherJwtService = new JwtService(
                    "AutreSecretKeyCompletelyDifferent1234567890ABCDEF",
                    ISSUER,
                    EXPIRATION_MS
            );
            String tokenFromOtherService = otherJwtService.generateToken(testPerson);

            // When & Then
            assertThatThrownBy(() -> jwtService.validateToken(tokenFromOtherService))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Devrait rejeter un token malformé")
        void shouldRejectMalformedToken() {
            // When & Then
            assertThatThrownBy(() -> jwtService.validateToken("ceci.nest.pas.un.jwt"))
                    .isInstanceOf(Exception.class);
        }
    }
    //endregion
}