package com.nortcali.api.service;

import com.nortcali.api.dto.request.CheckoutSessionRequest;
import com.nortcali.api.dto.response.CheckoutSessionResponse;
import com.nortcali.api.dto.response.PortalSessionResponse;
import com.nortcali.api.dto.response.SubscriptionStatusResponse;

public interface BillingService {

    CheckoutSessionResponse createCheckoutSession(CheckoutSessionRequest request);

    PortalSessionResponse createPortalSession();

    SubscriptionStatusResponse getSubscriptionStatus();

    /** Marca la suscripción vigente para cancelarse al final del periodo ya pagado — no cancela de inmediato. */
    SubscriptionStatusResponse cancelSubscription();

    /** Valida la firma de Stripe y aplica el evento — no expone el {@code Event} crudo al controller. */
    void handleWebhookEvent(String payload, String signatureHeader);
}
