package ca.mcgill.ecse321.gameorganizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootApplication
public class GameorganizerApplication {
	
	private static final Logger logger = LoggerFactory.getLogger(GameorganizerApplication.class);

	static {
		// Set strategy to allow context propagation to child threads
		// This might help if async operations or thread switches are causing context loss
		SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
	}

	public static void main(String[] args) {
		// Load environment variables from .env file if it exists
		loadEnvFile();
		SpringApplication.run(GameorganizerApplication.class, args);
	}
	
	/**
	 * Loads environment variables from .env file if it exists
	 */
	private static void loadEnvFile() {
		Path envPath = Paths.get(".env");
		
		if (Files.exists(envPath)) {
			try {
				List<String> lines = Files.readAllLines(envPath)
					.stream()
					.filter(line -> !line.startsWith("#") && line.contains("="))
					.collect(Collectors.toList());
				
				for (String line : lines) {
					String[] parts = line.split("=", 2);
					if (parts.length == 2) {
						String key = parts[0].trim();
						String value = parts[1].trim();
						
						// Set as system property which Spring will use
						System.setProperty(key, value);
						logger.info("Loaded environment variable: {}", key);
					}
				}
				logger.info("Loaded environment variables from .env file");
			} catch (IOException e) {
				logger.error("Failed to load .env file: {}", e.getMessage());
			}
		} else {
			logger.info("No .env file found, using environment variables or defaults");
		}
	}
	
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		configurer.setIgnoreResourceNotFound(true);
		configurer.setFileEncoding("UTF-8");
		
		// Try to load from .env file as a property source
		Path envPath = Paths.get(".env");
		if (Files.exists(envPath)) {
			configurer.setLocation(new FileSystemResource(envPath.toFile()));
		}
		
		return configurer;
	}
}
