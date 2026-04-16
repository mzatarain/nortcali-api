package com.nortcali.api.mapper;

import com.nortcali.api.dto.response.CashSessionItemResponse;
import com.nortcali.api.dto.response.CashSessionResponse;
import com.nortcali.api.entity.CashSession;
import com.nortcali.api.entity.CashSessionItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CashSessionMapper {

    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(source = "openedBy.id", target = "openedBy")
    @Mapping(source = "closedBy.id", target = "closedBy")
    @Mapping(expression = "java(entity.getStatus().name().toLowerCase())", target = "status")
    CashSessionResponse toResponse(CashSession entity);

    @Mapping(expression = "java(entity.getMethod().name().toLowerCase())", target = "method")
    CashSessionItemResponse toItemResponse(CashSessionItem entity);
}
