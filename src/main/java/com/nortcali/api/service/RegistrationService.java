package com.nortcali.api.service;

import com.nortcali.api.dto.request.RegisterRequest;
import com.nortcali.api.entity.Employee;

public interface RegistrationService {

    /**
     * Crea tenant + primer restaurante + primer empleado (TENANT_ADMIN) + catálogos base, y
     * devuelve el empleado creado (ya persistido, con sus restaurantes inicializados) para que
     * el caller (AuthController) emita el JWT igual que en login(). Registra además la
     * aceptación de los documentos legales vigentes con la IP y el user-agent del request.
     */
    Employee register(RegisterRequest request, String ipAddress, String userAgent);
}
