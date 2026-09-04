package com.nortcali.api.entity.converter;

import com.nortcali.api.entity.enums.TenantStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TenantStatusConverter implements AttributeConverter<TenantStatus, String> {
    @Override
    public String convertToDatabaseColumn(TenantStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }
    @Override
    public TenantStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TenantStatus.valueOf(dbData.toUpperCase());
    }
}
