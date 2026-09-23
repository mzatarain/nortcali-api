package com.nortcali.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth

                // ── Endpoints públicos ────────────────────────────────────────
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/logout", "/api/v1/auth/register",
                        "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                // Fase 8 (plan SaaS): panel de superadmin — login público (valida contra la
                // tabla separada `superadmins`), el resto de /superadmin/** exige ROLE_SUPERADMIN.
                // Debe declararse antes de cualquier matcher genérico de abajo.
                .requestMatchers(HttpMethod.POST, "/api/v1/superadmin/auth/login").permitAll()
                .requestMatchers("/api/v1/superadmin/**").hasRole("SUPERADMIN")
                // Fase 3 (plan SaaS): Stripe no manda JWT — la request se valida por firma
                // (Stripe-Signature) dentro de BillingServiceImpl.handleWebhookEvent, no por sesión
                .requestMatchers(HttpMethod.POST, "/api/v1/billing/webhook").permitAll()
                // Fase 4 (plan SaaS): el formulario de registro necesita listar país/estado/
                // ciudad sin sesión — datos de referencia, no sensibles. Debe ir antes del
                // hasRole("ADMIN") de abajo (primer matcher que aplica gana). Escrituras
                // (POST/PUT/DELETE) siguen restringidas a ADMIN.
                .requestMatchers(HttpMethod.GET, "/api/v1/countries/**", "/api/v1/states/**", "/api/v1/cities/**").permitAll()

                // ── Solo ADMIN ────────────────────────────────────────────────
                // Catálogos de configuración del sistema — rara vez modificados
                .requestMatchers("/api/v1/employee-roles/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/countries/**", "/api/v1/states/**", "/api/v1/cities/**").hasRole("ADMIN")

                // ── ADMIN o MANAGER ───────────────────────────────────────────
                // Solo el CRUD directo del restaurante; los sub-recursos (/orders,
                // /menu, /cash-sessions, etc.) caen en anyRequest().authenticated()
                .requestMatchers("/api/v1/restaurants", "/api/v1/restaurants/{id}").hasAnyRole("ADMIN", "MANAGER", "TENANT_ADMIN")
                .requestMatchers("/api/v1/units/**").hasAnyRole("ADMIN", "MANAGER", "TENANT_ADMIN")
                .requestMatchers("/api/v1/sales-sources/**").hasAnyRole("ADMIN", "MANAGER", "TENANT_ADMIN")

                // ── Cualquier usuario autenticado ─────────────────────────────
                // Operaciones del día a día: órdenes, menú, inventario,
                // clientes, delivery, gastos, ingresos, ventas, caja, reportes
                .anyRequest().authenticated()
            )
            .sessionManagement(sess ->
                sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write(jsonError(401, "Unauthorized", "Autenticación requerida"));
                })
                .accessDeniedHandler((request, response, denied) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write(jsonError(403, "Forbidden", "No tienes permiso para realizar esta acción"));
                })
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static String jsonError(int status, String error, String message) {
        return "{\"status\":" + status
                + ",\"error\":\"" + error + "\""
                + ",\"message\":\"" + message + "\""
                + ",\"timestamp\":\"" + Instant.now() + "\"}";
    }
}