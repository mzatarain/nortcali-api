package com.nortcali.api.entity.converter;

import com.nortcali.api.entity.enums.OrderSource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class OrderSourceConverter implements AttributeConverter<OrderSource, String> {
    @Override
    public String convertToDatabaseColumn(OrderSource attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }
    @Override
    public OrderSource convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OrderSource.valueOf(dbData.toUpperCase());
    }
}
