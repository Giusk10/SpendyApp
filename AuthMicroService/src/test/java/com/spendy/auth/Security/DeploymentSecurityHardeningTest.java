package com.spendy.auth.Security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Fase 10 - Test di Deployment, Segreti e Porte (AuthMicroService)")
class DeploymentSecurityHardeningTest {

    // 1. Avvio in produzione senza chiave privata fallisce categoricamente
    @Test
    @DisplayName("1. Avvio in produzione senza chiave privata (allowEphemeralKeys=false) fallisce con IllegalStateException")
    void testProductionStartupFailsWithoutPrivateKey() {
        JwtTokenService service = new JwtTokenService(new DefaultResourceLoader());
        service.setAllowEphemeralKeys(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class, service::init);
        assertTrue(ex.getMessage().contains("CRITICAL SECURITY ERROR"),
                "L'avvio senza chiave privata deve fallire con un chiaro errore di sicurezza");
    }

    // 2. Chiavi effimere vietate se allowEphemeralKeys è disattivato
    @Test
    @DisplayName("2. Chiavi effimere non consentite in modalità produzione")
    void testEphemeralKeysForbiddenInProduction() {
        JwtTokenService service = new JwtTokenService(new DefaultResourceLoader());
        assertFalse(service.isAllowEphemeralKeys(), "Il flag jwt.allow-ephemeral-keys deve essere false di default in produzione");
    }

    // 3. Verifica configurazione porte in docker-compose.yml
    @Test
    @DisplayName("3. Verifica delle porte in docker-compose: solo il Gateway espone porte sull'host")
    void testDockerComposePortsExposure() throws Exception {
        File dockerComposeFile = new File("../docker-compose.yml");
        if (!dockerComposeFile.exists()) {
            dockerComposeFile = new File("docker-compose.yml");
        }
        assertTrue(dockerComposeFile.exists(), "Il file docker-compose.yml deve esistere");

        List<String> lines = Files.readAllLines(dockerComposeFile.toPath());
        String currentService = "";
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.endsWith(":") && !trimmed.startsWith("#") && !line.startsWith(" ") && !line.startsWith("\t")) {
                currentService = trimmed.replace(":", "");
            }
            if (trimmed.startsWith("mysql:") || trimmed.startsWith("auth-service:") || trimmed.startsWith("expense-service:") || trimmed.startsWith("gateway-service:")) {
                currentService = trimmed.replace(":", "");
            }

            // MySQL, auth-service ed expense-service non devono avere porte mappate sull'host ("3306:3306", "8081:8081", "8084:8084")
            if (trimmed.startsWith("- \"3306:3306\"")) {
                fail("MySQL non deve esporre la porta 3306 pubblicamente verso l'host");
            }
            if (trimmed.startsWith("- \"8081:8081\"")) {
                fail("AuthMicroService non deve esporre la porta 8081 pubblicamente verso l'host");
            }
            if (trimmed.startsWith("- \"8084:8084\"")) {
                fail("ExpenseMicroService non deve esporre la porta 8084 pubblicamente verso l'host");
            }
        }
    }

    // 4. Verifica che non siano presenti chiavi private hardcoded committate
    @Test
    @DisplayName("4. Verifica assenza di chiavi private RSA committate nei file di configurazione")
    void testNoCommittedPrivateKeysInProperties() throws Exception {
        File appProps = new File("src/main/resources/application.properties");
        if (appProps.exists()) {
            String content = Files.readString(appProps.toPath());
            assertFalse(content.contains("BEGIN RSA PRIVATE KEY"), "application.properties non deve contenere chiavi private hardcoded");
            assertFalse(content.contains("BEGIN PRIVATE KEY"), "application.properties non deve contenere chiavi private hardcoded");
        }
    }

    // 5. Avvio in produzione con chiavi effimere fallisce con eccezione di sicurezza
    @Test
    @DisplayName("5. Avvio in produzione con chiavi effimere fallisce categoricamente con eccezione di sicurezza")
    void testProductionStartupWithEphemeralKeysRejected() {
        System.setProperty("spring.profiles.active", "prod");
        try {
            JwtTokenService service = new JwtTokenService(new DefaultResourceLoader());
            service.setAllowEphemeralKeys(true);
            IllegalStateException ex = assertThrows(IllegalStateException.class, service::init);
            assertTrue(ex.getMessage().contains("strictly forbidden in production"));
        } finally {
            System.clearProperty("spring.profiles.active");
        }
    }
}
