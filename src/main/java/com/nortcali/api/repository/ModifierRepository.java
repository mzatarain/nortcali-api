package com.nortcali.api.repository;

import com.nortcali.api.entity.Modifier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModifierRepository extends JpaRepository<Modifier, Long> {
    List<Modifier> findByGroupId(Long groupId);
}
