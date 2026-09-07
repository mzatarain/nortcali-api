package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.CheckoutSessionRequest;
import com.nortcali.api.dto.response.CheckoutSessionResponse;
import com.nortcali.api.dto.response.PortalSessionResponse;
import com.nortcali.api.dto.response.SubscriptionStatusResponse;
import com.nortcali.api.entity.Employee;
import com.nortcali.api.entity.Tenant;
import com.nortcali.api.entity.enums.TenantStatus;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ForbiddenException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.TenantRepository;
import com.nortcali.api.service.BillingService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.billingportal.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.billingportal.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem;
import com.stripe.param.checkout.SessionCreateParams.Mode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
@Slf4j
public class BillingServiceImpl implements BillingService {

    private final TenantRepository tenantRepo;
    private final EmployeeRepository employeeRepo;

    @Value("${stripe.prices.basico}")
    private String priceBasico;

    @Value("${stripe.prices.pro}")
    private String pricePro;

    @Value("${stripe.prices.enterprise}")
    private String priceEnterprise;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public BillingServiceImpl(TenantRepository tenantRepo, EmployeeRepository employeeRepo) {
        this.tenantRepo = tenantRepo;
        this.employeeRepo = employeeRepo;
    }

    @Override
    public CheckoutSessionResponse createCheckoutSession(CheckoutSessionRequest request) {
        Employee employee = resolveAuthenticatedEmployee();
        Tenant tenant = resolveAuthenticatedTenant(employee);
        String priceId = resolvePriceId(request.getPlan());

        ensureStripeCustomer(tenant, employee);

        // Cambio de plan sobre una suscripción ya existente: se modifica in-place en vez de
        // crear una Checkout Session nueva — de lo contrario cada cambio de plan apila una
        // suscripción adicional (y un cobro adicional) sobre el mismo customer en Stripe.
        if (tenant.getStripeSubscriptionId() != null
                && (tenant.getStatus() == TenantStatus.ACTIVE || tenant.getStatus() == TenantStatus.PAST_DUE)) {
            changeSubscriptionPlan(tenant, priceId, request.getPlan());
            return new CheckoutSessionResponse(null, true);
        }

        var params = com.stripe.param.checkout.SessionCreateParams.builder()
                .setMode(Mode.SUBSCRIPTION)
                .setCustomer(tenant.getStripeCustomerId())
                .setClientReferenceId(String.valueOf(tenant.getId()))
                .putMetadata("tenantId", String.valueOf(tenant.getId()))
                .putMetadata("plan", request.getPlan())
                .addLineItem(LineItem.builder().setPrice(priceId).setQuantity(1L).build())
                .setSuccessUrl(frontendUrl + "/settings/billing?checkout=success")
                .setCancelUrl(frontendUrl + "/settings/billing?checkout=cancel")
                .setSubscriptionData(
                        com.stripe.param.checkout.SessionCreateParams.SubscriptionData.builder()
                                .putMetadata("tenantId", String.valueOf(tenant.getId()))
                                .putMetadata("plan", request.getPlan())
                                .build())
                .build();

        try {
            com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(params);
            return new CheckoutSessionResponse(session.getUrl(), false);
        } catch (StripeException e) {
            log.error("Error creando Checkout Session en Stripe para tenant {}: {}", tenant.getId(), e.getMessage(), e);
            throw new BusinessRuleException("No se pudo iniciar el proceso de pago. Intenta de nuevo más tarde.");
        }
    }

    private void changeSubscriptionPlan(Tenant tenant, String priceId, String plan) {
        try {
            Subscription subscription = Subscription.retrieve(tenant.getStripeSubscriptionId());
            String subscriptionItemId = subscription.getItems().getData().get(0).getId();

            var params = SubscriptionUpdateParams.builder()
                    .addItem(SubscriptionUpdateParams.Item.builder()
                            .setId(subscriptionItemId)
                            .setPrice(priceId)
                            .build())
                    .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS)
                    .build();
            subscription.update(params);

            tenant.setPlan(plan);
            tenantRepo.save(tenant);
            log.info("Tenant {} cambió de plan a '{}' (subscription={})", tenant.getId(), plan, tenant.getStripeSubscriptionId());
        } catch (StripeException e) {
            log.error("Error cambiando de plan en Stripe para tenant {}: {}", tenant.getId(), e.getMessage(), e);
            throw new BusinessRuleException("No se pudo cambiar de plan. Intenta de nuevo más tarde.");
        }
    }

    @Override
    public PortalSessionResponse createPortalSession() {
        Tenant tenant = resolveAuthenticatedTenant(resolveAuthenticatedEmployee());
        if (tenant.getStripeCustomerId() == null) {
            throw new BusinessRuleException("Este negocio aún no tiene una suscripción activa en Stripe");
        }

        var params = SessionCreateParams.builder()
                .setCustomer(tenant.getStripeCustomerId())
                .setReturnUrl(frontendUrl + "/settings/billing")
                .build();

        try {
            Session portalSession = Session.create(params);
            return new PortalSessionResponse(portalSession.getUrl());
        } catch (StripeException e) {
            log.error("Error creando Billing Portal Session en Stripe para tenant {}: {}", tenant.getId(), e.getMessage(), e);
            throw new BusinessRuleException("No se pudo abrir el portal de facturación. Intenta de nuevo más tarde.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getSubscriptionStatus() {
        Tenant tenant = resolveAuthenticatedTenant(resolveAuthenticatedEmployee());
        return new SubscriptionStatusResponse(
                tenant.getStatus().name().toLowerCase(),
                tenant.getPlan(),
                tenant.getTrialEndsAt(),
                tenant.getStripeCustomerId() != null);
    }

    @Override
    public void handleWebhookEvent(String payload, String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Webhook de Stripe con firma inválida");
            throw new ForbiddenException("Firma de Stripe inválida");
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "invoice.payment_succeeded" -> applyStatusFromInvoice(event, TenantStatus.ACTIVE);
            case "invoice.payment_failed" -> applyStatusFromInvoice(event, TenantStatus.PAST_DUE);
            case "customer.subscription.deleted" -> applyStatusFromSubscription(event, TenantStatus.CANCELLED);
            default -> log.debug("Evento de Stripe ignorado: {}", event.getType());
        }
    }

    private void handleCheckoutCompleted(Event event) {
        StripeObject obj = deserialize(event);
        if (!(obj instanceof com.stripe.model.checkout.Session session)) {
            log.warn("checkout.session.completed sin objeto Session deserializable");
            return;
        }

        Tenant tenant = resolveTenantForWebhook(session.getClientReferenceId(), session.getCustomer());
        if (tenant == null) {
            log.error("checkout.session.completed: no se encontró tenant (clientReferenceId={}, customer={})",
                    session.getClientReferenceId(), session.getCustomer());
            return;
        }

        tenant.setStripeCustomerId(session.getCustomer());
        tenant.setStripeSubscriptionId(session.getSubscription());
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setTrialEndsAt(null);
        Map<String, String> metadata = session.getMetadata();
        if (metadata != null && metadata.get("plan") != null) {
            tenant.setPlan(metadata.get("plan"));
        }
        tenantRepo.save(tenant);
        log.info("Tenant {} activado tras checkout.session.completed (customer={})", tenant.getId(), session.getCustomer());
    }

    private void applyStatusFromInvoice(Event event, TenantStatus status) {
        StripeObject obj = deserialize(event);
        if (!(obj instanceof Invoice invoice)) {
            log.warn("Evento de invoice sin objeto Invoice deserializable");
            return;
        }
        applyStatus(invoice.getCustomer(), status);
    }

    private void applyStatusFromSubscription(Event event, TenantStatus status) {
        StripeObject obj = deserialize(event);
        if (!(obj instanceof Subscription subscription)) {
            log.warn("Evento de subscription sin objeto Subscription deserializable");
            return;
        }
        // Un customer puede tener más de una suscripción en Stripe (histórico, pruebas, o un
        // cambio de plan que no pasó por changeSubscriptionPlan). Solo la suscripción vigente
        // del tenant (stripe_subscription_id) debe poder cambiar su status — de lo contrario
        // cancelar una suscripción vieja marcaría como cancelado a un tenant que sigue activo.
        tenantRepo.findByStripeCustomerId(subscription.getCustomer()).ifPresent(tenant -> {
            if (!subscription.getId().equals(tenant.getStripeSubscriptionId())) {
                log.info("Evento de subscription {} ignorado para tenant {}: no es su suscripción vigente ({})",
                        subscription.getId(), tenant.getId(), tenant.getStripeSubscriptionId());
                return;
            }
            tenant.setStatus(status);
            tenantRepo.save(tenant);
            log.info("Tenant {} → status {} (subscription={})", tenant.getId(), status, subscription.getId());
        });
    }

    private void applyStatus(String stripeCustomerId, TenantStatus status) {
        if (stripeCustomerId == null) {
            return;
        }
        tenantRepo.findByStripeCustomerId(stripeCustomerId).ifPresentOrElse(
                tenant -> {
                    tenant.setStatus(status);
                    tenantRepo.save(tenant);
                    log.info("Tenant {} → status {} (customer={})", tenant.getId(), status, stripeCustomerId);
                },
                () -> log.warn("Webhook de Stripe: ningún tenant con stripe_customer_id={}", stripeCustomerId));
    }

    private StripeObject deserialize(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        return deserializer.getObject().orElseGet(() -> {
            // La cuenta de Stripe puede tener una versión de API distinta a la que stripe-java
            // trae compilada — getObject() devuelve vacío en ese caso por seguridad. Ya
            // verificamos la firma del webhook, así que forzar la deserialización es seguro.
            try {
                return deserializer.deserializeUnsafe();
            } catch (com.stripe.exception.EventDataObjectDeserializationException e) {
                log.error("No se pudo deserializar el objeto del evento {} ({}): {}",
                        event.getId(), event.getType(), e.getMessage());
                return null;
            }
        });
    }

    private Tenant resolveTenantForWebhook(String clientReferenceId, String stripeCustomerId) {
        Long tenantId = parseLongOrNull(clientReferenceId);
        if (tenantId != null) {
            var byId = tenantRepo.findById(tenantId);
            if (byId.isPresent()) {
                return byId.get();
            }
        }
        return stripeCustomerId == null ? null : tenantRepo.findByStripeCustomerId(stripeCustomerId).orElse(null);
    }

    private static Long parseLongOrNull(String value) {
        try {
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void ensureStripeCustomer(Tenant tenant, Employee employee) {
        if (tenant.getStripeCustomerId() != null) {
            return;
        }
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setName(tenant.getName())
                    .setEmail(employee.getEmail())
                    .putMetadata("tenantId", String.valueOf(tenant.getId()))
                    .build();
            Customer customer = Customer.create(params);
            tenant.setStripeCustomerId(customer.getId());
            tenantRepo.save(tenant);
        } catch (StripeException e) {
            log.error("Error creando Customer en Stripe para tenant {}: {}", tenant.getId(), e.getMessage(), e);
            throw new BusinessRuleException("No se pudo iniciar el proceso de pago. Intenta de nuevo más tarde.");
        }
    }

    private String resolvePriceId(String plan) {
        return switch (plan) {
            case "basico" -> priceBasico;
            case "pro" -> pricePro;
            case "enterprise" -> priceEnterprise;
            default -> throw new BusinessRuleException("Plan inválido: " + plan);
        };
    }

    private Tenant resolveAuthenticatedTenant(Employee employee) {
        return employee.getRestaurant().stream()
                .findFirst()
                .map(r -> r.getTenant())
                .orElseThrow(() -> new BusinessRuleException(
                        "El empleado autenticado no tiene ningún restaurante asignado — no se puede determinar su tenant"));
    }

    private Employee resolveAuthenticatedEmployee() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return employeeRepo.findByEmailWithRestaurants(email)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado autenticado no encontrado: " + email));
    }
}
