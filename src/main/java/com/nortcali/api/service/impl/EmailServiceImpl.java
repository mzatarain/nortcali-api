package com.nortcali.api.service.impl;

import com.nortcali.api.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final Map<String, String> PLAN_LABELS = Map.of(
            "basico", "Básico",
            "pro", "Pro",
            "enterprise", "Enterprise");

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${email.enabled:false}")
    private boolean enabled;

    @Value("${email.from}")
    private String fromAddress;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String employeeName, String tenantName) {
        Context ctx = baseContext();
        ctx.setVariable("employeeName", employeeName);
        ctx.setVariable("tenantName", tenantName);
        send(toEmail, "¡Bienvenido a NortCali!", "email/welcome", ctx);
    }

    @Override
    public void sendPaymentSucceeded(String toEmail, String tenantName, String plan, BigDecimal amount, String hostedInvoiceUrl) {
        Context ctx = baseContext();
        ctx.setVariable("tenantName", tenantName);
        ctx.setVariable("planLabel", planLabel(plan));
        ctx.setVariable("amount", formatMxn(amount));
        ctx.setVariable("hostedInvoiceUrl", hostedInvoiceUrl);
        send(toEmail, "Pago recibido — NortCali", "email/payment-succeeded", ctx);
    }

    @Override
    public void sendPaymentFailed(String toEmail, String tenantName) {
        Context ctx = baseContext();
        ctx.setVariable("tenantName", tenantName);
        send(toEmail, "Problema con tu pago — NortCali", "email/payment-failed", ctx);
    }

    @Override
    public void sendTrialEndingSoon(String toEmail, String tenantName, LocalDateTime trialEndsAt) {
        Context ctx = baseContext();
        ctx.setVariable("tenantName", tenantName);
        ctx.setVariable("trialEndsAt", formatDate(trialEndsAt));
        send(toEmail, "Tu prueba gratuita termina pronto — NortCali", "email/trial-ending", ctx);
    }

    @Override
    public void sendInactivityAlert(String toEmail, String tenantName, long daysInactive) {
        Context ctx = baseContext();
        ctx.setVariable("tenantName", tenantName);
        ctx.setVariable("daysInactive", daysInactive);
        send(toEmail, "Tenant inactivo — " + tenantName, "email/inactivity-alert", ctx);
    }

    @Override
    public void sendPasswordReset(String toEmail, String employeeName, String resetLink) {
        Context ctx = baseContext();
        ctx.setVariable("employeeName", employeeName);
        ctx.setVariable("resetLink", resetLink);
        send(toEmail, "Recupera tu contraseña — NortCali", "email/password-reset", ctx);
    }

    private Context baseContext() {
        Context ctx = new Context();
        ctx.setVariable("frontendUrl", frontendUrl);
        return ctx;
    }

    private String planLabel(String plan) {
        return plan == null ? "" : PLAN_LABELS.getOrDefault(plan, plan);
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.of("es", "MX")));
    }

    /** Los precios de Stripe están en MXN; amountPaid/amountDue llegan en centavos. */
    private String formatMxn(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        BigDecimal pesos = amount.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return String.format(Locale.of("es", "MX"), "$%,.2f MXN", pesos);
    }

    private void send(String to, String subject, String template, Context context) {
        if (!enabled) {
            log.info("email.enabled=false — se omite envío de '{}' a {}", template, to);
            return;
        }
        try {
            String html = templateEngine.process(template, context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(to);
            helper.setFrom(fromAddress);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Error enviando email '{}' a {}: {}", template, to, e.getMessage(), e);
        }
    }
}
