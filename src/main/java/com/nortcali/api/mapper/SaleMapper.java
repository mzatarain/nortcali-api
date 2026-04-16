package com.nortcali.api.mapper;

import com.nortcali.api.dto.response.SaleItemResponse;
import com.nortcali.api.dto.response.SaleResponse;
import com.nortcali.api.entity.Sale;
import com.nortcali.api.entity.SaleItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SaleMapper {

    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(source = "source.id", target = "sourceId")
    @Mapping(source = "source.name", target = "sourceName")
    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "active", target = "isActive")
    SaleResponse toResponse(Sale entity);

    @Mapping(source = "menuItem.id", target = "menuItemId")
    @Mapping(source = "menuItem.name", target = "menuItemName")
    @Mapping(source = "variant.id", target = "variantId")
    @Mapping(source = "variant.name", target = "variantName")
    SaleItemResponse toItemResponse(SaleItem entity);
}
