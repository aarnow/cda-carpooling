package com.cda.carpooling.service;

import com.cda.carpooling.dto.request.CreatePersonRequest;
import com.cda.carpooling.dto.request.UpdatePersonRequest;
import com.cda.carpooling.dto.response.PersonMinimalResponse;
import com.cda.carpooling.dto.response.PersonResponse;
import com.cda.carpooling.entity.Person;
import com.cda.carpooling.entity.Role;
import com.cda.carpooling.entity.PersonStatus;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.exception.DuplicateResourceException;
import com.cda.carpooling.mapper.PersonMapper;
import com.cda.carpooling.repository.RoleRepository;
import com.cda.carpooling.repository.PersonRepository;
import com.cda.carpooling.repository.PersonStatusRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonStatusRepository personStatusRepository;
    private final RoleRepository roleRepository;
    private final PersonMapper personMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Crée un nouvel utilisateur.
     */
    @Transactional
    public PersonResponse createPerson(CreatePersonRequest request) {
        if (personRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Utilisateur", "email", request.getEmail());
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
        return personMapper.toResponse(savedPerson);
    }

    /**
     * Récupère tous les utilisateurs.
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
    public PersonResponse getPersonById(Long id) {
        Person person = personRepository.findByIdWithProfileAndRoles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
        return personMapper.toResponse(person);
    }

    /**
     * Récupère un utilisateur par son email.
     */
    @NonNull
    public PersonResponse getPersonByEmail(String email) {
        Person person = personRepository.findByEmailWithProfileAndRoles(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "email", email));
        return personMapper.toResponse(person);
    }

    /**
     * Met à jour un utilisateur existant.
     */
    @Transactional
    public PersonResponse updatePerson(Long id, UpdatePersonRequest request) {
        Person person = personRepository.findByIdWithProfileAndRoles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        if (request.getEmail() != null && !request.getEmail().equals(person.getEmail())) {
            if (personRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Utilisateur", "email", request.getEmail());
            }
        }

        if (request.getPassword() != null) {
            request.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        personMapper.updateEntity(person, request);
        Person updatedPerson = personRepository.save(person);
        return personMapper.toResponse(updatedPerson);
    }

    /**
     * Supprime un utilisateur.
     *
     * Anonymise toutes les données personnelles.
     *
     * Conserve :
     * - Les relations (réservations, trajets) pour obligations légales
     * - L'ID pour intégrité référentielle
     *
     * @param id L'ID de l'utilisateur
     */
    @Transactional
    public void deletePerson(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        anonymizePersonalData(person);
        person.setDeletedAt(LocalDateTime.now());
        PersonStatus deletedStatus = personStatusRepository.findByLabel(PersonStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Le statut DELETED n'existe pas"));
        person.setStatus(deletedStatus);

        personRepository.save(person);
    }

    /**
     * Anonymise les données personnelles d'un utilisateur.
     * Appelé lors d'une demande de soft delete.
     */
    private void anonymizePersonalData(Person person) {
        person.setEmail("deleted-" + UUID.randomUUID() + "@anonymized.local");
        person.setPassword("DELETED");

        if (person.getProfile() != null) {
            person.getProfile().setFirstname("Utilisateur");
            person.getProfile().setLastname("Supprimé");
            person.getProfile().setPhone(null);
            person.getProfile().setBirthday(null);
            person.getProfile().setAvatarUrl(null);
        }
    }

    /**
     * Supprime définitivement un utilisateur.
     */
    @Transactional
    public void hardDeletePerson(Long id) {
        if (!personRepository.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur", "id", id);
        }
        personRepository.deleteById(id);
    }

    /**
     * Assigne un rôle à un utilisateur.
     */
    @Transactional
    public PersonResponse assignRole(Long personId, String roleLabel) {
        Person person = personRepository.findByIdWithProfileAndRoles(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", personId));

        Role role = roleRepository.findByLabel(roleLabel)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle", "label", roleLabel));

        person.addRole(role);
        Person updatedPerson = personRepository.save(person);
        return personMapper.toResponse(updatedPerson);
    }

    /**
     * Retire un rôle d'un utilisateur.
     */
    @Transactional
    public PersonResponse removeRole(Long personId, String roleLabel) {
        Person person = personRepository.findByIdWithProfileAndRoles(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", personId));

        Role role = roleRepository.findByLabel(roleLabel)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle", "label", roleLabel));

        person.removeRole(role);
        Person updatedPerson = personRepository.save(person);
        return personMapper.toResponse(updatedPerson);
    }
}