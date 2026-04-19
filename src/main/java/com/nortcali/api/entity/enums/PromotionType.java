package com.nortcali.api.entity.enums;

public enum PromotionType {
    PORCENTAJE("porcentaje"),
    PRECIO_FIJO("precio_fijo"),
    DOS_X_UNO("2x1");

    private final String value;

    PromotionType(String value) { this.value = value; }

    public String getValue() { return value; }

    public static PromotionType fromValue(String value) {
        for (PromotionType t : values()) {
            if (t.value.equals(value)) return t;
        }
        throw new IllegalArgumentException("Unknown promotion type: " + value);
    }
}
