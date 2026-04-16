package com.nortcali.api.entity.converter;

import com.nortcali.api.entity.enums.MovementType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MovementTypeConverter implements AttributeConverter<MovementType, String> {

    @Override
    public String convertToDatabaseColumn(MovementType attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public MovementType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MovementType.valueOf(dbData.toUpperCase());
    }
}
