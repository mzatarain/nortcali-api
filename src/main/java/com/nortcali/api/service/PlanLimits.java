package com.nortcali.api.service;

/**
 * Fase 11 (plan SaaS): límites de restaurantes/empleados por plan de suscripción. Los valores
 * espejean la tabla de la Fase 3 del roadmap y los mismos strings de plan que
 * {@code BillingServiceImpl.resolvePriceId} (`basico`, `pro`, `enterprise`).
 */
public enum PlanLimits {

    BASICO("basico", "Básico", 1, 5),
    PRO("pro", "Pro", 3, 20),
    ENTERPRISE("enterprise", "Enterprise", Integer.MAX_VALUE, Integer.MAX_VALUE);

    private final String plan;
    private final String displayName;
    private final int maxRestaurants;
    private final int maxEmployees;

    PlanLimits(String plan, String displayName, int maxRestaurants, int maxEmployees) {
        this.plan = plan;
        this.displayName = displayName;
        this.maxRestaurants = maxRestaurants;
        this.maxEmployees = maxEmployees;
    }

    public String getPlan() { return plan; }
    public String getDisplayName() { return displayName; }
    public int getMaxRestaurants() { return maxRestaurants; }
    public int getMaxEmployees() { return maxEmployees; }

    /**
     * Un tenant sin `plan` asignado (p.ej. en TRIAL antes de completar el checkout de Stripe)
     * se trata como el límite más restrictivo en vez de dejarlo pasar sin límite por accidente.
     */
    public static PlanLimits fromPlan(String plan) {
        if (plan == null) {
            return BASICO;
        }
        for (PlanLimits limits : values()) {
            if (limits.plan.equalsIgnoreCase(plan)) {
                return limits;
            }
        }
        return BASICO;
    }
}
