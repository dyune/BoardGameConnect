package ca.mcgill.ecse321.gameorganizer.repository;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import ca.mcgill.ecse321.gameorganizer.models.LendingRecord;
import ca.mcgill.ecse321.gameorganizer.repositories.LendingRecordRepository;

@DataJpaTest
public class LendingRecordRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private LendingRecordRepository lendingRecordRepository;

    private GameOwner owner;
    private Game game;
    private Account borrower;
    private BorrowRequest request;
    private LendingRecord record;
    private Date startDate;
    private Date endDate;

    @BeforeEach
    public void setup() {
        // Create test data with future dates
        startDate = new Date(System.currentTimeMillis() + 86400000); // 1 day in future
        endDate = new Date(startDate.getTime() + 7 * 24 * 60 * 60 * 1000); // 7 days after start

        owner = new GameOwner("Test Owner", "owner@test.com", "password123");
        owner = entityManager.persist(owner);

        game = new Game("Test Game", 2, 4, "test.jpg", new Date());
        game.setOwner(owner);
        game = entityManager.persist(game);

        borrower = new Account("Test Borrower", "borrower@test.com", "password123");
        borrower = entityManager.persist(borrower);

        request = new BorrowRequest(startDate, endDate, BorrowRequestStatus.APPROVED, new Date(), game, null);
        request.setRequester(borrower);
        request.setResponder(owner);
        request = entityManager.persist(request);

        record = new LendingRecord(startDate, endDate, LendingRecord.LendingStatus.ACTIVE, request, owner);
        record = entityManager.persistAndFlush(record);

        entityManager.clear();
    }

    @AfterEach
    public void cleanup() {
        lendingRecordRepository.deleteAll();
        entityManager.flush();
    }

    @Test
    public void testPersistAndLoadLendingRecord() {
        // Test basic persistence
        LendingRecord found = lendingRecordRepository.findLendingRecordById(record.getId()).orElse(null);
        assertNotNull(found);
        assertEquals(record.getId(), found.getId());
        assertEquals(record.getStartDate(), found.getStartDate());
        assertEquals(record.getEndDate(), found.getEndDate());
        assertEquals(record.getStatus(), found.getStatus());
    }

    @Test
    public void testFindByStatus() {
        List<LendingRecord> activeRecords = lendingRecordRepository.findByStatus(LendingRecord.LendingStatus.ACTIVE);
        assertFalse(activeRecords.isEmpty());
        assertEquals(LendingRecord.LendingStatus.ACTIVE, activeRecords.get(0).getStatus());

        List<LendingRecord> closedRecords = lendingRecordRepository.findByStatus(LendingRecord.LendingStatus.CLOSED);
        assertTrue(closedRecords.isEmpty());
    }

    @Test
    public void testFindByRecordOwner() {
        List<LendingRecord> ownerRecords = lendingRecordRepository.findByRecordOwner(owner);
        assertFalse(ownerRecords.isEmpty());
        assertEquals(owner.getId(), ownerRecords.get(0).getRecordOwner().getId());
    }

    @Test
    public void testFindByDateRange() {
        Date rangeStart = new Date(startDate.getTime() - 86400000); // 1 day before
        Date rangeEnd = new Date(endDate.getTime() + 86400000); // 1 day after

        List<LendingRecord> records = lendingRecordRepository.findByStartDateBetween(rangeStart, rangeEnd);
        assertFalse(records.isEmpty());
        assertTrue(records.get(0).getStartDate().after(rangeStart));
        assertTrue(records.get(0).getStartDate().before(rangeEnd));
    }

    @Test
    public void testFindOverdueRecords() {
        // Create an overdue record
        Date pastStart = new Date(System.currentTimeMillis() - 14 * 24 * 60 * 60 * 1000); // 14 days ago
        Date pastEnd = new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000); // 7 days ago
        
        BorrowRequest overdueRequest = new BorrowRequest(pastStart, pastEnd, BorrowRequestStatus.APPROVED, new Date(), game, null);
        overdueRequest.setRequester(borrower);
        overdueRequest.setResponder(owner);
        overdueRequest = entityManager.persist(overdueRequest);
        
        LendingRecord overdueRecord = new LendingRecord(pastStart, pastEnd, LendingRecord.LendingStatus.ACTIVE, overdueRequest, owner);
        overdueRecord = entityManager.persistAndFlush(overdueRecord);
        entityManager.clear();

        List<LendingRecord> overdueRecords = lendingRecordRepository.findByEndDateBeforeAndStatus(
            new Date(), LendingRecord.LendingStatus.ACTIVE);
        
        assertFalse(overdueRecords.isEmpty());
        assertTrue(overdueRecords.stream()
            .anyMatch(r -> r.getEndDate().before(new Date()) && r.getStatus() == LendingRecord.LendingStatus.ACTIVE));
    }

    @Test
    public void testFindByBorrower() {
        List<LendingRecord> borrowerRecords = lendingRecordRepository.findByRequest_Requester(borrower);
        assertFalse(borrowerRecords.isEmpty());
        assertEquals(borrower.getId(), borrowerRecords.get(0).getRequest().getRequester().getId());
    }

    @Test
    public void testCascadeDelete() {
        // Store IDs for verification
        final int ownerId = owner.getId();
        final int gameId = game.getId();
        final int borrowerId = borrower.getId();
        final int requestId = request.getId();

        // Delete the lending record
        lendingRecordRepository.delete(record);
        entityManager.flush();
        entityManager.clear();
        
        // Verify that related entities still exist
        assertNotNull(entityManager.find(GameOwner.class, ownerId));
        assertNotNull(entityManager.find(Game.class, gameId));
        assertNotNull(entityManager.find(Account.class, borrowerId));
        assertNotNull(entityManager.find(BorrowRequest.class, requestId));
    }

    @Test
    public void testRelationshipMappings() {
        LendingRecord found = lendingRecordRepository.findLendingRecordById(record.getId()).orElse(null);
        assertNotNull(found);
        
        // Test relationship with GameOwner
        assertNotNull(found.getRecordOwner());
        assertEquals(owner.getId(), found.getRecordOwner().getId());
        
        // Test relationship with BorrowRequest
        assertNotNull(found.getRequest());
        assertEquals(request.getId(), found.getRequest().getId());
        
        // Test nested relationships
        assertEquals(game.getId(), found.getRequest().getRequestedGame().getId());
        assertEquals(borrower.getId(), found.getRequest().getRequester().getId());
    }

    @Test
    public void testUpdateBasicAttributes() {
        LendingRecord found = lendingRecordRepository.findLendingRecordById(record.getId()).orElse(null);
        assertNotNull(found);
        
        // Update dates and status
        Date newStartDate = new Date(startDate.getTime() + 86400000); // +1 day
        Date newEndDate = new Date(endDate.getTime() + 86400000); // +1 day
        found.setStartDate(newStartDate);
        found.setEndDate(newEndDate);
        found.setStatus(LendingRecord.LendingStatus.CLOSED);
        
        entityManager.persistAndFlush(found);
        entityManager.clear();
        
        // Verify changes persisted
        LendingRecord updated = lendingRecordRepository.findLendingRecordById(record.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(newStartDate, updated.getStartDate());
        assertEquals(newEndDate, updated.getEndDate());
        assertEquals(LendingRecord.LendingStatus.CLOSED, updated.getStatus());
    }
}
