package com.cda.carpooling.security;

import com.cda.carpooling.dto.request.CreatePersonRequest;
import com.cda.carpooling.dto.request.AuthRequest;
import com.cda.carpooling.dto.response.AuthResponse;
import com.cda.carpooling.entity.Person;
import com.cda.carpooling.entity.PersonStatus;
import com.cda.carpooling.entity.Role;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.repository.PersonRepository;
import com.cda.carpooling.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service d'authentification (login, register).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final PersonRepository personRepository;
    private final PersonService personService;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Connexion d'un utilisateur.
     */
    public AuthResponse login(AuthRequest request) {
        Person person = personRepository.findByEmailWithProfileAndRoles(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Personne", "email", request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), person.getPassword())) {
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        if(!person.getStatus().getLabel().equals(PersonStatus.ACTIVE)){
           throw new DisabledException("Ce compte n'est pas accessible");
        }

        String token = jwtService.generateToken(person);

        String[] roles = person.getRoles().stream()
                .map(Role::getLabel)
                .toArray(String[]::new);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(person.getId())
                .roles(roles)
                .build();
    }

    /**
     * Inscription d'un nouvel utilisateur.
     */
    public AuthResponse register(CreatePersonRequest request) {
        personService.createPerson(request);

        return login(new AuthRequest(request.getEmail(), request.getPassword()));
    }
}