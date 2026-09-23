package com.nortcali.api.service.impl;

import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.PasswordResetToken;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.PasswordResetTokenRepository;
import com.nortcali.api.repository.SessionRepository;
import com.nortcali.api.service.EmailService;
import com.nortcali.api.service.PasswordResetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@Transactional
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Duration TOKEN_TTL = Duration.ofHours(1);

    private final EmployeeRepository employeeRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final SessionRepository sessionRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public PasswordResetServiceImpl(EmployeeRepository employeeRepo,
                                     PasswordResetTokenRepository tokenRepo,
                                     SessionRepository sessionRepo,
                                     PasswordEncoder passwordEncoder,
                                     EmailService emailService) {
        this.employeeRepo = employeeRepo;
        this.tokenRepo = tokenRepo;
        this.sessionRepo = sessionRepo;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    public void requestReset(String email) {
        employeeRepo.findByEmail(email).ifPresent(employee -> {
            // Invalida cualquier link anterior sin usar — solo el más reciente debe funcionar
            tokenRepo.findByEmployeeIdAndUsedAtIsNull(employee.getId())
                    .forEach(t -> t.setUsedAt(LocalDateTime.now()));

            String rawToken = generateRawToken();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setEmployee(employee);
            resetToken.setTokenHash(sha256Hex(rawToken));
            resetToken.setExpiresAt(LocalDateTime.now().plus(TOKEN_TTL));
            tokenRepo.save(resetToken);

            String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
            emailService.sendPasswordReset(employee.getEmail(), employee.getFirstName(), resetLink);
            log.info("Link de recuperación generado para '{}'", employee.getEmail());
        });
        // Si el email no existe, no se hace nada — la respuesta al llamador es la misma en
        // ambos casos (ver AuthController), para no filtrar qué correos están registrados.
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepo.findByTokenHashAndUsedAtIsNull(sha256Hex(token))
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BusinessRuleException("El enlace de recuperación no es válido o ya expiró."));

        Employee employee = resetToken.getEmployee();
        employee.setPassword(passwordEncoder.encode(newPassword));
        employeeRepo.save(employee);

        resetToken.setUsedAt(LocalDateTime.now());
        tokenRepo.save(resetToken);

        // La contraseña anterior pudo estar comprometida — cerrar toda sesión abierta con ella
        sessionRepo.findByEmployeeIdAndIsActiveTrue(employee.getId())
                .forEach(s -> s.setActive(false));

        log.info("Contraseña restablecida para '{}'", employee.getEmail());
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
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
