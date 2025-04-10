package ca.mcgill.ecse321.gameorganizer.integration;

import ca.mcgill.ecse321.gameorganizer.dto.request.AuthenticationDTO;
import ca.mcgill.ecse321.gameorganizer.dto.request.CreateEventRequest;
import ca.mcgill.ecse321.gameorganizer.dto.response.UserSummaryDto;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import java.util.List;
import ca.mcgill.ecse321.gameorganizer.models.Event;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.EventRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.GameRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import ca.mcgill.ecse321.gameorganizer.TestJwtConfig;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestJwtConfig.Initializer.class)
public class SimpleAuthTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private GameRepository gameRepository;
    
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private ca.mcgill.ecse321.gameorganizer.services.EventService eventService;
    
    private GameOwner testUser;
    private static final String TEST_PASSWORD = "password123";
    
    @BeforeEach
    public void setup() {
        // Clean repositories
        eventRepository.deleteAll();
        gameRepository.deleteAll();
        accountRepository.deleteAll();
        
        // Create test user as a GameOwner 
        testUser = new GameOwner("testuser", "testuser@example.com", passwordEncoder.encode(TEST_PASSWORD));
        testUser = (GameOwner) accountRepository.save(testUser);
        System.out.println("Created testUser with ID: " + testUser.getId() + ", email: " + testUser.getEmail());
        
        // Clear security context
        SecurityContextHolder.clearContext();
    }
    
    @AfterEach
    public void cleanup() {
        eventRepository.deleteAll();
        gameRepository.deleteAll();
        accountRepository.deleteAll();
        
        // Clear security context
        SecurityContextHolder.clearContext();
    }
    
    // Helper method to set up authentication for tests
    private void authenticateUser(String email) {
        UserDetails userDetails = new User(email, "", 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        
        SecurityContextHolder.getContext().setAuthentication(auth);
        System.out.println("Set up authentication for: " + email);
    }
    
    @Test
    public void testBasicAuthentication() {
        // Create login request
        AuthenticationDTO loginRequest = new AuthenticationDTO();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword(TEST_PASSWORD);
        
        // Create headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Create entity
        HttpEntity<AuthenticationDTO> requestEntity = new HttpEntity<>(loginRequest, headers);
        
        // Log information for debugging
        System.out.println("Login Request:");
        System.out.println("URL: http://localhost:" + port + "/auth/login");
        System.out.println("Email: " + testUser.getEmail());
        
        // Send login request
        ResponseEntity<UserSummaryDto> response = restTemplate.postForEntity(
            "/auth/login",
            requestEntity,
            UserSummaryDto.class
        );
        
        // Assert login successful
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Check for UserSummaryDto in body
        assertNotNull(response.getBody());
        assertEquals(testUser.getId(), response.getBody().getId()); // Check user ID
        assertEquals(testUser.getName(), response.getBody().getName()); // Check user name
        
        // Dump all headers for debugging
        System.out.println("Response headers:");
        response.getHeaders().forEach((key, value) -> {
            System.out.println(key + ": " + value);
        });
        
        // Check for Set-Cookie header
        List<String> cookies = response.getHeaders().get("Set-Cookie");
        assertNotNull(cookies, "Set-Cookie header should be present");
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.startsWith("accessToken=")), "accessToken cookie should be present");
        
        // Extract token for authentication
        String accessToken = cookies.stream()
            .filter(cookie -> cookie.startsWith("accessToken="))
            .findFirst()
            .map(cookie -> cookie.split(";")[0].split("=")[1])
            .orElse(null);
        assertNotNull(accessToken, "Could not extract token from Set-Cookie header");
        
        // Print token for debugging (first few chars)
        System.out.println("Token extracted: " + accessToken.substring(0, Math.min(20, accessToken.length())) + "...");
        
        // Approach 1: Create new headers with Authorization header
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.add("Authorization", "Bearer " + accessToken);
        
        // Print all headers being sent
        System.out.println("Request headers being sent:");
        authHeaders.forEach((key, value) -> {
            System.out.println(key + ": " + value);
        });
        
        HttpEntity<?> authEntity = new HttpEntity<>(authHeaders);
        
        // Send GET request to verify access
        System.out.println("Sending GET request to /api/events with Authorization header");
        ResponseEntity<String> getResponse = restTemplate.exchange(
            "/api/events",
            HttpMethod.GET,
            authEntity,
            String.class
        );
        
        System.out.println("GET response status: " + getResponse.getStatusCode());
    }

    @Test
    public void testCreateEventWithService() {
        // Create a game for the event
        Game testGame = new Game("Test Game", 2, 4, "test.jpg", new java.util.Date());
        testGame.setOwner(testUser);
        testGame = gameRepository.save(testGame);
        
        System.out.println("Game created successfully: " + testGame.getId());
        
        // Authenticate the user before calling the service
        authenticateUser(testUser.getEmail());
        
        // Create event request directly using service
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("New Event");
        request.setDateTime(new java.sql.Date(System.currentTimeMillis()));
        request.setLocation("New Location");
        request.setDescription("New Description");
        request.setMaxParticipants(20);
        request.setFeaturedGame(testGame);
        request.setHost(testUser);
        
        // Call service directly
        System.out.println("Calling eventService.createEvent with email: " + testUser.getEmail());
        
        try {
            ca.mcgill.ecse321.gameorganizer.models.Event event = 
                eventService.createEvent(request);
            
            // Assert event created successfully
            System.out.println("Event created successfully with ID: " + event.getId());
            assertNotNull(event);
            assertEquals("New Event", event.getTitle());
            assertEquals("New Location", event.getLocation());
            assertEquals(20, event.getMaxParticipants());
            
            // Verify event exists in the repository
            assertTrue(eventRepository.findEventById(event.getId()).isPresent());
        } catch (Exception e) {
            System.out.println("Error creating event: " + e.getMessage());
            e.printStackTrace();
            fail("Exception thrown: " + e.getMessage());
        }
    }
}