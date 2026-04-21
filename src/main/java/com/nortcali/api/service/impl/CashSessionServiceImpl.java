package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.CloseCashSessionRequest;
import com.nortcali.api.dto.request.OpenCashSessionRequest;
import com.nortcali.api.dto.response.CashSessionResponse;
import com.nortcali.api.entity.CashSession;
import com.nortcali.api.entity.CashSessionItem;
import com.nortcali.api.entity.enums.CashSessionStatus;
import com.nortcali.api.entity.enums.PaymentMethod;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.CashSessionMapper;
import com.nortcali.api.repository.*;
import com.nortcali.api.service.CashSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@Transactional
@Slf4j
public class CashSessionServiceImpl implements CashSessionService {

    private final CashSessionRepository sessionRepo;
    private final RestaurantRepository restaurantRepo;
    private final EmployeeRepository employeeRepo;
    private final SaleRepository saleRepo;
    private final ExpenseRepository expenseRepo;
    private final IncomeRepository incomeRepo;
    private final CashSessionMapper mapper;

    public CashSessionServiceImpl(CashSessionRepository sessionRepo,
                                   RestaurantRepository restaurantRepo,
                                   EmployeeRepository employeeRepo,
                                   SaleRepository saleRepo,
                                   ExpenseRepository expenseRepo,
                                   IncomeRepository incomeRepo,
                                   CashSessionMapper mapper) {
        this.sessionRepo = sessionRepo;
        this.restaurantRepo = restaurantRepo;
        this.employeeRepo = employeeRepo;
        this.saleRepo = saleRepo;
        this.expenseRepo = expenseRepo;
        this.incomeRepo = incomeRepo;
        this.mapper = mapper;
    }

    @Override
    public CashSessionResponse open(Long restaurantId, OpenCashSessionRequest request) {
        restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        // Regla de negocio: solo una sesión abierta por restaurante
        if (sessionRepo.existsByRestaurantIdAndStatus(restaurantId, CashSessionStatus.OPEN)) {
            throw new BusinessRuleException("Ya existe una sesión de caja abierta para este restaurante");
        }

        var employee = employeeRepo.findById(request.getOpenedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.getOpenedBy()));

        CashSession session = new CashSession();
        session.setRestaurant(restaurantRepo.getReferenceById(restaurantId));
        session.setOpenedBy(employee);
        session.setOpeningAmount(request.getOpeningAmount());
        session.setStatus(CashSessionStatus.OPEN);

        log.info("Abriendo sesión de caja para restaurante {}", restaurantId);
        return mapper.toResponse(sessionRepo.save(session));
    }

    @Override
    public CashSessionResponse close(Long sessionId, CloseCashSessionRequest request) {
        CashSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("CashSession", sessionId));

        if (session.getStatus() != CashSessionStatus.OPEN) {
            throw new BusinessRuleException("La sesión de caja no está abierta");
        }

        var closedBy = employeeRepo.findById(request.getClosedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.getClosedBy()));

        LocalDate today = LocalDate.now();
        Long restaurantId = session.getRestaurant().getId();

        // Calcular totales del día
        BigDecimal totalSales = saleRepo.sumByRestaurantAndPeriod(restaurantId, today, today);
        BigDecimal totalExpenses = expenseRepo.sumByRestaurantAndPeriod(restaurantId, today, today);
        BigDecimal totalIncomes = incomeRepo.sumByRestaurantAndPeriod(restaurantId, today, today);

        session.setTotalSales(totalSales);
        session.setTotalExpenses(totalExpenses);
        session.setTotalIncomes(totalIncomes);

        // Registrar conteos por método de pago
        BigDecimal totalCounted = BigDecimal.ZERO;
        for (var countDto : request.getCountedAmounts()) {
            PaymentMethod method = PaymentMethod.valueOf(countDto.getMethod().toUpperCase());
            CashSessionItem item = new CashSessionItem();
            item.setCashSession(session);
            item.setMethod(method);
            item.setExpectedAmount(BigDecimal.ZERO); // simplificado — en producción consultar pagos reales
            item.setCountedAmount(countDto.getCountedAmount());
            item.setDifference(countDto.getCountedAmount().subtract(item.getExpectedAmount()));
            session.getItems().add(item);
            totalCounted = totalCounted.add(countDto.getCountedAmount());
        }

        session.setCountedCash(totalCounted);
        session.setExpectedCash(totalSales);
        session.setDifference(totalCounted.subtract(session.getExpectedCash()));
        session.setStatus(CashSessionStatus.CLOSED);
        session.setClosedBy(closedBy);
        session.setClosedAt(LocalDateTime.now(ZoneOffset.UTC));

        log.info("Cerrando sesión de caja {} para restaurante {}", sessionId, restaurantId);
        return mapper.toResponse(sessionRepo.save(session));
    }

    @Override
    @Transactional(readOnly = true)
    public CashSessionResponse getCurrent(Long restaurantId) {
        CashSession session = sessionRepo.findByRestaurantIdAndStatus(restaurantId, CashSessionStatus.OPEN)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay sesión de caja abierta para el restaurante " + restaurantId));
        // totalSales se calcula dinámicamente para reflejar ventas acumuladas en la sesión activa
        session.setTotalSales(saleRepo.sumTotalByCashSessionId(session.getId()));
        return mapper.toResponse(session);
    }
}
