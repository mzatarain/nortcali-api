package com.nortcali.api.service.impl;

import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.PasswordResetToken;
import com.nortcali.api.entity.Session;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.PasswordResetTokenRepository;
import com.nortcali.api.repository.SessionRepository;
import com.nortcali.api.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock EmployeeRepository employeeRepo;
    @Mock PasswordResetTokenRepository tokenRepo;
    @Mock SessionRepository sessionRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailService emailService;

    @InjectMocks
    PasswordResetServiceImpl service;

    @BeforeEach
    void setFrontendUrl() {
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:5173");
    }

    private Employee employee() {
        Employee employee = new Employee();
        employee.setFirstName("Juan");
        employee.setEmail("juan@example.com");
        employee.setPassword("hash-original");
        return employee;
    }

    @Test
    void requestReset_conEmailExistente_generaTokenYEnviaEmail() {
        Employee employee = employee();
        when(employeeRepo.findByEmail("juan@example.com")).thenReturn(Optional.of(employee));
        when(tokenRepo.findByEmployeeIdAndUsedAtIsNull(any())).thenReturn(List.of());

        service.requestReset("juan@example.com");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepo).save(tokenCaptor.capture());
        PasswordResetToken saved = tokenCaptor.getValue();
        assertThat(saved.getEmployee()).isEqualTo(employee);
        assertThat(saved.getTokenHash()).hasSize(64);
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordReset(eq("juan@example.com"), eq("Juan"), linkCaptor.capture());
        assertThat(linkCaptor.getValue()).startsWith("http://localhost:5173/reset-password?token=");
    }

    @Test
    void requestReset_conEmailInexistente_noHaceNadaNiFiltraExistencia() {
        when(employeeRepo.findByEmail("noexiste@example.com")).thenReturn(Optional.empty());

        service.requestReset("noexiste@example.com");

        verify(tokenRepo, never()).save(any());
        verify(emailService, never()).sendPasswordReset(anyString(), anyString(), anyString());
    }

    @Test
    void requestReset_invalidaLinksAnterioresSinUsar() {
        Employee employee = employee();
        when(employeeRepo.findByEmail("juan@example.com")).thenReturn(Optional.of(employee));

        PasswordResetToken oldToken = new PasswordResetToken();
        when(tokenRepo.findByEmployeeIdAndUsedAtIsNull(any())).thenReturn(List.of(oldToken));

        service.requestReset("juan@example.com");

        assertThat(oldToken.getUsedAt()).isNotNull();
    }

    @Test
    void resetPassword_conTokenValido_actualizaPasswordYCierraSesiones() {
        Employee employee = employee();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmployee(employee);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        when(tokenRepo.findByTokenHashAndUsedAtIsNull(anyString())).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("NuevaClave1!")).thenReturn("hash-nuevo");

        Session activeSession = new Session();
        activeSession.setActive(true);
        when(sessionRepo.findByEmployeeIdAndIsActiveTrue(any())).thenReturn(List.of(activeSession));

        service.resetPassword("raw-token", "NuevaClave1!");

        assertThat(employee.getPassword()).isEqualTo("hash-nuevo");
        assertThat(resetToken.getUsedAt()).isNotNull();
        assertThat(activeSession.isActive()).isFalse();
        verify(employeeRepo).save(employee);
        verify(tokenRepo).save(resetToken);
    }

    @Test
    void resetPassword_conTokenInexistente_lanzaBusinessRuleException() {
        when(tokenRepo.findByTokenHashAndUsedAtIsNull(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("raw-token", "NuevaClave1!"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no es válido o ya expiró");

        verify(employeeRepo, never()).save(any());
    }

    @Test
    void resetPassword_conTokenExpirado_lanzaBusinessRuleException() {
        Employee employee = employee();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmployee(employee);
        resetToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(tokenRepo.findByTokenHashAndUsedAtIsNull(anyString())).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> service.resetPassword("raw-token", "NuevaClave1!"))
                .isInstanceOf(BusinessRuleException.class);

        verify(employeeRepo, never()).save(any());
    }
}
