package com.nortcali.api.mapper;

import com.nortcali.api.dto.request.DeliveryDriverRequest;
import com.nortcali.api.dto.response.DeliveryDriverResponse;
import com.nortcali.api.entity.DeliveryDriver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DeliveryDriverMapper {

    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(source = "active", target = "isActive")
    DeliveryDriverResponse toResponse(DeliveryDriver entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    DeliveryDriver toEntity(DeliveryDriverRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    void updateEntity(DeliveryDriverRequest request, @MappingTarget DeliveryDriver entity);
}
