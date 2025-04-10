package ca.mcgill.ecse321.gameorganizer.controllers;

// import jakarta.servlet.http.Cookie; // Replaced by ResponseCookie
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletResponse; // Import HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders; // Needed for Set-Cookie header
import org.springframework.http.ResponseCookie; // Import ResponseCookie for SameSite etc.
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.env.Environment;

import ca.mcgill.ecse321.gameorganizer.dto.request.AuthenticationDTO;
// import ca.mcgill.ecse321.gameorganizer.dto.JwtAuthenticationResponse; // No longer returning JWT in body
import ca.mcgill.ecse321.gameorganizer.dto.response.UserSummaryDto; // Import UserSummaryDto
import ca.mcgill.ecse321.gameorganizer.dto.request.PasswordResetDto;
import ca.mcgill.ecse321.gameorganizer.dto.request.PasswordResetRequestDto;
import ca.mcgill.ecse321.gameorganizer.exceptions.EmailNotFoundException;
import ca.mcgill.ecse321.gameorganizer.exceptions.InvalidPasswordException;
import ca.mcgill.ecse321.gameorganizer.exceptions.InvalidTokenException;
import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;
import ca.mcgill.ecse321.gameorganizer.security.JwtUtil;
import ca.mcgill.ecse321.gameorganizer.services.AuthenticationService;

import java.util.Arrays;

/**
 * Controller to handle authentication-related endpoints.
 * Provides endpoints for login, logout, and password reset.
 *
 * @author Shine111111
 */
@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    // Keep AuthenticationService for logout/reset password for now, but not for login
    @Autowired
    private AuthenticationService authenticationService; 

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    @Autowired // Inject AuthenticationManager
    private AuthenticationManager authenticationManager;

    @Autowired // Inject AccountRepository to get ID after successful auth
    private AccountRepository accountRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Environment environment;

    /**
     * Endpoint for user login.
     *
     * @param authenticationDTO the authentication data transfer object containing email and password
     * @return a ResponseEntity containing the JwtAuthenticationResponse if login is successful, or an error message if login fails
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationDTO authenticationDTO, HttpServletResponse response) { // Inject HttpServletResponse
        try {
            // Validate input fields
            if (authenticationDTO.getEmail() == null || authenticationDTO.getEmail().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Email is missing
            }
            if (authenticationDTO.getPassword() == null || authenticationDTO.getPassword().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Password is missing
            }

            System.out.println("Login attempt for: " + authenticationDTO.getEmail());

            // Create authentication token and authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authenticationDTO.getEmail(), authenticationDTO.getPassword()));

            // Set the successful authentication in the SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get UserDetails from Authentication object
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Find the user account (needed for ID and potentially other info for the token)
            Account user = accountRepository.findByEmail(authenticationDTO.getEmail())
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found in repository: " + authenticationDTO.getEmail()));

            // Generate JWT token using UserDetails and the Account object
            String jwt = jwtUtil.generateToken(userDetails, user);
            System.out.println("JWT token generated successfully for user: " + user.getId());

            // Check if the account is a GameOwner
            boolean isGameOwner = user instanceof GameOwner;
            
            // Create user summary with gameOwner flag
            UserSummaryDto userSummary = new UserSummaryDto(user.getId(), user.getName(), user.getEmail(), isGameOwner);
            
            // Log the account type for debugging
            System.out.println("AuthController: Login successful - User ID: " + user.getId() + 
                              ", Email: " + user.getEmail() + 
                              ", IsGameOwner: " + isGameOwner);
                              
            // Determine cookie expiration based on rememberMe flag
            int cookieMaxAge = authenticationDTO.isRememberMe() 
                ? 30 * 24 * 3600  // 30 days in seconds (if rememberMe is true)
                : -1;      // Session cookie (expires when browser closes)
                
            System.out.println("Setting cookie max age to: " + cookieMaxAge + " seconds. Remember me: " + authenticationDTO.isRememberMe());

            // --- Use ResponseCookie for setting cookies with SameSite ---

            // Create HttpOnly cookie for the JWT using ResponseCookie
            ResponseCookie.ResponseCookieBuilder accessTokenBuilder = ResponseCookie.from("accessToken", jwt)
                .httpOnly(true)
                .secure(false) // false for local HTTP development; set true for HTTPS
                .path("/")
                .sameSite("Lax"); // Changed from Strict to Lax for better cross-site functionality
                
            // Apply maxAge only if rememberMe is true
            if (authenticationDTO.isRememberMe()) {
                accessTokenBuilder.maxAge(cookieMaxAge);
            } // otherwise it's a session cookie (expires when browser closes)

            ResponseCookie accessTokenCookie = accessTokenBuilder.build();
            response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
            logger.debug("Added accessToken cookie using ResponseCookie. MaxAge: {}, SameSite: Lax", cookieMaxAge);
            logger.debug("Generated JWT for accessToken (start): {}...", jwt.substring(0, Math.min(jwt.length(), 20)));

            // Create non-HttpOnly cookie for client-side authentication status checking using ResponseCookie
            ResponseCookie.ResponseCookieBuilder isAuthenticatedBuilder = ResponseCookie.from("isAuthenticated", "true")
                .httpOnly(false) // Allow JS access
                .secure(false) // false for local HTTP development; set true for HTTPS
                .path("/")
                .sameSite("Lax"); // Changed from Strict to Lax for better cross-site functionality
                
            // Apply maxAge only if rememberMe is true
            if (authenticationDTO.isRememberMe()) {
                isAuthenticatedBuilder.maxAge(cookieMaxAge);
            } // otherwise it's a session cookie (expires when browser closes)

            ResponseCookie isAuthenticatedCookie = isAuthenticatedBuilder.build();
            response.addHeader(HttpHeaders.SET_COOKIE, isAuthenticatedCookie.toString());
            logger.debug("Added isAuthenticated cookie using ResponseCookie. MaxAge: {}, SameSite: Lax", cookieMaxAge);

            // Debug cookie setting
            System.out.println("Setting accessToken cookie via response.addCookie()");
            System.out.println("Setting isAuthenticated cookie via response.addCookie()");
            return ResponseEntity.ok(userSummary);
        } catch (BadCredentialsException e) {
            // Return 401 UNAUTHORIZED when credentials are invalid
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (AuthenticationException e) {
            // Other authentication issues (e.g., user disabled, locked - depends on UserDetails implementation)
            System.err.println("Authentication failed: " + e.getMessage()); // Log other auth errors
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            // Catch unexpected errors during login
            System.err.println("Unexpected error during login: " + e.getMessage());
            e.printStackTrace(); // Log stack trace for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint for user logout.
     *
     * @return a ResponseEntity indicating that the user has been logged out successfully
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        System.out.println("Logout requested");
        
        // --- Use ResponseCookie for clearing cookies ---

        // Clear the JWT cookie using ResponseCookie
        ResponseCookie clearAccessTokenCookie = ResponseCookie.from("accessToken", "") // Empty value
            .httpOnly(true)
            .secure(false) // Match setting during creation
            .path("/")
            .maxAge(0) // Expire immediately
            .sameSite("Lax") // Changed from Strict to Lax to match login settings
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearAccessTokenCookie.toString());
        System.out.println("Clearing accessToken cookie via ResponseCookie");

        // Clear the isAuthenticated cookie using ResponseCookie
        ResponseCookie clearIsAuthenticatedCookie = ResponseCookie.from("isAuthenticated", "") // Empty value
            .httpOnly(false)
            .secure(false) // Match setting during creation
            .path("/")
            .maxAge(0) // Expire immediately
            .sameSite("Lax") // Changed from Strict to Lax to match login settings
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearIsAuthenticatedCookie.toString());
        System.out.println("Clearing isAuthenticated cookie via ResponseCookie");

        // Remove the redundant traditional cookie clearing section below
        // Redundant traditional cookie clearing removed as we now consistently use response.addCookie() above.
        
        // Clear security context
        SecurityContextHolder.clearContext();
        
        return ResponseEntity.ok("Successfully logged out");
    }

    /**
     * Endpoint to request a password reset token.
     *
     * @param requestDto DTO containing the user's email.
     * @return ResponseEntity indicating success or failure.
     */
    @PostMapping("/request-password-reset")
    public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetRequestDto requestDto) {
        try {
            authenticationService.requestPasswordReset(requestDto);
            
            // In development mode, add a hint about the dev endpoint
            if (Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
                return ResponseEntity.ok("Password reset token generated. In development mode, you can retrieve it using: " +
                                        "/dev/password-reset-token/" + requestDto.getEmail() + " or generate a new one with " +
                                        "/dev/generate-reset-token?email=" + requestDto.getEmail());
            }
            
            // Standard response for production
            return ResponseEntity.ok("Password reset token request processed. If the email exists, a reset link will be sent."); // Generic message for security
        } catch (EmailNotFoundException e) {
            // Still return OK to prevent email enumeration attacks
            return ResponseEntity.ok("Password reset token request processed. If the email exists, a reset link will be sent.");
        } catch (IllegalArgumentException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Error requesting password reset: " + e.getMessage());
            e.printStackTrace(); // More detailed logging for troubleshooting
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred sending the email. Please verify SMTP settings or try again later.");
        } catch (Exception e) {
            System.err.println("Unexpected error requesting password reset: " + e.getMessage());
            e.printStackTrace(); // More detailed logging for troubleshooting
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred.");
        }
    }

    /**
     * Endpoint to perform the password reset using the token.
     *
     * @param resetDto DTO containing the token and new password.
     * @return ResponseEntity indicating success or failure.
     */
    @PostMapping("/perform-password-reset")
    public ResponseEntity<String> performPasswordReset(@RequestBody PasswordResetDto resetDto) {
        try {
            String result = authenticationService.performPasswordReset(resetDto);
            return ResponseEntity.ok(result);
        } catch (InvalidTokenException | InvalidPasswordException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error performing password reset: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred.");
        }
    }
    
    /**
     * Simple direct password reset endpoint for testing purposes.
     * This endpoint allows resetting a password by providing the email and new password directly.
     * In a production environment, this would be replaced with a more secure flow.
     *
     * @param email the email of the account to reset the password for
     * @param newPassword the new password
     * @return ResponseEntity indicating success or failure
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(String email, String newPassword) {
        try {
            if (email == null || email.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email is required");
            }
            if (newPassword == null || newPassword.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("New password is required");
            }
            
            // Find the account
            Account account = accountRepository.findByEmail(email)
                    .orElseThrow(() -> new EmailNotFoundException("Email not found: " + email));
            
            // Simple validation of the new password
            if (newPassword.length() < 8) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password must be at least 8 characters long");
            }
            
            // Update the password
            account.setPassword(passwordEncoder.encode(newPassword));
            accountRepository.save(account);
            
            return ResponseEntity.ok("Password has been reset successfully");
        } catch (EmailNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error resetting password: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred");
        }
    }
}
