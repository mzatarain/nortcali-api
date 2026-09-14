package com.nortcali.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Fase 8 (plan SaaS): registro de auditoría de impersonación — qué superadmin entró como qué
 * empleado y cuándo. Solo IDs planos (sin relaciones JPA): es un log de solo-escritura, no
 * necesita navegación de objetos.
 */
@Entity
@Table(name = "impersonation_audit")
public class ImpersonationAudit {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "superadmin_id", nullable = false)
    private Long superadminId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @PrePersist
    void onCreate() {
        this.startedAt = LocalDateTime.now();
    }

    public ImpersonationAudit() {}

    public Long getId() { return id; }

    public Long getSuperadminId() { return superadminId; }
    public void setSuperadminId(Long superadminId) { this.superadminId = superadminId; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getStartedAt() { return startedAt; }
}
