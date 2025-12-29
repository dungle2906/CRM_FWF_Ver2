package com.example.BasicCRM_FWF.Config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a local .env file (if present) and inserts its entries as the highest-priority
 * property source so Spring can resolve ${...} placeholders from environment values.
 */
public class DotenvPropertySourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            Path envPath = Paths.get(System.getProperty("user.dir"), ".env");
            if (!Files.exists(envPath)) {
                return;
            }

            List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
            Map<String, Object> map = new HashMap<>();
            for (String line : lines) {
                if (line == null) continue;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int idx = trimmed.indexOf('=');
                if (idx <= 0) continue;
                String key = trimmed.substring(0, idx).trim();
                String val = trimmed.substring(idx + 1).trim();
                // remove optional surrounding quotes
                if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                    val = val.substring(1, val.length() - 1);
                }
                map.put(key, val);
            }

            if (!map.isEmpty()) {
                applicationContext.getEnvironment().getPropertySources()
                        .addFirst(new MapPropertySource("dotenvProperties", map));
            }
        } catch (IOException e) {
            // If reading fails, do not block application startup — environment may be provided elsewhere
        }
    }
}
