package ca.mcgill.ecse321.gameorganizer.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import ca.mcgill.ecse321.gameorganizer.models.LendingRecord;
import ca.mcgill.ecse321.gameorganizer.models.LendingRecord.LendingStatus;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.services.LendingRecordService;
import ca.mcgill.ecse321.gameorganizer.services.AccountService;
import ca.mcgill.ecse321.gameorganizer.dto.request.LendingHistoryFilterDto;
import ca.mcgill.ecse321.gameorganizer.dto.request.UpdateLendingRecordStatusDto;
import ca.mcgill.ecse321.gameorganizer.dto.response.LendingRecordResponseDto;
import ca.mcgill.ecse321.gameorganizer.exceptions.ForbiddenException; // Import
import ca.mcgill.ecse321.gameorganizer.exceptions.ResourceNotFoundException;
import ca.mcgill.ecse321.gameorganizer.exceptions.InvalidOperationException;
import ca.mcgill.ecse321.gameorganizer.exceptions.UnauthedException; // Import

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collections;

/**
 * REST controller for managing lending records.
 * Provides endpoints for creating, retrieving, updating, and filtering lending records.
 * 
 * @author @YoussGm3o8
 */
@RestController
@RequestMapping("/api/lending-records")
public class LendingRecordController {

    @Autowired
    private LendingRecordService lendingRecordService;

    @Autowired
    private AccountService accountService;
    
    /**
     * Validates that the damage severity is within the acceptable range (0-3).
     * 
     * @param severity The damage severity to validate
     * @return The validated damage severity
     * @throws IllegalArgumentException if severity is outside the valid range
     */
    private int validateDamageSeverity(int severity) {
        if (severity < 0 || severity > 3) {
            throw new IllegalArgumentException("Damage severity must be between 0 and 3");
        }
        return severity;
    }

    /**
     * Get all lending records with pagination support.
     * 
     * @param page The page number (0-based)
     * @param size The page size
     * @param sort The field to sort by
     * @param direction The sort direction (asc or desc)
     * @return Paginated list of lending records
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllLendingRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        
        // Create pageable object
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        // Get paginated records
        List<LendingRecord> allRecords = lendingRecordService.getAllLendingRecords();
        
        // Manual pagination (since service doesn't support pageable yet)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allRecords.size());
        
        // Check if start is valid
        int adjustedPage = page;
        if (start > allRecords.size()) {
            start = 0;
            end = Math.min(pageable.getPageSize(), allRecords.size());
            adjustedPage = 0;
        }
        
        List<LendingRecord> paginatedRecords = allRecords.subList(start, end);
        
        // Convert to DTOs
        List<LendingRecordResponseDto> recordDtos = paginatedRecords.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
        
        // Create response with pagination metadata
        Map<String, Object> response = new HashMap<>();
        response.put("records", recordDtos);
        response.put("currentPage", adjustedPage);
        response.put("totalItems", allRecords.size());
        response.put("totalPages", (int) Math.ceil((double) allRecords.size() / size));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a specific lending record by ID.
     * Supports Use Case 9: View Lending History (detailed view)
     *
     * @param id The ID of the lending record
     * @return ResponseEntity containing the lending record details
     */
    @GetMapping("/{id}")
    public ResponseEntity<LendingRecordResponseDto> getLendingRecordById(@PathVariable int id) {
        try {
            LendingRecord record = lendingRecordService.getLendingRecordById(id);
            return ResponseEntity.ok(convertToResponseDto(record));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Retrieves a lending record by its associated borrow request ID.
     * 
     * @param requestId The ID of the borrow request
     * @return ResponseEntity containing the lending record if found
     */
    @GetMapping("/request/{requestId}")
    public ResponseEntity<LendingRecordResponseDto> getLendingRecordByRequestId(@PathVariable int requestId) {
        try {
            LendingRecord record = lendingRecordService.getLendingRecordByRequestId(requestId);
            return ResponseEntity.ok(convertToResponseDto(record));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Retrieves lending records by owner.
     * 
     * @param ownerId The ID of the game owner
     * @return ResponseEntity containing lending records for the owner
     */
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<LendingRecordResponseDto>> getLendingHistoryByOwner(
            @PathVariable int ownerId,
            @RequestParam(required = false) Integer userId) {

        try {
            // Log the request
            System.out.println("Fetching lending records for owner ID: " + ownerId);
            
            // Log authentication information
            try {
                var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null) {
                    System.out.println("Auth principal: " + authentication.getPrincipal());
                    System.out.println("Auth authorities: " + authentication.getAuthorities());
                    System.out.println("Auth name: " + authentication.getName());
                } else {
                    System.out.println("No authentication found in SecurityContextHolder");
                }
            } catch (Exception e) {
                System.out.println("Error accessing authentication: " + e.getMessage());
            }
            
            GameOwner owner = (GameOwner) accountService.getAccountById(ownerId);
            List<LendingRecord> records = lendingRecordService.getLendingRecordsByOwner(owner);
            
            List<LendingRecordResponseDto> recordDtos = records.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());
            
            System.out.println("Found " + recordDtos.size() + " lending records for owner ID: " + ownerId);
            return ResponseEntity.ok(recordDtos);
        } catch (IllegalArgumentException e) {
            System.out.println("Error retrieving lending records for owner: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.out.println("Unexpected error retrieving lending records: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves lending records for a game owner filtered by status.
     * Implements Use Case 9: View Lending History with filtering
     *
     * @param ownerId The ID of the game owner
     * @param status The status to filter by (ACTIVE, OVERDUE, CLOSED)
     * @return ResponseEntity containing filtered list of lending records
     */
    @GetMapping("/owner/{ownerId}/status/{status}")
    public ResponseEntity<List<LendingRecordResponseDto>> getLendingHistoryByOwnerAndStatus(
            @PathVariable int ownerId,
            @PathVariable String status) {
        try {
            GameOwner owner = (GameOwner) accountService.getAccountById(ownerId);
            List<LendingRecord> allRecords = lendingRecordService.getLendingRecordsByOwner(owner);
            
            // Filter by status
            LendingStatus requestedStatus = LendingStatus.valueOf(status.toUpperCase());
            List<LendingRecord> filteredRecords = allRecords.stream()
                    .filter(record -> record.getStatus() == requestedStatus)
                    .collect(Collectors.toList());
            
            List<LendingRecordResponseDto> recordDtos = filteredRecords.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(recordDtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Retrieves lending records for a game owner within a date range.
     * Implements Use Case 9: View Lending History with date filtering
     *
     * @param ownerId The ID of the game owner
     * @param startDate The start date of the range
     * @param endDate The end date of the range
     * @return ResponseEntity containing filtered list of lending records
     */
    @GetMapping("/owner/{ownerId}/date-range")
    public ResponseEntity<List<LendingRecordResponseDto>> getLendingHistoryByOwnerAndDateRange(
            @PathVariable int ownerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate) {
        try {
            // Validate that the start date is before the end date
            if (startDate.after(endDate)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.emptyList());
            }

            // Validate owner existence
            GameOwner owner = (GameOwner) accountService.getAccountById(ownerId);

            List<LendingRecord> records = lendingRecordService.getLendingRecordsByDateRange(startDate, endDate);
            
            // Further filter by owner
            List<LendingRecord> filteredRecords = records.stream()
                    .filter(record -> record.getRecordOwner().getId() == owner.getId())
                    .collect(Collectors.toList());
            
            List<LendingRecordResponseDto> recordDtos = filteredRecords.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(recordDtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.emptyList());
        }
    }

    /**
     * Retrieves lending records for a borrower.
     * 
     * @param borrowerId The ID of the borrower
     * @return ResponseEntity containing lending records for the borrower
     */
    @GetMapping("/borrower/{borrowerId}")
    public ResponseEntity<List<LendingRecordResponseDto>> getLendingRecordsByBorrower(
            @PathVariable int borrowerId,
            @RequestParam(required = false) Integer userId) {
            
        try {
            // Log the request
            System.out.println("Fetching lending records for borrower ID: " + borrowerId);
            
            // Log authentication information
            try {
                var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null) {
                    System.out.println("Auth principal: " + authentication.getPrincipal());
                    System.out.println("Auth authorities: " + authentication.getAuthorities());
                    System.out.println("Auth name: " + authentication.getName());
                } else {
                    System.out.println("No authentication found in SecurityContextHolder");
                }
            } catch (Exception e) {
                System.out.println("Error accessing authentication: " + e.getMessage());
            }
            
            Account borrower = accountService.getAccountById(borrowerId);
            List<LendingRecord> records = lendingRecordService.getLendingRecordsByBorrower(borrower);
            
            List<LendingRecordResponseDto> recordDtos = records.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());
            
            System.out.println("Found " + recordDtos.size() + " lending records for borrower ID: " + borrowerId);
            return ResponseEntity.ok(recordDtos);
        } catch (IllegalArgumentException e) {
            System.out.println("Error retrieving lending records for borrower: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.out.println("Unexpected error retrieving lending records: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves active lending records for a borrower.
     * Supports Use Case 13: Return Borrowed Game (view active borrows)
     *
     * @param borrowerId The ID of the borrower
     * @return ResponseEntity containing a list of active lending records
     */
    @GetMapping("/borrower/{borrowerId}/active")
    public ResponseEntity<List<LendingRecordResponseDto>> getActiveLendingRecordsByBorrower(@PathVariable int borrowerId) {
        try {
            Account borrower = accountService.getAccountById(borrowerId);
            List<LendingRecord> allRecords = lendingRecordService.getLendingRecordsByBorrower(borrower);
            
            // Filter by active status
            List<LendingRecord> activeRecords = allRecords.stream()
                    .filter(record -> record.getStatus() == LendingStatus.ACTIVE)
                    .collect(Collectors.toList());
            
            List<LendingRecordResponseDto> recordDtos = activeRecords.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(recordDtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Marks a game as returned by the borrower.
     * Implements Use Case 13: Return Borrowed Game (borrower action)
     *
     * @param id The ID of the lending record
     * @param userId ID of the user marking the game as returned (optional)
     * @return ResponseEntity with the result of the operation
     */
    @PostMapping("/{id}/mark-returned")
    public ResponseEntity<String> markGameAsReturned(@PathVariable int id) { // Removed userId parameter
        try {
            // Service now uses authenticated user for audit/checks if needed
            // Update the status to OVERDUE as a placeholder for "Pending Return Confirmation"
            // Note: The service's updateStatus now requires a reason.
            return lendingRecordService.updateStatus(id, LendingStatus.OVERDUE,
                    "Game marked as returned by borrower, awaiting owner confirmation");
        } catch (ForbiddenException | UnauthedException e) {
             // Re-throw auth exceptions for handler (e.g., GlobalExceptionHandler)
             throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Confirms receipt of a returned game by the owner.
     * Implements Use Case 13: Return Borrowed Game (owner confirmation)
     * This will close the lending record and capture any additional return information.
     *
     * @param id The ID of the lending record
     * @param isDamaged Flag indicating if the game is damaged
     * @param damageNotes Notes describing the damage
     * @param damageSeverity The severity of the damage (0-3)
     * @param userId The ID of the user confirming the return
     * @return ResponseEntity with the result
     */
    @PostMapping("/{id}/confirm-return")
    public ResponseEntity<Map<String, Object>> confirmGameReturn(
            @PathVariable int id,
            @RequestParam(required = false, defaultValue = "false") boolean isDamaged,
            @RequestParam(required = false) String damageNotes,
            @RequestParam(required = false, defaultValue = "0") int damageSeverity) { // Removed userId parameter
        
        try {
            // Validate damage severity
            damageSeverity = validateDamageSeverity(damageSeverity);
            
            // Close the lending record with damage assessment
            // Service now uses authenticated user for audit/checks if needed
            ResponseEntity<String> result = lendingRecordService.closeLendingRecordWithDamageAssessment(
                    id, isDamaged, damageNotes, damageSeverity, "Confirmed return of game");
            
            // Create successful response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result.getBody());
            response.put("recordId", id);
            response.put("returnTime", new Date());
            response.put("isDamaged", isDamaged);
            
            if (isDamaged) {
                response.put("damageSeverity", damageSeverity);
                response.put("damageSeverityLabel", LendingRecordResponseDto.DamageSeverityUtils.getLabelForSeverity(damageSeverity));
                response.put("damageNotes", damageNotes);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (ForbiddenException | UnauthedException e) {
             // Re-throw auth exceptions for handler
             throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Handle validation errors or state errors like already closed records
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage(),
                "recordId", id
            ));
        } catch (Exception e) { // Catch unexpected errors
            // Other errors
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage(),
                "recordId", id
            ));
        }
    }

    /**
     * Filters lending records based on various criteria.
     * Used for the lending history view.
     *
     * @param filterDto The filter criteria
     * @param page The page number (0-based)
     * @param size The page size
     * @return ResponseEntity with paginated filtered lending records
     */
    @PostMapping("/filter")
    public ResponseEntity<Map<String, Object>> filterLendingRecords(
            @RequestBody LendingHistoryFilterDto filterDto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            // Get filtered records without pagination first
            List<LendingRecord> filteredRecords = lendingRecordService.filterLendingRecords(filterDto);
            
            // Manual pagination
            int start = page * size;
            int end = Math.min(start + size, filteredRecords.size());
            
            // Check if start is valid
            if (start > filteredRecords.size()) {
                start = 0;
                end = Math.min(size, filteredRecords.size());
                page = 0; // Adjust currentPage to 0 when no records are available
            }
            
            List<LendingRecord> paginatedRecords = filteredRecords.subList(start, end);
            
            // Convert records to DTOs
            List<LendingRecordResponseDto> recordDtos = paginatedRecords.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());
            
            // Create response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("records", recordDtos);
            response.put("currentPage", page);
            response.put("totalItems", filteredRecords.size());
            response.put("totalPages", (int) Math.ceil((double) filteredRecords.size() / size));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // Note: Disputes about damage or other lending issues are intentionally handled directly 
    // between users rather than through the application. This keeps the app focused on 
    // documenting and tracking, while letting users manage their own conflict resolution.

    /**
     * Updates the end date of a lending record.
     *
     * @param id The ID of the lending record
     * @param newEndDate The new end date
     * @return ResponseEntity with the result of the operation
     */
    @PutMapping("/{id}/end-date")
    public ResponseEntity<String> updateEndDate(@PathVariable int id, @RequestBody Date newEndDate) {
        try {
            return lendingRecordService.updateEndDate(id, newEndDate);
        } catch (ForbiddenException | UnauthedException e) {
            // Re-throw auth exceptions for handler
            throw e;
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Creates a new lending record based on request details.
     *
     * @param requestDetails Details required to create a lending record
     * @return ResponseEntity containing the created lending record or error message
     */
    @PostMapping
    public ResponseEntity<?> createLendingRecord(@RequestBody Map<String, Object> requestDetails) {
        try {
            // Extract required data from the request
            int requestId = (int) requestDetails.get("requestId");
            int ownerId = (int) requestDetails.get("ownerId");
            Date startDate = new Date((long) requestDetails.get("startDate"));
            Date endDate = new Date((long) requestDetails.get("endDate"));
            
            // Validate that end date is after start date
            if (endDate.before(startDate) || endDate.equals(startDate)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("End date must be after start date");
            }
            
            // Get the game owner
            GameOwner owner = (GameOwner) accountService.getAccountById(ownerId);
            
            // Use our improved service method that handles the entity retrieval
            ResponseEntity<String> result = lendingRecordService.createLendingRecordFromRequestId(
                    startDate, endDate, requestId, owner);
            
            if (result.getStatusCode().is2xxSuccessful()) {
                // Get the latest records for this owner to find the newly created one
                List<LendingRecord> records = lendingRecordService.getLendingRecordsByOwner(owner);
                
                // Find the most recently created record (should be our new one)
                // This is a bit of a hack, but it works for demonstration
                LendingRecord newRecord = records.stream()
                        .sorted((r1, r2) -> Long.compare(r2.getId(), r1.getId())) // Sort by ID descending
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Record was created but not found"));
                
                return ResponseEntity.ok(convertToResponseDto(newRecord));
            } else {
                return result;
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Admin function to delete a lending record.
     * Note: In production, consider soft delete instead.
     *
     * @param id The ID of the lending record to delete
     * @return ResponseEntity with the result of the operation
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteLendingRecord(@PathVariable int id) {
        try {
            // Service now throws exceptions on failure or returns ResponseEntity on success
            return lendingRecordService.deleteLendingRecord(id);
        } catch (ForbiddenException | UnauthedException e) {
            // Re-throw auth exceptions for handler
            throw e;
        } catch (ResourceNotFoundException e) { // Catch specific not found from service
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) { // Catch specific state errors (e.g., deleting active)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) { // Catch unexpected errors
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Retrieves overdue records.
     * 
     * @return ResponseEntity containing overdue lending records
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<LendingRecordResponseDto>> getOverdueRecords() {
        try {
            List<LendingRecord> overdueRecords = lendingRecordService.findOverdueRecords();
            
            List<LendingRecordResponseDto> recordDtos = overdueRecords.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(recordDtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Retrieves overdue records by owner.
     * 
     * @param ownerId The ID of the game owner
     * @return ResponseEntity containing overdue records for the owner
     */
    @GetMapping("/owner/{ownerId}/overdue")
    public ResponseEntity<List<LendingRecordResponseDto>> getOverdueRecordsByOwner(@PathVariable int ownerId) {
        try {
            GameOwner owner = (GameOwner) accountService.getAccountById(ownerId);
            List<LendingRecord> allRecords = lendingRecordService.getLendingRecordsByOwner(owner);
            
            // Filter to only include overdue records
            List<LendingRecord> overdueRecords = allRecords.stream()
                    .filter(record -> record.getEndDate().before(new Date()) && 
                                     record.getStatus() == LendingStatus.ACTIVE)
                    .collect(Collectors.toList());
            
            List<LendingRecordResponseDto> recordDtos = overdueRecords.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(recordDtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Updates the status of a lending record using the provided status DTO.
     * This endpoint provides more flexibility for status updates and handles complex business logic.
     *
     * @param id The ID of the lending record
     * @param statusDto The DTO containing the new status
     * @return ResponseEntity with the result of the operation
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateLendingRecordStatus(
            @PathVariable int id,
            @RequestBody UpdateLendingRecordStatusDto statusDto) { // userId removed from DTO processing
        try {
            // Basic validation
            if (statusDto.getNewStatus() == null || statusDto.getNewStatus().trim().isEmpty()) {
                 return ResponseEntity.badRequest().body(
                     Map.of("success", false, "message", "Status cannot be empty", "recordId", id)
                 );
            }
            
            // Try to parse the status enum value
            LendingStatus newStatus;
            try {
                newStatus = LendingStatus.valueOf(statusDto.getNewStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                 return ResponseEntity.badRequest().body(
                     Map.of(
                         "success", false,
                         "message", "Invalid status: " + statusDto.getNewStatus() + ". Valid values are: " +
                                    String.join(", ", Arrays.stream(LendingStatus.values()).map(Enum::name).collect(Collectors.toList())),
                         "recordId", id
                     )
                 );
            }
            
            // Call the service to update the status (userId is handled by service now)
            ResponseEntity<String> serviceResult = lendingRecordService.updateStatus(
                    id, newStatus, statusDto.getReason()); // Removed userId from call
            
            // Return a structured response
            return ResponseEntity.status(serviceResult.getStatusCode()).body(
                Map.of(
                    "success", serviceResult.getStatusCode().is2xxSuccessful(),
                    "message", serviceResult.getBody(),
                    "recordId", id,
                    "newStatus", newStatus.name()
                )
            );
        } catch (ForbiddenException | UnauthedException e) {
             // Re-throw auth exceptions for handler
             throw e;
        } catch (ResourceNotFoundException e) { // Catch specific not found from service
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                 Map.of("success", false, "message", e.getMessage(), "recordId", id)
             );
        } catch (IllegalArgumentException | IllegalStateException e) { // Catch validation/state errors
             return ResponseEntity.badRequest().body(
                 Map.of("success", false, "message", e.getMessage(), "recordId", id)
             );
        } catch (Exception e) { // Catch unexpected errors
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                 Map.of("success", false, "message", "An unexpected error occurred: " + e.getMessage(), "recordId", id)
             );
        }
    }

    /**
     * Checks if the current authenticated user can review a specific game.
     * A user can only review a game if they have borrowed and returned it.
     * 
     * @param gameId The ID of the game to check
     * @return Map containing a boolean "canReview" flag
     */
    @GetMapping("/can-review")
    public ResponseEntity<Map<String, Boolean>> canUserReviewGame(@RequestParam int gameId) {
        try {
            // Get authenticated user
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() 
                    || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
                return ResponseEntity.ok(Map.of("canReview", false));
            }
            
            String username = authentication.getName();
            Account user = accountService.getAccountByEmail(username);
            
            if (user == null) {
                return ResponseEntity.ok(Map.of("canReview", false));
            }
            
            // Get user's lending records
            List<LendingRecord> userRecords = lendingRecordService.getLendingRecordsByBorrower(user);
            
            // Check if user has a closed (returned) lending record for this game
            boolean hasReturnedGame = userRecords.stream()
                .anyMatch(record -> record.getRequest() != null 
                    && record.getRequest().getRequestedGame() != null 
                    && record.getRequest().getRequestedGame().getId() == gameId
                    && record.getStatus() == LendingStatus.CLOSED);
            
            return ResponseEntity.ok(Map.of("canReview", hasReturnedGame));
        } catch (Exception e) {
            System.out.println("Error checking if user can review game " + gameId + ": " + e.getMessage());
            return ResponseEntity.ok(Map.of("canReview", false));
        }
    }

    /**
     * Converts a LendingRecord entity to a LendingRecordResponseDto.
     *
     * @param record The lending record entity
     * @return The corresponding response DTO
     */
    private LendingRecordResponseDto convertToResponseDto(LendingRecord record) {
        // Create game info
        LendingRecordResponseDto.GameInfo gameInfo = new LendingRecordResponseDto.GameInfo(
                record.getRequest().getRequestedGame().getId(),
                record.getRequest().getRequestedGame().getName(),
                record.getRequest().getRequestedGame().getCategory(),
                record.getRequest().getRequestedGame().getImage());
        
        // Create borrower info
        LendingRecordResponseDto.UserInfo borrowerInfo = new LendingRecordResponseDto.UserInfo(
                record.getRequest().getRequester().getId(),
                record.getRequest().getRequester().getName(),
                record.getRequest().getRequester().getEmail());
        
        // Create owner info
        LendingRecordResponseDto.UserInfo ownerInfo = new LendingRecordResponseDto.UserInfo(
                record.getRecordOwner().getId(),
                record.getRecordOwner().getName(),
                record.getRecordOwner().getEmail());
        
        // Check if the record has damage information
        if (record.isDamaged()) {
            return new LendingRecordResponseDto(
                    record.getId(),
                    record.getStartDate(),
                    record.getEndDate(),
                    record.getStatus().toString(),
                    gameInfo,
                    borrowerInfo,
                    ownerInfo,
                    record.getDurationInDays(),
                    record.isDamaged(),
                    record.getDamageNotes(),
                    record.getDamageSeverity(),
                    record.getDamageAssessmentDate());
        } else {
            return new LendingRecordResponseDto(
                    record.getId(),
                    record.getStartDate(),
                    record.getEndDate(),
                    record.getStatus().toString(),
                    gameInfo,
                    borrowerInfo,
                    ownerInfo,
                    record.getDurationInDays());
        }
    }
}

/**
 * Global exception handler for lending record operations.
 * Provides consistent error responses for various exception types.
 */
@ControllerAdvice
class LendingRecordControllerAdvice {
    
    /**
     * Handles ResourceNotFoundException.
     * 
     * @param ex The exception
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * Handles InvalidOperationException.
     * 
     * @param ex The exception
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidOperationException(InvalidOperationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handles IllegalArgumentException.
     * 
     * @param ex The exception
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handles IllegalStateException.
     * 
     * @param ex The exception
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}