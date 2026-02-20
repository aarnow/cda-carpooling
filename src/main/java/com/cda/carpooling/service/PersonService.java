package com.cda.carpooling.service;

import com.cda.carpooling.dto.request.CreatePersonProfileRequest;
import com.cda.carpooling.dto.request.CreatePersonRequest;
import com.cda.carpooling.dto.request.UpdatePersonProfileRequest;
import com.cda.carpooling.dto.response.PersonMinimalResponse;
import com.cda.carpooling.dto.response.PersonProfileResponse;
import com.cda.carpooling.dto.response.PersonResponse;
import com.cda.carpooling.entity.Person;
import com.cda.carpooling.entity.PersonProfile;
import com.cda.carpooling.entity.Role;
import com.cda.carpooling.entity.PersonStatus;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.exception.DuplicateResourceException;
import com.cda.carpooling.mapper.PersonMapper;
import com.cda.carpooling.mapper.PersonProfileMapper;
import com.cda.carpooling.repository.RoleRepository;
import com.cda.carpooling.repository.PersonRepository;
import com.cda.carpooling.repository.PersonStatusRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service de gestion des personnes et de leurs profils.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonStatusRepository personStatusRepository;
    private final RoleRepository roleRepository;
    private final PersonMapper personMapper;
    private final PersonProfileMapper personProfileMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Crée un nouvel utilisateur avec statut ACTIVE et rôle STUDENT par défaut.
     */
    @Transactional
    public PersonResponse createPerson(CreatePersonRequest request) {
        if (personRepository.existsByEmail(request.getEmail())) {
            log.warn("Tentative d'inscription avec email existant : {}", request.getEmail());
            throw new DuplicateResourceException("Personne", "email", request.getEmail());
        }

        Person person = personMapper.toEntity(request);
        person.setPassword(passwordEncoder.encode(person.getPassword()));

        PersonStatus defaultStatus = personStatusRepository.findByLabel(PersonStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Le statut par défaut n'existe pas"));
        person.setStatus(defaultStatus);

        Role defaultRole = roleRepository.findByLabel(Role.ROLE_STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Le rôle par défaut n'existe pas"));
        person.addRole(defaultRole);

        Person savedPerson = personRepository.save(person);
        log.info("Utilisateur créé : {} (id={})", savedPerson.getEmail(), savedPerson.getId());
        return personMapper.toResponse(savedPerson);
    }

    /**
     * Crée un profil pour la personne connectée.
     * Un utilisateur ne peut avoir qu'un seul profil.
     *
     * @param personId L'ID de la personne
     * @param request Les données du profil
     * @return PersonProfileResponse
     * @throws DuplicateResourceException Si la personne a déjà un profil
     */
    @Transactional
    public PersonProfileResponse createPersonProfile(Long personId, CreatePersonProfileRequest request) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Personne", "id", personId));

        if (person.getProfile() != null) {
            log.warn("Tentative de création d'un profil existant pour personne {}", personId);
            throw new DuplicateResourceException("Cette personne possède déjà un profil");
        }

        PersonProfile profile = PersonProfile.builder()
                .lastname(request.getLastname())
                .firstname(request.getFirstname())
                .birthday(request.getBirthday())
                .phone(request.getPhone())
                .person(person)
                .build();

        person.setProfile(profile);
        personRepository.save(person);
        log.info("Profil créé pour personne {} : {} {}",
                personId, profile.getFirstname(), profile.getLastname());

        return personProfileMapper.toResponse(profile);
    }

    /**
     * Récupère tous les utilisateurs avec leurs profils et rôles.
     */
    public List<PersonResponse> getAllPersons() {
        return personRepository.findAllWithProfileAndRoles().stream()
                .map(personMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère tous les utilisateurs (version minimale).
     */
    public List<PersonMinimalResponse> getAllPersonsMinimal() {
        return personRepository.findAll().stream()
                .map(personMapper::toMinimalResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un utilisateur par son ID.
     */
    @NonNull
    public PersonResponse getPersonById(Long personId) {
        Person person = personRepository.findByIdWithProfileAndRoles(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Personne", "id", personId));
        return personMapper.toResponse(person);
    }

    /**
     * Met à jour un utilisateur existant.
     */
    @Transactional
    public PersonProfileResponse updatePersonProfile(Long personId, UpdatePersonProfileRequest request) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Personne", "id", personId));

        PersonProfile profile = person.getProfile();
        if (profile == null) {
            throw new ResourceNotFoundException("Cette personne n'a pas de profil");
        }

        if (request.getLastname() != null) {
            profile.setLastname(request.getLastname());
        }
        if (request.getFirstname() != null) {
            profile.setFirstname(request.getFirstname());
        }
        if (request.getBirthday() != null) {
            profile.setBirthday(request.getBirthday());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone());
        }

        personRepository.save(person);
        log.info("Profil mis à jour : personne {}", personId);

        return personProfileMapper.toResponse(profile);
    }

    /**
     * Anonymise toutes les données d'une personne.
     *
     * Conserve :
     * - Les relations pour obligations légales
     * - L'ID pour intégrité référentielle
     *
     * @param personId L'ID de l'utilisateur
     * @return PersonResponse avec données anonymisées
     */
    @Transactional
    public PersonResponse softDeletePerson(Long personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Personne", "id", personId));

        if(person.getStatus().getLabel().equals(PersonStatus.DELETED)) {
            log.warn("Tentative de soft delete sur compte déjà anonymisé : personne {}", personId);
            throw new DuplicateResourceException("Ce compte est déjà anonymisé");
        }

        person.setEmail("deleted-" + UUID.randomUUID() + "@anonymized.local");
        person.setPassword("DELETED");

        if (person.getProfile() != null) {
            person.getProfile().setFirstname("Utilisateur");
            person.getProfile().setLastname("Supprimé");
            person.getProfile().setPhone(null);
            person.getProfile().setBirthday(null);
        }

        person.setDeletedAt(LocalDateTime.now());
        PersonStatus deletedStatus = personStatusRepository.findByLabel(PersonStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Le statut DELETED n'existe pas"));
        person.setStatus(deletedStatus);

        personRepository.save(person);
        log.warn("Soft delete personne {} (email anonymisé : {})", personId, person.getEmail());
        return personMapper.toResponse(person);
    }

    /**
     * Supprime définitivement un utilisateur.
     */
    @Transactional
    public void deletePerson(Long personId) {
        if (!personRepository.existsById(personId)) {
            throw new ResourceNotFoundException("Personne", "id", personId);
        }
        personRepository.deleteById(personId);
        log.warn("SUPPRESSION DÉFINITIVE personne {}", personId);
    }

    /**
     * Assigne un rôle à un utilisateur.
     */
    @Transactional
    public PersonResponse assignRole(Long personId, String roleLabel) {
        Person person = personRepository.findByIdWithProfileAndRoles(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Personne", "id", personId));

        Role role = roleRepository.findByLabel(roleLabel)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle", "label", roleLabel));

        person.addRole(role);
        Person updatedPerson = personRepository.save(person);
        log.info("Rôle {} assigné à personne {}", roleLabel, personId);
        return personMapper.toResponse(updatedPerson);
    }

    /**
     * Retire un rôle d'un utilisateur.
     */
    @Transactional
    public PersonResponse removeRole(Long personId, String roleLabel) {
        Person person = personRepository.findByIdWithProfileAndRoles(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Personne", "id", personId));

        Role role = roleRepository.findByLabel(roleLabel)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle", "label", roleLabel));

        person.removeRole(role);
        Person updatedPerson = personRepository.save(person);
        log.info("Rôle {} retiré à personne {}", roleLabel, personId);
        return personMapper.toResponse(updatedPerson);
    }
}