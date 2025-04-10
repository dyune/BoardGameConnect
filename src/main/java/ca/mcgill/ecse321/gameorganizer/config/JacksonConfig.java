package ca.mcgill.ecse321.gameorganizer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Configuration class for customizing Jackson JSON serialization.
 * This class helps prevent JSON serialization issues like circular references and improves performance.
 */
@Configuration
public class JacksonConfig {

    /**
     * Configures the Jackson ObjectMapper with settings to prevent circular reference issues
     * and improve serialization performance.
     * 
     * @return Jackson2ObjectMapperBuilder with customized settings
     */
    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        builder.featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        return builder;
    }
    
    /**
     * Creates a custom ObjectMapper with additional settings beyond what's in the builder.
     * 
     * @param builder The pre-configured Jackson2ObjectMapperBuilder
     * @return A fully configured ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();
        // Enable default typing if needed for polymorphic serialization
        // objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return objectMapper;
    }
} 