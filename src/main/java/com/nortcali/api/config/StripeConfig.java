package com.nortcali.api.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Fase 3 (plan SaaS): el SDK de Stripe usa un campo estático global (`Stripe.apiKey`) en vez de
 * un cliente inyectable — se setea una sola vez al arrancar el contexto.
 *
 * <p>En el perfil {@code prod} valida además que la configuración sea de modo LIVE y esté bien
 * formada — falla el arranque en vez de descubrir en el primer cobro real que se desplegó una
 * llave de test, un price id de la cuenta de test o un frontend-url con varios orígenes.
 */
@Component
public class StripeConfig {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${stripe.prices.basico}")
    private String priceBasico;

    @Value("${stripe.prices.pro}")
    private String pricePro;

    @Value("${stripe.prices.enterprise}")
    private String priceEnterprise;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final Environment environment;

    public StripeConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void init() {
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            validateLiveConfig();
        }
        Stripe.apiKey = secretKey;
    }

    private void validateLiveConfig() {
        List<String> errors = new ArrayList<>();

        if (secretKey == null || !(secretKey.startsWith("sk_live_") || secretKey.startsWith("rk_live_"))) {
            errors.add("STRIPE_SECRET_KEY debe ser una llave LIVE (sk_live_... o rk_live_...)");
        }
        if (webhookSecret == null || !webhookSecret.startsWith("whsec_")) {
            errors.add("STRIPE_WEBHOOK_SECRET debe empezar con whsec_");
        }
        Map<String, String> prices = Map.of(
                "STRIPE_PRICE_BASICO", nullToEmpty(priceBasico),
                "STRIPE_PRICE_PRO", nullToEmpty(pricePro),
                "STRIPE_PRICE_ENTERPRISE", nullToEmpty(priceEnterprise));
        prices.forEach((name, value) -> {
            if (!value.startsWith("price_")) {
                errors.add(name + " debe ser un price id (price_...)");
            }
        });
        if (frontendUrl == null || !frontendUrl.startsWith("https://") || frontendUrl.contains(",")
                || frontendUrl.endsWith("/")) {
            errors.add("APP_FRONTEND_URL debe ser un único origen https sin '/' final "
                    + "(ej. https://app.nortcali.com) — se usa para success_url/cancel_url/return_url de Stripe");
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                    "Configuración de Stripe inválida para producción:\n - " + String.join("\n - ", errors));
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
