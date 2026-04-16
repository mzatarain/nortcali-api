package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.InventoryMovementRequest;
import com.nortcali.api.dto.response.InventoryMovementResponse;
import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.InventoryMovement;
import com.nortcali.api.entity.Supply;
import com.nortcali.api.entity.enums.MovementType;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.InventoryMovementMapper;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.InventoryMovementRepository;
import com.nortcali.api.repository.SupplyRepository;
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
class InventoryMovementServiceImplTest {

    @Mock InventoryMovementRepository movementRepo;
    @Mock SupplyRepository supplyRepo;
    @Mock EmployeeRepository employeeRepo;
    @Mock InventoryMovementMapper mapper;

    @InjectMocks
    InventoryMovementServiceImpl service;

    // ── helpers ────────────────────────────────────────────────────────────────

    private Supply supplyWith(BigDecimal current, BigDecimal minimum) {
        Supply s = new Supply();
        s.setId(1L);
        s.setName("Harina");
        s.setActive(true);
        s.setCurrentStock(current);
        s.setMinimumStock(minimum);
        s.setUnitCost(new BigDecimal("10.00"));
        return s;
    }

    private InventoryMovementRequest request(String type, BigDecimal qty) {
        InventoryMovementRequest r = new InventoryMovementRequest();
        r.setMovementType(type);
        r.setQuantity(qty);
        r.setEmployeeId(10L);
        return r;
    }

    private void stubEmployee() {
        Employee emp = new Employee();
        emp.setUsername("admin");
        when(employeeRepo.findById(10L)).thenReturn(Optional.of(emp));
    }

    // ── ENTRADA ────────────────────────────────────────────────────────────────

    @Test
    void register_entrada_aumentaStock() {
        Supply supply = supplyWith(new BigDecimal("5.0"), new BigDecimal("2.0"));
        when(supplyRepo.findById(1L)).thenReturn(Optional.of(supply));
        stubEmployee();
        InventoryMovement saved = new InventoryMovement();
        when(movementRepo.save(any())).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(mock(InventoryMovementResponse.class));

        service.register(1L, request("entrada", new BigDecimal("3.0")));

        // stock = 5 + 3 = 8
        ArgumentCaptor<Supply> captor = ArgumentCaptor.forClass(Supply.class);
        verify(supplyRepo).save(captor.capture());
        assertThat(captor.getValue().getCurrentStock()).isEqualByComparingTo("8.0");
    }

    // ── SALIDA ─────────────────────────────────────────────────────────────────

    @Test
    void register_salida_disminuyeStock() {
        Supply supply = supplyWith(new BigDecimal("10.0"), new BigDecimal("2.0"));
        when(supplyRepo.findById(1L)).thenReturn(Optional.of(supply));
        stubEmployee();
        when(movementRepo.save(any())).thenReturn(new InventoryMovement());
        when(mapper.toResponse(any())).thenReturn(mock(InventoryMovementResponse.class));

        service.register(1L, request("salida", new BigDecimal("4.0")));

        // stock = 10 - 4 = 6
        ArgumentCaptor<Supply> captor = ArgumentCaptor.forClass(Supply.class);
        verify(supplyRepo).save(captor.capture());
        assertThat(captor.getValue().getCurrentStock()).isEqualByComparingTo("6.0");
    }

    @Test
    void register_salida_stockInsuficiente_lanzaBusinessRuleException() {
        Supply supply = supplyWith(new BigDecimal("2.0"), new BigDecimal("1.0"));
        when(supplyRepo.findById(1L)).thenReturn(Optional.of(supply));

        assertThatThrownBy(() -> service.register(1L, request("salida", new BigDecimal("5.0"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Stock insuficiente");

        verify(supplyRepo, never()).save(any());
    }

    // ── MERMA ──────────────────────────────────────────────────────────────────

    @Test
    void register_merma_disminuyeStock() {
        Supply supply = supplyWith(new BigDecimal("8.0"), new BigDecimal("2.0"));
        when(supplyRepo.findById(1L)).thenReturn(Optional.of(supply));
        stubEmployee();
        when(movementRepo.save(any())).thenReturn(new InventoryMovement());
        when(mapper.toResponse(any())).thenReturn(mock(InventoryMovementResponse.class));

        service.register(1L, request("merma", new BigDecimal("3.0")));

        // stock = 8 - 3 = 5
        ArgumentCaptor<Supply> captor = ArgumentCaptor.forClass(Supply.class);
        verify(supplyRepo).save(captor.capture());
        assertThat(captor.getValue().getCurrentStock()).isEqualByComparingTo("5.0");
    }

    // ── AJUSTE ─────────────────────────────────────────────────────────────────

    @Test
    void register_ajuste_reemplazaStock() {
        Supply supply = supplyWith(new BigDecimal("15.0"), new BigDecimal("2.0"));
        when(supplyRepo.findById(1L)).thenReturn(Optional.of(supply));
        stubEmployee();
        when(movementRepo.save(any())).thenReturn(new InventoryMovement());
        when(mapper.toResponse(any())).thenReturn(mock(InventoryMovementResponse.class));

        service.register(1L, request("ajuste", new BigDecimal("7.5")));

        // stock = 7.5 (reemplazado, no sumado/restado)
        ArgumentCaptor<Supply> captor = ArgumentCaptor.forClass(Supply.class);
        verify(supplyRepo).save(captor.capture());
        assertThat(captor.getValue().getCurrentStock()).isEqualByComparingTo("7.5");
    }

    // ── Alerta de stock mínimo ─────────────────────────────────────────────────

    @Test
    void register_salida_stockCaeDebajoDeMínimo_continúaSinExcepción() {
        // minimumStock = 5, currentStock = 6, salida = 3 → nuevo stock = 3 < 5
        Supply supply = supplyWith(new BigDecimal("6.0"), new BigDecimal("5.0"));
        when(supplyRepo.findById(1L)).thenReturn(Optional.of(supply));
        stubEmployee();
        when(movementRepo.save(any())).thenReturn(new InventoryMovement());
        when(mapper.toResponse(any())).thenReturn(mock(InventoryMovementResponse.class));

        // no debe lanzar excepción; solo loguea warn
        service.register(1L, request("salida", new BigDecimal("3.0")));

        ArgumentCaptor<Supply> captor = ArgumentCaptor.forClass(Supply.class);
        verify(supplyRepo).save(captor.capture());
        assertThat(captor.getValue().getCurrentStock()).isEqualByComparingTo("3.0");
    }

    // ── Insumo inactivo ────────────────────────────────────────────────────────

    @Test
    void register_insumoInactivo_lanzaBusinessRuleException() {
        Supply supply = supplyWith(new BigDecimal("10.0"), new BigDecimal("2.0"));
        supply.setActive(false);
        when(supplyRepo.findById(1L)).thenReturn(Optional.of(supply));

        assertThatThrownBy(() -> service.register(1L, request("entrada", new BigDecimal("1.0"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactivo");
    }

    // ── Tipo de movimiento inválido ────────────────────────────────────────────

    @Test
    void register_tipoInvalido_lanzaBusinessRuleException() {
        Supply supply = supplyWith(new BigDecimal("10.0"), new BigDecimal("2.0"));
        when(supplyRepo.findById(1L)).thenReturn(Optional.of(supply));

        assertThatThrownBy(() -> service.register(1L, request("invalido", new BigDecimal("1.0"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Tipo de movimiento inválido");
    }

    // ── Insumo no encontrado ───────────────────────────────────────────────────

    @Test
    void register_insumoNoEncontrado_lanzaResourceNotFoundException() {
        when(supplyRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(99L, request("entrada", new BigDecimal("1.0"))))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
