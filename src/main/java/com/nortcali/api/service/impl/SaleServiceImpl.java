package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.SaleRequest;
import com.nortcali.api.dto.response.SaleResponse;
import com.nortcali.api.dto.response.SalesBySourceResponse;
import com.nortcali.api.entity.CashSession;
import com.nortcali.api.entity.Order;
import com.nortcali.api.entity.Sale;
import com.nortcali.api.entity.SaleItem;
import com.nortcali.api.entity.enums.CashSessionStatus;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.SaleMapper;
import com.nortcali.api.repository.*;
import com.nortcali.api.service.SaleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@Slf4j
public class SaleServiceImpl implements SaleService {

    private final SaleRepository saleRepo;
    private final SalesSourceRepository sourceRepo;
    private final RestaurantRepository restaurantRepo;
    private final EmployeeRepository employeeRepo;
    private final MenuItemRepository menuItemRepo;
    private final MenuItemVariantRepository variantRepo;
    private final OrderRepository orderRepo;
    private final CashSessionRepository cashSessionRepo;
    private final SaleMapper mapper;

    public SaleServiceImpl(SaleRepository saleRepo,
                           SalesSourceRepository sourceRepo,
                           RestaurantRepository restaurantRepo,
                           EmployeeRepository employeeRepo,
                           MenuItemRepository menuItemRepo,
                           MenuItemVariantRepository variantRepo,
                           OrderRepository orderRepo,
                           CashSessionRepository cashSessionRepo,
                           SaleMapper mapper) {
        this.saleRepo = saleRepo;
        this.sourceRepo = sourceRepo;
        this.restaurantRepo = restaurantRepo;
        this.employeeRepo = employeeRepo;
        this.menuItemRepo = menuItemRepo;
        this.variantRepo = variantRepo;
        this.orderRepo = orderRepo;
        this.cashSessionRepo = cashSessionRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SaleResponse> getByRestaurant(Long restaurantId, Pageable pageable) {
        return saleRepo.findByRestaurantIdAndIsActiveTrueOrderBySaleDateDesc(restaurantId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public SaleResponse create(Long restaurantId, SaleRequest request) {
        restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        var source = sourceRepo.findById(request.getSourceId())
                .orElseThrow(() -> new ResourceNotFoundException("SalesSource", request.getSourceId()));
        var employee = employeeRepo.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.getEmployeeId()));

        Sale sale = new Sale();
        sale.setRestaurant(restaurantRepo.getReferenceById(restaurantId));
        sale.setSource(source);
        sale.setEmployee(employee);
        sale.setSaleDate(request.getSaleDate());

        // Construir items y calcular total
        List<SaleItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var dto : request.getItems()) {
            var menuItem = menuItemRepo.findById(dto.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("MenuItem", dto.getMenuItemId()));
            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setMenuItem(menuItem);
            item.setQuantity(dto.getQuantity());
            item.setSubtotal(dto.getSubtotal());
            if (dto.getVariantId() != null) {
                item.setVariant(variantRepo.findById(dto.getVariantId())
                        .orElseThrow(() -> new ResourceNotFoundException("MenuItemVariant", dto.getVariantId())));
            }
            items.add(item);
            total = total.add(dto.getSubtotal());
        }
        sale.setTotal(total);

        // Regla de negocio: commission = total * commissionPct / 100
        BigDecimal commission = total.multiply(source.getCommissionPct())
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        sale.setCommission(commission);
        sale.getItems().addAll(items);

        log.info("Creando venta para restaurante {} con total={}, comisión={}", restaurantId, total, commission);
        return mapper.toResponse(saleRepo.save(sale));
    }

    @Override
    public void deactivate(Long id) {
        Sale entity = findOrThrow(id);
        entity.setActive(false);
        saleRepo.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesBySourceResponse> getSalesBySource(Long restaurantId) {
        return saleRepo.findSalesBySource(restaurantId).stream()
                .map(row -> new SalesBySourceResponse(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        (BigDecimal) row[2]))
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createFromOrder(Long orderId, Long employeeId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        String sourceName = order.getSource().name().toLowerCase();
        var source = sourceRepo.findByNameIgnoreCaseAndIsActiveTrue(sourceName)
                .or(() -> sourceRepo.findByNameIgnoreCaseAndIsActiveTrue("pos"))
                .or(() -> sourceRepo.findFirstByIsActiveTrueOrderByIdAsc())
                .orElseThrow(() -> new BusinessRuleException(
                        "No hay fuentes de venta activas configuradas. Configúralas en Ajustes → Fuentes de venta."));

        var employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        Sale sale = new Sale();
        sale.setRestaurant(order.getRestaurant());
        sale.setSource(source);
        sale.setEmployee(employee);
        sale.setSaleDate(LocalDate.now());

        List<SaleItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var orderItem : order.getItems()) {
            SaleItem saleItem = new SaleItem();
            saleItem.setSale(sale);
            saleItem.setMenuItem(orderItem.getMenuItem());
            saleItem.setVariant(orderItem.getVariant());
            saleItem.setQuantity(orderItem.getQuantity());
            BigDecimal subtotal = orderItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(orderItem.getQuantity()));
            saleItem.setSubtotal(subtotal);
            items.add(saleItem);
            total = total.add(subtotal);
        }

        sale.setTotal(total);
        BigDecimal commission = total.multiply(source.getCommissionPct())
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        sale.setCommission(commission);
        sale.getItems().addAll(items);

        cashSessionRepo.findByRestaurantIdAndStatus(order.getRestaurant().getId(), CashSessionStatus.OPEN)
                .ifPresent(sale::setCashSession);

        saleRepo.save(sale);
        log.info("Venta creada automáticamente desde orden {} para restaurante {}",
                order.getFolio(), order.getRestaurant().getId());
    }

    private Sale findOrThrow(Long id) {
        return saleRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sale", id));
    }
}
