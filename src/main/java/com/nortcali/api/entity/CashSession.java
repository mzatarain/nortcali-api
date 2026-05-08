package com.nortcali.api.entity;

import com.nortcali.api.entity.converter.CashSessionStatusConverter;
import com.nortcali.api.entity.enums.CashSessionStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cash_sessions")
public class CashSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "opening_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal openingAmount = BigDecimal.ZERO;

    @Column(name = "expected_cash", precision = 10, scale = 2)
    private BigDecimal expectedCash = BigDecimal.ZERO;

    @Column(name = "counted_cash", precision = 10, scale = 2)
    private BigDecimal countedCash = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal difference = BigDecimal.ZERO;

    @Column(name = "total_sales", precision = 10, scale = 2)
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Column(name = "total_expenses", precision = 10, scale = 2)
    private BigDecimal totalExpenses = BigDecimal.ZERO;

    @Column(name = "total_incomes", precision = 10, scale = 2)
    private BigDecimal totalIncomes = BigDecimal.ZERO;

    @Convert(converter = CashSessionStatusConverter.class)
    @Column(nullable = false, length = 10)
    private CashSessionStatus status;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_by", nullable = false)
    private Employee openedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private Employee closedBy;

    @OneToMany(mappedBy = "cashSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CashSessionItem> items = new ArrayList<>();

    @PrePersist
    void onCreate() {
        this.openedAt = LocalDateTime.now();
    }

    public CashSession() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getOpeningAmount() { return openingAmount; }
    public void setOpeningAmount(BigDecimal openingAmount) { this.openingAmount = openingAmount; }

    public BigDecimal getExpectedCash() { return expectedCash; }
    public void setExpectedCash(BigDecimal expectedCash) { this.expectedCash = expectedCash; }

    public BigDecimal getCountedCash() { return countedCash; }
    public void setCountedCash(BigDecimal countedCash) { this.countedCash = countedCash; }

    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal difference) { this.difference = difference; }

    public BigDecimal getTotalSales() { return totalSales; }
    public void setTotalSales(BigDecimal totalSales) { this.totalSales = totalSales; }

    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(BigDecimal totalExpenses) { this.totalExpenses = totalExpenses; }

    public BigDecimal getTotalIncomes() { return totalIncomes; }
    public void setTotalIncomes(BigDecimal totalIncomes) { this.totalIncomes = totalIncomes; }

    public CashSessionStatus getStatus() { return status; }
    public void setStatus(CashSessionStatus status) { this.status = status; }

    public LocalDateTime getOpenedAt() { return openedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public Restaurant getRestaurant() { return restaurant; }
    public void setRestaurant(Restaurant restaurant) { this.restaurant = restaurant; }

    public Employee getOpenedBy() { return openedBy; }
    public void setOpenedBy(Employee openedBy) { this.openedBy = openedBy; }

    public Employee getClosedBy() { return closedBy; }
    public void setClosedBy(Employee closedBy) { this.closedBy = closedBy; }

    public List<CashSessionItem> getItems() { return items; }
    public void setItems(List<CashSessionItem> items) { this.items = items; }
}
