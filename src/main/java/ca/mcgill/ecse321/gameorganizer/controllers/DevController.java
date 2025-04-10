package ca.mcgill.ecse321.gameorganizer.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * Development-only controller with endpoints to assist in testing.
 * This controller should NEVER be available in production environments.
 * 
 * It is restricted to 'dev' profile only.
 */
@RestController
@RequestMapping("/dev")
@Profile("dev") // Only available in dev profile
public class DevController {

    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private Environment environment;
    
    /**
     * Retrieves a password reset token for a specified email.
     * This endpoint is for development and testing purposes only.
     * 
     * @param email The email address to get the token for
     * @return The reset token if found
     */
    @GetMapping("/password-reset-token/{email}")
    public ResponseEntity<String> getResetTokenForEmail(@PathVariable String email) {
        // Double-check that we're not in production
        if (isProduction()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (accountOpt.isEmpty() || accountOpt.get().getResetPasswordToken() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No reset token found for " + email);
        }
        
        Account account = accountOpt.get();
        // For security, in a real app, we would never expose this endpoint!
        return ResponseEntity.ok("Reset token for " + email + ": " + account.getResetPasswordToken() + 
                               "\nUse this URL to reset your password: http://localhost:5174/reset-password?token=" + 
                               account.getResetPasswordToken());
    }
    
    /**
     * Checks if the application is running in production mode.
     * 
     * @return true if in production, false otherwise
     */
    private boolean isProduction() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if (profile.equals("prod") || profile.equals("production")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates a password reset token for a user without sending an email (for development)
     * @param email The email of the user
     * @return Success message or error
     */
    @PostMapping("/generate-reset-token")
    public ResponseEntity<String> generateResetToken(@RequestParam String email) {
        try {
            Optional<Account> accountOpt = accountRepository.findByEmail(email);
            if (accountOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Account account = accountOpt.get();
            String token = UUID.randomUUID().toString();
            account.setResetPasswordToken(token);
            account.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(30));
            accountRepository.save(account);
            
            return ResponseEntity.ok("Reset token generated for " + email + ": " + token + 
                                     "\nUse this URL to reset your password: http://localhost:5174/reset-password?token=" + token);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
} 