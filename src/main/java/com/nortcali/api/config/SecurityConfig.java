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
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/logout").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()

                // ── Solo ADMIN ────────────────────────────────────────────────
                // Catálogos de configuración del sistema — rara vez modificados
                .requestMatchers("/api/v1/employee-roles/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/countries/**", "/api/v1/states/**", "/api/v1/cities/**").hasRole("ADMIN")

                // ── ADMIN o MANAGER ───────────────────────────────────────────
                // Gestión de restaurantes y catálogos operativos
                .requestMatchers("/api/v1/restaurants/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/v1/units/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/v1/sales-sources/**").hasAnyRole("ADMIN", "MANAGER")

                // ── Cualquier usuario autenticado ─────────────────────────────
                // Operaciones del día a día: órdenes, menú, inventario,
                // clientes, delivery, gastos, ingresos, ventas, caja, reportes
                .anyRequest().authenticated()
            )
            .sessionManagement(sess ->
                sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // Devolver 401 para acceso sin autenticar (por defecto Spring Security devuelve 403)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}