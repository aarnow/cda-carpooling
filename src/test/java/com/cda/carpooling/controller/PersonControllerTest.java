package com.cda.carpooling.controller;

import com.cda.carpooling.config.SecurityConfig;
import com.cda.carpooling.dto.request.CreatePersonProfileRequest;
import com.cda.carpooling.dto.request.UpdatePersonProfileRequest;
import com.cda.carpooling.dto.response.PersonMinimalResponse;
import com.cda.carpooling.dto.response.PersonProfileResponse;
import com.cda.carpooling.dto.response.PersonResponse;
import com.cda.carpooling.exception.DuplicateResourceException;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.security.JwtService;
import com.cda.carpooling.security.SecurityUtils;
import com.cda.carpooling.service.PersonService;
import com.cda.carpooling.service.ReservationService;
import com.cda.carpooling.service.TripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration - PersonController.
 *
 * Utilise @WebMvcTest pour charger uniquement la couche Web.
 * Utilise MockMvc pour simuler les requêtes HTTP.
 * Utilise jwt() de Spring Security Test pour simuler l'authentification.
 */
@WebMvcTest(PersonController.class)
@Import(SecurityConfig.class)
@DisplayName("PersonController - Tests d'intégration")
class PersonControllerTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TripService tripService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PersonService personService;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private SecurityUtils securityUtils;

    private PersonResponse personResponse;
    private PersonProfileResponse profileResponse;

    @BeforeEach
    void setUp() {
        personResponse = PersonResponse.builder()
                .id(1L)
                .email("test@test.fr")
                .build();

        profileResponse = PersonProfileResponse.builder()
                .lastname("Dupont")
                .firstname("Jean")
                .phone("0612345678")
                .build();
    }

    //region GET ALL PERSONS
    @Nested
    @DisplayName("GET /persons")
    class GetAllPersonsTests {

        @Test
        @DisplayName("Devrait retourner 200 avec liste complète si ADMIN")
        void shouldReturn200WithFullListForAdmin() throws Exception {
            // Given
            List<PersonResponse> persons = List.of(
                    PersonResponse.builder().id(1L).email("user1@test.fr").build(),
                    PersonResponse.builder().id(2L).email("user2@test.fr").build()
            );
            when(personService.getAllPersons()).thenReturn(persons);

            // When & Then
            mockMvc.perform(get("/persons")
                            .with(jwt()
                                    .jwt(j -> j.claim("userId", 1L))
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].email").value("user1@test.fr"))
                    .andExpect(jsonPath("$[1].email").value("user2@test.fr"));

            verify(personService).getAllPersons();
        }

        @Test
        @DisplayName("Devrait retourner 200 avec liste minimale si minimal=true")
        void shouldReturn200WithMinimalListWhenMinimalTrue() throws Exception {
            // Given
            List<PersonMinimalResponse> minimalPersons = List.of(
                    PersonMinimalResponse.builder().id(1L).build(),
                    PersonMinimalResponse.builder().id(2L).build()
            );
            when(personService.getAllPersonsMinimal()).thenReturn(minimalPersons);

            // When & Then
            mockMvc.perform(get("/persons")
                            .param("minimal", "true")
                            .with(jwt()
                                    .jwt(j -> j.claim("userId", 1L))
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(personService).getAllPersonsMinimal();
            verify(personService, never()).getAllPersons();
        }

        @Test
        @DisplayName("Devrait retourner 403 si rôle insuffisant (STUDENT)")
        void shouldReturn403ForStudentRole() throws Exception {
            // When & Then
            mockMvc.perform(get("/persons")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 2L)
                                    .claim("roles", List.of("ROLE_STUDENT")))))
                    .andExpect(status().isForbidden());

            verify(personService, never()).getAllPersons();
        }

        @Test
        @DisplayName("Devrait retourner 401 sans token")
        void shouldReturn401WithoutToken() throws Exception {
            // When & Then
            mockMvc.perform(get("/persons"))
                    .andExpect(status().isUnauthorized());

            verify(personService, never()).getAllPersons();
        }
    }
    //endregion

    //region GET PERSON BY ID
    @Nested
    @DisplayName("GET /persons/{id}")
    class GetPersonByIdTests {

        @Test
        @DisplayName("Devrait retourner 200 avec la personne")
        void shouldReturn200WithPerson() throws Exception {
            // Given
            when(personService.getPersonById(1L)).thenReturn(personResponse);

            // When & Then
            mockMvc.perform(get("/persons/1")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 1L)
                                    .claim("roles", List.of("ROLE_STUDENT")))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("test@test.fr"));

            verify(personService).getPersonById(1L);
        }

        @Test
        @DisplayName("Devrait retourner 404 si personne inexistante")
        void shouldReturn404WhenPersonNotFound() throws Exception {
            // Given
            when(personService.getPersonById(999L))
                    .thenThrow(new ResourceNotFoundException("Personne", "id", 999L));

            // When & Then
            mockMvc.perform(get("/persons/999")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 1L)
                                    .claim("roles", List.of("ROLE_STUDENT")))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value(containsString("Personne")));
        }

        @Test
        @DisplayName("Devrait retourner 400 si ID invalide (type incorrect)")
        void shouldReturn400WhenIdIsInvalid() throws Exception {
            // When & Then
            mockMvc.perform(get("/persons/abc")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 1L)
                                    .claim("roles", List.of("ROLE_STUDENT")))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devrait retourner 401 sans token")
        void shouldReturn401WithoutToken() throws Exception {
            // When & Then
            mockMvc.perform(get("/persons/1"))
                    .andExpect(status().isUnauthorized());
        }
    }
    //endregion

    //region POST /persons
    @Nested
    @DisplayName("POST /persons")
    class CreatePersonProfileTests {

        private CreatePersonProfileRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = CreatePersonProfileRequest.builder()
                    .lastname("Dupont")
                    .firstname("Jean")
                    .birthday(LocalDate.of(1995, 5, 15))
                    .phone("0612345678")
                    .build();
        }

        @Test
        @DisplayName("Devrait retourner 201 lors de la création du profil")
        void shouldReturn201WhenProfileCreated() throws Exception {
            // Given
            when(securityUtils.resolveTargetPersonId(isNull(), any(Jwt.class))).thenReturn(1L);
            when(personService.createPersonProfile(eq(1L), any())).thenReturn(profileResponse);

            // When & Then
            mockMvc.perform(post("/persons")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 1L)
                                    .claim("roles", List.of("ROLE_STUDENT"))))
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.lastname").value("Dupont"))
                    .andExpect(jsonPath("$.firstname").value("Jean"));

            verify(personService).createPersonProfile(eq(1L), any());
        }

        @Test
        @DisplayName("Devrait retourner 409 si profil déjà existant")
        void shouldReturn409WhenProfileAlreadyExists() throws Exception {
            // Given
            when(securityUtils.resolveTargetPersonId(isNull(), any(Jwt.class))).thenReturn(1L);
            when(personService.createPersonProfile(eq(1L), any()))
                    .thenThrow(new DuplicateResourceException("Cette personne possède déjà un profil"));

            // When & Then
            mockMvc.perform(post("/persons")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 1L)
                                    .claim("roles", List.of("ROLE_STUDENT"))))
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("Devrait retourner 400 si body manquant")
        void shouldReturn400WhenBodyMissing() throws Exception {
            // When & Then
            mockMvc.perform(post("/persons")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 1L)
                                    .claim("roles", List.of("ROLE_STUDENT"))))
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devrait retourner 400 si champs obligatoires manquants")
        void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
            // Given - Request sans lastname/firstname
            CreatePersonProfileRequest invalidRequest = CreatePersonProfileRequest.builder()
                    .phone("0612345678")
                    .build();

            // When & Then
            mockMvc.perform(post("/persons")
                            .with(jwt()
                                    .jwt(j -> j.claim("userId", 1L))
                                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devrait retourner 403 si non-admin tente de créer pour un autre utilisateur")
        void shouldReturn403WhenNonAdminCreatesForOtherUser() throws Exception {
            // Given
            when(securityUtils.resolveTargetPersonId(eq(5L), any(Jwt.class)))
                    .thenThrow(new AccessDeniedException("Seuls les administrateurs peuvent effectuer cette action"));

            CreatePersonProfileRequest requestForOther = CreatePersonProfileRequest.builder()
                    .lastname("Dupont")
                    .firstname("Jean")
                    .personId(5L) // Tente de créer pour quelqu'un d'autre
                    .build();

            // When & Then
            mockMvc.perform(post("/persons")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 1L)
                                    .claim("roles", List.of("ROLE_STUDENT"))))
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestForOther)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Devrait retourner 415 si Content-Type invalide")
        void shouldReturn415WhenContentTypeInvalid() throws Exception {
            // When & Then
            mockMvc.perform(post("/persons")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 1L)
                                    .claim("roles", List.of("ROLE_STUDENT"))))
                            .contentType("text/plain")
                            .content("du texte brut"))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }
    //endregion

    //region PATCH /persons/{id}
    @Nested
    @DisplayName("PATCH /persons/{id}")
    class UpdatePersonTests {

        private UpdatePersonProfileRequest updateRequest;

        @BeforeEach
        void setUp() {
            updateRequest = UpdatePersonProfileRequest.builder()
                    .phone("0687654321")
                    .build();
        }

        @Test
        @DisplayName("Devrait retourner 200 si owner met à jour son profil")
        void shouldReturn200WhenOwnerUpdatesProfile() throws Exception {
            // Given
            when(securityUtils.isOwnerOrAdmin(eq(1L), any(Jwt.class))).thenReturn(true);
            when(personService.updatePersonProfile(eq(1L), any())).thenReturn(profileResponse);

            // When & Then
            mockMvc.perform(patch("/persons/1")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 1L)
                                    .claim("roles", List.of("ROLE_STUDENT"))))
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.phone").value("0612345678"));

            verify(personService).updatePersonProfile(eq(1L), any());
        }

        @Test
        @DisplayName("Devrait retourner 200 si ADMIN met à jour n'importe quel profil")
        void shouldReturn200WhenAdminUpdatesAnyProfile() throws Exception {
            // Given
            when(securityUtils.isOwnerOrAdmin(eq(2L), any(Jwt.class))).thenReturn(true);
            when(personService.updatePersonProfile(eq(2L), any())).thenReturn(profileResponse);

            // When & Then
            mockMvc.perform(patch("/persons/2")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 1L)
                                    .claim("roles", List.of("ROLE_ADMIN"))))
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk());

            verify(personService).updatePersonProfile(eq(2L), any());
        }

        @Test
        @DisplayName("Devrait retourner 403 si l'utilisateur n'est pas owner ni admin")
        void shouldReturn403WhenNotOwnerNorAdmin() throws Exception {
            // Given
            when(securityUtils.isOwnerOrAdmin(eq(2L), any(Jwt.class))).thenReturn(false);

            // When & Then
            mockMvc.perform(patch("/persons/2")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 1L)
                                    .claim("roles", List.of("ROLE_STUDENT"))))
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isForbidden());

            verify(personService, never()).updatePersonProfile(any(), any());
        }

        @Test
        @DisplayName("Devrait retourner 404 si personne inexistante")
        void shouldReturn404WhenPersonNotFound() throws Exception {
            // Given
            when(securityUtils.isOwnerOrAdmin(eq(999L), any(Jwt.class))).thenReturn(true);
            when(personService.updatePersonProfile(eq(999L), any()))
                    .thenThrow(new ResourceNotFoundException("Personne", "id", 999L));

            // When & Then
            mockMvc.perform(patch("/persons/999")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 1L)
                                    .claim("roles", List.of("ROLE_ADMIN"))))
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound());
        }
    }
    //endregion

    //region PATCH /persons/{id}/soft-delete
    @Nested
    @DisplayName("PATCH /persons/{id}/soft-delete")
    class SoftDeletePersonTests {

        @Test
        @DisplayName("Devrait retourner 200 si owner supprime son compte")
        void shouldReturn200WhenOwnerDeletesAccount() throws Exception {
            // Given
            when(securityUtils.isOwnerOrAdmin(eq(1L), any(Jwt.class))).thenReturn(true);
            when(personService.softDeletePerson(1L)).thenReturn(personResponse);

            // When & Then
            mockMvc.perform(patch("/persons/1/soft-delete")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 1L)
                                    .claim("roles", List.of("ROLE_STUDENT")))))
                    .andExpect(status().isOk());

            verify(personService).softDeletePerson(1L);
        }

        @Test
        @DisplayName("Devrait retourner 403 si non autorisé")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            when(securityUtils.isOwnerOrAdmin(eq(2L), any(Jwt.class))).thenReturn(false);

            // When & Then
            mockMvc.perform(patch("/persons/2/soft-delete")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 1L)
                                    .claim("roles", List.of("ROLE_STUDENT")))))
                    .andExpect(status().isForbidden());

            verify(personService, never()).softDeletePerson(any());
        }

        @Test
        @DisplayName("Devrait retourner 404 si personne inexistante")
        void shouldReturn404WhenPersonNotFound() throws Exception {
            // Given
            when(securityUtils.isOwnerOrAdmin(eq(999L), any(Jwt.class))).thenReturn(true);
            when(personService.softDeletePerson(999L))
                    .thenThrow(new ResourceNotFoundException("Personne", "id", 999L));

            // When & Then
            mockMvc.perform(patch("/persons/999/soft-delete")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 1L)
                                    .claim("roles", List.of("ROLE_ADMIN")))))
                    .andExpect(status().isNotFound());
        }
    }
    //endregion

    //region DELETE /persons/{id}
    @Nested
    @DisplayName("DELETE /persons/{id}")
    class DeletePersonTests {

        @Test
        @DisplayName("Devrait retourner 204 si ADMIN supprime définitivement")
        void shouldReturn204WhenAdminHardDeletes() throws Exception {
            // Given
            doNothing().when(personService).deletePerson(1L);

            // When & Then
            mockMvc.perform(delete("/persons/1")
                            .with(jwt()
                                    .jwt(j -> j.claim("userId", 1L))
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isNoContent());

            verify(personService).deletePerson(1L);
        }

        @Test
        @DisplayName("Devrait retourner 403 si non ADMIN")
        void shouldReturn403WhenNotAdmin() throws Exception {
            // When & Then
            mockMvc.perform(delete("/persons/1")
                            .with(jwt()
                                    .jwt(j -> j.claim("userId", 1L))
                                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                    .andExpect(status().isForbidden());

            verify(personService, never()).deletePerson(any());
        }

        @Test
        @DisplayName("Devrait retourner 404 si personne inexistante")
        void shouldReturn404WhenPersonNotFound() throws Exception {
            // Given
            doThrow(new ResourceNotFoundException("Personne", "id", 999L))
                    .when(personService).deletePerson(999L);

            // When & Then
            mockMvc.perform(delete("/persons/999")
                            .with(jwt()
                                    .jwt(j -> j.claim("userId", 1L))
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isNotFound());
        }
    }
    //endregion

    //region POST /persons/{id}/roles/{roleLabel}
    @Nested
    @DisplayName("POST /persons/{personId}/roles/{roleLabel}")
    class AssignRoleTests {

        @Test
        @DisplayName("Devrait retourner 200 si ADMIN assigne un rôle")
        void shouldReturn200WhenAdminAssignsRole() throws Exception {
            // Given
            when(personService.assignRole(1L, "ROLE_DRIVER")).thenReturn(personResponse);

            // When & Then
            mockMvc.perform(post("/persons/1/roles/ROLE_DRIVER")
                            .with(jwt()
                                    .jwt(j -> j.claim("userId", 1L))
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk());

            verify(personService).assignRole(1L, "ROLE_DRIVER");
        }

        @Test
        @DisplayName("Devrait retourner 403 si non ADMIN")
        void shouldReturn403WhenNotAdmin() throws Exception {
            // When & Then
            mockMvc.perform(post("/persons/1/roles/ROLE_DRIVER")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 2L)
                                    .claim("roles", List.of("ROLE_STUDENT")))))
                    .andExpect(status().isForbidden());

            verify(personService, never()).assignRole(any(), any());
        }

        @Test
        @DisplayName("Devrait retourner 404 si rôle inexistant")
        void shouldReturn404WhenRoleNotFound() throws Exception {
            // Given
            when(personService.assignRole(1L, "ROLE_INVALID"))
                    .thenThrow(new ResourceNotFoundException("Rôle", "label", "ROLE_INVALID"));

            // When & Then
            mockMvc.perform(post("/persons/1/roles/ROLE_INVALID")
                            .with(jwt()
                                    .jwt(j -> j.claim("userId", 1L))
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(containsString("Rôle")));
        }
    }
    //endregion

    //region DELETE /persons/{id}/roles/{roleLabel}
    @Nested
    @DisplayName("DELETE /persons/{personId}/roles/{roleLabel}")
    class RemoveRoleTests {

        @Test
        @DisplayName("Devrait retourner 200 si ADMIN retire un rôle")
        void shouldReturn200WhenAdminRemovesRole() throws Exception {
            // Given
            when(personService.removeRole(1L, "ROLE_DRIVER")).thenReturn(personResponse);

            // When & Then
            mockMvc.perform(delete("/persons/1/roles/ROLE_DRIVER")
                            .with(jwt()
                                    .jwt(j -> j.claim("userId", 1L))
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk());

            verify(personService).removeRole(1L, "ROLE_DRIVER");
        }

        @Test
        @DisplayName("Devrait retourner 403 si non ADMIN")
        void shouldReturn403WhenNotAdmin() throws Exception {
            // When & Then
            mockMvc.perform(delete("/persons/1/roles/ROLE_DRIVER")
                            .with(jwt().jwt(j -> j
                                    .claim("userId", 2L)
                                    .claim("roles", List.of("ROLE_STUDENT")))))
                    .andExpect(status().isForbidden());

            verify(personService, never()).removeRole(any(), any());
        }
    }
    //endregion
}