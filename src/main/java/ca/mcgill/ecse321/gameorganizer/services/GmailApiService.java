package ca.mcgill.ecse321.gameorganizer.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * Service for sending emails using Gmail API with OAuth 2.0 authentication.
 * This service is conditionally loaded based on the 'use.gmail.api' property.
 */
@Service
@ConditionalOnProperty(name = "use.gmail.api", havingValue = "true")
public class GmailApiService {

    private static final Logger log = LoggerFactory.getLogger(GmailApiService.class);
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String USER = "me";

    @Value("${gmail.application.name}")
    private String applicationName;

    @Value("${gmail.user.email}")
    private String userEmail;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    private Gmail gmailService;

    /**
     * Initialize the Gmail service with OAuth 2.0 credentials.
     */
    @PostConstruct
    public void init() {
        try {
            log.info("Initializing Gmail API service");
            // Set up OAuth 2.0 credentials
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = getCredentials(HTTP_TRANSPORT);
            
            // Build Gmail service
            gmailService = new Gmail.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), credential)
                    .setApplicationName(applicationName)
                    .build();
            
            log.info("Gmail API service initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Gmail API service: {}", e.getMessage(), e);
        }
    }

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If there's an error with credentials.
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        log.info("Getting Gmail API credentials");
        
        // Create client secrets from configured values
        String clientSecretsJson = "{"
            + "\"installed\": {"
            + "\"client_id\": \"" + clientId + "\","
            + "\"client_secret\": \"" + clientSecret + "\","
            + "\"redirect_uris\": [\"http://localhost\"],"
            + "\"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\","
            + "\"token_uri\": \"https://oauth2.googleapis.com/token\""
            + "}"
            + "}";
            
        log.info("Client ID: {}", clientId);
        
        // Load client secrets
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(), 
                new StringReader(clientSecretsJson));

        // Build flow and trigger user authorization request
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, 
                GsonFactory.getDefaultInstance(), 
                clientSecrets, 
                Collections.singletonList(GmailScopes.GMAIL_SEND))
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        
        log.info("Starting OAuth2 authorization flow");
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Send an email using Gmail API.
     *
     * @param to      Email recipient
     * @param subject Email subject
     * @param bodyHtml HTML content of the email
     * @throws MessagingException If there's an error creating the email
     * @throws IOException If there's an error sending the email
     */
    public void sendEmail(String to, String subject, String bodyHtml) throws MessagingException, IOException {
        log.info("Preparing to send email to {}", to);
        
        if (gmailService == null) {
            log.error("Gmail service not initialized");
            throw new IllegalStateException("Gmail service not initialized");
        }
        
        // Create email content
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(userEmail));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setContent(bodyHtml, "text/html; charset=utf-8");

        // Encode and send the email
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);

        try {
            // Send the message
            log.info("Sending email via Gmail API");
            message = gmailService.users().messages().send(USER, message).execute();
            log.info("Email sent successfully: {}", message.getId());
        } catch (IOException e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
            throw e;
        }
    }
} 