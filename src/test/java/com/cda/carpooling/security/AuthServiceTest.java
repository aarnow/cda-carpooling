package com.cda.carpooling.security;

import com.cda.carpooling.dto.request.AuthRequest;
import com.cda.carpooling.dto.request.CreatePersonRequest;
import com.cda.carpooling.dto.response.AuthResponse;
import com.cda.carpooling.dto.response.PersonResponse;
import com.cda.carpooling.entity.Person;
import com.cda.carpooling.entity.PersonStatus;
import com.cda.carpooling.entity.Role;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.repository.PersonRepository;
import com.cda.carpooling.service.PersonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Tests Unitaires")
class AuthServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PersonService personService;

    @Mock
    private JwtService jwtService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    // Fixtures
    private Person activePerson;
    private Person suspendedPerson;
    private PersonStatus activeStatus;
    private PersonStatus suspendedStatus;
    private AuthRequest validAuthRequest;
    private PersonResponse mockPersonResponse;

    @BeforeEach
    void setUp() {
        activeStatus = PersonStatus.builder()
                .id(1L)
                .label(PersonStatus.ACTIVE)
                .build();

        suspendedStatus = PersonStatus.builder()
                .id(2L)
                .label(PersonStatus.SUSPENDED)
                .build();

        Role studentRole = Role.builder()
                .id(1L)
                .label("ROLE_STUDENT")
                .build();

        activePerson = Person.builder()
                .id(1L)
                .email("test@test.fr")
                .password("hashed_password")
                .status(activeStatus)
                .roles(Set.of(studentRole))
                .build();

        suspendedPerson = Person.builder()
                .id(2L)
                .email("suspended@test.fr")
                .password("hashed_password")
                .status(suspendedStatus)
                .roles(Set.of(studentRole))
                .build();

        mockPersonResponse = PersonResponse.builder()
                .id(10L)
                .email("nouveau@test.fr")
                .build();

        validAuthRequest = new AuthRequest("test@test.fr", "password123");
    }

    // ==================== LOGIN ====================

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("Devrait retourner un token JWT lors d'un login réussi")
        void shouldReturnTokenWhenLoginSucceeds() {
            // Given
            when(personRepository.findByEmailWithProfileAndRoles("test@test.fr"))
                    .thenReturn(Optional.of(activePerson));
            when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
            when(jwtService.generateToken(activePerson)).thenReturn("jwt.token.value");

            // When
            AuthResponse response = authService.login(validAuthRequest);

            // Then
            assertThat(response.getToken()).isEqualTo("jwt.token.value");
            assertThat(response.getType()).isEqualTo("Bearer");
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getRoles()).contains("ROLE_STUDENT");

            verify(jwtService).generateToken(activePerson);
        }

        @Test
        @DisplayName("Devrait lancer ResourceNotFoundException si email inconnu")
        void shouldThrowResourceNotFoundWhenEmailUnknown() {
            // Given
            when(personRepository.findByEmailWithProfileAndRoles("inconnu@test.fr"))
                    .thenReturn(Optional.empty());

            AuthRequest request = new AuthRequest("inconnu@test.fr", "password123");

            // When & Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Personne");

            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("Devrait lancer BadCredentialsException si mot de passe incorrect")
        void shouldThrowBadCredentialsWhenPasswordWrong() {
            // Given
            when(personRepository.findByEmailWithProfileAndRoles("test@test.fr"))
                    .thenReturn(Optional.of(activePerson));
            when(passwordEncoder.matches("mauvais_mdp", "hashed_password")).thenReturn(false);

            AuthRequest request = new AuthRequest("test@test.fr", "mauvais_mdp");

            // When & Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Email ou mot de passe incorrect");

            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("Devrait lancer DisabledException si le compte est suspendu")
        void shouldThrowDisabledExceptionWhenAccountSuspended() {
            // Given
            when(personRepository.findByEmailWithProfileAndRoles("suspended@test.fr"))
                    .thenReturn(Optional.of(suspendedPerson));
            when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);

            AuthRequest request = new AuthRequest("suspended@test.fr", "password123");

            // When & Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(DisabledException.class)
                    .hasMessageContaining("compte n'est pas accessible");

            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("Devrait lancer DisabledException si le compte est en attente")
        void shouldThrowDisabledExceptionWhenAccountPending() {
            // Given
            PersonStatus pendingStatus = PersonStatus.builder()
                    .id(3L)
                    .label(PersonStatus.PENDING)
                    .build();

            Person pendingPerson = Person.builder()
                    .id(3L)
                    .email("pending@test.fr")
                    .password("hashed_password")
                    .status(pendingStatus)
                    .roles(Set.of())
                    .build();

            when(personRepository.findByEmailWithProfileAndRoles("pending@test.fr"))
                    .thenReturn(Optional.of(pendingPerson));
            when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);

            AuthRequest request = new AuthRequest("pending@test.fr", "password123");

            // When & Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(DisabledException.class);

            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("Devrait inclure tous les rôles dans la réponse")
        void shouldIncludeAllRolesInResponse() {
            // Given
            Role driverRole = Role.builder().id(2L).label("ROLE_DRIVER").build();
            Role studentRole = Role.builder().id(1L).label("ROLE_STUDENT").build();

            Person multiRolePerson = Person.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .password("hashed_password")
                    .status(activeStatus)
                    .roles(Set.of(studentRole, driverRole))
                    .build();

            when(personRepository.findByEmailWithProfileAndRoles("test@test.fr"))
                    .thenReturn(Optional.of(multiRolePerson));
            when(passwordEncoder.matches(any(), any())).thenReturn(true);
            when(jwtService.generateToken(any())).thenReturn("jwt.token.value");

            // When
            AuthResponse response = authService.login(validAuthRequest);

            // Then
            assertThat(response.getRoles())
                    .hasSize(2)
                    .contains("ROLE_STUDENT", "ROLE_DRIVER");
        }
    }

    // ==================== REGISTER ====================

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        private CreatePersonRequest validRegisterRequest;

        @BeforeEach
        void setUp() {
            validRegisterRequest = new CreatePersonRequest("nouveau@test.fr", "Password123!");
        }

        @Test
        @DisplayName("Devrait créer un compte et retourner un token JWT")
        void shouldCreateAccountAndReturnToken() {
            // Given
            when(personService.createPerson(any(CreatePersonRequest.class))).thenReturn(mockPersonResponse);

            Person newPerson = Person.builder()
                    .id(10L)
                    .email("nouveau@test.fr")
                    .password("hashed_password")
                    .status(activeStatus)
                    .roles(Set.of())
                    .build();

            when(personRepository.findByEmailWithProfileAndRoles("nouveau@test.fr"))
                    .thenReturn(Optional.of(newPerson));
            when(passwordEncoder.matches("Password123!", "hashed_password")).thenReturn(true);
            when(jwtService.generateToken(newPerson)).thenReturn("nouveau.jwt.token");

            // When
            AuthResponse response = authService.register(validRegisterRequest);

            // Then
            assertThat(response.getToken()).isEqualTo("nouveau.jwt.token");
            assertThat(response.getType()).isEqualTo("Bearer");
            assertThat(response.getUserId()).isEqualTo(10L);

            verify(personService).createPerson(validRegisterRequest);
            verify(personRepository).findByEmailWithProfileAndRoles("nouveau@test.fr");
        }

        @Test
        @DisplayName("Devrait propager l'exception si createPerson échoue")
        void shouldPropagateExceptionWhenCreatePersonFails() {
            // Given
            doThrow(new RuntimeException("Email déjà utilisé"))
                    .when(personService).createPerson(any());

            // When & Then
            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email déjà utilisé");

            // Login ne doit jamais être appelé
            verify(personRepository, never()).findByEmailWithProfileAndRoles(any());
            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("Devrait appeler login avec les mêmes credentials après register")
        void shouldCallLoginWithSameCredentialsAfterRegister() {
            // Given
            when(personService.createPerson(any(CreatePersonRequest.class))).thenReturn(mockPersonResponse);

            when(personRepository.findByEmailWithProfileAndRoles("nouveau@test.fr"))
                    .thenReturn(Optional.of(activePerson));
            when(passwordEncoder.matches(eq("Password123!"), any())).thenReturn(true);
            when(jwtService.generateToken(any())).thenReturn("token");

            // When
            authService.register(validRegisterRequest);

            // Then
            verify(personRepository).findByEmailWithProfileAndRoles("nouveau@test.fr");
            verify(passwordEncoder).matches(eq("Password123!"), any());
        }
    }
}