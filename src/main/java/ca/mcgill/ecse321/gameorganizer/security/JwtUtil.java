package ca.mcgill.ecse321.gameorganizer.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

// Removed Value import as secret is read from env var
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import ca.mcgill.ecse321.gameorganizer.models.Account; // Keep Account import
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // Secret will be read from environment variable JWT_SECRET

    // Expiration is still read from properties
    @Value("${jwt.expiration}")
    private long expiration;

    private Key key;

    // IMPORTANT: Ensure the jwt.secret in application.properties is a Base64 encoded string
    // representing at least 64 secure random bytes (512 bits) for HS512 algorithm.
    // Generate a new one if the current key is too short.
    // Example generation (using jjwt library or online tool): Keys.secretKeyFor(SignatureAlgorithm.HS512) then Base64 encode the result.

    @PostConstruct
    public void init() {
        logger.info("Initializing JWT key from secret");
        try {
            // First try to get from environment variable
            String jwtSecretEnv = System.getenv("JWT_SECRET");
            
            if (jwtSecretEnv != null && !jwtSecretEnv.isEmpty()) {
                logger.info("Found JWT_SECRET in environment variables");
            }
            
            // If not found in environment, try system properties (for tests)
            if (jwtSecretEnv == null || jwtSecretEnv.isEmpty()) {
                jwtSecretEnv = System.getProperty("JWT_SECRET");
                if (jwtSecretEnv != null && !jwtSecretEnv.isEmpty()) {
                    logger.info("Using JWT_SECRET from system property");
                }
            }
            
            // Check if we're in a test context by checking for test profile
            boolean isTestContext = "test".equals(System.getProperty("spring.profiles.active")) || 
                                   (System.getenv("SPRING_PROFILES_ACTIVE") != null && 
                                    System.getenv("SPRING_PROFILES_ACTIVE").contains("test"));
            
            // Also check Spring Environment from TestJwtConfig
            if (!isTestContext && System.getProperty("spring.profiles.active") == null) {
                String[] activeProfiles = System.getProperty("spring.active.profiles", "").split(",");
                for (String profile : activeProfiles) {
                    if ("test".equals(profile.trim())) {
                        isTestContext = true;
                        break;
                    }
                }
            }
            
            // Check if we have a secret from either source
            if (jwtSecretEnv == null || jwtSecretEnv.isEmpty()) {
                // For testing purposes only, use a default secret if running in test profile
                if (isTestContext) {
                    logger.info("Running in test profile, using default test JWT_SECRET");
                    jwtSecretEnv = "tG8qcqi6M2XZ1s73QTdIHHGhBEzZARBOlDvcxkp4iAoCPU5f8OeYXFmNOkjr9XgJ";
                } else {
                    logger.error("JWT_SECRET environment variable or system property not set or empty.");
                    // Throwing an exception prevents the application from starting without a secret
                    throw new IllegalStateException("JWT_SECRET environment variable is required but not set.");
                }
            }
            
            byte[] secretBytes = Base64.getDecoder().decode(jwtSecretEnv);

            // Use HS256 in test mode to accept shorter secrets
            SignatureAlgorithm algorithm = SignatureAlgorithm.HS512; // Default for production
            if (isTestContext) {
                algorithm = SignatureAlgorithm.HS256; // Use weaker but suitable algorithm for tests
                logger.info("Using HS256 algorithm for JWT in test mode");
            } else {
                // Ensure the key length is sufficient for HS512 (at least 64 bytes / 512 bits)
                if (secretBytes.length * 8 < 512) {
                    logger.error("JWT_SECRET is too short for HS512 algorithm. Required 512 bits (64 bytes), found {} bits ({} bytes).", secretBytes.length * 8, secretBytes.length);
                    throw new IllegalStateException("JWT_SECRET provided via environment variable is too short for HS512 algorithm.");
                }
            }
            
            key = new SecretKeySpec(secretBytes, algorithm.getJcaName());
            logger.info("JWT signing key initialized successfully with algorithm: {}", algorithm.getJcaName());
        } catch (Exception e) {
            logger.error("Failed to initialize JWT key: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize JWT key", e);
        }
    }

    // Modify generateToken to accept UserDetails and Account
    public String generateToken(UserDetails userDetails, Account account) {
        logger.debug("Generating token for username: {}", userDetails.getUsername());
        if (account == null) {
             logger.error("Account object is null in generateToken. Cannot include userId claim.");
             // Decide how to handle this: throw exception, return null, or generate token without userId?
             // For now, let's generate without userId but log an error.
             // throw new IllegalArgumentException("Account cannot be null when generating token with userId claim.");
        } else if (!userDetails.getUsername().equals(account.getEmail())) {
            // Sanity check: ensure UserDetails username matches Account email
            logger.error("Username mismatch between UserDetails ({}) and Account ({}) in generateToken.",
                         userDetails.getUsername(), account.getEmail());
            // Handle mismatch appropriately
            throw new IllegalArgumentException("UserDetails username does not match Account email.");
        }

        // Extract roles
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
        // Get userId directly from the Account object
        Integer userId = (account != null) ? account.getId() : null; // Changed Long to Integer

        try {
            Date now = new Date();
            Date expiryDate = new Date(System.currentTimeMillis() + expiration);
            logger.debug("Token issuedAt: {}, expiresAt: {}, expiration ms: {}", now, expiryDate, expiration);
            
            String token = Jwts.builder()
                    .setSubject(userDetails.getUsername()) // Use username (email) as subject
                    .claim("roles", roles) // Add roles claim
                    .claim("userId", userId) // Add userId claim (will be null if account was null)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(key) // Algorithm is inferred from the key
                    .compact();
            
            logger.debug("Generated token: {}...", token.substring(0, Math.min(token.length(), 20)));
            return token;
        } catch (Exception e) {
            logger.error("Error generating token for user {}: {}", userDetails.getUsername(), e.getMessage(), e);
            // Consider throwing a specific exception type if needed
            throw new RuntimeException("Error generating JWT token", e);
        }
    }

    public String extractUsername(String token) {
        if (token == null) {
            logger.warn("Token is null in extractUsername");
            return null;
        }
        
        // Log token format for debugging
        logger.debug("Extracting username from token: {}...", token.substring(0, Math.min(token.length(), 20)));
        
        try {
            String username = extractClaim(token, Claims::getSubject);
            logger.debug("Extracted username: {}", username);
            return username;
        } catch (Exception e) {
            logger.error("Error extracting username from token: {}", e.getMessage());
            logger.debug("Token structure appears invalid: {}...", token.substring(0, Math.min(token.length(), 40)));
            return null; // Return null if extraction fails
        }
    }

    public Date extractExpiration(String token) {
        if (token == null) {
            logger.warn("Token is null in extractExpiration");
            return null;
        }
        
        try {
            Date expiration = extractClaim(token, Claims::getExpiration);
            logger.debug("Extracted expiration: {}", expiration);
            return expiration;
        } catch (Exception e) {
            logger.error("Error extracting expiration from token: {}", e.getMessage());
            return null; // Return null if extraction fails
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        if (claims != null) { // Check if claims extraction was successful
            try {
                T result = claimsResolver.apply(claims);
                logger.debug("Successfully extracted claim");
                return result;
            } catch (Exception e) {
                logger.error("Failed to resolve claim: {}", e.getMessage());
                return null;
            }
        }
        logger.warn("No claims extracted from token, returning null");
        return null; // Return null if claims are null (parsing failed)
    }

    private Claims extractAllClaims(String token) {
        if (token == null) {
            logger.warn("Token is null in extractAllClaims");
            return null;
        }
        
        // Remove "Bearer " prefix if present
        if (token.startsWith("Bearer ")) {
            logger.debug("Removing 'Bearer ' prefix from token");
            token = token.substring(7);
        }
        
        try {
            logger.debug("Parsing JWT token");
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            logger.debug("Successfully parsed JWT claims: subject={}, issued={}, expiration={}", 
                claims.getSubject(), claims.getIssuedAt(), claims.getExpiration());
            return claims;
        } catch (ExpiredJwtException e) {
            // Log expired as warn, but return claims. Validation logic handles the expiration check.
            logger.warn("JWT token is expired: {}", e.getMessage());
            logger.debug("Expired token details - subject: {}, issued: {}, expiration: {}", 
                e.getClaims().getSubject(), e.getClaims().getIssuedAt(), e.getClaims().getExpiration());
            return e.getClaims(); // Return claims even if expired, for potential use
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("JWT token is malformed: {}", e.getMessage());
            // Log a portion of the token to help diagnose issues
            logger.debug("Malformed token: {}...", token.substring(0, Math.min(token.length(), 40)));
        } catch (SignatureException e) {
            logger.error("JWT signature validation failed: {}", e.getMessage());
            logger.debug("Token with invalid signature: {}...", token.substring(0, Math.min(token.length(), 40)));
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty or null: {}", e.getMessage());
        } catch (Exception e) { // Catch any other unexpected exceptions
            logger.error("An unexpected error occurred during JWT parsing: {}", e.getMessage(), e); // Log stack trace too
        }
        return null; // Return null if any parsing error other than expired occurs
    }

    public boolean isTokenExpired(String token) {
        if (token == null) {
            logger.warn("Token is null in isTokenExpired");
            return true;
        }
        
        Date expirationDate = extractExpiration(token);
        // Check if expirationDate is null (due to parsing error or missing claim)
        if (expirationDate == null) {
             logger.warn("Could not determine token expiration due to parsing error or missing claim.");
             return true; // Treat as expired if expiration cannot be extracted
        }
        
        Date now = new Date();
        boolean expired = expirationDate.before(now);
        if (expired) {
            // Log details only when confirmed expired
            logger.warn("Token is expired. Expiration: {}, Current: {}", expirationDate, now);
        } else {
            logger.debug("Token is valid. Expiration: {}, Current: {}", expirationDate, now);
        }
        return expired;
    }

    // Updated validateToken
    public boolean validateToken(String token, String username) {
        if (token == null) {
            logger.warn("Token is null in validateToken");
            return false;
        }
        
        if (username == null) {
            logger.warn("Username is null in validateToken");
            return false;
        }
        
        logger.debug("Validating token for user: {}", username);
        logger.debug("Token for validation: {}...", token.substring(0, Math.min(token.length(), 20)));
        
        // Check if token starts with "Bearer "
        if (token.startsWith("Bearer ")) {
            logger.debug("Token includes 'Bearer ' prefix, removing for validation");
            token = token.substring(7);
        }
        
        final String extractedUsername = extractUsername(token);

        // Check if username could be extracted (parsing might have failed)
        if (extractedUsername == null) {
            logger.warn("Token validation failed: Could not extract username from token (check logs for parsing errors).");
            return false;
        }

        boolean isUsernameMatch = extractedUsername.equals(username);
        if (!isUsernameMatch) {
            logger.warn("Token validation failed: Username mismatch. Token Subject='{}', Expected Username='{}'", extractedUsername, username);
            return false;
        } else {
            logger.debug("Username match verified: {}", username);
        }

        // isTokenExpired now handles null expiration date and logs if expired
        boolean isExpired = isTokenExpired(token);
        if (isExpired) {
            // No need to log again here, isTokenExpired already logged it.
            return false;
        }

        // If all checks pass
        logger.debug("Token validated successfully for user: {}", username);
        return true; // Only return true if username matches AND token is not expired
    }
}