package com.nortcali.api.config;

import com.nortcali.api.entity.Superadmin;
import com.nortcali.api.repository.SuperadminRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Fase 8 (plan SaaS): siembra el primer superadmin desde variables de entorno, en vez de exponer
 * un endpoint HTTP de auto-registro (una superficie de ataque injustificada para una cuenta con
 * acceso total al SaaS). Solo actúa si la tabla {@code superadmins} está vacía — a partir del
 * primer superadmin creado, este componente queda en no-op para siempre, sin importar si las
 * variables de entorno se dejan configuradas.
 *
 * Uso local: exportar SUPERADMIN_BOOTSTRAP_EMAIL / SUPERADMIN_BOOTSTRAP_PASSWORD antes de
 * levantar la app una vez. En producción: mismo mecanismo vía Secrets Manager, una sola vez en
 * el primer deploy de esta fase.
 */
@Component
@Slf4j
public class SuperadminSeeder implements CommandLineRunner {

    private final SuperadminRepository superadminRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${SUPERADMIN_BOOTSTRAP_EMAIL:}")
    private String bootstrapEmail;

    @Value("${SUPERADMIN_BOOTSTRAP_PASSWORD:}")
    private String bootstrapPassword;

    @Value("${SUPERADMIN_BOOTSTRAP_NAME:Superadmin}")
    private String bootstrapName;

    public SuperadminSeeder(SuperadminRepository superadminRepo, PasswordEncoder passwordEncoder) {
        this.superadminRepo = superadminRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (superadminRepo.count() > 0) {
            return;
        }
        if (bootstrapEmail.isBlank() || bootstrapPassword.isBlank()) {
            log.info("Sin superadmins en la BD y sin SUPERADMIN_BOOTSTRAP_EMAIL/PASSWORD configurados — " +
                    "panel de superadmin sin cuenta inicial. Ver Fase 8 en SAAS-TASKS.md.");
            return;
        }

        Superadmin superadmin = new Superadmin();
        superadmin.setEmail(bootstrapEmail);
        superadmin.setPassword(passwordEncoder.encode(bootstrapPassword));
        superadmin.setName(bootstrapName);
        superadminRepo.save(superadmin);
        log.info("Superadmin inicial creado para '{}'", bootstrapEmail);
    }
}
