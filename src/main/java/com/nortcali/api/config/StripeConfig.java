package com.nortcali.api.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fase 3 (plan SaaS): el SDK de Stripe usa un campo estático global (`Stripe.apiKey`) en vez de
 * un cliente inyectable — se setea una sola vez al arrancar el contexto.
 */
@Component
public class StripeConfig {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @PostConstruct
    void init() {
        Stripe.apiKey = secretKey;
    }
}
