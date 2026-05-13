package com.nortcali.api.mapper;

import com.nortcali.api.dto.response.*;
import com.nortcali.api.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(expression = "java(entity.getOrderType().name().toLowerCase())", target = "orderType")
    @Mapping(expression = "java(entity.getSource().name().toLowerCase())", target = "source")
    @Mapping(expression = "java(entity.getStatus().name().toLowerCase())", target = "status")
    @Mapping(expression = "java(entity.getPaymentMethod() != null ? entity.getPaymentMethod().name().toLowerCase() : null)", target = "paymentMethod")
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.firstName", target = "customerFirstName")
    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "employee.username", target = "employeeUsername")
    @Mapping(source = "driver.id", target = "driverId")
    @Mapping(source = "driver.firstName", target = "driverFirstName")
    @Mapping(target = "saleId", ignore = true)
    OrderResponse toResponse(Order entity);

    default OrderResponse toResponse(Order entity, Long saleId) {
        OrderResponse r = toResponse(entity);
        return new OrderResponse(r.id(), r.restaurantId(), r.folio(), r.orderType(), r.source(),
                r.status(), r.total(), r.paymentMethod(), r.customerId(), r.customerFirstName(),
                r.employeeId(), r.employeeUsername(), r.driverId(), r.driverFirstName(),
                r.createdAt(), r.preparingAt(), r.readyAt(), r.preparationTimeSeconds(), r.items(), saleId, r.notes());
    }

    @Mapping(source = "menuItem.id", target = "menuItemId")
    @Mapping(source = "menuItem.name", target = "menuItemName")
    @Mapping(source = "variant.id", target = "variantId")
    @Mapping(source = "variant.name", target = "variantName")
    OrderItemResponse toItemResponse(OrderItem entity);

    @Mapping(source = "modifier.id", target = "modifierId")
    OrderItemModifierResponse toModifierResponse(OrderItemModifier entity);

    @Mapping(source = "order.id", target = "id")
    @Mapping(expression = "java(entity.getFromStatus() != null ? entity.getFromStatus().name().toLowerCase() : null)", target = "fromStatus")
    @Mapping(expression = "java(entity.getToStatus().name().toLowerCase())", target = "toStatus")
    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "employee.username", target = "employeeUsername")
    OrderStatusHistoryResponse toHistoryResponse(OrderStatusHistory entity);

    @Mapping(expression = "java(entity.getMethod().name().toLowerCase())", target = "method")
    @Mapping(source = "registeredBy.id", target = "registeredBy")
    PaymentResponse toPaymentResponse(Payment entity);
}
