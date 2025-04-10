package ca.mcgill.ecse321.gameorganizer.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*; // Import matchers
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*; // Import security post processors

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.MediaType; // Added for ContentType
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc; // Import MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders; // Import builders
import com.fasterxml.jackson.databind.ObjectMapper; // Import ObjectMapper
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; // Import AutoConfigureMockMvc
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import ca.mcgill.ecse321.gameorganizer.TestJwtConfig;
import org.junit.jupiter.api.BeforeAll;

// Removed TestRestTemplate, @LocalServerPort, @Import, HttpEntity, HttpHeaders imports

import ca.mcgill.ecse321.gameorganizer.dto.request.ReviewSubmissionDto;
// Removed TestConfig and SecurityConfig imports
import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner; // Import GameOwner
import ca.mcgill.ecse321.gameorganizer.models.Review;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.GameRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.ReviewRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.BorrowRequestRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.EventRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.LendingRecordRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) // Use MOCK environment
@ActiveProfiles("test")
@AutoConfigureMockMvc // Add this annotation
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = TestJwtConfig.Initializer.class)
public class ReviewIntegrationTests {

    // @LocalServerPort // Not needed with MockMvc
    // private int port;

    @Autowired
    private MockMvc mockMvc; // Inject MockMvc

    @Autowired
    private ObjectMapper objectMapper; // Inject ObjectMapper

    // @Autowired // Not needed with MockMvc
    // private TestRestTemplate restTemplate;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BorrowRequestRepository borrowRequestRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private LendingRecordRepository lendingRecordRepository;

    private Account testReviewer;
    private Game testGame;
    private Review testReview;
    private static final String BASE_URL = "/api/reviews";
    private static final String TEST_REVIEWER_EMAIL = "reviewer@example.com"; // Added constant
    private static final String TEST_PASSWORD = "password123"; // Added constant

    @BeforeAll
    public static void setTestEnvironment() {
        System.setProperty("spring.profiles.active", "test");
        
        // Ensure JWT_SECRET is set for tests if not already set
        if (System.getProperty("JWT_SECRET") == null && System.getenv("JWT_SECRET") == null) {
            System.setProperty("JWT_SECRET", "tG8qcqi6M2XZ1s73QTdIHHGhBEzZARBOlDvcxkp4iAoCPU5f8OeYXFmNOkjr9XgJ");
            System.out.println("Setting JWT_SECRET for ReviewIntegrationTests");
        }
    }

    @BeforeEach
    public void setup() {
        // Clean repositories first - order matters due to foreign key constraints
        cleanupRepositories();

        // Create test reviewer with unique email
        String reviewerEmail = TEST_REVIEWER_EMAIL;
        testReviewer = new Account("reviewer", reviewerEmail, passwordEncoder.encode(TEST_PASSWORD));
        testReviewer = accountRepository.save(testReviewer);
        System.out.println("Created testReviewer with ID: " + testReviewer.getId() + ", email: " + testReviewer.getEmail());

        // Create a GameOwner for the test game with a unique email
        String uniqueDummyEmail = "dummy-" + System.currentTimeMillis() + "@owner.com";
        GameOwner gameOwner = new GameOwner("dummyOwner", uniqueDummyEmail, passwordEncoder.encode("dummyPwd"));
        gameOwner = (GameOwner) accountRepository.save(gameOwner); // Save the owner
        System.out.println("Created gameOwner with ID: " + gameOwner.getId() + ", email: " + gameOwner.getEmail());

        // Create test game with unique name and assign the GameOwner
        String uniqueGameName = "Test Game " + System.currentTimeMillis();
        testGame = new Game(uniqueGameName, 2, 4, "test.jpg", new Date());
        testGame.setOwner(gameOwner); // Assign the saved GameOwner
        testGame = gameRepository.save(testGame); // Save the game before creating a review
        System.out.println("Created testGame with ID: " + testGame.getId() + ", name: " + testGame.getName());

        // Create test review with the saved game
        testReview = new Review(4, "Great game!", new Date());
        testReview.setReviewer(testReviewer);
        testReview.setGameReviewed(testGame); // Use the saved Game instance
        testReview = reviewRepository.save(testReview);
        System.out.println("Created testReview with ID: " + testReview.getId());

        // No need to login and store token with MockMvc
    }

    @AfterEach
    public void cleanup() {
        // Clean up after each test
        cleanupRepositories();
    }
    
    /**
     * Helper method to clean up repositories in the correct order
     * to respect foreign key constraints
     */
    private void cleanupRepositories() {
        System.out.println("Cleaning up test data...");
        // First delete reviews
        reviewRepository.deleteAll();
        System.out.println("Deleted all reviews");
        
        // Delete other entities that might reference Game
        try {
            // Delete borrow requests first as they reference Game
            borrowRequestRepository.deleteAll();
            System.out.println("Deleted all borrow requests");
            
            // Delete events as they reference Game
            eventRepository.deleteAll();
            System.out.println("Deleted all events");
            
            // Delete lending records as they reference BorrowRequest and GameOwner
            lendingRecordRepository.deleteAll();
            System.out.println("Deleted all lending records");
        } catch (Exception e) {
            System.out.println("Warning during cleanup of related entities: " + e.getMessage());
        }
        
        // Then delete games
        gameRepository.deleteAll();
        System.out.println("Deleted all games");
        
        // Finally delete accounts (including GameOwner)
        accountRepository.deleteAll();
        System.out.println("Deleted all accounts");
    }

    // ============================================================
    // CREATE Tests (4 tests)
    // ============================================================

    @Test
    @Order(2)
    public void testSubmitReviewWithInvalidGame() throws Exception {
        ReviewSubmissionDto request = new ReviewSubmissionDto(
            5,
            "Excellent game!",
            9999,  // non-existent game id
            TEST_REVIEWER_EMAIL
        );

        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                .with(user(TEST_REVIEWER_EMAIL).password(TEST_PASSWORD).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest()); // Service throws IllegalArgumentException -> 400
    }

    @Test
    @Order(3)
    public void testSubmitReviewMissingRating() throws Exception {
        ReviewSubmissionDto request = new ReviewSubmissionDto(
            0, // Invalid rating
            "Missing rating should be invalid",
            testGame.getId(),
            TEST_REVIEWER_EMAIL
        );

        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                .with(user(TEST_REVIEWER_EMAIL).password(TEST_PASSWORD).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    public void testSubmitReviewWithInvalidReviewer() throws Exception {
        ReviewSubmissionDto request = new ReviewSubmissionDto(
            4,
            "Invalid reviewer email",
            testGame.getId(),
            "nonexistent@example.com" // Non-existent email
        );

        // Authenticate as the valid user, but the DTO contains an invalid reviewer email
        // The service logic should catch this based on the DTO content.
        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                .with(user(TEST_REVIEWER_EMAIL).password(TEST_PASSWORD).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest()); // Service throws IllegalArgumentException -> 400
    }

    // ============================================================
    // UPDATE Tests (3 tests)
    // ============================================================

    @Test
    @Order(5)
    public void testUpdateReviewSuccess() throws Exception {
        ReviewSubmissionDto updateRequest = new ReviewSubmissionDto(
            3,
            "Updated opinion",
            testGame.getId(),
            TEST_REVIEWER_EMAIL // Reviewer ID might be needed by DTO, but service uses auth
        );

        mockMvc.perform(MockMvcRequestBuilders.put(BASE_URL + "/" + testReview.getId())
                .with(user(TEST_REVIEWER_EMAIL).password(TEST_PASSWORD).roles("USER")) // Authenticate as the reviewer
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rating").value(3))
            .andExpect(jsonPath("$.comment").value("Updated opinion"));
    }

    @Test
    @Order(6)
    public void testUpdateNonExistentReview() throws Exception {
        ReviewSubmissionDto updateRequest = new ReviewSubmissionDto(
            3,
            "Updated opinion",
            testGame.getId(),
            TEST_REVIEWER_EMAIL
        );

        mockMvc.perform(MockMvcRequestBuilders.put(BASE_URL + "/999") // Non-existent ID
                .with(user(TEST_REVIEWER_EMAIL).password(TEST_PASSWORD).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isNotFound()); // Service throws ResourceNotFoundException -> 404
    }

    @Test
    @Order(7)
    public void testUpdateReviewWithInvalidRating() throws Exception {
        ReviewSubmissionDto updateRequest = new ReviewSubmissionDto(
            6, // invalid rating
            "Rating out of range",
            testGame.getId(),
            TEST_REVIEWER_EMAIL
        );

        mockMvc.perform(MockMvcRequestBuilders.put(BASE_URL + "/" + testReview.getId())
                .with(user(TEST_REVIEWER_EMAIL).password(TEST_PASSWORD).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isBadRequest()); // Service throws IllegalArgumentException -> 400
    }

    // ============================================================
    // DELETE Tests (3 tests)
    // ============================================================

    @Test
    @Order(8)
    public void testDeleteReviewSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(BASE_URL + "/" + testReview.getId())
                .with(user(TEST_REVIEWER_EMAIL).password(TEST_PASSWORD).roles("USER"))) // Authenticate as reviewer
            .andExpect(status().isOk()); // Expect 200 OK

        assertFalse(reviewRepository.findById(testReview.getId()).isPresent());
    }

    @Test
    @Order(9)
    public void testDeleteNonExistentReview() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(BASE_URL + "/999") // Non-existent ID
                .with(user(TEST_REVIEWER_EMAIL).password(TEST_PASSWORD).roles("USER")))
            .andExpect(status().isNotFound()); // Service throws ResourceNotFoundException -> 404
    }

    @Test
    @Order(10)
    public void testDeleteReviewTwice() throws Exception {
        // First deletion
        mockMvc.perform(MockMvcRequestBuilders.delete(BASE_URL + "/" + testReview.getId())
                .with(user(TEST_REVIEWER_EMAIL).password(TEST_PASSWORD).roles("USER")))
            .andExpect(status().isOk()); // Expect 200 OK

        // Second deletion should fail (not found) -> 404 NOT_FOUND
        mockMvc.perform(MockMvcRequestBuilders.delete(BASE_URL + "/" + testReview.getId())
                .with(user(TEST_REVIEWER_EMAIL).password(TEST_PASSWORD).roles("USER")))
            .andExpect(status().isNotFound());
    }

    // ============================================================
    // GET Tests
    // ============================================================

    @Test
    @Order(11)
    public void testGetReviewByIdSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/" + testReview.getId())
                .with(user(TEST_REVIEWER_EMAIL).password(TEST_PASSWORD).roles("USER"))) // Authenticate as reviewer
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rating").value(testReview.getRating()))
            .andExpect(jsonPath("$.comment").value(testReview.getComment()));
    }


    // ----- Security: 401 Unauthorized Tests -----

    @Test
    @Order(14) // Renumbered
    public void testSubmitReviewUnauthenticated() throws Exception {
        ReviewSubmissionDto request = new ReviewSubmissionDto(
            5, "Unauth Review", testGame.getId(), TEST_REVIEWER_EMAIL
        );

        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                .with(anonymous()) // Attempt unauthenticated
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized()); // Expect 401
    }

    @Test
    @Order(15) // Renumbered
    public void testUpdateReviewUnauthenticated() throws Exception {
        ReviewSubmissionDto updateRequest = new ReviewSubmissionDto(
            3, "Unauth Update", testGame.getId(), TEST_REVIEWER_EMAIL
        );

        mockMvc.perform(MockMvcRequestBuilders.put(BASE_URL + "/" + testReview.getId())
                .with(anonymous()) // Attempt unauthenticated
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isUnauthorized()); // Expect 401
    }

    @Test
    @Order(16) // Renumbered
    public void testDeleteReviewUnauthenticated() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(BASE_URL + "/" + testReview.getId())
                .with(anonymous())) // Attempt unauthenticated
            .andExpect(status().isUnauthorized()); // Expect 401
    }

    @Test
    @Order(17) // Renumbered
    public void testGetReviewByIdUnauthenticated() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/" + testReview.getId())
                .with(anonymous())) // Attempt unauthenticated
            .andExpect(status().isUnauthorized()); // Expect 401
    }

    // ----- Security: 403 Forbidden Tests -----

    @Test
    @Order(18) // Renumbered
    public void testUpdateAnotherUserReviewForbidden() throws Exception {
        // Create another user
        Account otherUser = accountRepository.save(new Account("other", "other@example.com", passwordEncoder.encode("otherpass")));

        ReviewSubmissionDto updateRequest = new ReviewSubmissionDto(
            2, "Forbidden Update", testGame.getId(), TEST_REVIEWER_EMAIL // DTO still refers to original reviewer
        );

        // Authenticate as the other user trying to update the original review
        mockMvc.perform(MockMvcRequestBuilders.put(BASE_URL + "/" + testReview.getId())
                .with(user(otherUser.getEmail()).password("otherpass").roles("USER")) 
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isForbidden()); // Expect 403 (assuming service check on reviewer)
    }

    @Test
    @Order(19) // Renumbered
    public void testDeleteAnotherUserReviewForbidden() throws Exception {
        // Create another user
        Account otherUser = accountRepository.save(new Account("other", "other@example.com", passwordEncoder.encode("otherpass")));

        // Authenticate as the other user trying to delete the original review
        mockMvc.perform(MockMvcRequestBuilders.delete(BASE_URL + "/" + testReview.getId())
                .with(user(otherUser.getEmail()).password("otherpass").roles("USER"))) 
            .andExpect(status().isForbidden()); // Expect 403 (assuming service check on reviewer)
    }

}
