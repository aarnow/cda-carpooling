package com.cda.carpooling.controller;

import com.cda.carpooling.config.SecurityConfig;
import com.cda.carpooling.dto.request.AuthRequest;
import com.cda.carpooling.dto.request.CreatePersonRequest;
import com.cda.carpooling.dto.response.AuthResponse;
import com.cda.carpooling.exception.DuplicateResourceException;
import com.cda.carpooling.security.AuthService;
import com.cda.carpooling.security.JwtService;
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
import static org.mockito.ArgumentMatchers.any;
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
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JwtService jwtService;

    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        authResponse = AuthResponse.builder()
                .token("jwt.token.value")
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
        @DisplayName("Devrait retourner 200 avec token si credentials valides")
        void shouldReturn200WithTokenWhenCredentialsValid() throws Exception {
            // Given
            AuthRequest request = new AuthRequest("test@test.fr", "password123");
            when(authService.login(any(AuthRequest.class))).thenReturn(authResponse);

            // When & Then
            mockMvc.perform(post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt.token.value"))
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.userId").value(1));

            verify(authService).login(any(AuthRequest.class));
        }

        @Test
        @DisplayName("Devrait retourner 401 si credentials invalides")
        void shouldReturn401WhenCredentialsInvalid() throws Exception {
            // Given
            AuthRequest request = new AuthRequest("test@test.fr", "mauvais_mdp");
            when(authService.login(any())).thenThrow(
                    new BadCredentialsException("Email ou mot de passe incorrect")
            );

            // When & Then
            mockMvc.perform(post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(containsString("Email ou mot de passe incorrect")));
        }

        @Test
        @DisplayName("Devrait retourner 403 si le compte est désactivé")
        void shouldReturn401WhenAccountDisabled() throws Exception {
            // Given
            AuthRequest request = new AuthRequest("suspended@test.fr", "password123");
            when(authService.login(any())).thenThrow(
                    new DisabledException("Ce compte n'est pas accessible")
            );

            // When & Then
            mockMvc.perform(post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(containsString("compte n'est pas accessible")));
        }

        @Test
        @DisplayName("Devrait retourner 404 si email inconnu")
        void shouldReturn404WhenEmailUnknown() throws Exception {
            // Given
            AuthRequest request = new AuthRequest("inconnu@test.fr", "password123");
            when(authService.login(any())).thenThrow(
                    new com.cda.carpooling.exception.ResourceNotFoundException("Personne", "email", "inconnu@test.fr")
            );

            // When & Then
            mockMvc.perform(post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
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
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any());
        }

        @Test
        @DisplayName("Devrait retourner 400 si email invalide")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            // Given
            AuthRequest request = new AuthRequest("pas-un-email", "password123");

            // When & Then
            mockMvc.perform(post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any());
        }

        @Test
        @DisplayName("Devrait retourner 400 si mot de passe manquant")
        void shouldReturn400WhenPasswordMissing() throws Exception {
            // Given
            AuthRequest request = new AuthRequest("test@test.fr", "");

            // When & Then
            mockMvc.perform(post("/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any());
        }

        @Test
        @DisplayName("Devrait retourner 400 si body manquant")
        void shouldReturn400WhenBodyMissing() throws Exception {
            // When & Then
            mockMvc.perform(post("/login")
                            .contentType(APPLICATION_JSON))
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
            when(authService.register(any(CreatePersonRequest.class))).thenReturn(authResponse);

            // When & Then
            mockMvc.perform(post("/register")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value("jwt.token.value"))
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.userId").value(1));

            verify(authService).register(any(CreatePersonRequest.class));
        }

        @Test
        @DisplayName("Devrait retourner 409 si email déjà utilisé")
        void shouldReturn409WhenEmailAlreadyUsed() throws Exception {
            // Given
            CreatePersonRequest request = new CreatePersonRequest("nouveau@test.fr", "Password123!");
            when(authService.register(any())).thenThrow(
                    new DuplicateResourceException("Cet email est déjà utilisé")
            );

            // When & Then
            mockMvc.perform(post("/register")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
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
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("Devrait retourner 400 si mot de passe manquant")
        void shouldReturn400WhenPasswordMissing() throws Exception {
            // Given
            CreatePersonRequest request = new CreatePersonRequest("test@test.fr", "");

            // When & Then
            mockMvc.perform(post("/register")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any());
        }
    }
    //endregion
}