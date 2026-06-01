package com.nortcali.api.repository;

import com.nortcali.api.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    Optional<Recipe> findByMenuItemIdAndIsActiveTrue(Long menuItemId);

    Optional<Recipe> findByMenuItemIdAndVariantIdIsNullAndIsActiveTrue(Long menuItemId);

    Optional<Recipe> findByMenuItemIdAndVariantIdAndIsActiveTrue(Long menuItemId, Long variantId);

    List<Recipe> findAllByMenuItemIdAndIsActiveTrue(Long menuItemId);
}
