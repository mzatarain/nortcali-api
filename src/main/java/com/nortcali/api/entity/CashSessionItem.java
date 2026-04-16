package com.nortcali.api.entity;

import com.nortcali.api.entity.converter.PaymentMethodConverter;
import com.nortcali.api.entity.enums.PaymentMethod;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cash_session_items")
public class CashSessionItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = PaymentMethodConverter.class)
    @Column(nullable = false, length = 25)
    private PaymentMethod method;

    @Column(name = "expected_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal expectedAmount = BigDecimal.ZERO;

    @Column(name = "counted_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal countedAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal difference = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_session_id", nullable = false)
    private CashSession cashSession;

    public CashSessionItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }

    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }

    public BigDecimal getCountedAmount() { return countedAmount; }
    public void setCountedAmount(BigDecimal countedAmount) { this.countedAmount = countedAmount; }

    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal difference) { this.difference = difference; }

    public CashSession getCashSession() { return cashSession; }
    public void setCashSession(CashSession cashSession) { this.cashSession = cashSession; }
}
