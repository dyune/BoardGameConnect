package ca.mcgill.ecse321.gameorganizer.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

@Configuration
@TestConfiguration
public class TestConfig implements WebMvcConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(TestConfig.class);
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private EventRepository eventRepository;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @PostConstruct
    public void init() {
        // Set JWT_SECRET for tests
        if (System.getProperty("JWT_SECRET") == null && System.getenv("JWT_SECRET") == null) {
            // Try to load from .env.test file
            try {
                File envFile = new File(".env.test");
                if (envFile.exists()) {
                    Properties props = new Properties();
                    FileInputStream fis = new FileInputStream(envFile);
                    props.load(fis);
                    fis.close();
                    
                    String jwtSecret = props.getProperty("JWT_SECRET");
                    if (jwtSecret != null && !jwtSecret.trim().isEmpty()) {
                        System.setProperty("JWT_SECRET", jwtSecret.trim());
                        logger.info("Loaded JWT_SECRET from .env.test file for tests");
                    } else {
                        // Fallback to a default test secret if not found in .env.test
                        System.setProperty("JWT_SECRET", "tG8qcqi6M2XZ1s73QTdIHHGhBEzZARBOlDvcxkp4iAoCPU5f8OeYXFmNOkjr9XgJ");
                        logger.info("Using default JWT_SECRET for tests");
                    }
                } else {
                    // Fallback to a default test secret if .env.test not found
                    System.setProperty("JWT_SECRET", "tG8qcqi6M2XZ1s73QTdIHHGhBEzZARBOlDvcxkp4iAoCPU5f8OeYXFmNOkjr9XgJ");
                    logger.info("Using default JWT_SECRET for tests (no .env.test file found)");
                }
            } catch (Exception e) {
                logger.error("Error loading JWT_SECRET from .env.test", e);
                // Fallback to a default test secret on error
                System.setProperty("JWT_SECRET", "tG8qcqi6M2XZ1s73QTdIHHGhBEzZARBOlDvcxkp4iAoCPU5f8OeYXFmNOkjr9XgJ");
                logger.info("Using default JWT_SECRET for tests due to error loading from .env.test");
            }
        } else {
            logger.info("JWT_SECRET already set in environment or system properties");
        }
    }
}