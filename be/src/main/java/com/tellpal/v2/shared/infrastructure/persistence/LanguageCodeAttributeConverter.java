package com.tellpal.v2.shared.infrastructure.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.tellpal.v2.shared.domain.LanguageCode;

@Converter(autoApply = true)
public class LanguageCodeAttributeConverter implements AttributeConverter<LanguageCode, String> {

    @Override
    public String convertToDatabaseColumn(LanguageCode attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public LanguageCode convertToEntityAttribute(String dbData) {
        return dbData == null ? null : LanguageCode.from(dbData);
    }
}
