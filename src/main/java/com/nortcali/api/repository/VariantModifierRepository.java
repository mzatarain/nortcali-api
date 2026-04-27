package com.nortcali.api.repository;

import com.nortcali.api.entity.VariantModifier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VariantModifierRepository extends JpaRepository<VariantModifier, Long> {
    List<VariantModifier> findByVariantId(Long variantId);
    Optional<VariantModifier> findByVariantIdAndModifierId(Long variantId, Long modifierId);
    void deleteByVariantIdAndModifierId(Long variantId, Long modifierId);
}
