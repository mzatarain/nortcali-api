package com.nortcali.api.mapper;

import com.nortcali.api.dto.request.MenuItemVariantRequest;
import com.nortcali.api.dto.response.MenuItemVariantResponse;
import com.nortcali.api.entity.MenuItemVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MenuItemVariantMapper {

    @Mapping(source = "menuItem.id", target = "menuItemId")
    @Mapping(source = "menuItem.name", target = "menuItemName")
    @Mapping(source = "active", target = "isActive")
    MenuItemVariantResponse toResponse(MenuItemVariant entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "menuItem", ignore = true)
    MenuItemVariant toEntity(MenuItemVariantRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "menuItem", ignore = true)
    void updateEntity(MenuItemVariantRequest request, @MappingTarget MenuItemVariant entity);
}
