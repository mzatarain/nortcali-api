package com.nortcali.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nortcali.api.dto.request.LoginRequest;
import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.Session;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de integración para el flujo de autenticación:
 *   POST /api/v1/auth/login → JWT → GET /api/v1/auth/me
 *
 * Usa H2 en memoria (perfil "test") para arrancar el contexto sin MySQL.
 * Solo los repositorios de Auth son mockeados; el resto del stack es real:
 * SecurityConfig, JwtAuthFilter, JwtUtil, BCryptPasswordEncoder.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired PasswordEncoder passwordEncoder;

    // Instanciado directamente: Jackson no siempre está auto-configurado como bean en este perfil
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean EmployeeRepository employeeRepo;
    @MockitoBean SessionRepository sessionRepo;

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "Secure#2026";

    @BeforeEach
    void setUp() {
        Employee employee = new Employee();
        employee.setUsername(USERNAME);
        employee.setPassword(passwordEncoder.encode(PASSWORD));
        employee.setFirstName("Carlos");
        employee.setLastName("Reyes");
        employee.setRole("ADMIN");
        employee.setStatus("ACTIVE");
        employee.setLocked(false);
        employee.setHireDate(LocalDate.of(2024, 1, 15));

        when(employeeRepo.findByUsername(USERNAME)).thenReturn(Optional.of(employee));
        when(sessionRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // El filtro ahora valida la sesión en DB para cada request autenticado
        Session activeSession = new Session();
        activeSession.setActive(true);
        activeSession.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(24));
        activeSession.setEmployee(employee);
        when(sessionRepo.findByTokenAndIsActiveTrue(any())).thenReturn(Optional.of(activeSession));
    }

    // ── Login con credenciales correctas ──────────────────────────────────────

    @Test
    void login_credencialesCorrectas_devuelve200ConToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginBody(USERNAME, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    // ── Flujo completo: login → JWT → /me ─────────────────────────────────────

    @Test
    void flujoCompleto_login_luego_me_devuelveEmpleadoAutenticado() throws Exception {
        // Paso 1: obtener el JWT
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginBody(USERNAME, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();
        assertThat(token).isNotBlank();

        // Paso 2: usar el JWT para llamar a /me
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.firstName").value("Carlos"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    // ── /me sin token → 401 ───────────────────────────────────────────────────

    @Test
    void me_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ── /me con token inválido → 401 ──────────────────────────────────────────

    @Test
    void me_tokenInvalido_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer este.token.esinvalido"))
                .andExpect(status().isUnauthorized());
    }

    // ── Login con contraseña incorrecta → 401 ─────────────────────────────────

    @Test
    void login_contraseñaIncorrecta_devuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginBody(USERNAME, "wrongpassword"))))
                .andExpect(status().isUnauthorized());
    }

    // ── Login con username inexistente → 401 ──────────────────────────────────

    @Test
    void login_usuarioNoExiste_devuelve401() throws Exception {
        when(employeeRepo.findByUsername("noexiste")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginBody("noexiste", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    // ── Login con cuerpo vacío → 400 ──────────────────────────────────────────

    @Test
    void login_camposVacíos_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginBody("", ""))))
                .andExpect(status().isBadRequest());
    }

    // ── Logout invalida la sesión ──────────────────────────────────────────────

    @Test
    void logout_conToken_devuelve204() throws Exception {
        // Primero hacer login para obtener un token válido
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginBody(USERNAME, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();

        // Configurar mock para el logout (busca sesión activa)
        when(sessionRepo.findByTokenAndIsActiveTrue(token)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // ── Renovación de token ────────────────────────────────────────────────────

    @Test
    void refresh_conTokenValido_devuelveNuevoToken() throws Exception {
        // Obtener token inicial
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginBody(USERNAME, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        String oldToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();

        // Llamar al endpoint de renovación
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        String newToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .get("token").asText();
        assertThat(newToken).isNotBlank();
    }

    @Test
    void refresh_nuevoTokenPermiteAccederAMe() throws Exception {
        // Login → refresh → usar el nuevo token en /me
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginBody(USERNAME, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        String oldToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isOk())
                .andReturn();

        String newToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .get("token").asText();

        // El nuevo token debe dar acceso a /me
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME));
    }

    @Test
    void refresh_sinToken_devuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private LoginRequest loginBody(String username, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return req;
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
