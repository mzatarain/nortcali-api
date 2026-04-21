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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
                        .map(mapper::toResponse);
            }
            if (parsed.size() == 1) {
                return orderRepo.findByRestaurantIdAndStatusOrderByCreatedAtDesc(restaurantId, parsed.getFirst(), pageable)
                        .map(mapper::toResponse);
            }
            return orderRepo.findByRestaurantIdAndStatusInOrderByCreatedAtDesc(restaurantId, parsed, pageable)
                    .map(mapper::toResponse);
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        if (parsed.isEmpty()) {
            return orderRepo.findByRestaurantAndDate(restaurantId, start, end, pageable)
                    .map(mapper::toResponse);
        }
        if (parsed.size() == 1) {
            return orderRepo.findByRestaurantAndStatusAndDate(restaurantId, parsed.getFirst(), start, end, pageable)
                    .map(mapper::toResponse);
        }
        return orderRepo.findByRestaurantAndStatusInAndDate(restaurantId, parsed, start, end, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse create(Long restaurantId, OrderRequest request) {
        // PESSIMISTIC_WRITE serializa creaciones concurrentes del mismo restaurante,
        // evitando duplicados en la secuencia del folio
        var restaurant = restaurantRepo.findByIdWithLock(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        var employee = employeeRepo.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.getEmployeeId()));

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

        // Cliente y repartidor opcionales
        if (request.getCustomerId() != null) {
            order.setCustomer(customerRepo.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId())));
        }
        if (request.getDriverId() != null) {
            order.setDriver(driverRepo.findById(request.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("DeliveryDriver", request.getDriverId())));
        }

        // Construir items y calcular total
        List<OrderItem> items = buildItems(request.getItems(), order);
        BigDecimal total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Sumar extras
        for (OrderItem item : items) {
            for (OrderItemExtra extra : item.getExtras()) {
                total = total.add(extra.getUnitPrice());
            }
        }

        order.setTotal(total);
        order.getItems().addAll(items);

        // Generar folio: ORD-{restaurantId}-{yyyyMMdd}-{secuencia}
        LocalDate today = LocalDate.now();
        String prefix = FolioGenerator.folioPrefix(restaurantId, today) + "%";
        long sequence = orderRepo.countByFolioPrefix(restaurantId, prefix) + 1;
        order.setFolio(FolioGenerator.generateOrderFolio(restaurantId, today, sequence));

        Order saved = orderRepo.save(order);

        // Registrar primer estado en historial
        saveHistory(saved, null, OrderStatus.CONFIRMED, employee);

        // Descontar insumos al crear — la orden nace confirmada
        deductInventory(saved);

        log.info("Orden creada: {} para restaurante {}", saved.getFolio(), restaurantId);
        return mapper.toResponse(saved);
    }

    @Override
    public OrderResponse updateStatus(Long id, OrderStatusUpdateRequest request) {
        Order order = findOrThrow(id);
        var employee = employeeRepo.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.getEmployeeId()));

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
        orderRepo.save(order);

        saveHistory(order, previousStatus, newStatus, employee);

        // Al confirmar: descontar insumos del inventario
        if (newStatus == OrderStatus.CONFIRMED) {
            deductInventory(order);
        }

        // Al entregar: crear venta automáticamente (transacción independiente — fallo no revierte el estado)
        if (newStatus == OrderStatus.DELIVERED) {
            try {
                saleService.createFromOrder(order.getId(), request.getEmployeeId());
            } catch (Exception e) {
                log.error("No se pudo crear la venta para la orden {}: {}", order.getFolio(), e.getMessage());
            }
        }

        log.info("Orden {} cambió de {} a {}", order.getFolio(), previousStatus, newStatus);
        return mapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getHistory(Long orderId) {
        findOrThrow(orderId);
        return historyRepo.findByOrderIdOrderByChangedAtAsc(orderId)
                .stream().map(mapper::toHistoryResponse).toList();
    }

    @Override
    public PaymentResponse addPayment(Long orderId, PaymentRequest request) {
        Order order = findOrThrow(orderId);
        var registeredBy = employeeRepo.findById(request.getRegisteredBy())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.getRegisteredBy()));

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

    // =====================
    // Métodos privados
    // =====================

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
            item.setSubtotal(dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getQuantity())));

            if (dto.getVariantId() != null) {
                item.setVariant(variantRepo.findById(dto.getVariantId())
                        .orElseThrow(() -> new ResourceNotFoundException("MenuItemVariant", dto.getVariantId())));
            }

            if (dto.getExtras() != null) {
                for (var extraDto : dto.getExtras()) {
                    var extraItem = menuItemRepo.findById(extraDto.getMenuItemId())
                            .orElseThrow(() -> new ResourceNotFoundException("MenuItem", extraDto.getMenuItemId()));
                    OrderItemExtra extra = new OrderItemExtra();
                    extra.setOrderItem(item);
                    extra.setMenuItem(extraItem);
                    extra.setUnitPrice(extraDto.getUnitPrice());
                    item.getExtras().add(extra);
                }
            }
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
     * Si el stock cae bajo el mínimo se loguea una alerta pero no se bloquea.
     */
    private void deductInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            recipeRepo.findByMenuItemIdAndIsActiveTrue(item.getMenuItem().getId())
                    .ifPresent(recipe -> {
                        for (RecipeIngredient ingredient : recipe.getIngredients()) {
                            BigDecimal totalQty = ingredient.getQuantity()
                                    .multiply(BigDecimal.valueOf(item.getQuantity()));
                            InventoryMovementRequest movReq = new InventoryMovementRequest();
                            movReq.setMovementType("salida");
                            movReq.setQuantity(totalQty);
                            movReq.setEmployeeId(order.getEmployee().getId());
                            try {
                                inventoryService.register(ingredient.getSupply().getId(), movReq);
                            } catch (Exception e) {
                                log.warn("No se pudo descontar insumo {} para orden {}: {}",
                                        ingredient.getSupply().getId(), order.getFolio(), e.getMessage());
                            }
                        }
                    });
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Valor inválido para " + fieldName + ": " + value);
        }
    }

    private Order findOrThrow(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }
}
