package com.nortcali.api.entity.converter;

import com.nortcali.api.entity.enums.OrderType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class OrderTypeConverter implements AttributeConverter<OrderType, String> {
    @Override
    public String convertToDatabaseColumn(OrderType attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }
    @Override
    public OrderType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OrderType.valueOf(dbData.toUpperCase());
    }
}
