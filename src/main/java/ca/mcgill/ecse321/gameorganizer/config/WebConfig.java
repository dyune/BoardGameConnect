package ca.mcgill.ecse321.gameorganizer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Apply to all endpoints
                // Use allowedOriginPatterns for flexibility during development
                .allowedOriginPatterns("http://localhost:*")
                // Or list specific origins: .allowedOrigins("http://localhost:5173", "http://localhost:xxxx")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allowed methods
                .allowedHeaders("*") // Allow all headers
                .allowCredentials(true) // IMPORTANT: Allow cookies/auth headers
                .maxAge(3600); // Cache preflight response for 1 hour
    }
}