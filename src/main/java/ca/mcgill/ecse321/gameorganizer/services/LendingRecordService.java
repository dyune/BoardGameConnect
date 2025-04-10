package ca.mcgill.ecse321.gameorganizer.services;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize; // Import PreAuthorize

import ca.mcgill.ecse321.gameorganizer.dto.request.LendingHistoryFilterDto;
import ca.mcgill.ecse321.gameorganizer.exceptions.ForbiddenException; // Import ForbiddenException
import ca.mcgill.ecse321.gameorganizer.exceptions.ResourceNotFoundException;
import ca.mcgill.ecse321.gameorganizer.exceptions.UnauthedException;
import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.BorrowRequest;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.GameInstance;
import ca.mcgill.ecse321.gameorganizer.models.LendingRecord;
import ca.mcgill.ecse321.gameorganizer.models.LendingRecord.LendingStatus;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.BorrowRequestRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.GameInstanceRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.LendingRecordRepository;

/**
 * Service class that handles business logic for lending record operations.
 * Provides methods for managing the lending of games between owners and borrowers.
 * 
 * @author @YoussGm3o8
 */
@Service
public class LendingRecordService {
    
    private static final Logger log = LoggerFactory.getLogger(LendingRecordService.class);
    @Autowired
    private LendingRecordRepository lendingRecordRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final AccountRepository accountRepository; // Inject AccountRepository
    @Autowired
    private GameInstanceRepository gameInstanceRepository; // Add GameInstanceRepository

    @Autowired
    public LendingRecordService(LendingRecordRepository lendingRecordRepository, BorrowRequestRepository borrowRequestRepository, AccountRepository accountRepository) {
        this.lendingRecordRepository = lendingRecordRepository;
        this.borrowRequestRepository = borrowRequestRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Helper method to create a standardized error response.
     * 
     * @param status The HTTP status code
     * @param message The error message
     * @return ResponseEntity with error details
     */
    private ResponseEntity<String> createErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(message);
    }
    
    /**
     * Creates a new lending record for a game loan.
     *
     * @param startDate the start date of the lending period
     * @param endDate the end date of the lending period
     * @param request the associated borrow request
     * @param owner the game owner
     * @return ResponseEntity with success message
     * @throws IllegalArgumentException if parameters are invalid
     * @throws IllegalStateException if creation fails
     */
    @Transactional
    public ResponseEntity<String> createLendingRecord(Date startDate, Date endDate, BorrowRequest request, GameOwner owner) {
        try {
            // Validate parameters
            if (startDate == null || endDate == null || request == null || owner == null) {
                throw new IllegalArgumentException("Required parameters cannot be null");
            }

            // Validate owner consistency: the owner of the game must match the passed owner.
            if (request.getRequestedGame() == null || request.getRequestedGame().getOwner() == null ||
                request.getRequestedGame().getOwner().getId() != owner.getId()) {
                throw new IllegalArgumentException("The record owner must be the owner of the game in the borrow request");
            }

            // Check if the BorrowRequest is already associated with an existing LendingRecord
            if (lendingRecordRepository.findByRequest(request).isPresent()) {
                throw new IllegalArgumentException("The borrow request already has a lending record associated with it");
            }

            // Validate dates
            Date now = new Date();
            if (endDate.before(startDate)) {
                throw new IllegalArgumentException("End date cannot be before start date");
            }
            // Allow a margin of 1 second for the start date (to account for processing delays)
            if (startDate.getTime() < now.getTime() - 1000) {
                throw new IllegalArgumentException("Start date cannot be in the past");
            }

            // Create and save new lending record
            LendingRecord record = new LendingRecord(startDate, endDate, LendingStatus.ACTIVE, request, owner);
            lendingRecordRepository.save(record);
            
            // Find all game instances for the requested game and set the first available one to unavailable
            Game requestedGame = request.getRequestedGame();
            List<GameInstance> instances = gameInstanceRepository.findByGame(requestedGame);
            boolean instanceFound = false;
            
            for (GameInstance instance : instances) {
                if (instance.isAvailable()) {
                    // instance.setAvailable(false);
                    gameInstanceRepository.save(instance);
                    instanceFound = true;
                    log.info("Game instance ID: {} for game '{}' marked as unavailable due to lending", 
                            instance.getId(), requestedGame.getName());
                    break;
                }
            }
            
            if (!instanceFound) {
                log.warn("No available game instance found for game '{}'", requestedGame.getName());
            }

            return ResponseEntity.ok("Lending record created successfully");
        } catch (IllegalArgumentException e) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create lending record: " + e.getMessage());
        }
    }
    
    /**
     * Creates a new lending record for a game loan using borrowRequestId.
     * This method provides a convenient way to create lending records from API requests.
     *
     * @param startDate the start date of the lending period
     * @param endDate the end date of the lending period
     * @param requestId the ID of the associated borrow request
     * @param owner the game owner
     * @return ResponseEntity with success message and the created lending record ID
     * @throws IllegalArgumentException if parameters are invalid
     * @throws IllegalStateException if creation fails
     */
    @Transactional
    public ResponseEntity<String> createLendingRecordFromRequestId(Date startDate, Date endDate, int requestId, GameOwner owner) {
        // Retrieve the borrow request entity
        BorrowRequest request = borrowRequestRepository.findBorrowRequestById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("No borrow request found with ID " + requestId));
                
        // Delegate to the other method
        return createLendingRecord(startDate, endDate, request, owner);
    }

    /**
     * Retrieves a lending record by its ID.
     *
     * @param id The ID of the lending record to retrieve
     * @return The LendingRecord object
     * @throws ResourceNotFoundException if no record is found with the given ID
     */
    @Transactional
    public LendingRecord getLendingRecordById(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be positive");
        }
        Optional<LendingRecord> record = lendingRecordRepository.findLendingRecordById(id);
        if (record.isEmpty()) {
            throw new IllegalArgumentException("Lending record with ID " + id + " not found");
        }
        return record.get();
    }

    /**
     * Retrieves a lending record by its associated borrow request ID.
     *
     * @param requestId The ID of the borrow request
     * @return The lending record associated with the borrow request
     * @throws IllegalArgumentException if the request ID is invalid
     * @throws ResourceNotFoundException if no lending record is found for the request
     */
    @Transactional
    public LendingRecord getLendingRecordByRequestId(int requestId) {
        if (requestId <= 0) {
            throw new IllegalArgumentException("Request ID must be positive");
        }
        
        // First, check if the borrow request exists
        Optional<BorrowRequest> request = borrowRequestRepository.findBorrowRequestById(requestId);
        if (request.isEmpty()) {
            throw new ResourceNotFoundException("Borrow request with ID " + requestId + " not found");
        }
        
        // Then find the associated lending record
        Optional<LendingRecord> record = lendingRecordRepository.findByRequest(request.get());
        if (record.isEmpty()) {
            throw new ResourceNotFoundException("No lending record found for borrow request with ID " + requestId);
        }
        
        return record.get();
    }

    /**
     * Retrieves all lending records.
     *
     * @return List of all lending records
     */
    @Transactional
    public List<LendingRecord> getAllLendingRecords() {
        return lendingRecordRepository.findAll();
    }

    /**
     * Retrieves all lending records associated with a specific game owner.
     *
     * @param owner The GameOwner whose records to retrieve
     * @return List of lending records for the owner
     * @throws IllegalArgumentException if owner is null
     */
    @Transactional
    public List<LendingRecord> getLendingRecordsByOwner(GameOwner owner) {
        if (owner == null) {
            throw new IllegalArgumentException("Owner cannot be null");
        }
        return lendingRecordRepository.findByRecordOwner(owner);
    }

    /**
     * Retrieves lending records within a specific date range.
     *
     * @param startDate The start date of the range (inclusive)
     * @param endDate The end date of the range (inclusive)
     * @return List of lending records within the date range
     * @throws IllegalArgumentException if either date is null
     */
    @Transactional
    public List<LendingRecord> getLendingRecordsByDateRange(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Date range parameters cannot be null");
        }
        return lendingRecordRepository.findByStartDateBetween(startDate, endDate);
    }

    /**
     * Retrieves all lending records associated with a specific borrower.
     *
     * @param borrower The Account of the borrower
     * @return List of lending records for the borrower
     * @throws IllegalArgumentException if borrower is null
     */
    @Transactional
    public List<LendingRecord> getLendingRecordsByBorrower(Account borrower) {
        if (borrower == null) {
            throw new IllegalArgumentException("Borrower cannot be null");
        }
        return lendingRecordRepository.findByRequest_Requester(borrower);
    }
    
    /**
     * Applies multiple filters to lending records.
     * 
     * @param filterDto The DTO containing filter criteria
     * @return Filtered list of lending records
     */
    @Transactional
    public List<LendingRecord> filterLendingRecords(LendingHistoryFilterDto filterDto) {
        LendingStatus status = null;
        if (filterDto.getStatus() != null && !filterDto.getStatus().isEmpty()) {
            try {
                status = LendingStatus.valueOf(filterDto.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status is ignored
            }
        }
        return lendingRecordRepository.filterLendingRecords(
            filterDto.getFromDate(),
            filterDto.getToDate(),
            status,
            filterDto.getBorrowerId(),
            filterDto.getGameId()
        );
    }
    
    /**
     * Paginated version of filterLendingRecords.
     * 
     * @param filterDto The DTO containing filter criteria
     * @param pageable The pagination information
     * @return Page of filtered lending records
     */
    @Transactional
    public Page<LendingRecord> filterLendingRecordsPaginated(LendingHistoryFilterDto filterDto, Pageable pageable) {
        LendingStatus status = null;
        if (filterDto.getStatus() != null && !filterDto.getStatus().isEmpty()) {
            try {
                status = LendingStatus.valueOf(filterDto.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status is ignored
            }
        }
        return lendingRecordRepository.filterLendingRecords(
            filterDto.getFromDate(),
            filterDto.getToDate(),
            status,
            filterDto.getBorrowerId(),
            filterDto.getGameId(),
            pageable
        );
    }

    /**
     * Updates the status of a lending record with comprehensive validation of state transitions.
     * Prevents invalid transitions and automatically handles overdue detection.
     *
     * @param id The ID of the record to update
     * @param newStatus The new status to set
     * @param userId The ID of the user making the change
     * @param reason The reason for the status change
     * @return ResponseEntity with the result of the operation including the updated record ID
     * @throws IllegalArgumentException if no record is found with the given ID
     * @throws IllegalStateException if the status transition is not allowed
     */
    @Transactional
    @PreAuthorize("@lendingRecordService.isOwnerOrBorrower(#id, authentication.principal.username)")
    public ResponseEntity<String> updateStatus(int id, LendingStatus newStatus, String reason) { // Removed userId parameter
        log.info("Attempting to update status for record ID: {} to {}. Reason: {}", id, newStatus, reason);
        
        try {
            if (newStatus == null) {
                throw new IllegalArgumentException("New status cannot be null");
            }
            
            LendingRecord record = getLendingRecordById(id); // Throws ResourceNotFoundException if not found
            LendingStatus currentStatus = record.getStatus();
            log.debug("Record found: ID={}, Status={}", record.getId(), currentStatus);
            
            if (currentStatus == newStatus) {
                return ResponseEntity.ok("Status already set to " + newStatus.name());
            }

            // Authorization is handled by @PreAuthorize
            
            // Get current user ID for audit log (if needed, otherwise remove)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Account currentUser = accountRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new UnauthedException("Authenticated user not found in database."));
            Integer currentUserId = currentUser.getId();
            
            log.debug("Auth check passed for record ID: {}", id);
        log.debug("Attempting validateStatusTransition for record {} from {} to {}", id, currentStatus, newStatus);
        validateStatusTransition(record, newStatus);
        log.debug("validateStatusTransition passed for record {}", id);
        
        if (isRecordOverdue(record) && newStatus == LendingStatus.ACTIVE) {
            record.setStatus(LendingStatus.OVERDUE);
            record.setLastModifiedDate(new Date());
            record.setLastModifiedBy(currentUserId); // Use ID from authenticated user
            record.setStatusChangeReason("System automated change: Record is overdue");
            lendingRecordRepository.save(record);
            return ResponseEntity.ok("Record is overdue - status automatically set to OVERDUE instead of ACTIVE");
        }
        
        record.setStatus(newStatus);
        record.setLastModifiedDate(new Date());
        record.setLastModifiedBy(currentUserId); // Use ID from authenticated user
        record.setStatusChangeReason(reason != null ? reason : "Status updated by user");
        
        log.debug("Attempting to save updated record ID: {} with status {}", record.getId(), record.getStatus());
        // REMOVED REDUNDANT SAVE CALL HERE
        try {
            log.debug("Attempting final save for record ID: {}", record.getId());
            lendingRecordRepository.save(record);
            log.info("Successfully saved updated record ID: {}", record.getId());
        } catch (Exception e) {
            log.error("Error saving record ID: {} during status update", record.getId(), e);
            throw e; // Re-throw the exception to be handled by controller advice or caller
        }
        
            // Original save call removed, handled in try-catch above
            return ResponseEntity.ok("Lending record status updated successfully");
            
        } catch (ResourceNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException e) {
             throw new ForbiddenException("Access denied: Only the game owner or borrower can update the lending status.");
        } catch (UnauthedException e) { // Catch potential UnauthedException from fetching currentUser
             return createErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) { // Catch unexpected errors
            log.error("Unexpected error updating status for record {}: {}", id, e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        }
    }
    
    /**
     * Updates the status of a lending record without audit information.
     * This is a backward-compatible method for existing code.
     *
     * @param id The ID of the record to update
     * @param newStatus The new status to set
     * @return ResponseEntity with the result of the operation
     */
    @Transactional
    public ResponseEntity<String> updateStatus(int id, LendingStatus newStatus) {
        // userId is now derived from authentication context in the main updateStatus method
        return updateStatus(id, newStatus, "Status updated via API");
    }
    
    /**
     * Validates if a status transition is allowed based on business rules.
     * 
     * @param record The lending record
     * @param newStatus The new status to validate
     * @throws IllegalStateException if the status transition is not allowed
     */
    private void validateStatusTransition(LendingRecord record, LendingStatus newStatus) {
        LendingStatus currentStatus = record.getStatus();
        
        if (currentStatus == LendingStatus.CLOSED && newStatus != LendingStatus.CLOSED) {
            throw new IllegalStateException(
                String.format("Cannot change status of a closed lending record (ID: %d)", record.getId()));
        }
        
        if (newStatus == LendingStatus.ACTIVE && isRecordOverdue(record)) {
            throw new IllegalStateException(
                String.format("Cannot set record (ID: %d) to ACTIVE as it is overdue", record.getId()));
        }
    }
    
    /**
     * Checks if a lending record is overdue based on the current date and the record's end date.
     * Made protected for testing purposes.
     * 
     * @param record The lending record to check
     * @return true if the record is overdue, false otherwise
     */
    protected boolean isRecordOverdue(LendingRecord record) {
        Date now = new Date();
        return record.getEndDate().before(now);
    }
    
    /**
     * Closes a lending record by setting its status to CLOSED.
     * Performs additional validation before closing the record.
     *
     * @param id The ID of the record to close
     * @param userId ID of the user closing the record
     * @param reason Reason for closing the record
     * @return ResponseEntity with the result of the operation including the updated record ID
     * @throws IllegalArgumentException if no record is found with the given ID
     * @throws IllegalStateException if the record is already closed or cannot be closed
     */
    @Transactional
    @PreAuthorize("@lendingRecordService.isOwnerOfRecord(#id, authentication.principal.username)")
    public ResponseEntity<String> closeLendingRecord(int id, String reason) { // Removed userId parameter
        try {
            LendingRecord record = getLendingRecordById(id);
            LendingStatus currentStatus = record.getStatus();

            // Authorization handled by @PreAuthorize
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Account currentUser = accountRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new UnauthedException("Authenticated user not found in database."));
            Integer currentUserId = currentUser.getId();
        
            if (currentStatus == LendingStatus.CLOSED) {
                throw new IllegalStateException("Lending record is already closed");
            }
            
            record.recordClosing(currentUserId, reason != null ? reason : "Game returned in good condition");
            
            lendingRecordRepository.save(record);
            
            // Mark the corresponding game instance as available again
            BorrowRequest request = record.getRequest();
            if (request != null && request.getRequestedGame() != null) {
                Game game = request.getRequestedGame();
                
                // Find the first unavailable game instance for this game and mark it as available
                List<GameInstance> instances = gameInstanceRepository.findByGame(game);
                boolean instanceUpdated = false;
                
                for (GameInstance instance : instances) {
                    if (!instance.isAvailable()) {
                        instance.setAvailable(true);
                        gameInstanceRepository.save(instance);
                        instanceUpdated = true;
                        log.info("Game instance ID: {} for game '{}' marked as available after return", 
                                instance.getId(), game.getName());
                        break;
                    }
                }
                
                if (!instanceUpdated) {
                    log.warn("No unavailable game instance found for game '{}' to mark as available", game.getName());
                }
            }
        
            return ResponseEntity.ok(String.format(
                "Lending record (ID: %d) successfully closed. Previous status was %s", 
                    record.getId(), currentStatus));
        } catch (ResourceNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException e) {
             throw new ForbiddenException("Access denied: Only the game owner can close the lending record.");
        } catch (UnauthedException e) {
             return createErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
             log.error("Unexpected error closing record {}: {}", id, e.getMessage(), e);
             return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        }
    }
    
    /**
     * Closes a lending record without audit information.
     * This is a backward-compatible method for existing code.
     *
     * @param id The ID of the record to close
     * @return ResponseEntity with the result of the operation
     */
    @Transactional
    public ResponseEntity<String> closeLendingRecord(int id) {
        // userId is now derived from authentication context
        return closeLendingRecord(id, "Closed via API");
    }
    
    /**
     * Closes a lending record with damage information.
     * Records details about any damage to the game during the borrowing period.
     *
     * @param id The ID of the record to close
     * @param isDamaged Flag indicating if the game was damaged
     * @param damageNotes Description of the damage (if any)
     * @param damageSeverity Severity of the damage (0-3, where 0 is none and 3 is severe)
     * @param userId ID of the user closing the record
     * @param reason Additional reason for closing (beyond damage info)
     * @return ResponseEntity with the result of the operation
     * @throws IllegalArgumentException if no record is found with the given ID
     * @throws IllegalStateException if the record is already closed or cannot be closed
     */
    @Transactional
    @PreAuthorize("@lendingRecordService.isOwnerOfRecord(#id, authentication.principal.username)")
    public ResponseEntity<String> closeLendingRecordWithDamageAssessment(
            int id, boolean isDamaged, String damageNotes, int damageSeverity,
            String reason) { // Removed userId parameter
        try {
            // Validate severity
            if (damageSeverity < 0 || damageSeverity > 3) {
                throw new IllegalArgumentException("Damage severity must be between 0 and 3");
            }
            
            LendingRecord record = getLendingRecordById(id);
            LendingStatus currentStatus = record.getStatus();
            
            // Authorization handled by @PreAuthorize
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Account currentUser = accountRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new UnauthedException("Authenticated user not found in database."));
            Integer currentUserId = currentUser.getId();
            
            if (currentStatus == LendingStatus.CLOSED) {
                throw new IllegalStateException("Lending record is already closed");
            }
            
            // Record damage info
            record.recordDamage(isDamaged, damageNotes, damageSeverity);
            
            // Close the record
            record.recordClosing(currentUserId, reason != null ? reason : "Game returned with notes");
            
            lendingRecordRepository.save(record);
            
            // Mark the corresponding game instance as available again
            BorrowRequest request = record.getRequest();
            if (request != null && request.getRequestedGame() != null) {
                Game game = request.getRequestedGame();
                
                // Find the first unavailable game instance for this game and mark it as available
                List<GameInstance> instances = gameInstanceRepository.findByGame(game);
                boolean instanceUpdated = false;
                
                for (GameInstance instance : instances) {
                    if (!instance.isAvailable()) {
                        instance.setAvailable(true);
                        gameInstanceRepository.save(instance);
                        instanceUpdated = true;
                        log.info("Game instance ID: {} for game '{}' marked as available after return", 
                                instance.getId(), game.getName());
                        break;
                    }
                }
                
                if (!instanceUpdated) {
                    log.warn("No unavailable game instance found for game '{}' to mark as available", game.getName());
                }
            }
            
            return ResponseEntity.ok(String.format(
                "Lending record (ID: %d) successfully closed with damage assessment", record.getId()));
        } catch (ResourceNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw new ForbiddenException("Access denied: Only the game owner can close the lending record with damage assessment.");
        } catch (UnauthedException e) {
            return createErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in closeLendingRecordWithDamageAssessment for record {}: {}", id, e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        }
    }
    
    /**
     * Closes a lending record with damage information but without audit information.
     * This is a backward-compatible method for existing code.
     *
     * @param id The ID of the record to close
     * @param isDamaged Flag indicating if the game was damaged
     * @param damageNotes Description of the damage (if any)
     * @param damageSeverity Severity of the damage (0-3, where 0 is none and 3 is severe)
     * @return ResponseEntity with the result of the operation
     */
    @Transactional
    public ResponseEntity<String> closeLendingRecordWithDamageAssessment(
            int id, boolean isDamaged, String damageNotes, int damageSeverity) {
        // userId and reason are handled by the main method now
        return closeLendingRecordWithDamageAssessment(id, isDamaged, damageNotes, damageSeverity, null);
    }
    
    /**
     * Finds overdue lending records.
     *
     * @return list of overdue lending records
     */
    @Transactional
    public List<LendingRecord> findOverdueRecords() {
        return lendingRecordRepository.findByEndDateBeforeAndStatus(new Date(), LendingStatus.ACTIVE);
    }
    
    /**
     * Updates the end date of a lending record.
     *
     * @param id The ID of the record to update
     * @param newEndDate The new end date to set
     * @return ResponseEntity with the result of the operation
     * @throws IllegalArgumentException if no record is found with the given ID or if the new date is invalid
     * @throws IllegalStateException if the record is closed
     */
    @Transactional
    @PreAuthorize("@lendingRecordService.isOwnerOfRecord(#id, authentication.principal.username)")
    public ResponseEntity<String> updateEndDate(int id, Date newEndDate) {
        log.info("Attempting to update end date for record ID: {} to {}", id, newEndDate);
        
        try {
            if (newEndDate == null) {
                throw new IllegalArgumentException("New end date cannot be null");
            }

            LendingRecord record = getLendingRecordById(id);

            // Authorization handled by @PreAuthorize
        
        if (record.getStatus() == LendingStatus.CLOSED) {
            log.warn("Attempted to update end date on closed record ID: {}", id);
            throw new IllegalStateException("Cannot update end date of a closed lending record");
        }
        
        if (newEndDate.before(record.getStartDate())) {
            log.warn("Attempted to set invalid end date ({}) for record ID: {}. End date cannot be before start date ({}).", newEndDate, id, record.getStartDate());
            throw new IllegalArgumentException("New end date cannot be before start date");
        }
        
        record.setEndDate(newEndDate);
        log.debug("Attempting to save record ID: {} with updated end date.", record.getId());
        // REMOVED REDUNDANT SAVE CALL HERE
        try {
            lendingRecordRepository.save(record);
            log.info("Successfully saved record ID: {} with updated end date.", record.getId());
        } catch (Exception e) {
            log.error("Error saving record ID: {} during end date update", record.getId(), e);
            throw e;
        }
        
            // Original save call removed
            return ResponseEntity.ok("End date updated successfully");
        } catch (ResourceNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException e) {
             throw new ForbiddenException("Access denied: Only the game owner can update the end date.");
        } catch (UnauthedException e) {
             return createErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
             log.error("Unexpected error updating end date for record {}: {}", id, e.getMessage(), e);
             return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        }
    }
    
    /**
     * Deletes a lending record.
     *
     * @param id the ID of the lending record
     * @return ResponseEntity with success message
     * @throws IllegalStateException if the lending record is active
     */
    @Transactional
    @PreAuthorize("@lendingRecordService.isOwnerOfRecord(#id, authentication.principal.username)")
    public ResponseEntity<String> deleteLendingRecord(int id) {
        try {
            LendingRecord record = getLendingRecordById(id);

            // Authorization handled by @PreAuthorize
            
            if (record.getStatus() == LendingStatus.ACTIVE) {
                throw new IllegalStateException("Cannot delete an active lending record");
            }

            lendingRecordRepository.delete(record);
            return ResponseEntity.ok("Lending record deleted successfully");
        } catch (ResourceNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) { // Catch the "Cannot delete active record" error
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException e) {
             throw new ForbiddenException("Access denied: Only the game owner can delete this lending record.");
        } catch (UnauthedException e) {
             return createErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
             log.error("Unexpected error deleting record {}: {}", id, e.getMessage(), e);
             return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        }
    }

    /**


    // --- Helper methods for @PreAuthorize --- 

    /**
     * Checks if the given username corresponds to the owner of the lending record.
     */
    public boolean isOwnerOfRecord(int recordId, String username) {
        if (username == null) return false;
        try {
            LendingRecord record = lendingRecordRepository.findLendingRecordById(recordId).orElse(null);
            Account user = accountRepository.findByEmail(username).orElse(null);

            if (record == null || user == null || record.getRecordOwner() == null) {
                return false; // Cannot determine ownership
            }

            return record.getRecordOwner().getId() == user.getId();
        } catch (Exception e) {
            log.error("Error during isOwnerOfRecord check for record {}: {}", recordId, e.getMessage());
            return false; // Deny on error
        }
    }

    /**
     * Checks if the given username corresponds to either the owner or the borrower of the lending record.
     * (Replaces the private helper with a public one for @PreAuthorize)
     */
    public boolean isOwnerOrBorrower(int recordId, String username) {
        if (username == null) return false;
        try {
            LendingRecord record = lendingRecordRepository.findLendingRecordById(recordId).orElse(null);
            Account user = accountRepository.findByEmail(username).orElse(null);

            if (record == null || user == null) {
                return false; // Cannot determine relationship
            }

            // Check if user is the owner
            boolean isOwner = record.getRecordOwner() != null && record.getRecordOwner().getId() == user.getId();
            if (isOwner) return true;

            // Check if user is the borrower
            boolean isBorrower = record.getRequest() != null && 
                                 record.getRequest().getRequester() != null &&
                                 record.getRequest().getRequester().getId() == user.getId();
            
            return isBorrower;
        } catch (Exception e) {
            log.error("Error during isOwnerOrBorrower check for record {}: {}", recordId, e.getMessage());
            return false; // Deny on error
        }
    }

    // Old private isOwnerOrBorrower method removed.
}
