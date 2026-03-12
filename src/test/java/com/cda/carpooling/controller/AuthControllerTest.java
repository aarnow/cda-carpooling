package com.cda.carpooling.controller;

import com.cda.carpooling.config.SecurityConfig;
import com.cda.carpooling.dto.request.AuthRequest;
import com.cda.carpooling.dto.request.CreatePersonRequest;
import com.cda.carpooling.dto.request.RefreshRequest;
import com.cda.carpooling.dto.response.AuthResponse;
import com.cda.carpooling.exception.DuplicateResourceException;
import com.cda.carpooling.security.AuthService;
import com.cda.carpooling.security.JwtService;
import com.cda.carpooling.security.RefreshTokenService;
import com.cda.carpooling.entity.Person;
import com.cda.carpooling.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("AuthController - Tests d'intégration")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JwtService jwtService;

    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        authResponse = AuthResponse.builder()
                .token("jwt.token.value")
                .refreshToken("refresh.token.value")
                .type("Bearer")
                .userId(1L)
                .roles(new String[]{"ROLE_STUDENT"})
                .build();
    }

    //region POST /login
    @Nested
    @DisplayName("POST /login")
    class LoginTests {

        @Test
        @DisplayName("Devrait retourner 200 avec token + refresh token si credentials valides")
        void shouldReturn200WithTokensWhenCredentialsValid() throws Exception {
            // Given
            AuthRequest request = new AuthRequest("test@test.fr", "password123");
            when(authService.login(any(AuthRequest.class), anyString())).thenReturn(authResponse);

            // When & Then
            mockMvc.perform(post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("User-Agent", "Mozilla/5.0")
                            .header("X-Forwarded-For", "192.168.1.1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt.token.value"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh.token.value"))
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.userId").value(1));

            verify(authService).login(any(AuthRequest.class), anyString());
        }

        @Test
        @DisplayName("Devrait générer un device fingerprint à partir du User-Agent et IP")
        void shouldGenerateDeviceFingerprintFromUserAgentAndIp() throws Exception {
            // Given
            AuthRequest request = new AuthRequest("test@test.fr", "password123");
            when(authService.login(any(AuthRequest.class), anyString())).thenReturn(authResponse);

            // When
            mockMvc.perform(post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("User-Agent", "Mozilla/5.0 Chrome")
                            .header("X-Forwarded-For", "203.0.113.42"))
                    .andExpect(status().isOk());

            // Then
            verify(authService).login(any(AuthRequest.class), argThat(fingerprint ->
                    fingerprint != null && !fingerprint.isEmpty()
            ));
        }

        @Test
        @DisplayName("Devrait retourner 401 si credentials invalides")
        void shouldReturn401WhenCredentialsInvalid() throws Exception {
            // Given
            AuthRequest request = new AuthRequest("test@test.fr", "mauvais_mdp");
            when(authService.login(any(), anyString())).thenThrow(
                    new BadCredentialsException("Email ou mot de passe incorrect")
            );

            // When & Then
            mockMvc.perform(post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("User-Agent", "Mozilla/5.0"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(containsString("Email ou mot de passe incorrect")));
        }

        @Test
        @DisplayName("Devrait retourner 403 si le compte est désactivé")
        void shouldReturn403WhenAccountDisabled() throws Exception {
            // Given
            AuthRequest request = new AuthRequest("suspended@test.fr", "password123");
            when(authService.login(any(), anyString())).thenThrow(
                    new DisabledException("Ce compte n'est pas accessible")
            );

            // When & Then
            mockMvc.perform(post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("User-Agent", "Mozilla/5.0"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(containsString("compte n'est pas accessible")));
        }

        @Test
        @DisplayName("Devrait retourner 404 si email inconnu")
        void shouldReturn404WhenEmailUnknown() throws Exception {
            // Given
            AuthRequest request = new AuthRequest("inconnu@test.fr", "password123");
            when(authService.login(any(), anyString())).thenThrow(
                    new com.cda.carpooling.exception.ResourceNotFoundException("Personne", "email", "inconnu@test.fr")
            );

            // When & Then
            mockMvc.perform(post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("User-Agent", "Mozilla/5.0"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Devrait retourner 400 si email manquant")
        void shouldReturn400WhenEmailMissing() throws Exception {
            // Given
            AuthRequest request = new AuthRequest("", "password123");

            // When & Then
            mockMvc.perform(post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("User-Agent", "Mozilla/5.0"))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any(), anyString());
        }

        @Test
        @DisplayName("Devrait retourner 400 si email invalide")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            // Given
            AuthRequest request = new AuthRequest("pas-un-email", "password123");

            // When & Then
            mockMvc.perform(post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("User-Agent", "Mozilla/5.0"))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any(), anyString());
        }

        @Test
        @DisplayName("Devrait retourner 400 si mot de passe manquant")
        void shouldReturn400WhenPasswordMissing() throws Exception {
            // Given
            AuthRequest request = new AuthRequest("test@test.fr", "");

            // When & Then
            mockMvc.perform(post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("User-Agent", "Mozilla/5.0"))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any(), anyString());
        }

        @Test
        @DisplayName("Devrait retourner 400 si body manquant")
        void shouldReturn400WhenBodyMissing() throws Exception {
            // When & Then
            mockMvc.perform(post("/login")
                            .contentType(APPLICATION_JSON)
                            .header("User-Agent", "Mozilla/5.0"))
                    .andExpect(status().isBadRequest());
        }
    }
    //endregion

    //region POST /register
    @Nested
    @DisplayName("POST /register")
    class RegisterTests {

        @Test
        @DisplayName("Devrait retourner 201 avec token lors d'un register réussi")
        void shouldReturn201WithTokenWhenRegisterSucceeds() throws Exception {
            // Given
            CreatePersonRequest request = new CreatePersonRequest("nouveau@test.fr", "Password123!");
            when(authService.register(any(CreatePersonRequest.class), anyString())).thenReturn(authResponse);

            // When & Then
            mockMvc.perform(post("/register")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("User-Agent", "Mozilla/5.0")
                            .header("X-Forwarded-For", "192.168.1.1"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value("jwt.token.value"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh.token.value"))
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.userId").value(1));

            verify(authService).register(any(CreatePersonRequest.class), anyString());
        }

        @Test
        @DisplayName("Devrait retourner 409 si email déjà utilisé")
        void shouldReturn409WhenEmailAlreadyUsed() throws Exception {
            // Given
            CreatePersonRequest request = new CreatePersonRequest("nouveau@test.fr", "Password123!");
            when(authService.register(any(), anyString())).thenThrow(
                    new DuplicateResourceException("Cet email est déjà utilisé")
            );

            // When & Then
            mockMvc.perform(post("/register")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("User-Agent", "Mozilla/5.0"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Devrait retourner 400 si email invalide")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            // Given
            CreatePersonRequest request = new CreatePersonRequest("pas-un-email", "password123");

            // When & Then
            mockMvc.perform(post("/register")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("User-Agent", "Mozilla/5.0"))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any(), anyString());
        }

        @Test
        @DisplayName("Devrait retourner 400 si mot de passe manquant")
        void shouldReturn400WhenPasswordMissing() throws Exception {
            // Given
            CreatePersonRequest request = new CreatePersonRequest("test@test.fr", "");

            // When & Then
            mockMvc.perform(post("/register")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("User-Agent", "Mozilla/5.0"))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any(), anyString());
        }
    }
    //endregion

    //region POST /refresh
    @Nested
    @DisplayName("POST /refresh")
    class RefreshTests {

        @Test
        @DisplayName("Devrait retourner 200 avec nouveaux tokens si refresh token valide")
        void shouldReturn200WithNewTokensWhenRefreshTokenValid() throws Exception {
            // Given
            RefreshRequest request = new RefreshRequest("valid.refresh.token");
            Person mockPerson = Person.builder().id(1L).build();

            when(refreshTokenService.validateAndConsumeRefreshToken(anyString())).thenReturn(mockPerson);
            when(jwtService.generateToken(any())).thenReturn("new.access.token");
            when(refreshTokenService.createRefreshToken(any(), anyString(), isNull())).thenReturn("new.refresh.token");

            // When & Then
            mockMvc.perform(post("/refresh")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("User-Agent", "Mozilla/5.0")
                            .header("X-Forwarded-For", "192.168.1.1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.refreshToken").exists());

            verify(refreshTokenService).validateAndConsumeRefreshToken("valid.refresh.token");
        }

        @Test
        @DisplayName("Devrait retourner 403 si ReUse détecté")
        void shouldReturn403WhenReuseDetected() throws Exception {
            // Given
            RefreshRequest request = new RefreshRequest("reused.token");
            when(refreshTokenService.validateAndConsumeRefreshToken(anyString()))
                    .thenThrow(new SecurityException("Token déjà utilisé"));

            // When & Then
            mockMvc.perform(post("/refresh")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("User-Agent", "Mozilla/5.0"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("token_reuse_detected"));
        }

        @Test
        @DisplayName("Devrait retourner 401 si refresh token invalide")
        void shouldReturn401WhenRefreshTokenInvalid() throws Exception {
            // Given
            RefreshRequest request = new RefreshRequest("invalid.token");
            when(refreshTokenService.validateAndConsumeRefreshToken(anyString()))
                    .thenThrow(new IllegalArgumentException("Refresh token invalide"));

            // When & Then
            mockMvc.perform(post("/refresh")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("User-Agent", "Mozilla/5.0"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("invalid_token"));
        }
    }
    //endregion

    //region POST /logout
    @Nested
    @DisplayName("POST /logout")
    class LogoutTests {

        @Test
        @DisplayName("Devrait retourner 204 et révoquer le token")
        void shouldReturn204AndRevokeToken() throws Exception {
            // Given
            RefreshRequest request = new RefreshRequest("token.to.revoke");
            doNothing().when(refreshTokenService).revokeToken(anyString());

            // When & Then
            mockMvc.perform(post("/logout")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(refreshTokenService).revokeToken("token.to.revoke");
        }
    }
    //endregion
}