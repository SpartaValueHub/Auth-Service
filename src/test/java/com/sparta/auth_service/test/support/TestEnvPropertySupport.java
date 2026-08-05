package com.sparta.auth_service.test.support;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class TestEnvPropertySupport {

    private TestEnvPropertySupport() {
    }

    public static void registerOptionalDotEnv(DynamicPropertyRegistry registry) {
        registry.add("spring.config.import", () -> "optional:file:.env[.properties]");
    }

    public static void registerDatasourceFromDotEnvOrSystemEnv(DynamicPropertyRegistry registry) {
        Map<String, String> dotEnv = loadDotEnv(Path.of(".env"));

        registry.add("spring.datasource.url", () -> firstNonBlank(
                System.getenv("SPRING_DATASOURCE_URL"),
                dotEnv.get("SPRING_DATASOURCE_URL")
        ));
        registry.add("spring.datasource.username", () -> firstNonBlank(
                System.getenv("SPRING_DATASOURCE_USERNAME"),
                dotEnv.get("SPRING_DATASOURCE_USERNAME")
        ));
        registry.add("spring.datasource.password", () -> firstNonBlank(
                System.getenv("SPRING_DATASOURCE_PASSWORD"),
                dotEnv.get("SPRING_DATASOURCE_PASSWORD")
        ));
    }

    private static Map<String, String> loadDotEnv(Path path) {
        Map<String, String> values = new HashMap<>();
        if (!Files.exists(path)) {
            return values;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                String[] parts = trimmed.split("=", 2);
                values.put(parts[0].trim(), parts[1].trim());
            }
        } catch (IOException ignored) {
            return values;
        }
        return values;
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        throw new IllegalStateException("SPRING_DATASOURCE_* is required for integration tests");
    }
}
