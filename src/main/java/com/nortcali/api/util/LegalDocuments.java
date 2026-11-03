package com.nortcali.api.util;

/**
 * Versiones vigentes de los documentos legales publicados en nortcali-management
 * ({@code /terminos} y {@code /privacidad}). La versión es la fecha de "última actualización"
 * que muestra cada página — al cambiar el texto de un documento, actualizar aquí la constante
 * correspondiente en el mismo commit (src/pages/legal/legalVersions.ts en el frontend).
 */
public final class LegalDocuments {

    public static final String TERMS_VERSION = "2026-11-03";
    public static final String PRIVACY_VERSION = "2026-11-03";

    private LegalDocuments() {}
}
