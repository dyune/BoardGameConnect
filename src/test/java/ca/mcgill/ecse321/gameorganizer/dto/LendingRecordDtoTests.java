package ca.mcgill.ecse321.gameorganizer.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ca.mcgill.ecse321.gameorganizer.dto.request.LendingHistoryFilterDto;
import ca.mcgill.ecse321.gameorganizer.dto.request.UpdateLendingRecordStatusDto;
import ca.mcgill.ecse321.gameorganizer.dto.response.LendingRecordDto;
import ca.mcgill.ecse321.gameorganizer.dto.response.LendingRecordResponseDto;
import ca.mcgill.ecse321.gameorganizer.dto.response.LendingRecordResponseDto.GameInfo;
import ca.mcgill.ecse321.gameorganizer.dto.response.LendingRecordResponseDto.UserInfo;

/**
 * Test class for the LendingRecord DTOs.
 * Tests the functionality of the Data Transfer Objects related to lending records.
 * 
 * @author @YoussGm3o8
 */
public class LendingRecordDtoTests {

    @Test
    public void testLendingRecordDto() {
        // Create test data
        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + 86400000 * 7); // 7 days later
        
        // Create basic DTO
        LendingRecordDto dto = new LendingRecordDto(1, startDate, endDate, "ACTIVE", 2, 3);
        
        // Verify getters for basic fields
        assertEquals(1, dto.getId());
        assertEquals(startDate, dto.getStartDate());
        assertEquals(endDate, dto.getEndDate());
        assertEquals("ACTIVE", dto.getStatus());
        assertEquals(2, dto.getBorrowRequestId());
        assertEquals(3, dto.getRecordOwnerId());
        
        // Verify default values for damage fields
        assertFalse(dto.isDamaged());
        assertEquals(0, dto.getDamageSeverity());
        assertNull(dto.getDamageNotes());
        assertNull(dto.getDamageAssessmentDate());
        assertEquals("None", dto.getDamageSeverityLabel());
        
        // Create DTO with damage information
        Date damageDate = new Date();
        LendingRecordDto dtoWithDamage = new LendingRecordDto(1, startDate, endDate, "CLOSED", 2, 3,
                true, "Scratches on disc", 2, damageDate);
        
        // Verify getters for damage fields
        assertTrue(dtoWithDamage.isDamaged());
        assertEquals(2, dtoWithDamage.getDamageSeverity());
        assertEquals("Scratches on disc", dtoWithDamage.getDamageNotes());
        assertEquals(damageDate, dtoWithDamage.getDamageAssessmentDate());
        assertEquals("Moderate", dtoWithDamage.getDamageSeverityLabel());
    }
    
    @Test
    public void testLendingHistoryFilterDto() {
        // Create test data
        Date fromDate = new Date();
        Date toDate = new Date(fromDate.getTime() + 86400000 * 30); // 30 days later
        
        // Create DTO with constructor
        LendingHistoryFilterDto dto1 = new LendingHistoryFilterDto(fromDate, toDate, "ACTIVE", 1, 2);
        
        // Verify getters
        assertEquals(fromDate, dto1.getFromDate());
        assertEquals(toDate, dto1.getToDate());
        assertEquals("ACTIVE", dto1.getStatus());
        assertEquals(Integer.valueOf(1), dto1.getGameId());
        assertEquals(Integer.valueOf(2), dto1.getBorrowerId());
        
        // Create DTO with default constructor and setters
        LendingHistoryFilterDto dto2 = new LendingHistoryFilterDto();
        dto2.setFromDate(fromDate);
        dto2.setToDate(toDate);
        dto2.setStatus("OVERDUE");
        dto2.setGameId(3);
        dto2.setBorrowerId(4);
        
        // Verify getters
        assertEquals(fromDate, dto2.getFromDate());
        assertEquals(toDate, dto2.getToDate());
        assertEquals("OVERDUE", dto2.getStatus());
        assertEquals(Integer.valueOf(3), dto2.getGameId());
        assertEquals(Integer.valueOf(4), dto2.getBorrowerId());
    }
    
    @Test
    public void testUpdateLendingRecordStatusDto() {
        // Create DTO with minimal constructor
        UpdateLendingRecordStatusDto dto1 = new UpdateLendingRecordStatusDto("CLOSED");
        
        // Verify getters
        assertEquals("CLOSED", dto1.getNewStatus());
        assertNull(dto1.getUserId());
        assertNull(dto1.getReason());
        
        // Create DTO with full constructor
        UpdateLendingRecordStatusDto dto2 = new UpdateLendingRecordStatusDto("OVERDUE", 123, "Late return");
        
        // Verify getters
        assertEquals("OVERDUE", dto2.getNewStatus());
        assertEquals(Integer.valueOf(123), dto2.getUserId());
        assertEquals("Late return", dto2.getReason());
        
        // Create DTO with default constructor and setters
        UpdateLendingRecordStatusDto dto3 = new UpdateLendingRecordStatusDto();
        dto3.setNewStatus("ACTIVE");
        dto3.setUserId(456);
        dto3.setReason("Status change reason");
        
        // Verify getters
        assertEquals("ACTIVE", dto3.getNewStatus());
        assertEquals(Integer.valueOf(456), dto3.getUserId());
        assertEquals("Status change reason", dto3.getReason());
    }
    
    @Test
    public void testLendingRecordResponseDto() {
        // Create test data
        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + 86400000 * 7); // 7 days later
        GameInfo game = new GameInfo(1, "Test Game", "Strategy", null);
        UserInfo borrower = new UserInfo(2, "Test Borrower", "borrower@test.com");
        UserInfo owner = new UserInfo(3, "Test Owner", "owner@test.com");
        
        // Create DTO with basic constructor
        LendingRecordResponseDto dto1 = new LendingRecordResponseDto(
                1, startDate, endDate, "ACTIVE", game, borrower, owner, 7);
        
        // Verify getters
        assertEquals(1, dto1.getId());
        assertEquals(startDate, dto1.getStartDate());
        assertEquals(endDate, dto1.getEndDate());
        assertEquals("ACTIVE", dto1.getStatus());
        assertEquals(game, dto1.getGame());
        assertEquals(borrower, dto1.getBorrower());
        assertEquals(owner, dto1.getOwner());
        assertEquals(7, dto1.getDurationInDays());
        assertFalse(dto1.isDamaged());
        assertEquals(0, dto1.getDamageSeverity());
        assertEquals("None", dto1.getDamageSeverityLabel());
        
        // Create DTO with full constructor including damage info
        Date damageDate = new Date();
        LendingRecordResponseDto dto2 = new LendingRecordResponseDto(
                2, startDate, endDate, "CLOSED", game, borrower, owner, 7,
                true, "Scratches on disc", 2, damageDate);
        
        // Verify getters including damage info
        assertEquals(2, dto2.getId());
        assertTrue(dto2.isDamaged());
        assertEquals("Scratches on disc", dto2.getDamageNotes());
        assertEquals(2, dto2.getDamageSeverity());
        assertEquals(damageDate, dto2.getDamageAssessmentDate());
        assertEquals("Moderate", dto2.getDamageSeverityLabel());
    }
    
    @Test
    public void testGameInfoInnerClass() {
        // Test the GameInfo inner class of LendingRecordResponseDto
        GameInfo gameInfo = new GameInfo(1, "Test Game", "Strategy", null);
        
        assertEquals(1, gameInfo.getId());
        assertEquals("Test Game", gameInfo.getName());
        assertEquals("Strategy", gameInfo.getCategory());
    }
    
    @Test
    public void testUserInfoInnerClass() {
        // Test the UserInfo inner class of LendingRecordResponseDto
        UserInfo userInfo = new UserInfo(1, "Test User", "user@test.com");
        
        assertEquals(1, userInfo.getId());
        assertEquals("Test User", userInfo.getName());
        assertEquals("user@test.com", userInfo.getEmail());
    }
    
    @Test
    public void testDamageSeverityLabel() {
        // Create test data
        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + 86400000 * 7); // 7 days later
        GameInfo game = new GameInfo(1, "Test Game", "Strategy", null);
        UserInfo borrower = new UserInfo(2, "Test Borrower", "borrower@test.com");
        UserInfo owner = new UserInfo(3, "Test Owner", "owner@test.com");
        
        // Test different damage severity levels
        LendingRecordResponseDto none = new LendingRecordResponseDto(
                1, startDate, endDate, "ACTIVE", game, borrower, owner, 7,
                false, null, 0, null);
        assertEquals("None", none.getDamageSeverityLabel());
        
        LendingRecordResponseDto minor = new LendingRecordResponseDto(
                2, startDate, endDate, "ACTIVE", game, borrower, owner, 7,
                true, "Minor scratches", 1, new Date());
        assertEquals("Minor", minor.getDamageSeverityLabel());
        
        LendingRecordResponseDto moderate = new LendingRecordResponseDto(
                3, startDate, endDate, "ACTIVE", game, borrower, owner, 7,
                true, "Moderate damage", 2, new Date());
        assertEquals("Moderate", moderate.getDamageSeverityLabel());
        
        LendingRecordResponseDto severe = new LendingRecordResponseDto(
                4, startDate, endDate, "ACTIVE", game, borrower, owner, 7,
                true, "Severe damage", 3, new Date());
        assertEquals("Severe", severe.getDamageSeverityLabel());
        
        // Test invalid damage severity
        LendingRecordResponseDto invalid = new LendingRecordResponseDto(
                5, startDate, endDate, "ACTIVE", game, borrower, owner, 7,
                true, "Unknown damage", -1, new Date());
        assertEquals("Unknown", invalid.getDamageSeverityLabel());
    }
} 