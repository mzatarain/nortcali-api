package com.nortcali.api.entity.converter;

import com.nortcali.api.entity.enums.PromotionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PromotionTypeConverter implements AttributeConverter<PromotionType, String> {
    @Override
    public String convertToDatabaseColumn(PromotionType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PromotionType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PromotionType.fromValue(dbData);
    }
}
