package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.SaleItemRequest;
import com.nortcali.api.dto.request.SaleRequest;
import com.nortcali.api.dto.response.SaleResponse;
import com.nortcali.api.entity.*;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.SaleMapper;
import com.nortcali.api.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceImplTest {

    @Mock SaleRepository saleRepo;
    @Mock SalesSourceRepository sourceRepo;
    @Mock RestaurantRepository restaurantRepo;
    @Mock EmployeeRepository employeeRepo;
    @Mock MenuItemRepository menuItemRepo;
    @Mock MenuItemVariantRepository variantRepo;
    @Mock SaleMapper mapper;

    @InjectMocks
    SaleServiceImpl service;

    // ── helpers ────────────────────────────────────────────────────────────────

    private Restaurant restaurant(Long id) {
        Restaurant r = new Restaurant();
        r.setId(id);
        return r;
    }

    private SalesSource source(Long id, BigDecimal commissionPct) {
        SalesSource s = new SalesSource();
        s.setId(id);
        s.setName("Rappi");
        s.setCommissionPct(commissionPct);
        return s;
    }

    private Employee employee(Long id) {
        Employee e = new Employee();
        e.setUsername("cajero");
        return e;
    }

    private MenuItem menuItem(Long id) {
        MenuItem m = new MenuItem();
        m.setId(id);
        m.setName("Hamburguesa");
        return m;
    }

    private SaleItemRequest saleItem(Long menuItemId, BigDecimal subtotal) {
        SaleItemRequest item = new SaleItemRequest();
        item.setMenuItemId(menuItemId);
        item.setQuantity(1);
        item.setSubtotal(subtotal);
        return item;
    }

    private SaleRequest saleRequest(Long sourceId, Long employeeId, List<SaleItemRequest> items) {
        SaleRequest req = new SaleRequest();
        req.setSourceId(sourceId);
        req.setEmployeeId(employeeId);
        req.setSaleDate(LocalDate.now());
        req.setItems(items);
        return req;
    }

    // ── Cálculo de comisión ────────────────────────────────────────────────────

    @Test
    void create_calculaComisiónConHalfUp() {
        // total = 100 + 50 = 150, commissionPct = 15% → commission = 150 * 15 / 100 = 22.50
        Long restaurantId = 1L;
        Long sourceId = 2L;
        Long employeeId = 3L;

        when(restaurantRepo.findById(restaurantId)).thenReturn(Optional.of(restaurant(restaurantId)));
        when(sourceRepo.findById(sourceId)).thenReturn(Optional.of(source(sourceId, new BigDecimal("15.00"))));
        when(employeeRepo.findById(employeeId)).thenReturn(Optional.of(employee(employeeId)));
        when(menuItemRepo.findById(10L)).thenReturn(Optional.of(menuItem(10L)));
        when(menuItemRepo.findById(11L)).thenReturn(Optional.of(menuItem(11L)));
        when(restaurantRepo.getReferenceById(restaurantId)).thenReturn(restaurant(restaurantId));

        Sale savedSale = new Sale();
        when(saleRepo.save(any(Sale.class))).thenReturn(savedSale);
        when(mapper.toResponse(savedSale)).thenReturn(mock(SaleResponse.class));

        SaleRequest request = saleRequest(sourceId, employeeId,
                List.of(
                        saleItem(10L, new BigDecimal("100.00")),
                        saleItem(11L, new BigDecimal("50.00"))
                ));

        service.create(restaurantId, request);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(saleRepo).save(captor.capture());
        Sale persisted = captor.getValue();

        assertThat(persisted.getTotal()).isEqualByComparingTo("150.00");
        assertThat(persisted.getCommission()).isEqualByComparingTo("22.50");
    }

    @Test
    void create_comisiónCero_cuandoFuenteNoTieneComisión() {
        Long restaurantId = 1L;
        Long sourceId = 2L;
        Long employeeId = 3L;

        when(restaurantRepo.findById(restaurantId)).thenReturn(Optional.of(restaurant(restaurantId)));
        when(sourceRepo.findById(sourceId)).thenReturn(Optional.of(source(sourceId, BigDecimal.ZERO)));
        when(employeeRepo.findById(employeeId)).thenReturn(Optional.of(employee(employeeId)));
        when(menuItemRepo.findById(10L)).thenReturn(Optional.of(menuItem(10L)));
        when(restaurantRepo.getReferenceById(restaurantId)).thenReturn(restaurant(restaurantId));

        Sale savedSale = new Sale();
        when(saleRepo.save(any(Sale.class))).thenReturn(savedSale);
        when(mapper.toResponse(savedSale)).thenReturn(mock(SaleResponse.class));

        service.create(restaurantId, saleRequest(sourceId, employeeId,
                List.of(saleItem(10L, new BigDecimal("200.00")))));

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(saleRepo).save(captor.capture());
        assertThat(captor.getValue().getCommission()).isEqualByComparingTo("0.00");
    }

    @Test
    void create_comisiónRedondeoHalfUp() {
        // total = 100, commission_pct = 10.005% → 10.005 → HALF_UP → 10.01
        Long restaurantId = 1L;
        Long sourceId = 2L;
        Long employeeId = 3L;

        when(restaurantRepo.findById(restaurantId)).thenReturn(Optional.of(restaurant(restaurantId)));
        when(sourceRepo.findById(sourceId)).thenReturn(Optional.of(source(sourceId, new BigDecimal("10.005"))));
        when(employeeRepo.findById(employeeId)).thenReturn(Optional.of(employee(employeeId)));
        when(menuItemRepo.findById(10L)).thenReturn(Optional.of(menuItem(10L)));
        when(restaurantRepo.getReferenceById(restaurantId)).thenReturn(restaurant(restaurantId));

        Sale savedSale = new Sale();
        when(saleRepo.save(any(Sale.class))).thenReturn(savedSale);
        when(mapper.toResponse(savedSale)).thenReturn(mock(SaleResponse.class));

        service.create(restaurantId, saleRequest(sourceId, employeeId,
                List.of(saleItem(10L, new BigDecimal("100.00")))));

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(saleRepo).save(captor.capture());
        // 100 * 10.005 / 100 = 10.005 → HALF_UP a 2 decimales = 10.01
        assertThat(captor.getValue().getCommission()).isEqualByComparingTo("10.01");
    }

    // ── Fuente no encontrada ───────────────────────────────────────────────────

    @Test
    void create_fuenteNoEncontrada_lanzaResourceNotFoundException() {
        Long restaurantId = 1L;
        when(restaurantRepo.findById(restaurantId)).thenReturn(Optional.of(restaurant(restaurantId)));
        when(sourceRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(restaurantId, saleRequest(99L, 1L, List.of())))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(saleRepo, never()).save(any());
    }

    // ── Restaurante no encontrado ──────────────────────────────────────────────

    @Test
    void create_restauranteNoEncontrado_lanzaResourceNotFoundException() {
        when(restaurantRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(99L, saleRequest(1L, 1L, List.of())))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(saleRepo, never()).save(any());
    }
}
