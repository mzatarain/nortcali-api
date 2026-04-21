package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.InventoryMovementRequest;
import com.nortcali.api.dto.request.OrderItemRequest;
import com.nortcali.api.dto.request.OrderRequest;
import com.nortcali.api.dto.request.OrderStatusUpdateRequest;
import com.nortcali.api.dto.response.OrderResponse;
import com.nortcali.api.entity.*;
import com.nortcali.api.entity.enums.OrderStatus;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.OrderMapper;
import com.nortcali.api.repository.*;
import com.nortcali.api.service.InventoryMovementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock OrderRepository orderRepo;
    @Mock OrderStatusHistoryRepository historyRepo;
    @Mock PaymentRepository paymentRepo;
    @Mock RestaurantRepository restaurantRepo;
    @Mock EmployeeRepository employeeRepo;
    @Mock CustomerRepository customerRepo;
    @Mock DeliveryDriverRepository driverRepo;
    @Mock MenuItemRepository menuItemRepo;
    @Mock MenuItemVariantRepository variantRepo;
    @Mock RecipeRepository recipeRepo;
    @Mock InventoryMovementService inventoryService;
    @Mock OrderMapper mapper;

    @InjectMocks
    OrderServiceImpl service;

    // ── helpers ────────────────────────────────────────────────────────────────

    private Restaurant restaurant(Long id) {
        Restaurant r = new Restaurant();
        r.setId(id);
        return r;
    }

    private Employee employee(Long id) {
        Employee e = new Employee();
        e.setUsername("mesero01");
        return e;
    }

    private MenuItem menuItem(Long id, String name) {
        MenuItem m = new MenuItem();
        m.setId(id);
        m.setName(name);
        return m;
    }

    private OrderItemRequest itemRequest(Long menuItemId, int qty, BigDecimal price) {
        OrderItemRequest r = new OrderItemRequest();
        r.setMenuItemId(menuItemId);
        r.setQuantity(qty);
        r.setUnitPrice(price);
        return r;
    }

    private OrderStatusUpdateRequest statusRequest(String toStatus, Long employeeId) {
        OrderStatusUpdateRequest r = new OrderStatusUpdateRequest();
        r.setToStatus(toStatus);
        r.setEmployeeId(employeeId);
        return r;
    }

    /**
     * Construye un Order ya persistido con status dado y un item.
     * Útil para tests de updateStatus.
     */
    private Order orderWithStatus(Long orderId, OrderStatus status) {
        MenuItem item = menuItem(10L, "Tacos");
        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setMenuItem(item);
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(new BigDecimal("50.00"));
        orderItem.setSubtotal(new BigDecimal("100.00"));

        Order order = new Order();
        order.setId(orderId);
        order.setFolio("ORD-1-20260415-0001");
        order.setStatus(status);
        order.setEmployee(employee(5L));
        order.getItems().add(orderItem);
        return order;
    }

    // ── create: folio, total y primer historial ────────────────────────────────

    @Test
    void create_generaFolio_calculaTotal_yRegistraPrimerHistorial() {
        Long restaurantId = 1L;
        Long employeeId = 5L;
        Long menuItemId = 10L;
        BigDecimal price = new BigDecimal("80.00");

        when(restaurantRepo.findByIdWithLock(restaurantId)).thenReturn(Optional.of(restaurant(restaurantId)));
        when(employeeRepo.findById(employeeId)).thenReturn(Optional.of(employee(employeeId)));
        when(menuItemRepo.findById(menuItemId)).thenReturn(Optional.of(menuItem(menuItemId, "Burrito")));
        when(orderRepo.countByFolioPrefix(eq(restaurantId), anyString())).thenReturn(0L);

        // El save devuelve la misma orden que le entra (con id simulado)
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(100L);
            return o;
        });
        when(historyRepo.save(any())).thenReturn(new OrderStatusHistory());
        when(mapper.toResponse(any(Order.class))).thenReturn(mock(OrderResponse.class));

        OrderRequest request = new OrderRequest();
        request.setOrderType("dine_in");
        request.setSource("pos");
        request.setEmployeeId(employeeId);
        request.setItems(List.of(itemRequest(menuItemId, 2, price)));

        service.create(restaurantId, request);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepo).save(orderCaptor.capture());
        Order persisted = orderCaptor.getValue();

        // total = 80 * 2 = 160
        assertThat(persisted.getTotal()).isEqualByComparingTo("160.00");
        // folio debe tener el formato ORD-1-{fecha}-0001 (seq = 0 + 1)
        assertThat(persisted.getFolio()).startsWith("ORD-1-").endsWith("-0001");
        // status inicial = CONFIRMED (las órdenes nuevas nacen confirmadas)
        assertThat(persisted.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // Verifica que se guardó el historial con fromStatus = null, toStatus = CONFIRMED
        ArgumentCaptor<OrderStatusHistory> histCaptor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(historyRepo).save(histCaptor.capture());
        assertThat(histCaptor.getValue().getFromStatus()).isNull();
        assertThat(histCaptor.getValue().getToStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void create_variosItems_sumaTotalCorrectamente() {
        Long restaurantId = 1L;
        Long employeeId = 5L;

        when(restaurantRepo.findByIdWithLock(restaurantId)).thenReturn(Optional.of(restaurant(restaurantId)));
        when(employeeRepo.findById(employeeId)).thenReturn(Optional.of(employee(employeeId)));
        when(menuItemRepo.findById(10L)).thenReturn(Optional.of(menuItem(10L, "Tacos")));
        when(menuItemRepo.findById(11L)).thenReturn(Optional.of(menuItem(11L, "Refresco")));
        when(orderRepo.countByFolioPrefix(eq(restaurantId), anyString())).thenReturn(3L);
        when(orderRepo.save(any(Order.class))).thenAnswer(inv -> { Order o = inv.getArgument(0); o.setId(1L); return o; });
        when(historyRepo.save(any())).thenReturn(new OrderStatusHistory());
        when(mapper.toResponse(any(Order.class))).thenReturn(mock(OrderResponse.class));

        OrderRequest request = new OrderRequest();
        request.setOrderType("takeout");
        request.setSource("whatsapp");
        request.setEmployeeId(employeeId);
        request.setItems(List.of(
                itemRequest(10L, 3, new BigDecimal("30.00")),  // 90
                itemRequest(11L, 2, new BigDecimal("20.00"))   // 40
        ));

        service.create(restaurantId, request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepo).save(captor.capture());
        // total = 90 + 40 = 130
        assertThat(captor.getValue().getTotal()).isEqualByComparingTo("130.00");
        // folio: seq = 3 + 1 = 4 → termina en -0004
        assertThat(captor.getValue().getFolio()).endsWith("-0004");
    }

    @Test
    void create_restauranteNoEncontrado_lanzaResourceNotFoundException() {
        when(restaurantRepo.findByIdWithLock(99L)).thenReturn(Optional.empty());

        OrderRequest req = new OrderRequest();
        req.setOrderType("dine_in");
        req.setSource("pos");
        req.setEmployeeId(1L);
        req.setItems(List.of());

        assertThatThrownBy(() -> service.create(99L, req))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepo, never()).save(any());
    }

    // ── updateStatus: transición válida ───────────────────────────────────────

    @Test
    void updateStatus_transiciónVálida_actualizaEstadoYGuardaHistorial() {
        Long orderId = 1L;
        Order order = orderWithStatus(orderId, OrderStatus.PENDING);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
        when(employeeRepo.findById(5L)).thenReturn(Optional.of(employee(5L)));
        when(orderRepo.save(any())).thenReturn(order);
        when(historyRepo.save(any())).thenReturn(new OrderStatusHistory());
        when(mapper.toResponse(any())).thenReturn(mock(OrderResponse.class));
        // Sin recetas: el descuento de inventario no ocurre para CONFIRMED en este test
        // pero aquí transitamos a CONFIRMED
        when(recipeRepo.findByMenuItemIdAndIsActiveTrue(10L)).thenReturn(Optional.empty());

        service.updateStatus(orderId, statusRequest("confirmed", 5L));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        ArgumentCaptor<OrderStatusHistory> captor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(historyRepo).save(captor.capture());
        assertThat(captor.getValue().getFromStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(captor.getValue().getToStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    // ── updateStatus: transición inválida ────────────────────────────────────

    @Test
    void updateStatus_transiciónInválida_lanzaBusinessRuleException() {
        Long orderId = 1L;
        Order order = orderWithStatus(orderId, OrderStatus.DELIVERED);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
        when(employeeRepo.findById(5L)).thenReturn(Optional.of(employee(5L)));

        // DELIVERED → CONFIRMED no está permitido
        assertThatThrownBy(() -> service.updateStatus(orderId, statusRequest("confirmed", 5L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Transición de estado no permitida");

        verify(orderRepo, never()).save(any());
    }

    @Test
    void updateStatus_estadoFinalCancelled_noPermiteOtraTransición() {
        Long orderId = 1L;
        Order order = orderWithStatus(orderId, OrderStatus.CANCELLED);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
        when(employeeRepo.findById(5L)).thenReturn(Optional.of(employee(5L)));

        assertThatThrownBy(() -> service.updateStatus(orderId, statusRequest("pending", 5L)))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ── updateStatus: CONFIRMED descuenta inventario ──────────────────────────

    @Test
    void updateStatus_aConfirmed_descuentaInventarioPorReceta() {
        Long orderId = 1L;
        Order order = orderWithStatus(orderId, OrderStatus.PENDING);
        // item: menuItem id=10, qty=2

        Supply supply = new Supply();
        supply.setId(20L);

        RecipeIngredient ingredient = new RecipeIngredient();
        ingredient.setSupply(supply);
        ingredient.setQuantity(new BigDecimal("0.5")); // 0.5 * 2 items = 1.0 en total

        Recipe recipe = new Recipe();
        recipe.getIngredients().add(ingredient);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
        when(employeeRepo.findById(5L)).thenReturn(Optional.of(employee(5L)));
        when(orderRepo.save(any())).thenReturn(order);
        when(historyRepo.save(any())).thenReturn(new OrderStatusHistory());
        when(mapper.toResponse(any())).thenReturn(mock(OrderResponse.class));
        when(recipeRepo.findByMenuItemIdAndIsActiveTrue(10L)).thenReturn(Optional.of(recipe));

        service.updateStatus(orderId, statusRequest("confirmed", 5L));

        // Debe llamarse register para el suministro con qty = 0.5 * 2 = 1.0
        ArgumentCaptor<InventoryMovementRequest> movCaptor =
                ArgumentCaptor.forClass(InventoryMovementRequest.class);
        verify(inventoryService).register(eq(20L), movCaptor.capture());

        InventoryMovementRequest movReq = movCaptor.getValue();
        assertThat(movReq.getMovementType()).isEqualTo("salida");
        assertThat(movReq.getQuantity()).isEqualByComparingTo("1.0");
    }

    @Test
    void updateStatus_aConfirmed_siInventarioFalla_continúaSinExcepción() {
        Long orderId = 1L;
        Order order = orderWithStatus(orderId, OrderStatus.PENDING);

        Supply supply = new Supply();
        supply.setId(20L);
        RecipeIngredient ingredient = new RecipeIngredient();
        ingredient.setSupply(supply);
        ingredient.setQuantity(new BigDecimal("1.0"));
        Recipe recipe = new Recipe();
        recipe.getIngredients().add(ingredient);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
        when(employeeRepo.findById(5L)).thenReturn(Optional.of(employee(5L)));
        when(orderRepo.save(any())).thenReturn(order);
        when(historyRepo.save(any())).thenReturn(new OrderStatusHistory());
        when(mapper.toResponse(any())).thenReturn(mock(OrderResponse.class));
        when(recipeRepo.findByMenuItemIdAndIsActiveTrue(10L)).thenReturn(Optional.of(recipe));
        // Simula que el descuento falla (ej. stock insuficiente)
        doThrow(new BusinessRuleException("Stock insuficiente"))
                .when(inventoryService).register(anyLong(), any());

        // La orden igual cambia de estado; el error de inventario solo se loguea
        service.updateStatus(orderId, statusRequest("confirmed", 5L));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    // ── updateStatus: orden no encontrada ────────────────────────────────────

    @Test
    void updateStatus_ordenNoEncontrada_lanzaResourceNotFoundException() {
        when(orderRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(99L, statusRequest("confirmed", 1L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getByRestaurant: sin filtro devuelve todas ────────────────────────────

    @Test
    void getByRestaurant_sinFiltro_llamaQuerySinStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> emptyPage = new PageImpl<>(List.of());
        when(orderRepo.findByRestaurantIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(emptyPage);

        service.getByRestaurant(1L, null, null, pageable);

        verify(orderRepo).findByRestaurantIdOrderByCreatedAtDesc(1L, pageable);
        verify(orderRepo, never()).findByRestaurantIdAndStatusOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    void getByRestaurant_listaVacia_llamaQuerySinStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> emptyPage = new PageImpl<>(List.of());
        when(orderRepo.findByRestaurantIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(emptyPage);

        service.getByRestaurant(1L, List.of(), null, pageable);

        verify(orderRepo).findByRestaurantIdOrderByCreatedAtDesc(1L, pageable);
        verify(orderRepo, never()).findByRestaurantIdAndStatusOrderByCreatedAtDesc(any(), any(), any());
    }

    // ── getByRestaurant: con filtro de status ─────────────────────────────────

    @Test
    void getByRestaurant_unStatus_llamaQueryConStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> emptyPage = new PageImpl<>(List.of());
        when(orderRepo.findByRestaurantIdAndStatusOrderByCreatedAtDesc(1L, OrderStatus.PENDING, pageable))
                .thenReturn(emptyPage);

        service.getByRestaurant(1L, List.of("pending"), null, pageable);

        verify(orderRepo).findByRestaurantIdAndStatusOrderByCreatedAtDesc(1L, OrderStatus.PENDING, pageable);
        verify(orderRepo, never()).findByRestaurantIdOrderByCreatedAtDesc(any(), any());
        verify(orderRepo, never()).findByRestaurantIdAndStatusInOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    void getByRestaurant_unStatusEnMayúsculas_esAceptado() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> emptyPage = new PageImpl<>(List.of());
        when(orderRepo.findByRestaurantIdAndStatusOrderByCreatedAtDesc(1L, OrderStatus.CONFIRMED, pageable))
                .thenReturn(emptyPage);

        service.getByRestaurant(1L, List.of("CONFIRMED"), null, pageable);

        verify(orderRepo).findByRestaurantIdAndStatusOrderByCreatedAtDesc(1L, OrderStatus.CONFIRMED, pageable);
    }

    @Test
    void getByRestaurant_variosStatus_llamaQueryIN() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> emptyPage = new PageImpl<>(List.of());
        List<OrderStatus> expected = List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.READY);
        when(orderRepo.findByRestaurantIdAndStatusInOrderByCreatedAtDesc(1L, expected, pageable))
                .thenReturn(emptyPage);

        service.getByRestaurant(1L, List.of("confirmed", "preparing", "ready"), null, pageable);

        verify(orderRepo).findByRestaurantIdAndStatusInOrderByCreatedAtDesc(1L, expected, pageable);
        verify(orderRepo, never()).findByRestaurantIdOrderByCreatedAtDesc(any(), any());
        verify(orderRepo, never()).findByRestaurantIdAndStatusOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    void getByRestaurant_statusInválido_lanzaBusinessRuleException() {
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> service.getByRestaurant(1L, List.of("inexistente"), null, pageable))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Valor inválido para status");
    }

    // ── getByRestaurant: filtro de fecha ──────────────────────────────────────

    @Test
    void getByRestaurant_soloFecha_llamaQueryDateSinStatus() {
        LocalDate date = LocalDate.of(2026, 4, 20);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> emptyPage = new PageImpl<>(List.of());
        when(orderRepo.findByRestaurantAndDate(1L, start, end, pageable)).thenReturn(emptyPage);

        service.getByRestaurant(1L, null, date, pageable);

        verify(orderRepo).findByRestaurantAndDate(1L, start, end, pageable);
        verify(orderRepo, never()).findByRestaurantIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void getByRestaurant_fechaYUnStatus_llamaQueryDateStatus() {
        LocalDate date = LocalDate.of(2026, 4, 20);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> emptyPage = new PageImpl<>(List.of());
        when(orderRepo.findByRestaurantAndStatusAndDate(1L, OrderStatus.DELIVERED, start, end, pageable))
                .thenReturn(emptyPage);

        service.getByRestaurant(1L, List.of("delivered"), date, pageable);

        verify(orderRepo).findByRestaurantAndStatusAndDate(1L, OrderStatus.DELIVERED, start, end, pageable);
        verify(orderRepo, never()).findByRestaurantAndDate(any(), any(), any(), any());
    }

    @Test
    void getByRestaurant_fechaYVariosStatus_llamaQueryDateStatusIn() {
        LocalDate date = LocalDate.of(2026, 4, 20);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        Pageable pageable = PageRequest.of(0, 20);
        List<OrderStatus> expected = List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.READY);
        Page<Order> emptyPage = new PageImpl<>(List.of());
        when(orderRepo.findByRestaurantAndStatusInAndDate(1L, expected, start, end, pageable))
                .thenReturn(emptyPage);

        service.getByRestaurant(1L, List.of("confirmed", "preparing", "ready"), date, pageable);

        verify(orderRepo).findByRestaurantAndStatusInAndDate(1L, expected, start, end, pageable);
        verify(orderRepo, never()).findByRestaurantAndDate(any(), any(), any(), any());
    }
}
