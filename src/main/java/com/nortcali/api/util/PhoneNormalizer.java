package com.nortcali.api.util;

import com.nortcali.api.exception.BusinessRuleException;

/**
 * Normaliza teléfonos de clientes a 10 dígitos para que "686 227 9910", "(686) 227-9910"
 * y "+52 686 227 9910" se guarden igual y la búsqueda por teléfono no genere duplicados.
 */
public final class PhoneNormalizer {

    /** Valor que el frontend envía cuando el cliente no tiene teléfono. */
    public static final String NO_PHONE_PLACEHOLDER = "SIN-TELEFONO";

    private static final int LOCAL_LENGTH = 10;

    private PhoneNormalizer() {}

    /**
     * @return los 10 dígitos del teléfono, o {@link #NO_PHONE_PLACEHOLDER} si viene vacío.
     * @throws BusinessRuleException si no quedan exactamente 10 dígitos.
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank() || raw.trim().equalsIgnoreCase(NO_PHONE_PLACEHOLDER)) {
            return NO_PHONE_PLACEHOLDER;
        }
        String digits = raw.replaceAll("\\D", "");
        // Lada internacional de México: +52 y el antiguo prefijo de celular +521
        if (digits.length() == LOCAL_LENGTH + 3 && digits.startsWith("521")) {
            digits = digits.substring(3);
        } else if (digits.length() == LOCAL_LENGTH + 2 && digits.startsWith("52")) {
            digits = digits.substring(2);
        }
        if (digits.length() != LOCAL_LENGTH) {
            throw new BusinessRuleException("El teléfono debe tener 10 dígitos");
        }
        return digits;
    }

    public static boolean isPlaceholder(String phone) {
        return NO_PHONE_PLACEHOLDER.equals(phone);
    }
}
