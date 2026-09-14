package com.nortcali.api.repository;

import com.nortcali.api.entity.ImpersonationAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImpersonationAuditRepository extends JpaRepository<ImpersonationAudit, Long> {
}
