package ca.mcgill.ecse321.gameorganizer.services;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize; // Import PreAuthorize

import ca.mcgill.ecse321.gameorganizer.dto.request.BorrowRequestDto;
import ca.mcgill.ecse321.gameorganizer.dto.request.CreateBorrowRequestDto;
import ca.mcgill.ecse321.gameorganizer.exceptions.UnauthedException;
import ca.mcgill.ecse321.gameorganizer.exceptions.ForbiddenException;
// UserContext import removed
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.BorrowRequest;
import ca.mcgill.ecse321.gameorganizer.models.BorrowRequestStatus;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.BorrowRequestRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.GameRepository;
import ca.mcgill.ecse321.gameorganizer.models.GameInstance;
import ca.mcgill.ecse321.gameorganizer.repositories.GameInstanceRepository;
/**
 * Service for managing borrow requests in the game organizer system.
 * Handles request creation, retrieval, updates, and deletion.
 * 
 * @autor Rayan Baida
 */
@Service
public class BorrowRequestService {

    private static final Logger logger = LoggerFactory.getLogger(BorrowRequestService.class);

    private final BorrowRequestRepository borrowRequestRepository;
    private final GameRepository gameRepository;
    private final AccountRepository accountRepository;
    private final LendingRecordService lendingRecordService; // Added dependency
    private final GameInstanceRepository gameInstanceRepository; // Added dependency

    // UserContext field removed

    /**
     * Constructs a BorrowRequestService with required repositories.
     * 
     * @param borrowRequestRepository Repository for borrow requests.
     * @param gameRepository Repository for games.
     * @param accountRepository Repository for user accounts.
     */
    // Updated constructor to remove UserContext
    @Autowired
    public BorrowRequestService(BorrowRequestRepository borrowRequestRepository, GameRepository gameRepository, AccountRepository accountRepository, LendingRecordService lendingRecordService, GameInstanceRepository gameInstanceRepository) { // Added LendingRecordService and GameInstanceRepository
        this.borrowRequestRepository = borrowRequestRepository;
        this.gameRepository = gameRepository;
        this.accountRepository = accountRepository;
        this.lendingRecordService = lendingRecordService; // Initialize LendingRecordService
        this.gameInstanceRepository = gameInstanceRepository; // Initialize GameInstanceRepository
    }

    /**
     * Creates a new borrow request.
     * 
     * @param requestDTO The DTO containing request details.
     * @return The created borrow request as a DTO.
     * @throws IllegalArgumentException if the game or requester is not found, the owner requests their own game, or dates are invalid.
     */
    @Transactional
    public BorrowRequestDto createBorrowRequest(CreateBorrowRequestDto requestDTO) {
        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthedException("User must be authenticated to create a borrow request.");
        }
        String currentUsername = authentication.getName(); // Assuming username is email
        Account requester = accountRepository.findByEmail(currentUsername).orElseThrow(
             () -> new UnauthedException("Authenticated user '" + currentUsername + "' not found in database.")
        );

        // Fetch game using ID from DTO
        Optional<Game> gameOpt = gameRepository.findById(requestDTO.getRequestedGameId());
        if (!gameOpt.isPresent()) {
            throw new IllegalArgumentException("Game not found.");
        }
        
        // Validate the dates early to avoid null pointer issues in subsequent checks
        if (!requestDTO.getEndDate().after(requestDTO.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date.");
        }

        Game game = gameOpt.get();
        // Account requester is already fetched from authentication context

        // Check if the requester is the owner of the specific game instance, not just the game type
        Optional<GameInstance> gameInstanceOpt = gameInstanceRepository.findById(requestDTO.getGameInstanceId());
        if (!gameInstanceOpt.isPresent()) {
            throw new IllegalArgumentException("Game instance not found.");
        }
        
        GameInstance gameInstance = gameInstanceOpt.get();
        if (gameInstance.getOwner() != null && gameInstance.getOwner().getId() == requester.getId()) {
            throw new IllegalArgumentException("You cannot request your own game instance.");
        }

        // Check if the instance is available for the requested period
        List<BorrowRequest> overlappingRequests = borrowRequestRepository.findOverlappingApprovedRequestsForGameInstance(
                gameInstance.getId(), requestDTO.getStartDate(), requestDTO.getEndDate());

        if (!overlappingRequests.isEmpty()) {
            throw new IllegalArgumentException("Game instance is unavailable for the requested period.");
        }

        BorrowRequest borrowRequest = new BorrowRequest();
        borrowRequest.setRequestedGame(game);
        borrowRequest.setRequester(requester);
        borrowRequest.setStartDate(requestDTO.getStartDate());
        borrowRequest.setEndDate(requestDTO.getEndDate());
        borrowRequest.setStatus(BorrowRequestStatus.PENDING);
        borrowRequest.setRequestDate(new Date());
        borrowRequest.setGameInstance(gameInstance);

        BorrowRequest savedRequest = borrowRequestRepository.save(borrowRequest);

        return new BorrowRequestDto(
                savedRequest.getId(),
                savedRequest.getRequester().getId(),
                savedRequest.getRequestedGame().getId(),
                savedRequest.getGameInstance().getId(),
                savedRequest.getStartDate(),
                savedRequest.getEndDate(),
                savedRequest.getStatus().name(),
                savedRequest.getRequestDate()
        );
    }

    /**
     * Retrieves a borrow request by its ID.
     * 
     * @param id The borrow request ID.
     * @return The corresponding borrow request DTO.
     * @throws IllegalArgumentException if no request is found.
     */
    @Transactional
    @PreAuthorize("@borrowRequestService.isOwnerOrRequesterOfRequest(#id, authentication.principal.username)")
    public BorrowRequestDto getBorrowRequestById(int id) {
        try {
            BorrowRequest request = borrowRequestRepository.findBorrowRequestById(id)
                    .orElseThrow(() -> new IllegalArgumentException("No borrow request found with ID " + id));

            Integer requesterId = (request.getRequester() != null) ? request.getRequester().getId() : null;
            Integer gameId = (request.getRequestedGame() != null) ? request.getRequestedGame().getId() : null;
            
            return new BorrowRequestDto(
                    request.getId(),
                    requesterId,
                    gameId,
                    request.getGameInstance().getId(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getStatus().name(),
                    request.getRequestDate()
            );
        } catch (IllegalArgumentException e) {
            // Re-throw specific exceptions
            throw e;
        } catch (org.springframework.security.access.AccessDeniedException e) {
            // Catch potential AccessDeniedException from @PreAuthorize
            throw new ForbiddenException("Access denied: You can only view your own borrow requests or requests for games you own.");
        }
    }

    /**
     * Retrieves all borrow requests.
     * 
     * @return List of all borrow request DTOs.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or authentication.isAuthenticated()")
    public List<BorrowRequestDto> getAllBorrowRequests() {
        try {
            // Get all requests
            List<BorrowRequest> allRequests = borrowRequestRepository.findAll();
            
            // Get current authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                throw new UnauthedException("Authentication required to view borrow requests.");
            }
            
            String username = auth.getName();
            Account currentUser = accountRepository.findByEmail(username).orElse(null);
            
            if (currentUser == null) {
                throw new UnauthedException("Invalid user credentials.");
            }
            
            // Check if admin role (can see all)
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            if (isAdmin) {
                // Admin can see all borrow requests
                return allRequests.stream()
                    .map(request -> {
                        Integer requesterId = (request.getRequester() != null) ? request.getRequester().getId() : null;
                        Integer gameId = (request.getRequestedGame() != null) ? request.getRequestedGame().getId() : null;
                        Integer instanceId = (request.getGameInstance() != null) ? request.getGameInstance().getId() : null;
                        return new BorrowRequestDto(
                                request.getId(),
                                requesterId,
                                gameId,
                                instanceId != null ? instanceId : 0,
                                request.getStartDate(),
                                request.getEndDate(),
                                request.getStatus().name(),
                                request.getRequestDate()
                        );
                    })
                    .collect(Collectors.toList());
            } else {
                // Regular users can only see their own requests and requests for instances they own
                return allRequests.stream()
                    .filter(request -> 
                        // User is the requester
                        (request.getRequester() != null && request.getRequester().getId() == currentUser.getId()) ||
                        // User is the instance owner (not just the game creator)
                        (request.getGameInstance() != null && 
                         request.getGameInstance().getOwner() != null &&
                         request.getGameInstance().getOwner().getId() == currentUser.getId())
                    )
                    .map(request -> {
                        Integer requesterId = (request.getRequester() != null) ? request.getRequester().getId() : null;
                        Integer gameId = (request.getRequestedGame() != null) ? request.getRequestedGame().getId() : null;
                        Integer instanceId = (request.getGameInstance() != null) ? request.getGameInstance().getId() : null;
                        return new BorrowRequestDto(
                                request.getId(),
                                requesterId,
                                gameId,
                                instanceId != null ? instanceId : 0,
                                request.getStartDate(),
                                request.getEndDate(),
                                request.getStatus().name(),
                                request.getRequestDate()
                        );
                    })
                    .collect(Collectors.toList());
            }
        } catch (UnauthedException e) {
            throw e;
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw new ForbiddenException("Access denied: You do not have permission to view these borrow requests.");
        } catch (Exception e) {
            logger.error("Error retrieving borrow requests: {}", e.getMessage(), e);
            throw new RuntimeException("Error retrieving borrow requests", e);
        }
    }

    /**
     * Updates the status of a borrow request.
     * 
     * @param id The borrow request ID.
     * @param newStatus The new status (APPROVED or DECLINED).
     * @return The updated borrow request DTO.
     * @throws IllegalArgumentException if the request is not found or status is invalid.
     */
    @Transactional
    @PreAuthorize("@borrowRequestService.isGameOwnerOfRequest(#id, authentication.principal.username)")
    public BorrowRequestDto updateBorrowRequestStatus(int id, BorrowRequestStatus newStatus) {
        BorrowRequest request; // Declare request outside the try block
        try {
            request = borrowRequestRepository.findBorrowRequestById(id) // Assign inside the try block
                    .orElseThrow(() -> new IllegalArgumentException("No borrow request found with ID " + id));

            // Authorization is handled by @PreAuthorize

        if (newStatus != BorrowRequestStatus.APPROVED && newStatus != BorrowRequestStatus.DECLINED) {
            throw new IllegalArgumentException("Invalid status.");
        }

        // If the status is being set to APPROVED, create a LendingRecord
        if (newStatus == BorrowRequestStatus.APPROVED) {
            Game requestedGame = request.getRequestedGame();
            if (requestedGame == null) {
                throw new IllegalStateException("Cannot approve request: Game details are missing.");
            }
            GameOwner owner = requestedGame.getOwner();
            if (owner == null) {
                // This case might indicate an orphaned game or configuration issue.
                throw new IllegalStateException("Cannot approve request: Game owner is not set.");
            }
            
            // Check if there is at least one available game instance
            List<GameInstance> instances = gameInstanceRepository.findByGame(requestedGame);
            boolean hasAvailableInstance = false;
            
            for (GameInstance instance : instances) {
                if (instance.isAvailable()) {
                    hasAvailableInstance = true;
                    break;
                }
            }
            
            if (!hasAvailableInstance) {
                throw new IllegalStateException("Cannot approve request: No available game instance found for the requested game.");
            }

            // Inner try-catch specifically for lending record creation issues
            try {
                // Call LendingRecordService to create the record
                ResponseEntity<String> response = lendingRecordService.createLendingRecord(
                        request.getStartDate(),
                        request.getEndDate(),
                        request,
                        owner
                );

                // Check if the lending record creation was successful
                if (response.getStatusCode() != HttpStatus.OK) {
                    // Log the error and throw an exception to indicate failure during the transaction
                    String errorMessage = String.format("Failed to create lending record for approved borrow request %d. Status: %s, Body: %s",
                                                        request.getId(), response.getStatusCode(), response.getBody());
                    logger.error(errorMessage);
                    throw new RuntimeException(errorMessage); // Will cause rollback
                }
                 logger.info("Successfully created lending record for approved borrow request {}", request.getId());

            } catch (Exception e) { // Catch exceptions from lendingRecordService call
                 String errorMessage = String.format("Error creating lending record for approved borrow request %d: %s",
                                                      request.getId(), e.getMessage());
                 logger.error(errorMessage, e);
                 // Re-throw as a runtime exception to ensure transaction rollback
                 throw new RuntimeException(errorMessage, e);
            }
        } // End of 'if (newStatus == BorrowRequestStatus.APPROVED)' block

        // Update the status of the BorrowRequest
        request.setStatus(newStatus);
        BorrowRequest updatedRequest = borrowRequestRepository.save(request);

        // Prepare and return the DTO
        Integer requesterId = (updatedRequest.getRequester() != null) ? updatedRequest.getRequester().getId() : null;
        Integer gameId = (updatedRequest.getRequestedGame() != null) ? updatedRequest.getRequestedGame().getId() : null;
        Integer instanceId = (updatedRequest.getGameInstance() != null) ? updatedRequest.getGameInstance().getId() : null;

        return new BorrowRequestDto(
                updatedRequest.getId(),
                requesterId,
                gameId,
                instanceId != null ? instanceId : 0,
                updatedRequest.getStartDate(),
                updatedRequest.getEndDate(),
                updatedRequest.getStatus().name(),
                updatedRequest.getRequestDate()
        );

    } catch (IllegalArgumentException | IllegalStateException e) {
        // Catch specific business logic exceptions and re-throw
        throw e;
    } catch (org.springframework.security.access.AccessDeniedException e) {
        // Catch potential AccessDeniedException from @PreAuthorize and convert
        throw new ForbiddenException("Access denied: Only the game owner can approve or decline requests.");
    } // End of outer try-catch block for the whole method
}


    /**
     * Deletes a borrow request by its ID.
     * 
     * @param id The borrow request ID.
     * @throws IllegalArgumentException if no request is found.
     */
    @Transactional
    @PreAuthorize("@borrowRequestService.isOwnerOrRequesterOfRequest(#id, authentication.principal.username)")
    public void deleteBorrowRequest(int id) {
        try {
            BorrowRequest request = borrowRequestRepository.findBorrowRequestById(id)
                    .orElseThrow(() -> new IllegalArgumentException("No borrow request found with ID " + id));

            // Authorization handled by @PreAuthorize

            logger.info("User authorized. Deleting borrow request with ID: {}", id);
            borrowRequestRepository.delete(request);

        } catch (IllegalArgumentException e) {
            // Re-throw specific exceptions if needed, or let GlobalExceptionHandler handle them
            throw e;
        } catch (org.springframework.security.access.AccessDeniedException e) {
            // Catch potential AccessDeniedException from @PreAuthorize
            throw new ForbiddenException("Access denied: You can only delete your own borrow requests or requests for games you own.");
        }
    }



    // --- Helper methods for @PreAuthorize --- 

    /**
     * Checks if the given username corresponds to the owner of the game instance associated with the borrow request.
     */
    public boolean isGameOwnerOfRequest(int requestId, String username) {
        if (username == null) return false;
        try {
            BorrowRequest request = borrowRequestRepository.findBorrowRequestById(requestId).orElse(null);
            Account user = accountRepository.findByEmail(username).orElse(null);

            if (request == null || user == null || request.getGameInstance() == null || request.getGameInstance().getOwner() == null) {
                return false; // Cannot determine ownership
            }

            return request.getGameInstance().getOwner().getId() == user.getId();
        } catch (Exception e) {
            logger.error("Error during isGameOwnerOfRequest check for request {}: {}", requestId, e.getMessage());
            return false; // Deny on error
        }
    }

    /**
     * Checks if the given username corresponds to either the owner of the game instance 
     * or the requester of the borrow request.
     */
    public boolean isOwnerOrRequesterOfRequest(int requestId, String username) {
        if (username == null) return false;
        try {
            BorrowRequest request = borrowRequestRepository.findBorrowRequestById(requestId).orElse(null);
            Account user = accountRepository.findByEmail(username).orElse(null);

            if (request == null || user == null) {
                return false; // Cannot determine relationship
            }

            // Check if user is the requester
            boolean isRequester = request.getRequester() != null && request.getRequester().getId() == user.getId();
            if (isRequester) return true;

            // Check if user is the game instance owner
            boolean isGameInstanceOwner = request.getGameInstance() != null && 
                                  request.getGameInstance().getOwner() != null &&
                                  request.getGameInstance().getOwner().getId() == user.getId();
            
            return isGameInstanceOwner;
        } catch (Exception e) {
            logger.error("Error during isOwnerOrRequesterOfRequest check for request {}: {}", requestId, e.getMessage());
            return false; // Deny on error
        }
    }

    /**
     * Finds all borrow requests associated with a specific game instance owner by their ID.
     *
     * @param ownerId The ID of the game instance owner
     * @return List of borrow request DTOs associated with the specified game instance owner
     */
    @Transactional
    public List<BorrowRequestDto> getBorrowRequestsByOwnerId(int ownerId) {
        // Find all game instances owned by this owner
        List<GameInstance> ownedInstances = gameInstanceRepository.findByOwnerId(ownerId);
        
        // Get IDs of owned instances
        List<Integer> ownedInstanceIds = ownedInstances.stream()
            .map(GameInstance::getId)
            .collect(Collectors.toList());
            
        // Get all borrow requests
        List<BorrowRequest> allRequests = borrowRequestRepository.findAll();
        
        // Filter requests for owned instances
        List<BorrowRequest> ownerRequests = allRequests.stream()
            .filter(request -> request.getGameInstance() != null && 
                ownedInstanceIds.contains(request.getGameInstance().getId()))
            .collect(Collectors.toList());

        return ownerRequests.stream()
                .map(request -> {
                    Integer requesterId = (request.getRequester() != null) ? request.getRequester().getId() : null;
                    Integer gameId = (request.getRequestedGame() != null) ? request.getRequestedGame().getId() : null;
                    Integer instanceId = (request.getGameInstance() != null) ? request.getGameInstance().getId() : null;
                    return new BorrowRequestDto(
                            request.getId(),
                            requesterId,
                            gameId,
                            instanceId != null ? instanceId : 0,
                            request.getStartDate(),
                            request.getEndDate(),
                            request.getStatus().name(),
                            request.getRequestDate()
                    );
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Checks if the given username is the requester for a specific borrow request
     *
     * @param requestId The ID of the borrow request to check
     * @param username The username (email) to check
     * @return true if the username is the requester, false otherwise
     */
    @Transactional
    public boolean isRequesterForRequest(int requestId, String username) {
        Optional<BorrowRequest> request = borrowRequestRepository.findById(requestId);
        if (!request.isPresent()) {
            throw new IllegalArgumentException("Borrow request with ID " + requestId + " not found.");
        }
        
        Account requester = request.get().getRequester();
        if (requester == null) {
            return false;
        }
        
        return requester.getEmail().equals(username);
    }
    
    /**
     * Updates a borrow request with new details.
     * Only updates allowed fields like dates, message, etc.
     * Status must be handled separately.
     *
     * @param requestId The ID of the borrow request to update
     * @param updatedRequestDto The DTO containing updated fields
     * @return The updated borrow request DTO
     */
    @Transactional
    public BorrowRequestDto updateBorrowRequestDetails(int requestId, BorrowRequestDto updatedRequestDto) {
        Optional<BorrowRequest> existingRequestOpt = borrowRequestRepository.findById(requestId);
        if (!existingRequestOpt.isPresent()) {
            throw new IllegalArgumentException("Borrow request with ID " + requestId + " not found.");
        }
        
        BorrowRequest existingRequest = existingRequestOpt.get();
        
        // Only update if the request is in PENDING status
        if (existingRequest.getStatus() != BorrowRequestStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING requests can be modified.");
        }
        
        // Update the dates
        if (updatedRequestDto.getStartDate() != null) {
            existingRequest.setStartDate(updatedRequestDto.getStartDate());
        }
        
        if (updatedRequestDto.getEndDate() != null) {
            existingRequest.setEndDate(updatedRequestDto.getEndDate());
        }
        
        // Validate that the end date is after the start date
        if (existingRequest.getEndDate().before(existingRequest.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date.");
        }
        
        // Check if the dates conflict with existing requests
        if (!isGameAvailableForPeriod(existingRequest.getRequestedGame().getId(), 
                                      existingRequest.getStartDate(), 
                                      existingRequest.getEndDate(),
                                      existingRequest.getId())) {
            throw new IllegalArgumentException("The game is not available for the selected time period.");
        }
        
        // Save the updated request
        BorrowRequest updatedRequest = borrowRequestRepository.save(existingRequest);
        
        // Convert to DTO and return
        return new BorrowRequestDto(
            updatedRequest.getId(),
            updatedRequest.getRequester().getId(),
            updatedRequest.getRequestedGame().getId(),
            updatedRequest.getGameInstance().getId(),
            updatedRequest.getStartDate(),
            updatedRequest.getEndDate(),
            updatedRequest.getStatus().name(),
            updatedRequest.getRequestDate()
        );
    }
    
    /**
     * Checks if a game is available for a specific period, excluding a specific request
     * Used when updating a request to make sure the new dates don't conflict
     * 
     * @param gameId The ID of the game to check
     * @param startDate The desired start date
     * @param endDate The desired end date
     * @param excludeRequestId The ID of the request to exclude from the check
     * @return true if the game is available, false otherwise
     */
    private boolean isGameAvailableForPeriod(int gameId, Date startDate, Date endDate, int excludeRequestId) {
        // Get all approved borrow requests for this game that overlap with the period
        List<BorrowRequest> overlappingRequests = borrowRequestRepository
            .findOverlappingApprovedRequestsForGame(gameId, startDate, endDate);
        
        // Exclude the current request being updated
        overlappingRequests = overlappingRequests.stream()
            .filter(request -> request.getId() != excludeRequestId)
            .collect(Collectors.toList());
        
        // If there are any overlapping requests, the game is not available
        return overlappingRequests.isEmpty();
    }
}
