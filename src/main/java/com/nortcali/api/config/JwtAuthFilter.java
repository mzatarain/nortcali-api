package com.nortcali.api.config;

import com.nortcali.api.repository.SessionRepository;
import com.nortcali.api.security.JwtUtil;
import com.nortcali.api.service.EmployeeDetailsService;
import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final EmployeeDetailsService userDetailsService;
    private final SessionRepository sessionRepo;

    public JwtAuthFilter(JwtUtil jwtUtil,
                         EmployeeDetailsService userDetailsService,
                         SessionRepository sessionRepo) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.sessionRepo = sessionRepo;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException, java.io.IOException {

        final String authHeader = request.getHeader("Authorization");

        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtUtil.getUsername(token);
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        if (username != null &&
            SecurityContextHolder.getContext().getAuthentication() == null) {

            // Fase 8 (plan SaaS): un token de superadmin no tiene fila en `employees` ni en
            // `sessions` — se autentica directo como ROLE_SUPERADMIN a partir del propio JWT,
            // sin tocar EmployeeDetailsService/SessionRepository. Ver JwtUtil.generateSuperadminToken.
            if (jwtUtil.isSuperadminToken(token)) {
                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN"));
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                filterChain.doFilter(request, response);
                return;
            }

            // Verificar que la sesión existe en DB, está activa y no ha expirado
            var session = sessionRepo.findByTokenAndIsActiveTrue(sha256Hex(token));

            boolean sessionValid = session.isPresent();

            if (sessionValid) {
                LocalDateTime expiresAt = session.get().getExpiresAt();
                if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
                    // Sesión expirada — invalidar automáticamente y no autenticar
                    var s = session.get();
                    s.setActive(false);
                    sessionRepo.save(s);
                    sessionValid = false;
                }
            }

            if (sessionValid) {
                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);

                // Fase 1 (plan SaaS): tenantId viaja en el JWT para que RestaurantAccessInterceptor
                // lo verifique sin una consulta extra a Employee. Tokens emitidos antes de este
                // cambio no traen el claim — jwtUtil.getTenantId devuelve null en ese caso.
                request.setAttribute("tenantId", jwtUtil.getTenantId(token));
            }
            // Si la sesión es inválida/expirada, no se setea autenticación.
            // Spring Security devuelve 401 para endpoints protegidos y permite
            // el paso a endpoints permitAll (como /logout).
        }

        filterChain.doFilter(request, response);
    }

    private static String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}