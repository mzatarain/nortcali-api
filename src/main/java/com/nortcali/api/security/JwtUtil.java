package com.nortcali.api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiration;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration
    ) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiration = expiration;
    }

    public String generateToken(String username, Long tenantId) {
        return Jwts.builder()
                .subject(username)
                .claim("tenantId", tenantId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public String getUsername(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Devuelve el claim {@code tenantId} del token, o {@code null} si el token no lo trae
     * (p.ej. tokens emitidos antes de la Fase 1 del plan SaaS, todavía vigentes hasta que
     * expiren). {@code Claims.get(key, Class)} evita depender de si el deserializador JSON
     * de jjwt guardó el número como Integer o Long.
     */
    public Long getTenantId(String token) {
        Object raw = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("tenantId");
        return raw == null ? null : Long.valueOf(raw.toString());
    }

    /** Devuelve la duración de expiración en milisegundos (para calcular expiresAt al crear sesión). */
    public long getExpiration() {
        return expiration;
    }
}