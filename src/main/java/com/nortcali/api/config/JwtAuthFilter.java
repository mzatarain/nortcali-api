package com.nortcali.api.config;

import com.nortcali.api.repository.SessionRepository;
import com.nortcali.api.security.JwtUtil;
import com.nortcali.api.service.EmployeeDetailsService;
import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

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

            // Verificar que la sesión existe en DB, está activa y no ha expirado
            var session = sessionRepo.findByTokenAndIsActiveTrue(token);

            boolean sessionValid = session.isPresent();

            if (sessionValid) {
                LocalDateTime expiresAt = session.get().getExpiresAt();
                if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
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
            }
            // Si la sesión es inválida/expirada, no se setea autenticación.
            // Spring Security devuelve 401 para endpoints protegidos y permite
            // el paso a endpoints permitAll (como /logout).
        }

        filterChain.doFilter(request, response);
    }
}