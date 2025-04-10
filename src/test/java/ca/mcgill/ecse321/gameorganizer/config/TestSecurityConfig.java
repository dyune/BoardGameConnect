package ca.mcgill.ecse321.gameorganizer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import ca.mcgill.ecse321.gameorganizer.security.JwtAuthenticationFilter;

@Configuration
@Profile("test")
@EnableMethodSecurity
public class TestSecurityConfig {

    static {
        // Use InheritableThreadLocal to ensure SecurityContext propagation across threads
        System.setProperty(SecurityContextHolder.SYSTEM_PROPERTY, SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    private static final Logger log = LoggerFactory.getLogger(TestSecurityConfig.class);
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    public TestSecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Primary
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Configure security to mirror main config for accurate testing
        log.info("Configuring test security filter chain - Mirroring main security rules");

        http
            // Mirroring main security rules
            .authorizeHttpRequests(authz -> authz
                // Allow unauthenticated access for auth endpoints and account creation
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/account").permitAll()
                // Require authentication for account GET requests
                .requestMatchers(HttpMethod.GET, "/account/**").authenticated()
                // Allow GET operations for browsing content
                .requestMatchers(HttpMethod.GET, "/games/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/events/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/users/*/games").permitAll()
                .requestMatchers(HttpMethod.GET, "/lending-records/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/borrowrequests/**").authenticated()
                // Explicitly allow auth-test endpoint
                .requestMatchers("/api/events/auth-test").permitAll()
                .requestMatchers("/api/events/auth-debug").permitAll()
                // Account management
                .requestMatchers(HttpMethod.PUT, "/account/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/account/**").authenticated()
                // Game operations - Assume only game owners or admins can modify
                .requestMatchers(HttpMethod.POST, "/games/**").hasAnyRole("GAME_OWNER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/games/**").hasAnyRole("GAME_OWNER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/games/**").hasAnyRole("GAME_OWNER", "ADMIN")
                // Event operations
                .requestMatchers(HttpMethod.POST, "/events/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/events/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/events/**").authenticated()
                // Borrow Requests
                .requestMatchers(HttpMethod.POST, "/borrowrequests/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/borrowrequests/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/borrowrequests/**").authenticated()
                // Lending Records
                .requestMatchers(HttpMethod.POST, "/lending-records/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/lending-records/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/lending-records/**").hasAnyRole("GAME_OWNER", "ADMIN")
                // Reviews
                .requestMatchers("/reviews/**").authenticated()
                // Registrations
                .requestMatchers("/registrations/**").authenticated()
                // Default: require authentication
                .anyRequest().authenticated()
            )
            // Handle authentication exceptions by returning 401 Unauthorized
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            // Disable CSRF for REST APIs tests
            .csrf(csrf -> csrf.disable())
            // Enable CORS
            .cors(cors -> cors.configure(http))
            // Use stateless session management
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Explicitly set the SecurityContextRepository
            .securityContext(context -> context
                .securityContextRepository(new RequestAttributeSecurityContextRepository())
            )
            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}