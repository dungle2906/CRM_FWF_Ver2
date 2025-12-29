package com.example.basiccrmfwf.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

//    @Value("${spring.application.deploy.path}") // Secret key cho HMAC
//    private String ngrokLink;

//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        // Allow all origins for testing configuration
//        configuration.setAllowedOrigins(List.of("https://" + ngrokLink + ".ngrok-free.app", "http://localhost:3000"));   // Alternative if setAllowedOrigins("*") doesn't work
//         configuration.addAllowedOriginPattern("*");   configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
//        configuration.setAllowedHeaders(List.of("*"));   configuration.setAllowCredentials(true); // Must be false when using "*" for allowedOrigins
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);   return source;
//    }
}
