package com.nortcali.api.mapper;

import com.nortcali.api.dto.response.PromotionItemResponse;
import com.nortcali.api.dto.response.PromotionResponse;
import com.nortcali.api.entity.Promotion;
import com.nortcali.api.entity.PromotionItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PromotionMapper {

    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(expression = "java(entity.getType().getValue())", target = "type")
    @Mapping(source = "active", target = "isActive")
    PromotionResponse toResponse(Promotion entity);

    @Mapping(source = "menuItem.id", target = "menuItemId")
    @Mapping(source = "menuItem.name", target = "menuItemName")
    @Mapping(source = "variant.id", target = "variantId")
    @Mapping(source = "variant.name", target = "variantName")
    PromotionItemResponse toItemResponse(PromotionItem entity);
}
