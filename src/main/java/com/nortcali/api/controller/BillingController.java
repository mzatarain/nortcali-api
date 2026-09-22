package com.nortcali.api.controller;

import com.nortcali.api.dto.request.CheckoutSessionRequest;
import com.nortcali.api.dto.response.CheckoutSessionResponse;
import com.nortcali.api.dto.response.PortalSessionResponse;
import com.nortcali.api.dto.response.SubscriptionStatusResponse;
import com.nortcali.api.service.BillingService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/billing")
@Slf4j
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    /**
     * Fase 3 (plan SaaS): restringido a ADMIN/TENANT_ADMIN — el primer uso real de
     * TENANT_ADMIN (catálogo agregado en Fase 2, sin consumidor hasta ahora).
     */
    @PostMapping("/checkout-session")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(@Valid @RequestBody CheckoutSessionRequest request) {
        return ResponseEntity.ok(billingService.createCheckoutSession(request));
    }

    @PostMapping("/portal-session")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<PortalSessionResponse> createPortalSession() {
        return ResponseEntity.ok(billingService.createPortalSession());
    }

    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionStatusResponse> getSubscriptionStatus() {
        return ResponseEntity.ok(billingService.getSubscriptionStatus());
    }

    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<SubscriptionStatusResponse> cancelSubscription() {
        return ResponseEntity.ok(billingService.cancelSubscription());
    }

    /**
     * Público a nivel Spring Security (`permitAll` en {@code SecurityConfig}) — Stripe no manda
     * JWT. La autenticidad de la request se valida por firma (`Stripe-Signature`), no por sesión.
     * `@RequestBody String` (no un DTO) porque `Webhook.constructEvent` necesita el JSON crudo
     * exacto que Stripe firmó — deserializarlo primero rompería la verificación de firma.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        billingService.handleWebhookEvent(payload, signature);
        return ResponseEntity.ok().build();
    }
}
