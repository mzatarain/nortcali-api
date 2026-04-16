package com.nortcali.api.entity.converter;

import com.nortcali.api.entity.enums.PeriodType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PeriodTypeConverter implements AttributeConverter<PeriodType, String> {
    @Override
    public String convertToDatabaseColumn(PeriodType attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }
    @Override
    public PeriodType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PeriodType.valueOf(dbData.toUpperCase());
    }
}
