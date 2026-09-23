package com.nortcali.api.controller;

import com.nortcali.api.config.LoginRateLimiter;
import com.nortcali.api.dto.request.ForgotPasswordRequest;
import com.nortcali.api.dto.request.LoginRequest;
import com.nortcali.api.dto.request.RegisterRequest;
import com.nortcali.api.dto.request.ResetPasswordRequest;
import com.nortcali.api.dto.response.EmployeeResponse;
import com.nortcali.api.dto.response.LoginResponse;
import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.Session;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.EmployeeMapper;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.SessionRepository;
import com.nortcali.api.security.JwtUtil;
import com.nortcali.api.service.PasswordResetService;
import com.nortcali.api.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final SessionRepository sessionRepo;
    private final EmployeeRepository employeeRepo;
    private final EmployeeMapper employeeMapper;
    private final LoginRateLimiter loginRateLimiter;
    private final RegistrationService registrationService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthenticationManager authManager,
                          JwtUtil jwtUtil,
                          SessionRepository sessionRepo,
                          EmployeeRepository employeeRepo,
                          EmployeeMapper employeeMapper,
                          LoginRateLimiter loginRateLimiter,
                          RegistrationService registrationService,
                          PasswordResetService passwordResetService) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.sessionRepo = sessionRepo;
        this.employeeRepo = employeeRepo;
        this.employeeMapper = employeeMapper;
        this.loginRateLimiter = loginRateLimiter;
        this.registrationService = registrationService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        if (!loginRateLimiter.tryConsume(httpRequest.getRemoteAddr())) {
            throw new BusinessRuleException("Demasiados intentos de inicio de sesión. Intenta de nuevo en un minuto.");
        }

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        var employee = employeeRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Employee with email: " + request.getEmail()));

        employee.setLastLogin(LocalDateTime.now());
        employeeRepo.save(employee);

        String token = jwtUtil.generateToken(request.getEmail(), resolveTenantId(employee));

        // Registrar sesión activa en tabla sessions, con fecha de expiración alineada al JWT
        Session session = new Session();
        session.setToken(sha256Hex(token));
        session.setEmployee(employee);
        session.setIpAddress(httpRequest.getRemoteAddr());
        session.setActive(true);
        session.setExpiresAt(LocalDateTime.now()
                .plusSeconds(jwtUtil.getExpiration() / 1000));
        sessionRepo.save(session);

        log.info("Login exitoso para '{}'", request.getEmail());
        return ResponseEntity.ok(new LoginResponse(token, employee.getUsername(), employee.getRole()));
    }

    /**
     * Fase 4 (plan SaaS): alta de negocio nuevo — crea tenant + restaurante + admin inicial y
     * responde ya autenticado (mismo shape que /login) para que el frontend continúe directo
     * al checkout de Stripe sin pedir credenciales de nuevo.
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletRequest httpRequest) {
        if (!loginRateLimiter.tryConsume(httpRequest.getRemoteAddr())) {
            throw new BusinessRuleException("Demasiados intentos. Intenta de nuevo en un minuto.");
        }

        Employee employee = registrationService.register(request);
        employee.setLastLogin(LocalDateTime.now());
        employeeRepo.save(employee);

        String token = jwtUtil.generateToken(employee.getEmail(), resolveTenantId(employee));

        Session session = new Session();
        session.setToken(sha256Hex(token));
        session.setEmployee(employee);
        session.setIpAddress(httpRequest.getRemoteAddr());
        session.setActive(true);
        session.setExpiresAt(LocalDateTime.now()
                .plusSeconds(jwtUtil.getExpiration() / 1000));
        sessionRepo.save(session);

        log.info("Registro exitoso: nuevo negocio con admin '{}'", employee.getEmail());
        return ResponseEntity.ok(new LoginResponse(token, employee.getUsername(), employee.getRole()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // Soft-delete: marcar sesión como inactiva
            sessionRepo.findByTokenAndIsActiveTrue(sha256Hex(token)).ifPresent(s -> {
                s.setActive(false);
                sessionRepo.save(s);
                log.info("Sesión invalidada para '{}'", s.getEmployee().getUsername());
            });
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    /** Recuperación de contraseña — siempre 204, exista o no el email (no filtra qué correos están registrados). */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                               HttpServletRequest httpRequest) {
        if (!loginRateLimiter.tryConsume(httpRequest.getRemoteAddr())) {
            throw new BusinessRuleException("Demasiados intentos. Intenta de nuevo en un minuto.");
        }
        passwordResetService.requestReset(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                              HttpServletRequest httpRequest) {
        if (!loginRateLimiter.tryConsume(httpRequest.getRemoteAddr())) {
            throw new BusinessRuleException("Demasiados intentos. Intenta de nuevo en un minuto.");
        }
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest httpRequest) {
        // El JWT fue validado por JwtAuthFilter — el email ya está en el SecurityContext
        // (Fase 2: el principal es el email, ver EmployeeDetailsService)
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        String authHeader = httpRequest.getHeader("Authorization");
        String oldToken = authHeader.substring(7); // "Bearer " ya validado por el filtro

        // Fase 8 (plan SaaS): una sesión de impersonación no se renueva — debe expirar sola en
        // vez de extenderse indefinidamente mientras un superadmin la tenga abierta. El
        // superadmin vuelve a impersonar desde el panel si necesita más tiempo.
        if (jwtUtil.getImpersonatedBy(oldToken) != null) {
            throw new BusinessRuleException("No se puede renovar una sesión de impersonación.");
        }

        // Localizar sesión activa actual (garantizado por el filtro, pero verificamos por coherencia)
        Session oldSession = sessionRepo.findByTokenAndIsActiveTrue(sha256Hex(oldToken))
                .orElseThrow(() -> new ResourceNotFoundException("Sesión activa no encontrada para el token"));

        var employee = employeeRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee with email: " + email));

        // Generar nuevo JWT
        String newToken = jwtUtil.generateToken(email, resolveTenantId(employee));

        // Invalidar sesión anterior (rotación de token)
        oldSession.setActive(false);
        sessionRepo.save(oldSession);

        // Crear nueva sesión con el token renovado
        Session newSession = new Session();
        newSession.setToken(sha256Hex(newToken));
        newSession.setEmployee(employee);
        newSession.setIpAddress(httpRequest.getRemoteAddr());
        newSession.setActive(true);
        newSession.setExpiresAt(LocalDateTime.now()
                .plusSeconds(jwtUtil.getExpiration() / 1000));
        sessionRepo.save(newSession);

        log.info("Token renovado para '{}'", email);
        return ResponseEntity.ok(new LoginResponse(newToken, employee.getUsername(), employee.getRole()));
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> me() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var employee = employeeRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee with email: " + email));
        return ResponseEntity.ok(employeeMapper.toResponse(employee));
    }
    /**
     * Deriva el tenant del empleado a partir de sus restaurantes asignados (todavía no existe
     * un `employee.tenant_id` explícito — Fase 1 del plan SaaS). Con un solo tenant por
     * restaurante y, hoy, un solo tenant en todo el sistema, cualquier restaurante asignado
     * sirve. Si el empleado no tiene ningún restaurante asignado, devuelve null — el JWT queda
     * sin tenantId y RestaurantAccessInterceptor rechazará cualquier acceso con restaurantId en
     * el path (igual que ya rechaza hoy la pertenencia a restaurante).
     */
    private static Long resolveTenantId(Employee employee) {
        return employee.getRestaurant().stream()
                .findFirst()
                .map(r -> r.getTenant().getId())
                .orElse(null);
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
