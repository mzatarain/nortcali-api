package com.nortcali.api.mapper;

import com.nortcali.api.dto.request.MenuItemRequest;
import com.nortcali.api.dto.response.MenuItemResponse;
import com.nortcali.api.entity.MenuItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MenuItemMapper {

    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "active", target = "isActive")
    MenuItemResponse toResponse(MenuItem entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "category", ignore = true)
    MenuItem toEntity(MenuItemRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "category", ignore = true)
    void updateEntity(MenuItemRequest request, @MappingTarget MenuItem entity);
}
