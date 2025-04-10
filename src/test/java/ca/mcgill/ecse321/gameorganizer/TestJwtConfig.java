package ca.mcgill.ecse321.gameorganizer;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for setting up test JWT environment.
 * This is designed to be included in test classes with 
 * @ContextConfiguration(initializers = TestJwtConfig.Initializer.class)
 */
public class TestJwtConfig {

    /**
     * Application context initializer that sets the JWT_SECRET environment variable.
     */
    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            
            // Use the same secret as in .env.test file
            String testJwtSecret = "tG8qcqi6M2XZ1s73QTdIHHGhBEzZARBOlDvcxkp4iAoCPU5f8OeYXFmNOkjr9XgJ";
            
            Map<String, Object> props = new HashMap<>();
            props.put("JWT_SECRET", testJwtSecret);
            
            // Explicitly set the spring.profiles.active property to "test"
            props.put("spring.profiles.active", "test");
            
            MapPropertySource testProperties = new MapPropertySource("testJwtProperties", props);
            environment.getPropertySources().addFirst(testProperties);
            
            // Also set as a system property for methods that check there
            System.setProperty("JWT_SECRET", testJwtSecret);
            System.setProperty("spring.profiles.active", "test");
            
            System.out.println("JWT_SECRET environment variable and test profile set for testing");
        }
    }
} 