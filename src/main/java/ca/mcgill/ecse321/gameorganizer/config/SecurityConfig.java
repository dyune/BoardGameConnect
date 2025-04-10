package ca.mcgill.ecse321.gameorganizer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import java.util.Arrays;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import ca.mcgill.ecse321.gameorganizer.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    static {
    }

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // Main security filter chain
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Apply rules only to paths starting with /api or /auth or /users/me or /dev
            .securityMatcher("/api/**", "/auth/**", "/users/**", "/dev/**")
            .authorizeHttpRequests(authz -> authz
                // --- Authentication & Account Creation ---
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/account").permitAll() // Account creation via API
                .requestMatchers("/dev/**").permitAll() // Allow dev endpoints

                // --- Public Read Operations (using /api prefix) ---
                .requestMatchers(HttpMethod.GET, "/api/games/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
                // User games endpoints - allow public access
                .requestMatchers(HttpMethod.GET, "/api/users/*/games/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/*/games/played").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/*/games/borrowed").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/*/games/owned").permitAll()

                // --- Authenticated Operations (using /api prefix and direct paths) ---
                .requestMatchers("/users/me").authenticated() // User profile endpoint without /api prefix
                .requestMatchers(HttpMethod.GET, "/api/account/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/account/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/account/**").authenticated()
                .requestMatchers("/api/registrations/**").authenticated()
                .requestMatchers("/api/borrowrequests/**").authenticated()
                .requestMatchers("/api/lending-records/**").authenticated()
                .requestMatchers("/api/reviews/**").authenticated()
                // Add specific role checks if needed (example below)
                .requestMatchers(HttpMethod.POST, "/api/games/**").hasRole("GAME_OWNER") // Example role check
                .requestMatchers(HttpMethod.PUT, "/api/games/**").hasRole("GAME_OWNER")
                .requestMatchers(HttpMethod.DELETE, "/api/games/**").hasRole("GAME_OWNER")
                .requestMatchers(HttpMethod.DELETE, "/api/lending-records/**").hasRole("GAME_OWNER")
                // Default: require authentication for any other /api endpoints
                .requestMatchers("/api/**").authenticated()
                // Fallback for any other matched request (shouldn't be hit often with specific matcher)
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults()) // Uses corsConfigurationSource bean
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Removed explicit .securityContext configuration.
            // Will rely on SecurityContextHolderFilter added below.
            // Removed explicit requestCache and logout configurations.
            .anonymous(anonymous -> anonymous.disable())
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            // Reverted adding SecurityContextHolderFilter explicitly due to ordering issues.
            // Relying on STATELESS + NullSecurityContextRepository.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Removed the second filter chain bean

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Can't use * with allowCredentials=true, so specify the frontend origin
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000", 
            "http://localhost:5173", 
            "http://127.0.0.1:3000", 
            "http://127.0.0.1:5173"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "authorization", 
            "content-type", 
            "x-auth-token", 
            "Authorization",
            "X-Remember-Me", 
            "X-User-Id"
        ));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
        configuration.setAllowCredentials(true); // Allow credentials
        configuration.setMaxAge(3600L); // Cache preflight response for 1 hour
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // --- Authentication Provider Beans (remain the same) ---

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService, @Lazy PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder); // passwordEncoder is lazily resolved
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
