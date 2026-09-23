package com.nortcali.api.service;

/**
 * Recuperación de contraseña desde el login. Token de un solo uso, expira a la hora de
 * generarse (ver {@code PasswordResetServiceImpl}).
 */
public interface PasswordResetService {

    /** No revela si el email existe — misma respuesta siempre, para no filtrar cuentas existentes. */
    void requestReset(String email);

    void resetPassword(String token, String newPassword);
}
