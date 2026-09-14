package com.nortcali.api.repository;

import com.nortcali.api.entity.Superadmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuperadminRepository extends JpaRepository<Superadmin, Long> {

    Optional<Superadmin> findByEmail(String email);
}
