package com.cda.carpooling.security;

import com.cda.carpooling.entity.Person;
import com.cda.carpooling.entity.Role;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Service de gestion des JWT .
 * Utilise HS256 pour signer les tokens.
 */
@Service
public class JwtService {
    private final Clock clock;

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public JwtService(
            @Value("${jwt.secret-key}") String secretKey,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.expiration-ms}") long expirationMs,
            Clock clock) {

        this.secretKey = secretKey;
        this.issuer = issuer;
        this.expirationMs = expirationMs;
        this.clock = clock;

        SecretKey key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");

        this.jwtEncoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(key)
        );

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        JwtTimestampValidator timestampValidator = new JwtTimestampValidator();
        timestampValidator.setClock(this.clock);

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(issuer),
                        timestampValidator
                )
        );

        this.jwtDecoder = decoder;
    }

    /**
     * Génère un JWT pour une personne.
     */
    public String generateToken(Person person) {
        Instant now = clock.instant();
        Instant expiry = now.plusMillis(expirationMs);

        List<String> roles = person.getRoles().stream()
                .map(Role::getLabel)
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(person.getId().toString())
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Valide et décode un JWT.
     */
    public Jwt validateToken(String token) {
        return jwtDecoder.decode(token);
    }
}