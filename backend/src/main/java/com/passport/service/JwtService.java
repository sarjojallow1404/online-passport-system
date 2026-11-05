package com.passport.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.passport.entity.User;

@Service
public class JwtService {

    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(15);

    private final JwtEncoder jwtEncoder;

    public JwtService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String createToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("passport-backend")
            .issuedAt(now)
            .expiresAt(now.plus(TOKEN_LIFETIME))
            .subject(user.getEmail())
            .claim("uid", user.getId())
            .claim("role", user.getRole())
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(
            JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    public long getLifetimeSeconds() {
        return TOKEN_LIFETIME.toSeconds();
    }
}
