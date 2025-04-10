package ca.mcgill.ecse321.gameorganizer.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Service for handling email-related functionality.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private Environment environment;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;
    
    @Value("${email.send.in.dev:false}")
    private boolean sendEmailInDev;

    /**
     * Sends a password reset email to the user with a reset link.
     *
     * @param toEmail The recipient's email address
     * @param token The password reset token
     * @param username The user's name/username for personalization
     * @throws MessagingException If there's an error sending the email
     */
    public void sendPasswordResetEmail(String toEmail, String token, String username) throws MessagingException {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        
        String htmlContent = 
            "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;'>" +
            "   <div style='text-align: center; margin-bottom: 20px;'>" +
            "       <h1 style='color: #333;'>Password Reset Request</h1>" +
            "   </div>" +
            "   <div style='color: #555; line-height: 1.5;'>" +
            "       <p>Hello " + (username != null ? username : "there") + ",</p>" +
            "       <p>We received a request to reset your password for your BoardGameConnect account. Click the button below to set a new password:</p>" +
            "       <div style='text-align: center; margin: 30px 0;'>" +
            "           <a href='" + resetUrl + "' style='background-color: #4a56e2; color: white; padding: 12px 25px; text-decoration: none; border-radius: 4px; font-weight: bold;'>Reset Password</a>" +
            "       </div>" +
            "       <p>If the button doesn't work, copy and paste this link into your browser:</p>" +
            "       <p style='word-break: break-all;'><a href='" + resetUrl + "'>" + resetUrl + "</a></p>" +
            "       <p>If you did not request a password reset, please ignore this email or contact support if you have concerns.</p>" +
            "       <p>This link will expire in 30 minutes for security reasons.</p>" +
            "       <p><strong>Note:</strong> This email might be delivered to your spam folder. Please check there if you don't see it in your inbox.</p>" +
            "       <p>Regards,<br>The BoardGameConnect Team</p>" +
            "   </div>" +
            "</div>";
            
        // Use traditional SMTP with credentials from environment variables
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("BoardGameConnect - Reset Your Password");
        helper.setText(htmlContent, true); // true indicates HTML content
        
        try {
            log.info("Sending password reset email via SMTP to: {}", toEmail);
            log.info("Using mail username: {}", fromEmail);
            log.debug("Email content set, attempting to send message");
            log.debug("Reset URL: {}", resetUrl);
            
            // In dev mode, just log the URL but don't actually send the email
            boolean isDevMode = Arrays.asList(environment.getActiveProfiles()).contains("dev");
            if (isDevMode && !sendEmailInDev) {
                log.info("DEV MODE: Not sending email. Reset link would be: {}", resetUrl);
                log.info("To reset password, go to: {}", resetUrl);
                log.info("To send real emails in dev mode, set email.send.in.dev=true in application.properties");
            } else {
                mailSender.send(message);
                log.info("Password reset email sent successfully via SMTP to: {}", toEmail);
            }
        } catch (Exception e) {
            log.error("Failed to send password reset email via SMTP to: {}, Error: {}", toEmail, e.getMessage(), e);
            // Get more detailed error information
            if (e.getCause() != null) {
                log.error("Cause: {}", e.getCause().getMessage());
            }
            
            // Add more helpful debugging information
            log.error("SMTP Server: {}:{}", 
                environment.getProperty("spring.mail.host"),
                environment.getProperty("spring.mail.port"));
            log.error("SSL Enabled: {}", 
                environment.getProperty("spring.mail.properties.mail.smtp.ssl.enable"));
            
            throw new MessagingException("Failed to send password reset email: " + e.getMessage() + 
                ". Please check your mail server configuration or use the development endpoints.", e);
        }
    }
} 