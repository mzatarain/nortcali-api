package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.SaleRequest;
import com.nortcali.api.dto.response.SaleResponse;
import com.nortcali.api.dto.response.SalesBySourceResponse;
import com.nortcali.api.entity.Order;
import com.nortcali.api.entity.Sale;
import com.nortcali.api.entity.SaleItem;
import com.nortcali.api.entity.enums.CashSessionStatus;
import com.nortcali.api.entity.enums.PaymentMethod;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.SaleMapper;
import com.nortcali.api.repository.*;
import com.nortcali.api.util.FolioGenerator;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    public Page<SaleResponse> getByRestaurant(Long restaurantId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        if (startDate != null && endDate != null) {
            return saleRepo.findByRestaurantIdAndIsActiveTrueAndSaleDateBetweenOrderBySaleDateDesc(
                    restaurantId, startDate, endDate, pageable).map(mapper::toResponse);
        }
        if (startDate != null) {
            return saleRepo.findByRestaurantIdAndIsActiveTrueAndSaleDateBetweenOrderBySaleDateDesc(
                    restaurantId, startDate, LocalDate.of(9999, 12, 31), pageable).map(mapper::toResponse);
        }
        if (endDate != null) {
            return saleRepo.findByRestaurantIdAndIsActiveTrueAndSaleDateBetweenOrderBySaleDateDesc(
                    restaurantId, LocalDate.of(1970, 1, 1), endDate, pageable).map(mapper::toResponse);
        }
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

        LocalDate saleDate = request.getSaleDate();
        String folioPrefix = FolioGenerator.saleFolioPrefix(restaurantId, saleDate) + "%";
        long sequence = saleRepo.countByFolioPrefix(restaurantId, folioPrefix) + 1;

        Sale sale = new Sale();
        sale.setRestaurant(restaurantRepo.getReferenceById(restaurantId));
        sale.setSource(source);
        sale.setEmployee(employee);
        sale.setSaleDate(saleDate);
        sale.setFolio(FolioGenerator.generateSaleFolio(restaurantId, saleDate, sequence));
        if (request.getPaymentMethod() != null) {
            sale.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
        }
        sale.setNotes(request.getNotes());

        // Construir items y calcular total
        List<SaleItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var dto : request.getItems()) {
            var menuItem = menuItemRepo.findById(dto.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("MenuItem", dto.getMenuItemId()));
            BigDecimal unitPrice = dto.getSubtotal()
                    .divide(BigDecimal.valueOf(dto.getQuantity()), 2, RoundingMode.HALF_UP);
            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setMenuItem(menuItem);
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(unitPrice);
            item.setSubtotal(dto.getSubtotal());
            if (dto.getVariantId() != null) {
                item.setVariant(variantRepo.findById(dto.getVariantId())
                        .orElseThrow(() -> new ResourceNotFoundException("MenuItemVariant", dto.getVariantId())));
            }
            items.add(item);
            total = total.add(dto.getSubtotal());
        }
        sale.setSubtotal(total);
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
    public List<SalesBySourceResponse> getSalesBySource(Long restaurantId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> rows = (startDate != null && endDate != null)
                ? saleRepo.findSalesBySourceAndDateRange(restaurantId, startDate, endDate)
                : saleRepo.findSalesBySource(restaurantId);
        return rows.stream()
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
        sale.setSaleDate(LocalDate.now(ZoneId.of(order.getRestaurant().getTimezone())));
        sale.setFolio(order.getFolio());
        sale.setPaymentMethod(order.getPaymentMethod());
        sale.setCustomer(order.getCustomer());

        BigDecimal total = BigDecimal.ZERO;
        List<SaleItem> saleItems = new ArrayList<>();
        for (var orderItem : order.getItems()) {
              BigDecimal subtotal = orderItem.getSubtotal(); // incluye modificadores
              SaleItem saleItem = new SaleItem();
              saleItem.setSale(sale);
              saleItem.setMenuItem(orderItem.getMenuItem());
              saleItem.setVariant(orderItem.getVariant());
              saleItem.setQuantity(orderItem.getQuantity());
              saleItem.setUnitPrice(orderItem.getUnitPrice());
              saleItem.setSubtotal(subtotal);
              saleItem.setGroupLabel(orderItem.getGroupLabel());
              saleItems.add(saleItem);
              total = total.add(subtotal);
          }

        sale.setSubtotal(total);
        sale.setTotal(total);
        BigDecimal commission = total.multiply(source.getCommissionPct())
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        sale.setCommission(commission);
        sale.setItems(saleItems);

        cashSessionRepo.findByRestaurantIdAndStatus(order.getRestaurant().getId(), CashSessionStatus.OPEN)
                .ifPresent(sale::setCashSession);

        sale.setOrderId(orderId);
        saleRepo.save(sale);
        log.info("Venta creada automáticamente desde orden {} para restaurante {}",
                order.getFolio(), order.getRestaurant().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findSaleIdByOrderId(Long orderId) {
        return saleRepo.findByOrderId(orderId).map(Sale::getId);
    }

    @Override
    public void deleteLinkedSale(Long orderId) {
        saleRepo.findByOrderId(orderId).ifPresent(saleRepo::delete);
    }

    private Sale findOrThrow(Long id) {
        return saleRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sale", id));
    }
}
