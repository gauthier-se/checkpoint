package com.checkpoint.api.services.impl;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.checkpoint.api.services.SteamOpenIdStateService;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class SteamOpenIdStateServiceImpl implements SteamOpenIdStateService {

    private static final String TOKEN_TYPE = "steam_state";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ACTION = "action";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NONCE = "nonce";

    private final SecretKey signingKey;
    private final long ttlMs;

    public SteamOpenIdStateServiceImpl(
            @Value("${jwt.secret}") String secretKey,
            @Value("${steam.openid.state-ttl-ms:600000}") long ttlMs) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.ttlMs = ttlMs;
    }

    @Override
    public String issue(String action, String email) {
        var builder = Jwts.builder()
                .claim(CLAIM_TYPE, TOKEN_TYPE)
                .claim(CLAIM_ACTION, action)
                .claim(CLAIM_NONCE, UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(signingKey);

        if (email != null) {
            builder.claim(CLAIM_EMAIL, email);
        }

        return builder.compact();
    }

    @Override
    public Optional<Claims> verify(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        try {
            io.jsonwebtoken.Claims raw = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TOKEN_TYPE.equals(raw.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }

            String action = raw.get(CLAIM_ACTION, String.class);
            if (action == null) {
                return Optional.empty();
            }

            String email = raw.get(CLAIM_EMAIL, String.class);
            return Optional.of(new Claims(action, email));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
