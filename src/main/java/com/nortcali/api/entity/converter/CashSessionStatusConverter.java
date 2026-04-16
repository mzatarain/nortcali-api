package com.nortcali.api.entity.converter;

import com.nortcali.api.entity.enums.CashSessionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CashSessionStatusConverter implements AttributeConverter<CashSessionStatus, String> {
    @Override
    public String convertToDatabaseColumn(CashSessionStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }
    @Override
    public CashSessionStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CashSessionStatus.valueOf(dbData.toUpperCase());
    }
}
