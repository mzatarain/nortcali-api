package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.*;
import com.nortcali.api.dto.response.*;
import com.nortcali.api.entity.*;
import com.nortcali.api.entity.enums.*;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.OrderMapper;
import com.nortcali.api.repository.*;
import com.nortcali.api.service.InventoryMovementService;
import com.nortcali.api.service.OrderService;
import com.nortcali.api.service.SaleService;
import com.nortcali.api.util.FolioGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    // Transiciones de estado permitidas
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING,    EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED,  EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.PREPARING,  EnumSet.of(OrderStatus.READY, OrderStatus.CANCELLED),
            OrderStatus.READY,      EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED),
            OrderStatus.DELIVERED,  EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELLED,  EnumSet.noneOf(OrderStatus.class)
    );

    private final OrderRepository orderRepo;
    private final OrderStatusHistoryRepository historyRepo;
    private final PaymentRepository paymentRepo;
    private final RestaurantRepository restaurantRepo;
    private final EmployeeRepository employeeRepo;
    private final CustomerRepository customerRepo;
    private final DeliveryDriverRepository driverRepo;
    private final MenuItemRepository menuItemRepo;
    private final MenuItemVariantRepository variantRepo;
    private final ModifierRepository modifierRepo;
    private final RecipeRepository recipeRepo;
    private final InventoryMovementService inventoryService;
    private final SaleService saleService;
    private final OrderMapper mapper;

    public OrderServiceImpl(OrderRepository orderRepo,
                            OrderStatusHistoryRepository historyRepo,
                            PaymentRepository paymentRepo,
                            RestaurantRepository restaurantRepo,
                            EmployeeRepository employeeRepo,
                            CustomerRepository customerRepo,
                            DeliveryDriverRepository driverRepo,
                            MenuItemRepository menuItemRepo,
                            MenuItemVariantRepository variantRepo,
                            ModifierRepository modifierRepo,
                            RecipeRepository recipeRepo,
                            InventoryMovementService inventoryService,
                            SaleService saleService,
                            OrderMapper mapper) {
        this.orderRepo = orderRepo;
        this.historyRepo = historyRepo;
        this.paymentRepo = paymentRepo;
        this.restaurantRepo = restaurantRepo;
        this.employeeRepo = employeeRepo;
        this.customerRepo = customerRepo;
        this.driverRepo = driverRepo;
        this.menuItemRepo = menuItemRepo;
        this.variantRepo = variantRepo;
        this.modifierRepo = modifierRepo;
        this.recipeRepo = recipeRepo;
        this.inventoryService = inventoryService;
        this.saleService = saleService;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getByRestaurant(Long restaurantId, List<String> statuses, LocalDate date, Pageable pageable) {
        List<OrderStatus> parsed = (statuses == null || statuses.isEmpty()) ? List.of() :
                statuses.stream().map(s -> parseEnum(OrderStatus.class, s, "status")).toList();

        if (date == null) {
            if (parsed.isEmpty()) {
                return orderRepo.findByRestaurantIdOrderByCreatedAtDesc(restaurantId, pageable)
                        .map(this::toOrderResponse);
            }
            if (parsed.size() == 1) {
                return orderRepo.findByRestaurantIdAndStatusOrderByCreatedAtDesc(restaurantId, parsed.getFirst(), pageable)
                        .map(this::toOrderResponse);
            }
            return orderRepo.findByRestaurantIdAndStatusInOrderByCreatedAtDesc(restaurantId, parsed, pageable)
                    .map(this::toOrderResponse);
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        if (parsed.isEmpty()) {
            return orderRepo.findByRestaurantAndDate(restaurantId, start, end, pageable)
                    .map(this::toOrderResponse);
        }
        if (parsed.size() == 1) {
            return orderRepo.findByRestaurantAndStatusAndDate(restaurantId, parsed.getFirst(), start, end, pageable)
                    .map(this::toOrderResponse);
        }
        return orderRepo.findByRestaurantAndStatusInAndDate(restaurantId, parsed, start, end, pageable)
                .map(this::toOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long restaurantId, Long id) {
        Order order = findOrThrow(id);
        if (!order.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Order", id);
        }
        return toOrderResponse(order);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse create(Long restaurantId, OrderRequest request) {
        // PESSIMISTIC_WRITE serializa creaciones concurrentes del mismo restaurante,
        // evitando duplicados en la secuencia del folio
        var restaurant = restaurantRepo.findByIdWithLock(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        var employee = resolveAuthenticatedEmployee();

        Order order = new Order();
        order.setRestaurant(restaurant);
        order.setEmployee(employee);

        // Parsear enums desde el request (con mensajes claros si son inválidos)
        order.setOrderType(parseEnum(OrderType.class, request.getOrderType(), "order_type"));
        order.setSource(parseEnum(OrderSource.class, request.getSource(), "source"));
        order.setStatus(OrderStatus.CONFIRMED);

        if (request.getPaymentMethod() != null) {
            order.setPaymentMethod(parseEnum(PaymentMethod.class, request.getPaymentMethod(), "payment_method"));
        }
        order.setNotes(request.getNotes());

        // Cliente y repartidor opcionales
        if (request.getCustomerId() != null) {
            order.setCustomer(customerRepo.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId())));
        }
        if (request.getDriverId() != null) {
            order.setDriver(driverRepo.findById(request.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("DeliveryDriver", request.getDriverId())));
        }

        // Construir items y calcular total (el subtotal de cada item ya incluye modificadores)
        List<OrderItem> items = buildItems(request.getItems(), order);
        BigDecimal total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotal(total);
        order.getItems().addAll(items);

        // Generar folio: ORD-{restaurantId}-{yyyyMMdd}-{secuencia}
        // MAX en vez de COUNT para que hard-deletes no provoquen colisión de folio
        LocalDate today = LocalDate.now(ZoneId.of(restaurant.getTimezone()));
        String prefix = FolioGenerator.folioPrefix(restaurantId, today) + "%";
        Integer maxSeq = orderRepo.findMaxSequenceByFolioPrefix(restaurantId, prefix);
        long sequence = (maxSeq != null ? maxSeq : 0) + 1;
        order.setFolio(FolioGenerator.generateOrderFolio(restaurantId, today, sequence));

        Order saved = orderRepo.save(order);

        // Registrar primer estado en historial
        saveHistory(saved, null, OrderStatus.CONFIRMED, employee);

        // Descontar insumos al crear — la orden nace confirmada
        deductInventory(saved);

        log.info("Orden creada: {} para restaurante {}", saved.getFolio(), restaurantId);
        return toOrderResponse(saved);
    }

    @Override
    public OrderResponse updateStatus(Long restaurantId, Long id, OrderStatusUpdateRequest request) {
        log.info(">>> OrderServiceImpl.updateStatus llamado. orderId={}, toStatus={}", id, request.getToStatus());
        Order order = findOrThrow(id);
        if (!order.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Order", id);
        }
        var employee = resolveAuthenticatedEmployee();

        OrderStatus newStatus = parseEnum(OrderStatus.class, request.getToStatus(), "status");
        OrderStatus currentStatus = order.getStatus();

        // Validar transición permitida
        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(OrderStatus.class)).contains(newStatus)) {
            throw new BusinessRuleException(
                    "Transición de estado no permitida: " + currentStatus.name().toLowerCase()
                    + " → " + newStatus.name().toLowerCase());
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(newStatus);

        LocalDateTime now = LocalDateTime.now();
        if (newStatus == OrderStatus.PREPARING) {
            order.setPreparingAt(now);
        }
        if (newStatus == OrderStatus.READY && order.getPreparingAt() != null) {
            order.setReadyAt(now);
            order.setPreparationTimeSeconds((int) ChronoUnit.SECONDS.between(order.getPreparingAt(), now));
        }

        orderRepo.save(order);

        saveHistory(order, previousStatus, newStatus, employee);

        // Al confirmar: descontar insumos del inventario
        if (newStatus == OrderStatus.CONFIRMED) {
            deductInventory(order);
        }

        // Al entregar: crear venta automáticamente (transacción independiente — fallo no revierte el estado)
        if (newStatus == OrderStatus.DELIVERED) {
            log.info("Iniciando auto-creación de venta para orden {}", order.getFolio());
            try {
                saleService.createFromOrder(order.getId(), employee.getId());
            } catch (Exception e) {
                log.error("Error en auto-creación de venta para orden {}: {}", order.getFolio(), e.getMessage(), e);
            }
        }

        log.info("Orden {} cambió de {} a {}", order.getFolio(), previousStatus, newStatus);
        return toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getHistory(Long restaurantId, Long orderId) {
        Order order = findOrThrow(orderId);
        if (!order.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Order", orderId);
        }
        return historyRepo.findByOrderIdOrderByChangedAtAsc(orderId)
                .stream().map(mapper::toHistoryResponse).toList();
    }

    @Override
    public PaymentResponse addPayment(Long restaurantId, Long orderId, PaymentRequest request) {
        Order order = findOrThrow(orderId);
        if (!order.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Order", orderId);
        }
        var registeredBy = resolveAuthenticatedEmployee();

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(parseEnum(PaymentMethod.class, request.getMethod(), "payment_method"));
        payment.setAmount(request.getAmount());
        payment.setReference(request.getReference());
        payment.setRegisteredBy(registeredBy);

        // Actualizar método de pago principal de la orden
        order.setPaymentMethod(payment.getMethod());
        orderRepo.save(order);

        return mapper.toPaymentResponse(paymentRepo.save(payment));
    }

    @Override
    public void delete(Long restaurantId, Long orderId) {
        Order order = findOrThrow(orderId);
        if (!order.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Order", orderId);
        }
        saleService.deleteLinkedSale(orderId);
        // El historial no tiene cascade JPA desde Order — borrar primero
        historyRepo.deleteByOrderId(orderId);
        orderRepo.delete(order);
        log.info("Orden {} eliminada del restaurante {}", order.getFolio(), restaurantId);
    }

    @Override
    public CloseDayResponse closeDay(Long restaurantId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.READY);

        List<Order> orders = orderRepo.findActiveOrdersForDay(restaurantId, activeStatuses, start, end);

        for (Order order : orders) {
            OrderStatus previousStatus = order.getStatus();
            order.setStatus(OrderStatus.DELIVERED);
            orderRepo.save(order);
            saveHistory(order, previousStatus, OrderStatus.DELIVERED, order.getEmployee());

            if (saleService.findSaleIdByOrderId(order.getId()).isEmpty()) {
                try {
                    saleService.createFromOrder(order.getId(), order.getEmployee().getId());
                } catch (Exception e) {
                    log.error("Error en auto-creación de venta para orden {} (close-day): {}",
                            order.getFolio(), e.getMessage(), e);
                }
            }
        }

        log.info("close-day restaurante {}: {} órdenes cerradas", restaurantId, orders.size());
        return new CloseDayResponse(orders.size());
    }

    // =====================
    // Métodos privados
    // =====================

    private OrderResponse toOrderResponse(Order order) {
        Long saleId = saleService.findSaleIdByOrderId(order.getId()).orElse(null);
        return mapper.toResponse(order, saleId);
    }

    private List<OrderItem> buildItems(List<OrderItemRequest> itemRequests, Order order) {
        List<OrderItem> result = new ArrayList<>();
        for (var dto : itemRequests) {
            var menuItem = menuItemRepo.findById(dto.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("MenuItem", dto.getMenuItemId()));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setMenuItem(menuItem);
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(dto.getUnitPrice());
            item.setGroupLabel(dto.getGroupLabel());

            if (dto.getVariantId() != null) {
                item.setVariant(variantRepo.findById(dto.getVariantId())
                        .orElseThrow(() -> new ResourceNotFoundException("MenuItemVariant", dto.getVariantId())));
            }

            // Resolver modificadores y calcular precio extra por unidad
            BigDecimal modifierPricePerUnit = BigDecimal.ZERO;
            if (dto.getModifiers() != null) {
                for (var modDto : dto.getModifiers()) {
                    var modifier = modifierRepo.findById(modDto.getModifierId())
                            .orElseThrow(() -> new ResourceNotFoundException("Modifier", modDto.getModifierId()));
                    OrderItemModifier oim = new OrderItemModifier();
                    oim.setOrderItem(item);
                    oim.setModifier(modifier);
                    oim.setModifierName(modifier.getName());
                    oim.setGroupName(modifier.getGroup().getName());
                    oim.setPrice(modDto.getPrice());
                    item.getModifiers().add(oim);
                    modifierPricePerUnit = modifierPricePerUnit.add(modDto.getPrice());
                }
            }

            // subtotal incluye el precio de modificadores multiplicado por cantidad
            item.setSubtotal(
                    dto.getUnitPrice().add(modifierPricePerUnit)
                            .multiply(BigDecimal.valueOf(dto.getQuantity()))
            );

            result.add(item);
        }
        return result;
    }

    private void saveHistory(Order order, OrderStatus from, OrderStatus to, Employee employee) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setEmployee(employee);
        historyRepo.save(history);
    }

    /**
     * Al confirmar una orden, descuenta los insumos según las recetas.
     * Descuenta receta base + adiciones de la variante seleccionada (modelo aditivo).
     * Si el stock cae bajo el mínimo se loguea una alerta pero no se bloquea.
     */
    private void deductInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            Long menuItemId = item.getMenuItem().getId();
            Long employeeId = order.getEmployee().getId();

            // Receta base (variant_id IS NULL)
            recipeRepo.findByMenuItemIdAndVariantIdIsNullAndIsActiveTrue(menuItemId)
                    .ifPresent(r -> deductRecipeIngredients(r, item.getQuantity(), employeeId, order.getFolio()));

            // Adiciones específicas de la variante pedida
            if (item.getVariant() != null) {
                recipeRepo.findByMenuItemIdAndVariantIdAndIsActiveTrue(menuItemId, item.getVariant().getId())
                        .ifPresent(r -> deductRecipeIngredients(r, item.getQuantity(), employeeId, order.getFolio()));
            }
        }
    }

    private void deductRecipeIngredients(Recipe recipe, int quantity, Long employeeId, String folio) {
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            BigDecimal totalQty = ingredient.getQuantity()
                    .multiply(BigDecimal.valueOf(quantity));
            InventoryMovementRequest movReq = new InventoryMovementRequest();
            movReq.setMovementType("salida");
            movReq.setQuantity(totalQty);
            movReq.setEmployeeId(employeeId);
            try {
                inventoryService.register(ingredient.getSupply().getId(), movReq);
            } catch (Exception e) {
                log.warn("No se pudo descontar insumo {} para orden {}: {}",
                        ingredient.getSupply().getId(), folio, e.getMessage());
            }
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Valor inválido para " + fieldName + ": " + value);
        }
    }

    private Employee resolveAuthenticatedEmployee() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return employeeRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado autenticado no encontrado: " + username));
    }

    private Order findOrThrow(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }
}
