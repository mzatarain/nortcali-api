package com.nortcali.api;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class NortcaliApiApplication {

	private final Environment environment;

	public NortcaliApiApplication(Environment environment) {
		this.environment = environment;
	}

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(NortcaliApiApplication.class, args);
	}

	@PostConstruct
	void validateProfile() {
		boolean isDevActive = Arrays.asList(environment.getActiveProfiles()).contains("dev");
		boolean isProductionEnv = "true".equalsIgnoreCase(System.getenv("PRODUCTION_ENV"));
		if (isDevActive && isProductionEnv) {
			throw new IllegalStateException(
					"Perfil 'dev' activo en un entorno de producción (PRODUCTION_ENV=true). " +
					"Arranca con SPRING_PROFILES_ACTIVE=prod.");
		}
	}

}
