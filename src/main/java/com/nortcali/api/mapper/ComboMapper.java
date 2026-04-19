package com.nortcali.api.mapper;

import com.nortcali.api.dto.response.ComboItemResponse;
import com.nortcali.api.dto.response.ComboResponse;
import com.nortcali.api.entity.Combo;
import com.nortcali.api.entity.ComboItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ComboMapper {

    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(source = "active", target = "isActive")
    ComboResponse toResponse(Combo entity);

    @Mapping(source = "menuItem.id", target = "menuItemId")
    @Mapping(source = "menuItem.name", target = "menuItemName")
    @Mapping(source = "variant.id", target = "variantId")
    @Mapping(source = "variant.name", target = "variantName")
    ComboItemResponse toItemResponse(ComboItem entity);
}
