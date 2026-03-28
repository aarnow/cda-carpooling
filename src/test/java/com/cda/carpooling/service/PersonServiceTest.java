package com.cda.carpooling.service;

import com.cda.carpooling.dto.request.CreatePersonProfileRequest;
import com.cda.carpooling.dto.request.CreatePersonRequest;
import com.cda.carpooling.dto.request.UpdatePersonProfileRequest;
import com.cda.carpooling.dto.response.PersonMinimalResponse;
import com.cda.carpooling.dto.response.PersonProfileResponse;
import com.cda.carpooling.dto.response.PersonResponse;
import com.cda.carpooling.entity.*;
import com.cda.carpooling.exception.DuplicateResourceException;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.mapper.PersonMapper;
import com.cda.carpooling.mapper.PersonProfileMapper;
import com.cda.carpooling.repository.PersonRepository;
import com.cda.carpooling.repository.PersonStatusRepository;
import com.cda.carpooling.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires - PersonService
 *
 * Utilise Mockito pour isoler le service de ses dépendances.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PersonService - Tests Unitaires Complets")
class PersonServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PersonStatusRepository personStatusRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PersonMapper personMapper;

    @Mock
    private PersonProfileMapper personProfileMapper;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private PersonService personService;

    @Mock
    private ReservationService reservationService;

    private Person testPerson;
    private PersonStatus activeStatus;
    private PersonStatus deletedStatus;
    private Role studentRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Statuts
        activeStatus = new PersonStatus();
        activeStatus.setId(1L);
        activeStatus.setLabel(PersonStatus.ACTIVE);

        deletedStatus = new PersonStatus();
        deletedStatus.setId(2L);
        deletedStatus.setLabel(PersonStatus.DELETED);

        // Rôles
        studentRole = new Role();
        studentRole.setId(1L);
        studentRole.setLabel(Role.ROLE_STUDENT);

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setLabel(Role.ROLE_ADMIN);

        testPerson = Person.builder()
                .id(1L)
                .email("test@test.fr")
                .password("hashedPassword")
                .status(activeStatus)
                .roles(new HashSet<>(Collections.singletonList(studentRole)))
                .build();
    }

    //region CREATE PERSON
    @Nested
    @DisplayName("createPerson()")
    class CreatePersonTests {

        private CreatePersonRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = CreatePersonRequest.builder()
                    .email("nouveau@test.fr")
                    .password("password123")
                    .build();
        }

        @Test
        @DisplayName("Devrait créer une personne avec succès")
        void shouldCreatePersonSuccessfully() {
            // Given
            when(personRepository.existsByEmail("nouveau@test.fr")).thenReturn(false);

            Person mappedPerson = Person.builder()
                    .email("nouveau@test.fr")
                    .password("password123")
                    .build();
            when(personMapper.toEntity(validRequest)).thenReturn(mappedPerson);

            when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
            when(personStatusRepository.findByLabel(PersonStatus.ACTIVE))
                    .thenReturn(Optional.of(activeStatus));
            when(roleRepository.findByLabel(Role.ROLE_STUDENT))
                    .thenReturn(Optional.of(studentRole));

            Person savedPerson = Person.builder()
                    .id(2L)
                    .email("nouveau@test.fr")
                    .password("hashedPassword123")
                    .status(activeStatus)
                    .roles(new HashSet<>(Collections.singletonList(studentRole)))
                    .build();
            when(personRepository.save(any(Person.class))).thenReturn(savedPerson);

            PersonResponse expectedResponse = PersonResponse.builder()
                    .id(2L)
                    .email("nouveau@test.fr")
                    .build();
            when(personMapper.toResponse(savedPerson)).thenReturn(expectedResponse);

            // When
            PersonResponse result = personService.createPerson(validRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getEmail()).isEqualTo("nouveau@test.fr");

            verify(personRepository).existsByEmail("nouveau@test.fr");
            verify(passwordEncoder).encode("password123");
            verify(personStatusRepository).findByLabel(PersonStatus.ACTIVE);
            verify(roleRepository).findByLabel(Role.ROLE_STUDENT);
            verify(personRepository).save(any(Person.class));
        }

        @Test
        @DisplayName("Devrait lever DuplicateResourceException si email existe déjà")
        void shouldThrowDuplicateResourceExceptionWhenEmailExists() {
            // Given
            when(personRepository.existsByEmail("nouveau@test.fr")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> personService.createPerson(validRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Personne")
                    .hasMessageContaining("email")
                    .hasMessageContaining("nouveau@test.fr");

            verify(personRepository).existsByEmail("nouveau@test.fr");
            verify(personRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si statut par défaut inexistant")
        void shouldThrowResourceNotFoundExceptionWhenDefaultStatusNotExists() {
            // Given
            when(personRepository.existsByEmail("nouveau@test.fr")).thenReturn(false);

            Person mappedPerson = Person.builder()
                    .email("nouveau@test.fr")
                    .password("password123")
                    .build();
            when(personMapper.toEntity(validRequest)).thenReturn(mappedPerson);
            when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
            when(personStatusRepository.findByLabel(PersonStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> personService.createPerson(validRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("statut par défaut");

            verify(personRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si rôle par défaut inexistant")
        void shouldThrowResourceNotFoundExceptionWhenDefaultRoleNotExists() {
            // Given
            when(personRepository.existsByEmail("nouveau@test.fr")).thenReturn(false);

            Person mappedPerson = Person.builder()
                    .email("nouveau@test.fr")
                    .password("password123")
                    .build();
            when(personMapper.toEntity(validRequest)).thenReturn(mappedPerson);
            when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
            when(personStatusRepository.findByLabel(PersonStatus.ACTIVE))
                    .thenReturn(Optional.of(activeStatus));
            when(roleRepository.findByLabel(Role.ROLE_STUDENT))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> personService.createPerson(validRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("rôle par défaut");

            verify(personRepository, never()).save(any());
        }
    }
    //endregion

    //region CREATE PERSON PROFILE
    @Nested
    @DisplayName("createPersonProfile()")
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
        @DisplayName("Devrait créer un profil avec succès")
        void shouldCreateProfileSuccessfully() {
            // Given
            when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));

            PersonProfile.builder()
                    .id(1L)
                    .lastname("Dupont")
                    .firstname("Jean")
                    .birthday(LocalDate.of(1995, 5, 15))
                    .phone("0612345678")
                    .person(testPerson)
                    .build();

            PersonProfileResponse expectedResponse = PersonProfileResponse.builder()
                    .lastname("Dupont")
                    .firstname("Jean")
                    .build();

            when(personRepository.save(any(Person.class))).thenReturn(testPerson);
            when(personProfileMapper.toResponse(any(PersonProfile.class))).thenReturn(expectedResponse);

            // When
            PersonProfileResponse result = personService.createPersonProfile(1L, validRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getLastname()).isEqualTo("Dupont");
            assertThat(result.getFirstname()).isEqualTo("Jean");

            verify(personRepository).findById(1L);
            verify(personRepository).save(any(Person.class));
            verify(personProfileMapper).toResponse(any(PersonProfile.class));
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si personne inexistante")
        void shouldThrowResourceNotFoundExceptionWhenPersonNotExists() {
            // Given
            when(personRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> personService.createPersonProfile(999L, validRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Personne")
                    .hasMessageContaining("999");

            verify(personRepository).findById(999L);
            verify(personRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait lever DuplicateResourceException si profil déjà existant")
        void shouldThrowDuplicateResourceExceptionWhenProfileAlreadyExists() {
            // Given
            PersonProfile existingProfile = PersonProfile.builder()
                    .id(1L)
                    .lastname("Ancien")
                    .firstname("Profil")
                    .person(testPerson)
                    .build();

            testPerson.setProfile(existingProfile);

            when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));

            // When & Then
            assertThatThrownBy(() -> personService.createPersonProfile(1L, validRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("profil");

            verify(personRepository).findById(1L);
            verify(personRepository, never()).save(any());
        }
    }
    //endregion

    //region GET ALL PERSONS
    @Nested
    @DisplayName("getAllPersons()")
    class GetAllPersonsTests {

        @Test
        @DisplayName("Devrait retourner une liste vide si aucune personne")
        void shouldReturnEmptyListWhenNoPersons() {
            // Given
            when(personRepository.findAllWithProfileAndRoles()).thenReturn(Collections.emptyList());

            // When
            List<PersonResponse> result = personService.getAllPersons();

            // Then
            assertThat(result).isEmpty();
            verify(personRepository).findAllWithProfileAndRoles();
        }

        @Test
        @DisplayName("Devrait retourner toutes les personnes")
        void shouldReturnAllPersons() {
            // Given
            Person person1 = Person.builder()
                    .id(1L)
                    .email("user1@test.fr")
                    .build();

            Person person2 = Person.builder()
                    .id(2L)
                    .email("user2@test.fr")
                    .build();

            List<Person> persons = Arrays.asList(person1, person2);
            when(personRepository.findAllWithProfileAndRoles()).thenReturn(persons);

            PersonResponse response1 = PersonResponse.builder()
                    .id(1L)
                    .email("user1@test.fr")
                    .build();

            PersonResponse response2 = PersonResponse.builder()
                    .id(2L)
                    .email("user2@test.fr")
                    .build();

            when(personMapper.toResponse(person1)).thenReturn(response1);
            when(personMapper.toResponse(person2)).thenReturn(response2);

            // When
            List<PersonResponse> result = personService.getAllPersons();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(PersonResponse::getId)
                    .containsExactly(1L, 2L);

            verify(personRepository).findAllWithProfileAndRoles();
            verify(personMapper, times(2)).toResponse(any(Person.class));
        }
    }
    //endregion

    //region GET ALL PERSONS MINIMAL
    @Nested
    @DisplayName("getAllPersonsMinimal()")
    class GetAllPersonsMinimalTests {

        @Test
        @DisplayName("Devrait retourner une liste vide si aucune personne")
        void shouldReturnEmptyListWhenNoPersons() {
            // Given
            when(personRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<PersonMinimalResponse> result = personService.getAllPersonsMinimal();

            // Then
            assertThat(result).isEmpty();
            verify(personRepository).findAll();
        }

        @Test
        @DisplayName("Devrait retourner toutes les personnes (version minimale)")
        void shouldReturnAllPersonsMinimal() {
            // Given
            Person person1 = Person.builder()
                    .id(1L)
                    .email("user1@test.fr")
                    .build();

            Person person2 = Person.builder()
                    .id(2L)
                    .email("user2@test.fr")
                    .build();

            List<Person> persons = Arrays.asList(person1, person2);
            when(personRepository.findAll()).thenReturn(persons);

            PersonMinimalResponse response1 = PersonMinimalResponse.builder()
                    .id(1L)
                    .email("user1@test.fr")
                    .build();

            PersonMinimalResponse response2 = PersonMinimalResponse.builder()
                    .id(2L)
                    .email("user2@test.fr")
                    .build();

            when(personMapper.toMinimalResponse(person1)).thenReturn(response1);
            when(personMapper.toMinimalResponse(person2)).thenReturn(response2);

            // When
            List<PersonMinimalResponse> result = personService.getAllPersonsMinimal();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(PersonMinimalResponse::getId)
                    .containsExactly(1L, 2L);

            verify(personRepository).findAll();
            verify(personMapper, times(2)).toMinimalResponse(any(Person.class));
        }
    }
    //endregion

    //region GET BY ID
    @Nested
    @DisplayName("getPersonById()")
    class GetPersonByIdTests {

        @Test
        @DisplayName("Devrait retourner une personne par ID")
        void shouldReturnPersonById() {
            // Given
            when(personRepository.findByIdWithProfileAndRoles(1L))
                    .thenReturn(Optional.of(testPerson));
            when(personMapper.toResponse(testPerson))
                    .thenReturn(PersonResponse.builder()
                            .id(1L)
                            .email("test@test.fr")
                            .build());

            // When
            PersonResponse result = personService.getPersonById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("test@test.fr");

            verify(personRepository).findByIdWithProfileAndRoles(1L);
            verify(personMapper).toResponse(testPerson);
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si personne inexistante")
        void shouldThrowResourceNotFoundExceptionWhenNotExists() {
            // Given
            when(personRepository.findByIdWithProfileAndRoles(999L))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> personService.getPersonById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Personne")
                    .hasMessageContaining("999");

            verify(personMapper, never()).toResponse(any());
        }
    }
    //endregion

    //region UPDATE PERSON PROFILE
    @Nested
    @DisplayName("updatePersonProfile()")
    class UpdatePersonProfileTests {

        private UpdatePersonProfileRequest updateRequest;
        private PersonProfile existingProfile;

        @BeforeEach
        void setUp() {
            updateRequest = UpdatePersonProfileRequest.builder()
                    .phone("0687654321")
                    .build();

            existingProfile = PersonProfile.builder()
                    .id(1L)
                    .lastname("Dupont")
                    .firstname("Jean")
                    .phone("0612345678")
                    .birthday(LocalDate.of(1995, 5, 15))
                    .person(testPerson)
                    .build();

            testPerson.setProfile(existingProfile);
        }

        @Test
        @DisplayName("Devrait mettre à jour uniquement le téléphone")
        void shouldUpdateOnlyPhone() {
            // Given
            when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));

            PersonProfileResponse expectedResponse = PersonProfileResponse.builder()
                    .lastname("Dupont")
                    .firstname("Jean")
                    .phone("0687654321")
                    .build();

            when(personRepository.save(any(Person.class))).thenReturn(testPerson);
            when(personProfileMapper.toResponse(any(PersonProfile.class))).thenReturn(expectedResponse);

            // When
            PersonProfileResponse result = personService.updatePersonProfile(1L, updateRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPhone()).isEqualTo("0687654321");
            assertThat(existingProfile.getLastname()).isEqualTo("Dupont");
            assertThat(existingProfile.getFirstname()).isEqualTo("Jean");

            verify(personRepository).findById(1L);
            verify(personRepository).save(testPerson);
        }

        @Test
        @DisplayName("Devrait mettre à jour uniquement le nom")
        void shouldUpdateOnlyLastname() {
            // Given
            UpdatePersonProfileRequest lastnameRequest = UpdatePersonProfileRequest.builder()
                    .lastname("Martin")
                    .build();

            when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));
            when(personRepository.save(any(Person.class))).thenReturn(testPerson);
            when(personProfileMapper.toResponse(any(PersonProfile.class)))
                    .thenReturn(PersonProfileResponse.builder().build());

            // When
            personService.updatePersonProfile(1L, lastnameRequest);

            // Then
            assertThat(existingProfile.getLastname()).isEqualTo("Martin");
            assertThat(existingProfile.getFirstname()).isEqualTo("Jean");
            assertThat(existingProfile.getPhone()).isEqualTo("0612345678");
            verify(personRepository).save(testPerson);
        }

        @Test
        @DisplayName("Devrait mettre à jour uniquement le prénom")
        void shouldUpdateOnlyFirstname() {
            // Given
            UpdatePersonProfileRequest firstnameRequest = UpdatePersonProfileRequest.builder()
                    .firstname("Sophie")
                    .build();

            when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));
            when(personRepository.save(any(Person.class))).thenReturn(testPerson);
            when(personProfileMapper.toResponse(any(PersonProfile.class)))
                    .thenReturn(PersonProfileResponse.builder().build());

            // When
            personService.updatePersonProfile(1L, firstnameRequest);

            // Then
            assertThat(existingProfile.getFirstname()).isEqualTo("Sophie");
            assertThat(existingProfile.getLastname()).isEqualTo("Dupont");
            verify(personRepository).save(testPerson);
        }

        @Test
        @DisplayName("Devrait mettre à jour uniquement la date de naissance")
        void shouldUpdateOnlyBirthday() {
            // Given
            LocalDate newBirthday = LocalDate.of(1990, 3, 20);
            UpdatePersonProfileRequest birthdayRequest = UpdatePersonProfileRequest.builder()
                    .birthday(newBirthday)
                    .build();

            when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));
            when(personRepository.save(any(Person.class))).thenReturn(testPerson);
            when(personProfileMapper.toResponse(any(PersonProfile.class)))
                    .thenReturn(PersonProfileResponse.builder().build());

            // When
            personService.updatePersonProfile(1L, birthdayRequest);

            // Then
            assertThat(existingProfile.getBirthday()).isEqualTo(newBirthday);
            assertThat(existingProfile.getFirstname()).isEqualTo("Jean");
            verify(personRepository).save(testPerson);
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si personne inexistante")
        void shouldThrowResourceNotFoundExceptionWhenPersonNotExists() {
            // Given
            when(personRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> personService.updatePersonProfile(999L, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Personne");

            verify(personRepository).findById(999L);
            verify(personRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si pas de profil")
        void shouldThrowResourceNotFoundExceptionWhenNoProfile() {
            // Given
            testPerson.setProfile(null);
            when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));

            // When & Then
            assertThatThrownBy(() -> personService.updatePersonProfile(1L, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("profil");

            verify(personRepository).findById(1L);
            verify(personRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait mettre à jour tous les champs fournis")
        void shouldUpdateAllProvidedFields() {
            // Given
            UpdatePersonProfileRequest fullUpdateRequest = UpdatePersonProfileRequest.builder()
                    .lastname("Martin")
                    .firstname("Sophie")
                    .birthday(LocalDate.of(1990, 3, 20))
                    .phone("0698765432")
                    .build();

            when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));
            when(personRepository.save(any(Person.class))).thenReturn(testPerson);
            when(personProfileMapper.toResponse(any(PersonProfile.class)))
                    .thenReturn(PersonProfileResponse.builder()
                            .lastname("Martin")
                            .firstname("Sophie")
                            .build());

            // When
            personService.updatePersonProfile(1L, fullUpdateRequest);

            // Then
            assertThat(existingProfile.getLastname()).isEqualTo("Martin");
            assertThat(existingProfile.getFirstname()).isEqualTo("Sophie");
            assertThat(existingProfile.getBirthday()).isEqualTo(LocalDate.of(1990, 3, 20));
            assertThat(existingProfile.getPhone()).isEqualTo("0698765432");

            verify(personRepository).save(testPerson);
        }
    }
    //endregion

    //region SOFT DELETE
    @Nested
    @DisplayName("softDeletePerson()")
    class SoftDeletePersonTests {

        private PersonProfile profile;

        @BeforeEach
        void setUp() {
            profile = PersonProfile.builder()
                    .id(1L)
                    .lastname("Dupont")
                    .firstname("Jean")
                    .phone("0612345678")
                    .birthday(LocalDate.of(1995, 5, 15))
                    .person(testPerson)
                    .build();

            testPerson.setProfile(profile);
        }

        @Test
        @DisplayName("Devrait anonymiser les données personnelles")
        void shouldAnonymizePersonalData() {
            // Given
            when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));
            when(personStatusRepository.findByLabel(PersonStatus.DELETED))
                    .thenReturn(Optional.of(deletedStatus));
            when(personRepository.save(any(Person.class))).thenReturn(testPerson);
            when(personMapper.toResponse(any(Person.class)))
                    .thenReturn(PersonResponse.builder().build());

            // When
            personService.softDeletePerson(1L);

            // Then
            assertThat(testPerson.getEmail()).startsWith("deleted-");
            assertThat(testPerson.getEmail()).endsWith("@anonymized.local");
            assertThat(testPerson.getPassword()).isEqualTo("DELETED");
            assertThat(testPerson.getStatus()).isEqualTo(deletedStatus);
            assertThat(testPerson.getDeletedAt()).isNotNull();

            assertThat(profile.getFirstname()).isEqualTo("Utilisateur");
            assertThat(profile.getLastname()).isEqualTo("Supprimé");
            assertThat(profile.getPhone()).isNull();
            assertThat(profile.getBirthday()).isNull();

            verify(personRepository).save(testPerson);
        }

        @Test
        @DisplayName("Devrait anonymiser même sans profil")
        void shouldAnonymizeEvenWithoutProfile() {
            // Given
            testPerson.setProfile(null);
            when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));
            when(personStatusRepository.findByLabel(PersonStatus.DELETED))
                    .thenReturn(Optional.of(deletedStatus));
            when(personRepository.save(any(Person.class))).thenReturn(testPerson);
            when(personMapper.toResponse(any(Person.class)))
                    .thenReturn(PersonResponse.builder().build());

            // When
            personService.softDeletePerson(1L);

            // Then
            assertThat(testPerson.getEmail()).startsWith("deleted-");
            assertThat(testPerson.getPassword()).isEqualTo("DELETED");
            assertThat(testPerson.getStatus()).isEqualTo(deletedStatus);

            verify(personRepository).save(testPerson);
        }

        @Test
        @DisplayName("Devrait conserver les relations (rôles)")
        void shouldPreserveRelationships() {
            // Given
            when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));
            when(personStatusRepository.findByLabel(PersonStatus.DELETED))
                    .thenReturn(Optional.of(deletedStatus));
            when(personRepository.save(any(Person.class))).thenReturn(testPerson);
            when(personMapper.toResponse(any(Person.class)))
                    .thenReturn(PersonResponse.builder().build());

            // When
            personService.softDeletePerson(1L);

            // Then
            assertThat(testPerson.getRoles()).isNotEmpty();
            assertThat(testPerson.getRoles()).contains(studentRole);

            verify(personRepository).save(testPerson);
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si personne inexistante")
        void shouldThrowResourceNotFoundExceptionWhenPersonNotExists() {
            // Given
            when(personRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> personService.softDeletePerson(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Personne");

            verify(personRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait lever DuplicateResourceException si déjà supprimé")
        void shouldThrowDuplicateResourceExceptionWhenAlreadyDeleted() {
            // Given
            testPerson.setStatus(deletedStatus);
            when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));

            // When & Then
            assertThatThrownBy(() -> personService.softDeletePerson(1L))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("déjà anonymisé");

            verify(personRepository, never()).save(any());
            verify(personStatusRepository, never()).findByLabel(anyString());
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si statut DELETED inexistant")
        void shouldThrowResourceNotFoundExceptionWhenDeletedStatusNotExists() {
            // Given
            when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));
            when(personStatusRepository.findByLabel(PersonStatus.DELETED))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> personService.softDeletePerson(1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("DELETED");

            verify(personRepository, never()).save(any());
        }
    }
    //endregion

    //region DELETE PERSON
    @Nested
    @DisplayName("deletePerson()")
    class DeletePersonTests {

        @Test
        @DisplayName("Devrait supprimer définitivement une personne")
        void shouldDeletePersonPermanently() {
            // Given
            when(personRepository.existsById(1L)).thenReturn(true);

            // When
            personService.deletePerson(1L);

            // Then
            verify(personRepository).existsById(1L);
            verify(personRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si personne inexistante")
        void shouldThrowResourceNotFoundExceptionWhenPersonNotExists() {
            // Given
            when(personRepository.existsById(999L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> personService.deletePerson(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Personne")
                    .hasMessageContaining("999");

            verify(personRepository).existsById(999L);
            verify(personRepository, never()).deleteById(anyLong());
        }
    }
    //endregion

    //region ASSIGN ROLE
    @Nested
    @DisplayName("assignRole()")
    class AssignRoleTests {

        @Test
        @DisplayName("Devrait assigner un rôle avec succès")
        void shouldAssignRoleSuccessfully() {
            // Given
            when(personRepository.findByIdWithProfileAndRoles(1L))
                    .thenReturn(Optional.of(testPerson));
            when(roleRepository.findByLabel(Role.ROLE_ADMIN))
                    .thenReturn(Optional.of(adminRole));
            when(personRepository.save(any(Person.class))).thenReturn(testPerson);
            when(personMapper.toResponse(any(Person.class)))
                    .thenReturn(PersonResponse.builder().build());

            // When
            personService.assignRole(1L, Role.ROLE_ADMIN);

            // Then
            assertThat(testPerson.getRoles()).contains(adminRole);

            verify(personRepository).findByIdWithProfileAndRoles(1L);
            verify(roleRepository).findByLabel(Role.ROLE_ADMIN);
            verify(personRepository).save(testPerson);
        }

        @Test
        @DisplayName("Devrait être idempotent (assigner 2 fois le même rôle)")
        void shouldBeIdempotentWhenRoleAlreadyAssigned() {
            // Given
            testPerson.addRole(adminRole);

            when(personRepository.findByIdWithProfileAndRoles(1L))
                    .thenReturn(Optional.of(testPerson));
            when(roleRepository.findByLabel(Role.ROLE_ADMIN))
                    .thenReturn(Optional.of(adminRole));
            when(personRepository.save(any(Person.class))).thenReturn(testPerson);
            when(personMapper.toResponse(any(Person.class)))
                    .thenReturn(PersonResponse.builder().build());

            int initialRoleCount = testPerson.getRoles().size();

            // When
            personService.assignRole(1L, Role.ROLE_ADMIN);

            // Then
            assertThat(testPerson.getRoles()).hasSize(initialRoleCount);

            verify(personRepository).save(testPerson);
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si personne inexistante")
        void shouldThrowResourceNotFoundExceptionWhenPersonNotExists() {
            // Given
            when(personRepository.findByIdWithProfileAndRoles(999L))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> personService.assignRole(999L, Role.ROLE_ADMIN))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Personne");

            verify(roleRepository, never()).findByLabel(anyString());
            verify(personRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si rôle inexistant")
        void shouldThrowResourceNotFoundExceptionWhenRoleNotExists() {
            // Given
            when(personRepository.findByIdWithProfileAndRoles(1L))
                    .thenReturn(Optional.of(testPerson));
            when(roleRepository.findByLabel("ROLE_INVALID"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> personService.assignRole(1L, "ROLE_INVALID"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Rôle");

            verify(personRepository, never()).save(any());
        }
    }
    //endregion

    //region REMOVE ROLE
    @Nested
    @DisplayName("removeRole()")
    class RemoveRoleTests {

        @BeforeEach
        void setUp() {
            testPerson.addRole(adminRole);
        }

        @Test
        @DisplayName("Devrait retirer un rôle avec succès")
        void shouldRemoveRoleSuccessfully() {
            // Given
            when(personRepository.findByIdWithProfileAndRoles(1L))
                    .thenReturn(Optional.of(testPerson));
            when(roleRepository.findByLabel(Role.ROLE_ADMIN))
                    .thenReturn(Optional.of(adminRole));
            when(personRepository.save(any(Person.class))).thenReturn(testPerson);
            when(personMapper.toResponse(any(Person.class)))
                    .thenReturn(PersonResponse.builder().build());

            // When
            personService.removeRole(1L, Role.ROLE_ADMIN);

            // Then
            assertThat(testPerson.getRoles()).doesNotContain(adminRole);
            assertThat(testPerson.getRoles()).contains(studentRole);

            verify(personRepository).save(testPerson);
        }

        @Test
        @DisplayName("Devrait être idempotent (retirer un rôle déjà absent)")
        void shouldBeIdempotentWhenRoleAlreadyRemoved() {
            // Given
            testPerson.removeRole(adminRole);

            when(personRepository.findByIdWithProfileAndRoles(1L))
                    .thenReturn(Optional.of(testPerson));
            when(roleRepository.findByLabel(Role.ROLE_ADMIN))
                    .thenReturn(Optional.of(adminRole));
            when(personRepository.save(any(Person.class))).thenReturn(testPerson);
            when(personMapper.toResponse(any(Person.class)))
                    .thenReturn(PersonResponse.builder().build());

            // When
            personService.removeRole(1L, Role.ROLE_ADMIN);

            // Then
            assertThat(testPerson.getRoles()).doesNotContain(adminRole);

            verify(personRepository).save(testPerson);
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si personne inexistante")
        void shouldThrowResourceNotFoundExceptionWhenPersonNotExists() {
            // Given
            when(personRepository.findByIdWithProfileAndRoles(999L))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> personService.removeRole(999L, Role.ROLE_ADMIN))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Personne");

            verify(roleRepository, never()).findByLabel(anyString());
            verify(personRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si rôle inexistant")
        void shouldThrowResourceNotFoundExceptionWhenRoleNotExists() {
            // Given
            when(personRepository.findByIdWithProfileAndRoles(1L))
                    .thenReturn(Optional.of(testPerson));
            when(roleRepository.findByLabel("ROLE_INVALID"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> personService.removeRole(1L, "ROLE_INVALID"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Rôle");

            verify(personRepository, never()).save(any());
        }
    }
    //endregion
}