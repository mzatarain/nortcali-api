package com.nortcali.api.exception;

/**
 * Fase 3 (plan SaaS): tenant en {@code past_due} — acceso de solo lectura. Cualquier operación
 * de escritura (`POST`/`PUT`/`PATCH`/`DELETE`) lanza esta excepción hasta que el pago se
 * regularice y Stripe dispare `invoice.payment_succeeded` (vuelve el tenant a {@code active}).
 */
public class PaymentRequiredException extends RuntimeException {

    public PaymentRequiredException(String message) {
        super(message);
    }
}
