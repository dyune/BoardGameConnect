package ca.mcgill.ecse321.gameorganizer.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import ca.mcgill.ecse321.gameorganizer.models.Account;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Component
@Primary
@Profile("test")
public class TestJwtUtil extends JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(TestJwtUtil.class);
    private static final String TEST_JWT_SECRET = "tG8qcqi6M2XZ1s73QTdIHHGhBEzZARBOlDvcxkp4iAoCPU5f8OeYXFmNOkjr9XgJ";

    @Value("${jwt.expiration}")
    private long expiration;

    private Key key;

    @PostConstruct
    @Override
    public void init() {
        logger.info("Initializing TEST JWT key with fixed test secret");
        try {
            byte[] secretBytes = Base64.getDecoder().decode(TEST_JWT_SECRET);
            key = new SecretKeySpec(secretBytes, SignatureAlgorithm.HS256.getJcaName());
            logger.info("TEST JWT signing key initialized successfully for tests");
        } catch (Exception e) {
            logger.error("Failed to initialize TEST JWT key: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize TEST JWT key", e);
        }
    }

    @Override
    public String generateToken(UserDetails userDetails, Account account) {
        logger.debug("Generating test token for username: {}", userDetails.getUsername());
        
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
        Integer userId = (account != null) ? account.getId() : null;

        try {
            Date now = new Date();
            Date expiryDate = new Date(System.currentTimeMillis() + expiration);
            
            String token = Jwts.builder()
                    .setSubject(userDetails.getUsername())
                    .claim("roles", roles)
                    .claim("userId", userId)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(key)
                    .compact();
            
            logger.debug("Generated test token: {}...", token.substring(0, Math.min(token.length(), 20)));
            return token;
        } catch (Exception e) {
            logger.error("Error generating test token for user {}: {}", userDetails.getUsername(), e.getMessage(), e);
            throw new RuntimeException("Error generating JWT test token", e);
        }
    }

    @Override
    public String extractUsername(String token) {
        if (token == null) {
            logger.warn("Test token is null in extractUsername");
            return null;
        }
        
        try {
            String username = extractClaim(token, Claims::getSubject);
            logger.debug("Extracted username from test token: {}", username);
            return username;
        } catch (Exception e) {
            logger.error("Error extracting username from test token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Date extractExpiration(String token) {
        if (token == null) {
            logger.warn("Test token is null in extractExpiration");
            return null;
        }
        
        try {
            Date expiration = extractClaim(token, Claims::getExpiration);
            logger.debug("Extracted expiration from test token: {}", expiration);
            return expiration;
        } catch (Exception e) {
            logger.error("Error extracting expiration from test token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        if (claims != null) {
            try {
                T result = claimsResolver.apply(claims);
                logger.debug("Successfully extracted claim from test token");
                return result;
            } catch (Exception e) {
                logger.error("Failed to resolve claim from test token: {}", e.getMessage());
                return null;
            }
        }
        logger.warn("No claims extracted from test token, returning null");
        return null;
    }

    private Claims extractAllClaims(String token) {
        if (token == null) {
            logger.warn("Test token is null in extractAllClaims");
            return null;
        }
        
        if (token.startsWith("Bearer ")) {
            logger.debug("Removing 'Bearer ' prefix from test token");
            token = token.substring(7);
        }
        
        try {
            logger.debug("Parsing test JWT token");
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            logger.debug("Successfully parsed test JWT claims");
            return claims;
        } catch (Exception e) {
            logger.error("Error parsing test JWT token: {}", e.getMessage());
            return null;
        }
    }
} 