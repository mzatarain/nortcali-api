package com.nortcali.api.entity.converter;

import com.nortcali.api.entity.enums.TableStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TableStatusConverter implements AttributeConverter<TableStatus, String> {
    @Override
    public String convertToDatabaseColumn(TableStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }
    @Override
    public TableStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TableStatus.valueOf(dbData.toUpperCase());
    }
}
