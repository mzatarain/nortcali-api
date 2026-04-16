package com.nortcali.api.mapper;

import com.nortcali.api.dto.request.CustomerRequest;
import com.nortcali.api.dto.response.CustomerResponse;
import com.nortcali.api.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(source = "active", target = "isActive")
    CustomerResponse toResponse(Customer entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "totalOrders", ignore = true)
    Customer toEntity(CustomerRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "totalOrders", ignore = true)
    void updateEntity(CustomerRequest request, @MappingTarget Customer entity);
}
