-- Fase 9 (plan SaaS): marca cuándo se envió la última alerta de inactividad de un tenant, para
-- no reenviarla cada día mientras siga inactivo. Se limpia (NULL) en cuanto vuelve a tener
-- actividad, permitiendo que un futuro episodio de inactividad alerte de nuevo.
ALTER TABLE tenants ADD COLUMN inactivity_alerted_at DATETIME NULL;
