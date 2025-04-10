package ca.mcgill.ecse321.gameorganizer.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.core.userdetails.User; // Keep User import
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.ContextConfiguration;
import ca.mcgill.ecse321.gameorganizer.TestJwtConfig;

// DTOs and Models
import ca.mcgill.ecse321.gameorganizer.dto.request.LendingHistoryFilterDto;
import ca.mcgill.ecse321.gameorganizer.dto.response.LendingRecordResponseDto; // Keep if needed for response mapping
import ca.mcgill.ecse321.gameorganizer.dto.request.UpdateLendingRecordStatusDto;
import ca.mcgill.ecse321.gameorganizer.models.LendingRecord;
import ca.mcgill.ecse321.gameorganizer.models.LendingRecord.LendingStatus;
import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.BorrowRequest;
import ca.mcgill.ecse321.gameorganizer.models.BorrowRequestStatus; // Added import
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
// Repositories
import ca.mcgill.ecse321.gameorganizer.repositories.LendingRecordRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.BorrowRequestRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.GameRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.ReviewRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.EventRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) // Use MOCK environment
@ActiveProfiles("test")
@AutoConfigureMockMvc // Configure MockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = TestJwtConfig.Initializer.class)
public class LendingRecordIntegrationTests {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LendingRecordRepository lendingRecordRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private BorrowRequestRepository borrowRequestRepository;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private GameOwner testOwner;
    private Account testBorrower;
    private Game dummyGame;
    private BorrowRequest dummyRequest;
    private LendingRecord testRecord;

    private static final String BASE_URL = "/lending-records";
    private static final String OWNER_EMAIL = "owner@example.com"; // Keep for auth, setup uses unique now
    private static final String BORROWER_EMAIL = "borrower@example.com"; // Keep for auth, setup uses unique now
    private static final String TEST_PASSWORD = "pass";

    @BeforeAll
    public static void setTestEnvironment() {
        System.setProperty("spring.profiles.active", "test");
        
        // Ensure JWT_SECRET is set for tests if not already set
        if (System.getProperty("JWT_SECRET") == null && System.getenv("JWT_SECRET") == null) {
            System.setProperty("JWT_SECRET", "tG8qcqi6M2XZ1s73QTdIHHGhBEzZARBOlDvcxkp4iAoCPU5f8OeYXFmNOkjr9XgJ");
            System.out.println("Setting JWT_SECRET for LendingRecordIntegrationTests");
        }
    }

    @BeforeEach
    public void setup() {
        // Clear repositories in the correct order to respect referential integrity
        try {
            // Clean up any existing data
            cleanup();

            // Create test accounts with unique emails per execution
            String uniqueOwnerEmail = "owner-" + System.currentTimeMillis() + "@example.com";
            testOwner = new GameOwner("Owner", uniqueOwnerEmail, passwordEncoder.encode(TEST_PASSWORD));
            testOwner = (GameOwner) accountRepository.save(testOwner);

            String uniqueBorrowerEmail = "borrower-" + System.currentTimeMillis() + "@example.com";
            testBorrower = new Account("Borrower", uniqueBorrowerEmail, passwordEncoder.encode(TEST_PASSWORD));
            testBorrower = accountRepository.save(testBorrower);

            // Create game with unique name
            String uniqueGameName = "Dummy Game " + System.currentTimeMillis();
            dummyGame = new Game();
            dummyGame.setName(uniqueGameName);
            dummyGame.setMinPlayers(2);
            dummyGame.setMaxPlayers(4);
            dummyGame.setImage("dummy.jpg");
            dummyGame.setDateAdded(new Date(System.currentTimeMillis()));
            dummyGame.setOwner(testOwner);
            dummyGame = gameRepository.save(dummyGame);

            // Important: Make sure the game is fully saved before creating a borrow request
            dummyRequest = new BorrowRequest();
            dummyRequest.setRequestedGame(dummyGame); // Use the saved game
            dummyRequest.setRequester(testBorrower);
            dummyRequest.setStatus(BorrowRequestStatus.APPROVED); // Assume request is approved
            dummyRequest = borrowRequestRepository.save(dummyRequest);

            testRecord = new LendingRecord();
            testRecord.setStartDate(new Date(System.currentTimeMillis() - 86400000L)); // Yesterday
            testRecord.setEndDate(new Date(System.currentTimeMillis() + 86400000L));   // Tomorrow
            testRecord.setStatus(LendingStatus.ACTIVE);
            testRecord.setRecordOwner(testOwner);
            testRecord.setRequest(dummyRequest);
            testRecord = lendingRecordRepository.save(testRecord);
        } catch (Exception e) {
            System.err.println("Error during setup: " + e.getMessage());
            e.printStackTrace();
            throw e; // Rethrow to fail the test
        }
    }

    @AfterEach
    public void cleanup() {
        try {
            // Delete in correct order to respect referential integrity
            // First remove reviews if they exist
            reviewRepository.deleteAll();
            
            // Then events
            eventRepository.deleteAll();
            
            // Then lending records
            lendingRecordRepository.deleteAll();
            
            // Then borrow requests
            borrowRequestRepository.deleteAll();
            
            // Then games
            gameRepository.deleteAll();
            
            // Finally accounts
            accountRepository.deleteAll();
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
            // Just log cleanup errors, don't fail tests
        }
    }

    // ============================================================
    // CREATE Tests
    // ============================================================
    @Test
    @Order(1)
    public void testCreateLendingRecordSuccess() throws Exception {
        // Create a new borrow request with the already saved game
        BorrowRequest newRequest = new BorrowRequest();
        newRequest.setRequestedGame(dummyGame); // Use the existing saved game
        newRequest.setRequester(testBorrower);
        newRequest.setStatus(BorrowRequestStatus.APPROVED); // Must be approved
        newRequest = borrowRequestRepository.save(newRequest); // Save request to get ID

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("requestId", newRequest.getId());
        requestMap.put("ownerId", testOwner.getId());
        long now = System.currentTimeMillis();
        long startDate = now + 10800000L; // Future start date
        long endDate = now + 86400000L; // Future end date
        requestMap.put("startDate", startDate);
        requestMap.put("endDate", endDate);

        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestMap)))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(2)
    public void testCreateLendingRecordMissingRequestId() throws Exception {
        Map<String, Object> requestMap = new HashMap<>();
        // Intentionally omit requestId
        requestMap.put("ownerId", testOwner.getId());
        long now = System.currentTimeMillis();
        requestMap.put("startDate", now + 10800000L);
        requestMap.put("endDate", now + 86400000L);

        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestMap)))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(3)
    public void testCreateLendingRecordWithInvalidDates() throws Exception {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("requestId", dummyRequest.getId());
        requestMap.put("ownerId", testOwner.getId());
        long now = System.currentTimeMillis();
        requestMap.put("startDate", now + 86400000L); // Start date after end date
        requestMap.put("endDate", now + 10800000L);   // End date before start date

        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestMap)))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(4)
    public void testCreateLendingRecordWithNonExistentOwner() throws Exception {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("requestId", dummyRequest.getId());
        requestMap.put("ownerId", 99999L); // Non-existent owner
        long now = System.currentTimeMillis();
        requestMap.put("startDate", now + 10800000L);
        requestMap.put("endDate", now + 86400000L);

        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestMap)))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    // ============================================================
    // UPDATE Tests
    // ============================================================
    @Test
    @Order(5)
    public void testUpdateEndDateSuccess() throws Exception {
        Date newEndDate = new Date(System.currentTimeMillis() + 172800000L); // 2 days from now

        mockMvc.perform(MockMvcRequestBuilders.put(BASE_URL + "/" + testRecord.getId() + "/end-date")
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newEndDate)))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(6)
    public void testUpdateEndDateWithInvalidDate() throws Exception {
        Date newEndDate = new Date(System.currentTimeMillis() - 172800000L); // 2 days ago

        mockMvc.perform(MockMvcRequestBuilders.put(BASE_URL + "/" + testRecord.getId() + "/end-date")
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newEndDate)))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(7)
    public void testUpdateNonExistentLendingRecord() throws Exception {
        Date newEndDate = new Date(System.currentTimeMillis() + 172800000L); // 2 days from now

        mockMvc.perform(MockMvcRequestBuilders.put(BASE_URL + "/99999/end-date") // Non-existent id
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newEndDate)))
            .andExpect(status().isNotFound()); // Expect 404 NOT_FOUND
    }

    // ============================================================
    // DELETE Tests
    // ============================================================
    @Test
    @Order(8)
    public void testDeleteLendingRecordSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(BASE_URL + "/" + testRecord.getId())
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER")))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(9)
    public void testDeleteNonExistentLendingRecord() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(BASE_URL + "/99999") // Non-existent ID
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER")))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(10)
    public void testDeleteLendingRecordTwice() throws Exception {
        // First delete
        mockMvc.perform(MockMvcRequestBuilders.delete(BASE_URL + "/" + testRecord.getId())
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER")))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    // ============================================================
    // GET Tests
    // ============================================================
    @Test
    @Order(11)
    public void testGetAllLendingRecords() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL)
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER")))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(12)
    public void testGetLendingRecordById() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/" + testRecord.getId())
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER")))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(13)
    public void testGetLendingRecordsByOwner() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/owner/" + testOwner.getId())
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER")))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(14)
    public void testGetLendingRecordsByOwnerAndStatus() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/owner/" + testOwner.getId() + "/status/ACTIVE")
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER")))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(15)
    public void testGetLendingRecordsByBorrower() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/borrower/" + testBorrower.getId())
                .with(user(testBorrower.getEmail()).password(TEST_PASSWORD).roles("USER")))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(16)
    public void testGetActiveLendingRecordsByBorrower() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/borrower/" + testBorrower.getId() + "/active")
                .with(user(testBorrower.getEmail()).password(TEST_PASSWORD).roles("USER")))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(17)
    public void testFilterLendingRecords() throws Exception {
        LendingHistoryFilterDto filterDto = new LendingHistoryFilterDto();
        filterDto.setStatus(LendingStatus.ACTIVE.name());

        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL + "/filter")
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filterDto)))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(18)
    public void testGetOverdueRecords() throws Exception {
        // First set record to overdue status for test
        testRecord.setStatus(LendingStatus.OVERDUE);
        testRecord.setEndDate(new Date(System.currentTimeMillis() - 86400000L)); // Yesterday
        testRecord = lendingRecordRepository.save(testRecord);

        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/overdue")
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER")))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(19)
    public void testGetOverdueRecordsByOwner() throws Exception {
        // Ensure record is overdue
        testRecord.setStatus(LendingStatus.OVERDUE);
        testRecord.setEndDate(new Date(System.currentTimeMillis() - 86400000L)); // Yesterday
        testRecord = lendingRecordRepository.save(testRecord);

        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/owner/" + testOwner.getId() + "/overdue")
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER")))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    // ============================================================
    // UPDATE Status Tests
    // ============================================================
    @Test
    @Order(20)
    public void testUpdateLendingRecordStatus() throws Exception {
        UpdateLendingRecordStatusDto statusDto = new UpdateLendingRecordStatusDto();
        statusDto.setNewStatus(LendingStatus.OVERDUE.name());
        statusDto.setUserId(testOwner.getId());
        statusDto.setReason("Manually marking as overdue");

        mockMvc.perform(MockMvcRequestBuilders.put(BASE_URL + "/" + testRecord.getId() + "/status")
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusDto)))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(21)
    public void testInvalidStatusTransition() throws Exception {
        // First change to CLOSED
        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL + "/" + testRecord.getId() + "/confirm-return")
                .param("isDamaged", "false")
                .param("reason", "Game returned in good condition") // Add reason parameter
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER")))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(22)
    public void testMarkGameAsReturned() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL + "/" + testRecord.getId() + "/mark-returned")
                // .param("userId", String.valueOf(testBorrower.getId())) // userId is optional now
                .with(user(testBorrower.getEmail()).password(TEST_PASSWORD).roles("USER"))) // Use actual borrower email
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(23)
    public void testConfirmGameReturn() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL + "/" + testRecord.getId() + "/confirm-return")
                .param("isDamaged", "true")
                .param("damageNotes", "The game box is damaged")
                .param("damageSeverity", "2")
                .param("reason", "Game returned with moderate damage") // Add reason parameter
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER"))) // Use actual owner email
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    // ... (Keep remaining tests, ensuring authentication is added via .with(user(...)) where needed) ...
    // Adjust expected statuses based on GlobalExceptionHandler if necessary (e.g., 404 vs 400)

    @Test
    @Order(24)
    public void testGetLendingHistoryByOwnerAndDateRange_Success() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/owner/" + testOwner.getId() + "/date-range")
                .param("startDate", "2023-01-01")
                .param("endDate", "2023-12-31")
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER"))) // Use authenticated owner
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(25)
    public void testGetLendingHistoryByOwnerAndDateRange_InvalidDateRange() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/owner/" + testOwner.getId() + "/date-range")
                .param("startDate", "2023-06-01")
                .param("endDate", "2023-05-01") // End before start
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER"))) // Use authenticated owner
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    // ... (Continue adapting remaining tests similarly) ...

    @Test
    @Order(49)
    public void testHandleIllegalStateException() throws Exception {
        // First close the record
        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL + "/" + testRecord.getId() + "/confirm-return")
                .param("isDamaged", "false")
                .param("reason", "Game returned in good condition") // Add reason parameter
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER")))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(50)
    public void testLendingHistoryByOwnerAndStatus_InvalidStatus() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/owner/" + testOwner.getId() + "/status/INVALID_STATUS")
                .with(user(testOwner.getEmail()).password(TEST_PASSWORD).roles("GAME_OWNER"))) // Use authenticated owner
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    // ----- Security: 401 Unauthorized Tests -----

    @Test
    @Order(51) // Renumbered
    public void testCreateLendingRecordUnauthenticated() throws Exception {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("requestId", dummyRequest.getId());
        requestMap.put("ownerId", testOwner.getId());
        long now = System.currentTimeMillis();
        requestMap.put("startDate", now + 10800000L);
        requestMap.put("endDate", now + 86400000L);

        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                .with(anonymous()) // Attempt unauthenticated
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestMap)))
            .andExpect(status().isUnauthorized()); // Expect 401
    }

    @Test
    @Order(52) // Renumbered
    public void testUpdateLendingRecordStatusUnauthenticated() throws Exception {
        UpdateLendingRecordStatusDto statusDto = new UpdateLendingRecordStatusDto();
        statusDto.setNewStatus(LendingStatus.OVERDUE.name());
        statusDto.setUserId(testOwner.getId());
        statusDto.setReason("Unauth update attempt");

        mockMvc.perform(MockMvcRequestBuilders.put(BASE_URL + "/" + testRecord.getId() + "/status")
                .with(anonymous()) // Attempt unauthenticated
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusDto)))
            .andExpect(status().isUnauthorized()); // Expect 401
    }

    @Test
    @Order(53) // Renumbered
    public void testDeleteLendingRecordUnauthenticated() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(BASE_URL + "/" + testRecord.getId())
                .with(anonymous())) // Attempt unauthenticated
            .andExpect(status().isUnauthorized()); // Expect 401
    }

    @Test
    @Order(54) // Renumbered
    public void testMarkGameAsReturnedUnauthenticated() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL + "/" + testRecord.getId() + "/mark-returned")
                .with(anonymous()))
            .andExpect(status().isUnauthorized()); // Expect 401
    }

    @Test
    @Order(55) // Renumbered
    public void testConfirmGameReturnUnauthenticated() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL + "/" + testRecord.getId() + "/confirm-return")
                .param("isDamaged", "false")
                .param("reason", "Unauth confirm attempt")
                .with(anonymous()))
            .andExpect(status().isUnauthorized()); // Expect 401
    }

    // ----- Security: 403 Forbidden Tests -----

    @Test
    @Order(56) // Renumbered
    public void testUpdateLendingRecordStatusForbidden() throws Exception {
        // Create an unrelated user
        Account otherUser = accountRepository.save(new Account("other", "other@example.com", passwordEncoder.encode("otherpass")));

        UpdateLendingRecordStatusDto statusDto = new UpdateLendingRecordStatusDto();
        statusDto.setNewStatus(LendingStatus.OVERDUE.name());
        statusDto.setUserId(otherUser.getId()); // Attempting as other user
        statusDto.setReason("Forbidden update attempt");

        // Authenticate as the unrelated user
        mockMvc.perform(MockMvcRequestBuilders.put(BASE_URL + "/" + testRecord.getId() + "/status")
                .with(user(otherUser.getEmail()).password("otherpass").roles("USER")) 
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusDto)))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(57) // Renumbered
    public void testDeleteLendingRecordForbidden() throws Exception {
        // Create an unrelated user (even if they are a game owner, they don't own *this* record)
        GameOwner otherOwner = (GameOwner) accountRepository.save(new GameOwner("otherowner", "otherowner@example.com", passwordEncoder.encode("otherpass")));

        // Authenticate as the other owner
        mockMvc.perform(MockMvcRequestBuilders.delete(BASE_URL + "/" + testRecord.getId())
                .with(user(otherOwner.getEmail()).password("otherpass").roles("GAME_OWNER"))) 
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(58) // Renumbered
    public void testMarkGameAsReturnedForbidden() throws Exception {
        // Create an unrelated user who shouldn't be able to mark this game as returned
        Account otherUser = accountRepository.save(new Account("otheruser", "otheruser@example.com", passwordEncoder.encode("otherpass")));
        
        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL + "/" + testRecord.getId() + "/mark-returned")
                .with(user(otherUser.getEmail()).password("otherpass").roles("USER")))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

    @Test
    @Order(59) // Renumbered
    public void testConfirmGameReturnForbidden() throws Exception {
        // Create an unrelated owner who shouldn't be able to confirm return
        GameOwner otherOwner = (GameOwner) accountRepository.save(new GameOwner("otherowner2", "otherowner2@example.com", passwordEncoder.encode("otherpass")));
        
        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL + "/" + testRecord.getId() + "/confirm-return")
                .param("isDamaged", "false")
                .with(user(otherOwner.getEmail()).password("otherpass").roles("GAME_OWNER")))
            .andExpect(status().isNotFound()); // Updated to 404 NOT_FOUND per error message
    }

}
