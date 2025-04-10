package ca.mcgill.ecse321.gameorganizer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import java.util.Arrays;

/** this is for communication with the front end and will be changed to include jwt 
 * @author Shayan
*/

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration config = new CorsConfiguration();
        // Add all possible frontend development URLs
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",  // Vite default
            "http://localhost:3000",  // React default
            "http://127.0.0.1:5173",  // Using IP instead of localhost
            "http://127.0.0.1:3000"   // Using IP instead of localhost
        ));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // 1 hour

        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}