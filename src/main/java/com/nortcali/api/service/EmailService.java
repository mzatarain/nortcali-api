package com.nortcali.api.service;

import java.math.BigDecimal;

/**
 * Fase 5 (plan SaaS): email transaccional. Cada método envía un template HTML (Thymeleaf) por
 * SMTP. Nunca lanza excepción hacia el llamador — un email fallido no debe tumbar el flujo de
 * negocio que lo dispara (registro, webhook de Stripe), igual que el patrón ya usado para la
 * venta auto-creada al entregar una orden.
 */
public interface EmailService {

    void sendWelcomeEmail(String toEmail, String employeeName, String tenantName);

    void sendPaymentSucceeded(String toEmail, String tenantName, String plan, BigDecimal amount, String hostedInvoiceUrl);

    void sendPaymentFailed(String toEmail, String tenantName);
}
