package com.nortcali.api.controller;

import com.nortcali.api.config.LoginRateLimiter;
import com.nortcali.api.dto.request.LoginRequest;
import com.nortcali.api.dto.response.EmployeeResponse;
import com.nortcali.api.dto.response.LoginResponse;
import com.nortcali.api.entity.Session;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.EmployeeMapper;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.SessionRepository;
import com.nortcali.api.security.JwtUtil;
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

    public AuthController(AuthenticationManager authManager,
                          JwtUtil jwtUtil,
                          SessionRepository sessionRepo,
                          EmployeeRepository employeeRepo,
                          EmployeeMapper employeeMapper,
                          LoginRateLimiter loginRateLimiter) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.sessionRepo = sessionRepo;
        this.employeeRepo = employeeRepo;
        this.employeeMapper = employeeMapper;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        if (!loginRateLimiter.tryConsume(httpRequest.getRemoteAddr())) {
            throw new BusinessRuleException("Demasiados intentos de inicio de sesión. Intenta de nuevo en un minuto.");
        }

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        var employee = employeeRepo.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Employee with username: " + request.getUsername()));

        String token = jwtUtil.generateToken(request.getUsername());

        // Registrar sesión activa en tabla sessions, con fecha de expiración alineada al JWT
        Session session = new Session();
        session.setToken(sha256Hex(token));
        session.setEmployee(employee);
        session.setIpAddress(httpRequest.getRemoteAddr());
        session.setActive(true);
        session.setExpiresAt(LocalDateTime.now()
                .plusSeconds(jwtUtil.getExpiration() / 1000));
        sessionRepo.save(session);

        log.info("Login exitoso para usuario '{}'", request.getUsername());
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

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest httpRequest) {
        // El JWT fue validado por JwtAuthFilter — el username ya está en el SecurityContext
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        String authHeader = httpRequest.getHeader("Authorization");
        String oldToken = authHeader.substring(7); // "Bearer " ya validado por el filtro

        // Localizar sesión activa actual (garantizado por el filtro, pero verificamos por coherencia)
        Session oldSession = sessionRepo.findByTokenAndIsActiveTrue(sha256Hex(oldToken))
                .orElseThrow(() -> new ResourceNotFoundException("Sesión activa no encontrada para el token"));

        var employee = employeeRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee with username: " + username));

        // Generar nuevo JWT
        String newToken = jwtUtil.generateToken(username);

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

        log.info("Token renovado para usuario '{}'", username);
        return ResponseEntity.ok(new LoginResponse(newToken, employee.getUsername(), employee.getRole()));
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var employee = employeeRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee with username: " + username));
        return ResponseEntity.ok(employeeMapper.toResponse(employee));
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
