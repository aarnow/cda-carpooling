package com.cda.carpooling.security;

import com.cda.carpooling.entity.Person;
import com.cda.carpooling.entity.Role;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de gestion des JWT .
 * Utilise HS256 pour signer les tokens.
 */
@Service
public class JwtService {

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
            @Value("${jwt.expiration-ms}") long expirationMs) {

        this.secretKey = secretKey;
        this.issuer = issuer;
        this.expirationMs = expirationMs;

        SecretKey key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");

        this.jwtEncoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(key)
        );

        this.jwtDecoder = NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Génère un JWT pour une personne.
     */
    public String generateToken(Person person) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        List<String> roles = person.getRoles().stream()
                .map(Role::getLabel)
                .collect(Collectors.toList());

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