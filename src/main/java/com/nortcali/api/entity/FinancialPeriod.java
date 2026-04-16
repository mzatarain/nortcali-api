package com.nortcali.api.entity;

import com.nortcali.api.entity.converter.PeriodTypeConverter;
import com.nortcali.api.entity.enums.PeriodType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "financial_periods")
public class FinancialPeriod {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = PeriodTypeConverter.class)
    @Column(name = "period_type", nullable = false, length = 10)
    private PeriodType periodType;

    @Column(name = "period_label", nullable = false, length = 30)
    private String periodLabel;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "gross_income", precision = 12, scale = 2)
    private BigDecimal grossIncome = BigDecimal.ZERO;

    @Column(name = "total_commissions", precision = 12, scale = 2)
    private BigDecimal totalCommissions = BigDecimal.ZERO;

    @Column(name = "total_expenses", precision = 12, scale = 2)
    private BigDecimal totalExpenses = BigDecimal.ZERO;

    @Column(name = "net_profit", precision = 12, scale = 2)
    private BigDecimal netProfit = BigDecimal.ZERO;

    @Column(name = "payment_breakdown", columnDefinition = "JSON")
    private String paymentBreakdown;

    @Column(nullable = false, length = 10)
    private String status = "open";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    public FinancialPeriod() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PeriodType getPeriodType() { return periodType; }
    public void setPeriodType(PeriodType periodType) { this.periodType = periodType; }

    public String getPeriodLabel() { return periodLabel; }
    public void setPeriodLabel(String periodLabel) { this.periodLabel = periodLabel; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getGrossIncome() { return grossIncome; }
    public void setGrossIncome(BigDecimal grossIncome) { this.grossIncome = grossIncome; }

    public BigDecimal getTotalCommissions() { return totalCommissions; }
    public void setTotalCommissions(BigDecimal totalCommissions) { this.totalCommissions = totalCommissions; }

    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(BigDecimal totalExpenses) { this.totalExpenses = totalExpenses; }

    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }

    public String getPaymentBreakdown() { return paymentBreakdown; }
    public void setPaymentBreakdown(String paymentBreakdown) { this.paymentBreakdown = paymentBreakdown; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Restaurant getRestaurant() { return restaurant; }
    public void setRestaurant(Restaurant restaurant) { this.restaurant = restaurant; }
}
