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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service d'authentification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final PersonRepository personRepository;
    private final PersonService personService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Authentifie un utilisateur et génère un access token + refresh token.
     *
     * @param request Email et mot de passe
     * @param deviceFingerprint Hash unique du device
     * @return AuthResponse avec tokens et infos utilisateur
     * @throws ResourceNotFoundException Si l'email n'existe pas
     * @throws BadCredentialsException Si le mot de passe est incorrect
     * @throws DisabledException Si le compte n'est pas actif
     */
    public AuthResponse login(AuthRequest request, String deviceFingerprint) {
        log.debug("Tentative de connexion pour : {}", request.getEmail());

        Person person = personRepository.findByEmailWithProfileAndRoles(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Connexion échouée : email '{}' inconnu", request.getEmail());
                    return new ResourceNotFoundException("Personne", "email", request.getEmail());
                });

        if (!passwordEncoder.matches(request.getPassword(), person.getPassword())) {
            log.warn("Connexion échouée : mot de passe incorrect pour '{}'", request.getEmail());
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        if (!person.getStatus().getLabel().equals(PersonStatus.ACTIVE)) {
            log.warn("Connexion bloquée : compte '{}' non actif (statut: {})",
                    request.getEmail(), person.getStatus().getLabel());
            throw new DisabledException("Ce compte n'est pas accessible");
        }

        // Générer les tokens
        String accessToken = jwtService.generateToken(person);
        String refreshToken = refreshTokenService.createRefreshToken(
                person,
                deviceFingerprint,
                null
        );

        String[] roles = person.getRoles().stream()
                .map(Role::getLabel)
                .toArray(String[]::new);

        person.setLastLogin(LocalDateTime.now());
        personRepository.save(person);

        log.info("Connexion réussie : {} (roles: {})", request.getEmail(), String.join(", ", roles));

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .type("Bearer")
                .userId(person.getId())
                .roles(roles)
                .build();
    }

    /**
     * Inscrit un nouvel utilisateur et le connecte automatiquement.
     */
    public AuthResponse register(CreatePersonRequest request, String deviceFingerprint) {
        log.info("Inscription d'un nouvel utilisateur : {}", request.getEmail());
        personService.createPerson(request);

        log.debug("Connexion automatique après inscription : {}", request.getEmail());
        return login(
                new AuthRequest(request.getEmail(), request.getPassword()),
                deviceFingerprint
        );
    }
}