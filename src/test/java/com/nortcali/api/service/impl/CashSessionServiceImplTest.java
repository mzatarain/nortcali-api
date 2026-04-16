package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.OpenCashSessionRequest;
import com.nortcali.api.dto.response.CashSessionResponse;
import com.nortcali.api.entity.CashSession;
import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.Restaurant;
import com.nortcali.api.entity.enums.CashSessionStatus;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.CashSessionMapper;
import com.nortcali.api.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashSessionServiceImplTest {

    @Mock CashSessionRepository sessionRepo;
    @Mock RestaurantRepository restaurantRepo;
    @Mock EmployeeRepository employeeRepo;
    @Mock SaleRepository saleRepo;
    @Mock ExpenseRepository expenseRepo;
    @Mock IncomeRepository incomeRepo;
    @Mock CashSessionMapper mapper;

    @InjectMocks
    CashSessionServiceImpl service;

    // ── helpers ────────────────────────────────────────────────────────────────

    private OpenCashSessionRequest openRequest(BigDecimal amount, Long employeeId) {
        OpenCashSessionRequest req = new OpenCashSessionRequest();
        req.setOpeningAmount(amount);
        req.setOpenedBy(employeeId);
        return req;
    }

    private Restaurant restaurant(Long id) {
        Restaurant r = new Restaurant();
        r.setId(id);
        return r;
    }

    private Employee employee(Long id) {
        Employee e = new Employee();
        e.setUsername("caja01");
        return e;
    }

    // ── open: caso feliz ───────────────────────────────────────────────────────

    @Test
    void open_creaLaSesion_cuandoNoHayNingunaAbierta() {
        Long restaurantId = 1L;
        Long employeeId = 5L;
        BigDecimal opening = new BigDecimal("500.00");

        when(restaurantRepo.findById(restaurantId)).thenReturn(Optional.of(restaurant(restaurantId)));
        when(sessionRepo.existsByRestaurantIdAndStatus(restaurantId, CashSessionStatus.OPEN)).thenReturn(false);
        when(employeeRepo.findById(employeeId)).thenReturn(Optional.of(employee(employeeId)));
        when(restaurantRepo.getReferenceById(restaurantId)).thenReturn(restaurant(restaurantId));

        CashSession saved = new CashSession();
        saved.setStatus(CashSessionStatus.OPEN);
        when(sessionRepo.save(any(CashSession.class))).thenReturn(saved);
        CashSessionResponse expectedResponse = mock(CashSessionResponse.class);
        when(mapper.toResponse(saved)).thenReturn(expectedResponse);

        CashSessionResponse result = service.open(restaurantId, openRequest(opening, employeeId));

        assertThat(result).isEqualTo(expectedResponse);

        // Verificar que se persistió con status OPEN y monto de apertura correcto
        ArgumentCaptor<CashSession> captor = ArgumentCaptor.forClass(CashSession.class);
        verify(sessionRepo).save(captor.capture());
        CashSession persisted = captor.getValue();
        assertThat(persisted.getStatus()).isEqualTo(CashSessionStatus.OPEN);
        assertThat(persisted.getOpeningAmount()).isEqualByComparingTo(opening);
    }

    // ── open: excepción si ya hay sesión abierta ───────────────────────────────

    @Test
    void open_lanzaBusinessRuleException_cuandoYaHayUnaSesionAbierta() {
        Long restaurantId = 1L;

        when(restaurantRepo.findById(restaurantId)).thenReturn(Optional.of(restaurant(restaurantId)));
        when(sessionRepo.existsByRestaurantIdAndStatus(restaurantId, CashSessionStatus.OPEN)).thenReturn(true);

        assertThatThrownBy(() -> service.open(restaurantId, openRequest(new BigDecimal("500.00"), 5L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("sesión de caja abierta");

        verify(sessionRepo, never()).save(any());
    }

    // ── open: restaurante no encontrado ───────────────────────────────────────

    @Test
    void open_lanzaResourceNotFoundException_cuandoRestauranteNoExiste() {
        when(restaurantRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.open(99L, openRequest(new BigDecimal("100.00"), 5L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(sessionRepo, never()).save(any());
    }

    // ── open: empleado no encontrado ──────────────────────────────────────────

    @Test
    void open_lanzaResourceNotFoundException_cuandoEmpleadoNoExiste() {
        Long restaurantId = 1L;

        when(restaurantRepo.findById(restaurantId)).thenReturn(Optional.of(restaurant(restaurantId)));
        when(sessionRepo.existsByRestaurantIdAndStatus(restaurantId, CashSessionStatus.OPEN)).thenReturn(false);
        when(employeeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.open(restaurantId, openRequest(new BigDecimal("100.00"), 99L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(sessionRepo, never()).save(any());
    }
}
