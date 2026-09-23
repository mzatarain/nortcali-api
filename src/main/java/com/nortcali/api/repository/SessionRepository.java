package com.nortcali.api.repository;

import com.nortcali.api.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByTokenAndIsActiveTrue(String token);

    void deleteByToken(String token);

    /** Recuperación de contraseña: cierra toda sesión abierta con la contraseña anterior. */
    List<Session> findByEmployeeIdAndIsActiveTrue(Long employeeId);
}
