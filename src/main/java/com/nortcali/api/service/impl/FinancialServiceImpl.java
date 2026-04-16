package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.FinancialPeriodRequest;
import com.nortcali.api.dto.response.FinancialPeriodResponse;
import com.nortcali.api.dto.response.FinancialSummaryResponse;
import com.nortcali.api.entity.FinancialPeriod;
import com.nortcali.api.entity.enums.PeriodType;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.FinancialPeriodMapper;
import com.nortcali.api.repository.*;
import com.nortcali.api.service.FinancialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@Transactional
@Slf4j
public class FinancialServiceImpl implements FinancialService {

    private final FinancialPeriodRepository periodRepo;
    private final RestaurantRepository restaurantRepo;
    private final SaleRepository saleRepo;
    private final ExpenseRepository expenseRepo;
    private final IncomeRepository incomeRepo;
    private final FinancialPeriodMapper mapper;

    public FinancialServiceImpl(FinancialPeriodRepository periodRepo,
                                RestaurantRepository restaurantRepo,
                                SaleRepository saleRepo,
                                ExpenseRepository expenseRepo,
                                IncomeRepository incomeRepo,
                                FinancialPeriodMapper mapper) {
        this.periodRepo = periodRepo;
        this.restaurantRepo = restaurantRepo;
        this.saleRepo = saleRepo;
        this.expenseRepo = expenseRepo;
        this.incomeRepo = incomeRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialSummaryResponse getSummary(Long restaurantId, String period, String dateParam) {
        restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        LocalDate from;
        LocalDate to;

        if ("daily".equalsIgnoreCase(period)) {
            from = LocalDate.parse(dateParam);
            to = from;
        } else if ("monthly".equalsIgnoreCase(period)) {
            YearMonth ym = YearMonth.parse(dateParam);
            from = ym.atDay(1);
            to = ym.atEndOfMonth();
        } else {
            throw new BusinessRuleException("Período inválido. Use 'daily' o 'monthly'");
        }

        BigDecimal grossIncome = saleRepo.sumByRestaurantAndPeriod(restaurantId, from, to);
        BigDecimal totalCommissions = saleRepo.sumCommissionByRestaurantAndPeriod(restaurantId, from, to);
        BigDecimal totalExpenses = expenseRepo.sumByRestaurantAndPeriod(restaurantId, from, to);
        BigDecimal totalIncomes = incomeRepo.sumByRestaurantAndPeriod(restaurantId, from, to);

        // Regla de negocio: netProfit = grossIncome + totalIncomes - totalCommissions - totalExpenses
        BigDecimal netProfit = grossIncome
                .add(totalIncomes)
                .subtract(totalCommissions)
                .subtract(totalExpenses);

        return new FinancialSummaryResponse(from, to, period, grossIncome, totalCommissions,
                totalExpenses, totalIncomes, netProfit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialPeriodResponse> getPeriods(Long restaurantId) {
        return periodRepo.findByRestaurantIdOrderByStartDateDesc(restaurantId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    public FinancialPeriodResponse createPeriod(Long restaurantId, FinancialPeriodRequest request) {
        restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        PeriodType type;
        try {
            type = PeriodType.valueOf(request.getPeriodType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Tipo de período inválido: " + request.getPeriodType());
        }

        LocalDate from = request.getStartDate();
        LocalDate to = request.getEndDate();

        BigDecimal grossIncome = saleRepo.sumByRestaurantAndPeriod(restaurantId, from, to);
        BigDecimal totalCommissions = saleRepo.sumCommissionByRestaurantAndPeriod(restaurantId, from, to);
        BigDecimal totalExpenses = expenseRepo.sumByRestaurantAndPeriod(restaurantId, from, to);
        BigDecimal totalIncomes = incomeRepo.sumByRestaurantAndPeriod(restaurantId, from, to);
        BigDecimal netProfit = grossIncome.add(totalIncomes).subtract(totalCommissions).subtract(totalExpenses);

        FinancialPeriod entity = new FinancialPeriod();
        entity.setRestaurant(restaurantRepo.getReferenceById(restaurantId));
        entity.setPeriodType(type);
        entity.setPeriodLabel(request.getPeriodLabel());
        entity.setStartDate(from);
        entity.setEndDate(to);
        entity.setGrossIncome(grossIncome);
        entity.setTotalCommissions(totalCommissions);
        entity.setTotalExpenses(totalExpenses);
        entity.setNetProfit(netProfit);
        entity.setPaymentBreakdown("{}");
        entity.setStatus("open");

        return mapper.toResponse(periodRepo.save(entity));
    }

    @Override
    public FinancialPeriodResponse closePeriod(Long periodId) {
        FinancialPeriod entity = periodRepo.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialPeriod", periodId));
        if ("closed".equals(entity.getStatus())) {
            throw new BusinessRuleException("El período ya está cerrado");
        }
        entity.setStatus("closed");
        return mapper.toResponse(periodRepo.save(entity));
    }
}
