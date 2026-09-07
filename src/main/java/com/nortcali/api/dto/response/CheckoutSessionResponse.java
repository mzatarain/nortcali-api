package com.nortcali.api.dto.response;

/**
 * {@code url} es {@code null} cuando el cambio de plan se aplicó de inmediato sobre una
 * suscripción existente (ver {@code immediate}) — no hay a dónde redirigir porque Stripe
 * ya tiene el método de pago del tenant.
 */
public record CheckoutSessionResponse(String url, boolean immediate) {}
