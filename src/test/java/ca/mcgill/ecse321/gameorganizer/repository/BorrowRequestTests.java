package ca.mcgill.ecse321.gameorganizer.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.BorrowRequest;
import ca.mcgill.ecse321.gameorganizer.models.BorrowRequestStatus;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.BorrowRequestRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.GameRepository;

@DataJpaTest
public class BorrowRequestTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BorrowRequestRepository borrowRequestRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private AccountRepository accountRepository;

    private GameOwner testOwner;
    private Game testGame;
    private Account testRequester;
    private Date testStartDate;
    private Date testEndDate;

    @BeforeEach
    public void setUp() {
        // Create test owner
        testOwner = new GameOwner("owner", "owner@test.com", "password");
        testOwner = entityManager.persist(testOwner);

        // Create test game
        testGame = new Game("Test Game", 2, 4, "test.jpg", new Date());
        testGame.setOwner(testOwner);
        testGame = entityManager.persist(testGame);

        // Create test requester
        testRequester = new Account("requester", "requester@test.com", "password");
        testRequester = entityManager.persist(testRequester);

        // Set test dates
        testStartDate = new Date(System.currentTimeMillis() + 3600 * 1000); // 1 hour later
        testEndDate = new Date(System.currentTimeMillis() + 7200 * 1000);   // 2 hours later

        entityManager.flush();
    }

    @AfterEach
    public void cleanUp() {
        borrowRequestRepository.deleteAll();
        entityManager.flush();
    }

    @Test
    public void testPersistAndLoadBorrowRequest() {
        // Create a new borrow request
        BorrowRequest request = new BorrowRequest();
        request.setRequestedGame(testGame);
        request.setRequester(testRequester);
        request.setStartDate(testStartDate);
        request.setEndDate(testEndDate);
        request.setStatus(BorrowRequestStatus.PENDING);
        request.setRequestDate(new Date());

        // Save the request using entity manager
        request = entityManager.persistAndFlush(request);
        int id = request.getId();

        // Clear persistence context
        entityManager.clear();

        // Retrieve the request
        Optional<BorrowRequest> retrievedOpt = borrowRequestRepository.findBorrowRequestById(id);
        assertTrue(retrievedOpt.isPresent(), "The borrow request should be present in the database");

        BorrowRequest retrieved = retrievedOpt.get();
        assertEquals(testGame.getId(), retrieved.getRequestedGame().getId());
        assertEquals(testRequester.getId(), retrieved.getRequester().getId());
        assertEquals(testStartDate, retrieved.getStartDate());
        assertEquals(testEndDate, retrieved.getEndDate());
        assertEquals(BorrowRequestStatus.PENDING, retrieved.getStatus());
    }

    @Test
    public void testFindByRequester() {
        // Create and save a borrow request
        BorrowRequest request = new BorrowRequest();
        request.setRequestedGame(testGame);
        request.setRequester(testRequester);
        request.setStartDate(testStartDate);
        request.setEndDate(testEndDate);
        request.setStatus(BorrowRequestStatus.PENDING);
        request.setRequestDate(new Date());
        entityManager.persistAndFlush(request);
        entityManager.clear();

        // Find requests by requester
        List<BorrowRequest> requests = borrowRequestRepository.findByRequester(testRequester);
        assertEquals(1, requests.size());
        assertEquals(testRequester.getId(), requests.get(0).getRequester().getId());
    }

    @Test
    public void testFindByRequesterName() {
        // Create and save a borrow request
        BorrowRequest request = new BorrowRequest();
        request.setRequestedGame(testGame);
        request.setRequester(testRequester);
        request.setStartDate(testStartDate);
        request.setEndDate(testEndDate);
        request.setStatus(BorrowRequestStatus.PENDING);
        request.setRequestDate(new Date());
        entityManager.persistAndFlush(request);
        entityManager.clear();

        // Find requests by requester name
        List<BorrowRequest> requests = borrowRequestRepository.findBorrowRequestsByRequesterName(testRequester.getName());
        assertEquals(1, requests.size());
        assertEquals(testRequester.getName(), requests.get(0).getRequester().getName());
    }

    @Test
    public void testFindByOwnerAndStatus() {
        // Create and save requests with different statuses
        BorrowRequest pendingRequest = new BorrowRequest();
        pendingRequest.setRequestedGame(testGame);
        pendingRequest.setRequester(testRequester);
        pendingRequest.setStartDate(testStartDate);
        pendingRequest.setEndDate(testEndDate);
        pendingRequest.setStatus(BorrowRequestStatus.PENDING);
        pendingRequest.setRequestDate(new Date());
        entityManager.persist(pendingRequest);

        BorrowRequest approvedRequest = new BorrowRequest();
        approvedRequest.setRequestedGame(testGame);
        approvedRequest.setRequester(testRequester);
        approvedRequest.setStartDate(new Date(testStartDate.getTime() + 86400000)); // Next day
        approvedRequest.setEndDate(new Date(testEndDate.getTime() + 86400000));
        approvedRequest.setStatus(BorrowRequestStatus.APPROVED);
        approvedRequest.setRequestDate(new Date());
        entityManager.persist(approvedRequest);

        entityManager.flush();
        entityManager.clear();

        // Find requests by owner and status
        List<BorrowRequest> pendingRequests = borrowRequestRepository.findByRequestedGame_OwnerAndStatus(testOwner, BorrowRequestStatus.PENDING);
        List<BorrowRequest> approvedRequests = borrowRequestRepository.findByRequestedGame_OwnerAndStatus(testOwner, BorrowRequestStatus.APPROVED);

        assertEquals(1, pendingRequests.size());
        assertEquals(1, approvedRequests.size());
        assertEquals(BorrowRequestStatus.PENDING, pendingRequests.get(0).getStatus());
        assertEquals(BorrowRequestStatus.APPROVED, approvedRequests.get(0).getStatus());
    }

    @Test
    public void testFindOverlappingRequests() {
        // Create and save an approved request
        BorrowRequest request1 = new BorrowRequest();
        request1.setRequestedGame(testGame);
        request1.setRequester(testRequester);
        request1.setStartDate(testStartDate);
        request1.setEndDate(testEndDate);
        request1.setStatus(BorrowRequestStatus.APPROVED);
        request1.setRequestDate(new Date());
        request1 = entityManager.persistAndFlush(request1);
        entityManager.clear();

        // Find overlapping approved requests
        List<BorrowRequest> overlapping = borrowRequestRepository.findOverlappingApprovedRequests(
            testGame.getId(),
            testStartDate,
            testEndDate
        );

        assertEquals(1, overlapping.size());
        assertEquals(request1.getId(), overlapping.get(0).getId());
    }

    @Test
    public void testNonExistentRequest() {
        // Try to find a request that doesn't exist
        Optional<BorrowRequest> nonExistent = borrowRequestRepository.findBorrowRequestById(999);
        assertFalse(nonExistent.isPresent(), "Should not find non-existent request");
    }

    @Test
    public void testDeleteRequest() {
        // Create and save a request
        BorrowRequest request = new BorrowRequest();
        request.setRequestedGame(testGame);
        request.setRequester(testRequester);
        request.setStartDate(testStartDate);
        request.setEndDate(testEndDate);
        request.setStatus(BorrowRequestStatus.PENDING);
        request.setRequestDate(new Date());
        request = entityManager.persistAndFlush(request);
        int id = request.getId();

        // Delete the request
        borrowRequestRepository.delete(request);
        entityManager.flush();

        // Verify deletion
        Optional<BorrowRequest> deleted = borrowRequestRepository.findBorrowRequestById(id);
        assertFalse(deleted.isPresent(), "Request should be deleted");
    }
}
