package com.nortcali.api.repository;

import com.nortcali.api.entity.MenuItemVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemVariantRepository extends JpaRepository<MenuItemVariant, Long> {

    List<MenuItemVariant> findByMenuItemIdAndIsActiveTrue(Long menuItemId);
}
