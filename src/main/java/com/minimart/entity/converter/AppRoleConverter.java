package com.minimart.entity.converter;

import com.minimart.entity.AppRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter
public class AppRoleConverter implements AttributeConverter<AppRole, String> {

    private static final String LEGACY_INVENTORY_ROLE = "WAREHOUSE_STAFF";

    @Override
    public String convertToDatabaseColumn(AppRole role) {
        return role == null ? null : role.name();
    }

    @Override
    public AppRole convertToEntityAttribute(String databaseValue) {
        if (databaseValue == null) {
            return null;
        }
        String normalized = databaseValue.trim().toUpperCase(Locale.ROOT);
        if (LEGACY_INVENTORY_ROLE.equals(normalized)) {
            return AppRole.INVENTORY_STAFF;
        }
        return AppRole.valueOf(normalized);
    }
}
