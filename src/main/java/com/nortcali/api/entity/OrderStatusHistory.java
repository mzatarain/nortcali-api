package com.nortcali.api.entity;

import com.nortcali.api.entity.converter.OrderStatusConverter;
import com.nortcali.api.entity.enums.OrderStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = OrderStatusConverter.class)
    @Column(name = "from_status", length = 15)
    private OrderStatus fromStatus;

    @Convert(converter = OrderStatusConverter.class)
    @Column(name = "to_status", nullable = false, length = 15)
    private OrderStatus toStatus;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @PrePersist
    void onCreate() {
        this.changedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public OrderStatusHistory() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OrderStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(OrderStatus fromStatus) { this.fromStatus = fromStatus; }

    public OrderStatus getToStatus() { return toStatus; }
    public void setToStatus(OrderStatus toStatus) { this.toStatus = toStatus; }

    public LocalDateTime getChangedAt() { return changedAt; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
}
