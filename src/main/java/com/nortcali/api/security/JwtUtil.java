package com.nortcali.api.security;

import io.jsonwebtoken.Claims;
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
        return parseClaims(token).getSubject();
    }

    /**
     * Devuelve el claim {@code tenantId} del token, o {@code null} si el token no lo trae
     * (p.ej. tokens emitidos antes de la Fase 1 del plan SaaS, todavía vigentes hasta que
     * expiren, o un token de superadmin — que no lleva tenant). {@code Claims.get(key, Class)}
     * evita depender de si el deserializador JSON de jjwt guardó el número como Integer o Long.
     */
    public Long getTenantId(String token) {
        Object raw = parseClaims(token).get("tenantId");
        return raw == null ? null : Long.valueOf(raw.toString());
    }

    /**
     * Fase 8 (plan SaaS): token de sesión de superadmin — sin {@code tenantId}, con
     * {@code type=superadmin} para que {@code JwtAuthFilter} lo autentique como
     * {@code ROLE_SUPERADMIN} sin pasar por {@code EmployeeDetailsService}/`sessions`.
     */
    public String generateSuperadminToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claim("type", "superadmin")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public boolean isSuperadminToken(String token) {
        return "superadmin".equals(parseClaims(token).get("type"));
    }

    /**
     * Fase 8 (plan SaaS): token de impersonación — mismo shape que un login normal de empleado
     * (subject=email, tenantId), más el claim {@code impersonatedBy} (email del superadmin) que
     * {@code AuthController.refresh} usa para bloquear la renovación de estas sesiones — la
     * impersonación debe expirar sola, no extenderse indefinidamente.
     */
    public String generateImpersonationToken(String employeeEmail, Long tenantId, String impersonatedBySuperadminEmail) {
        return Jwts.builder()
                .subject(employeeEmail)
                .claim("tenantId", tenantId)
                .claim("impersonatedBy", impersonatedBySuperadminEmail)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    /** Devuelve el email del superadmin que originó esta sesión, o {@code null} si no es una impersonación. */
    public String getImpersonatedBy(String token) {
        Object raw = parseClaims(token).get("impersonatedBy");
        return raw == null ? null : raw.toString();
    }

    /** Devuelve la duración de expiración en milisegundos (para calcular expiresAt al crear sesión). */
    public long getExpiration() {
        return expiration;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}