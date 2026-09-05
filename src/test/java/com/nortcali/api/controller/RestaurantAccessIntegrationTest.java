package com.nortcali.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nortcali.api.dto.request.LoginRequest;
import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.Restaurant;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que un empleado autenticado NO pueda operar sobre un restaurante al que no
 * pertenece con solo cambiar el {restaurantId} de la URL — RestaurantAccessInterceptor.
 *
 * Antes de este fix, cualquier empleado autenticado (con el rol adecuado para el endpoint)
 * podía acceder a datos de cualquier restaurante; solo se validaba el rol y, en algunos
 * services, que el recurso perteneciera al restaurantId de la URL — nunca que el empleado
 * perteneciera a ese restaurantId.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestaurantAccessIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean EmployeeRepository employeeRepo;
    @MockitoBean SessionRepository sessionRepo;

    // USERNAME: campo interno, ya no usado para login (Fase 2). EMAIL es el identificador.
    private static final String USERNAME = "cajero1";
    private static final String EMAIL = "cajero1@nortcali.com";
    private static final String PASSWORD = "Secure#2026";

    private static final Long OWN_RESTAURANT_ID = 1L;
    private static final Long OTHER_RESTAURANT_ID = 2L;

    @BeforeEach
    void setUp() {
        Restaurant ownRestaurant = new Restaurant();
        ownRestaurant.setId(OWN_RESTAURANT_ID);

        Employee employee = new Employee();
        employee.setUsername(USERNAME);
        employee.setEmail(EMAIL);
        employee.setPassword(passwordEncoder.encode(PASSWORD));
        employee.setFirstName("Ana");
        employee.setLastName("Gómez");
        employee.setRole("CASHIER");
        employee.setStatus("ACTIVE");
        employee.setLocked(false);
        employee.setHireDate(LocalDate.of(2024, 1, 15));
        employee.setRestaurant(Set.of(ownRestaurant));

        when(employeeRepo.findByEmail(EMAIL)).thenReturn(Optional.of(employee));
        when(employeeRepo.findByEmailWithRestaurants(EMAIL)).thenReturn(Optional.of(employee));
        when(employeeRepo.findByRestaurantsId(anyLong())).thenReturn(java.util.List.of());
        when(sessionRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Session activeSession = new Session();
        activeSession.setActive(true);
        activeSession.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(24));
        activeSession.setEmployee(employee);
        when(sessionRepo.findByTokenAndIsActiveTrue(any())).thenReturn(Optional.of(activeSession));
    }

    @Test
    void restauranteAjeno_devuelve403() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/v1/restaurants/" + OTHER_RESTAURANT_ID + "/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void restaurantePropio_permiteAcceso() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/v1/restaurants/" + OWN_RESTAURANT_ID + "/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void endpointSinRestaurantId_noSeVeAfectado() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String login() throws Exception {
        LoginRequest body = new LoginRequest();
        body.setEmail(EMAIL);
        body.setPassword(PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }
}
