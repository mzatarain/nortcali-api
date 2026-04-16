package com.nortcali.api.mapper;

import com.nortcali.api.dto.request.MenuCategoryRequest;
import com.nortcali.api.dto.response.MenuCategoryResponse;
import com.nortcali.api.entity.MenuCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MenuCategoryMapper {

    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(source = "active", target = "isActive")
    MenuCategoryResponse toResponse(MenuCategory entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    MenuCategory toEntity(MenuCategoryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    void updateEntity(MenuCategoryRequest request, @MappingTarget MenuCategory entity);
}
