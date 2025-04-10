package ca.mcgill.ecse321.gameorganizer.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import ca.mcgill.ecse321.gameorganizer.models.BorrowRequestStatus;
import ca.mcgill.ecse321.gameorganizer.dto.request.BorrowRequestDto;
import ca.mcgill.ecse321.gameorganizer.dto.request.CreateBorrowRequestDto;
import ca.mcgill.ecse321.gameorganizer.services.BorrowRequestService;
import ca.mcgill.ecse321.gameorganizer.repositories.BorrowRequestRepository;
import ca.mcgill.ecse321.gameorganizer.exceptions.ForbiddenException; // Import
import ca.mcgill.ecse321.gameorganizer.exceptions.UnauthedException; // Import

/**
 * Controller for managing borrow requests.
 * Handles creating, retrieving, updating, and deleting borrow requests.
 *
 * @author Rayan Baida
 */
@RestController
@RequestMapping("/api/borrowrequests")
public class BorrowRequestController {

    private final BorrowRequestService borrowRequestService;
    private final BorrowRequestRepository borrowRequestRepository;

    /**
     * Constructor to inject the BorrowRequestService and BorrowRequestRepository.
     *
     * @param borrowRequestService Service handling borrow request logic.
     * @param borrowRequestRepository Repository handling borrow request data.
     */
    @Autowired
    public BorrowRequestController(BorrowRequestService borrowRequestService, BorrowRequestRepository borrowRequestRepository) {
        this.borrowRequestService = borrowRequestService;
        this.borrowRequestRepository = borrowRequestRepository;
    }

    /**
     * Creates a new borrow request.
     *
     * @param dto Data transfer object containing request details.
     * @return The created borrow request.
     */
    @PostMapping
    public ResponseEntity<BorrowRequestDto> createBorrowRequest(@RequestBody CreateBorrowRequestDto dto) {
        System.out.println("Received Borrow Request: " + dto);
        try {
            return ResponseEntity.ok(borrowRequestService.createBorrowRequest(dto));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Retrieves a borrow request by its ID.
     *
     * @param id The ID of the borrow request.
     * @return The borrow request if found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BorrowRequestDto> getBorrowRequestById(@PathVariable int id) {
        try {
            return ResponseEntity.ok(borrowRequestService.getBorrowRequestById(id));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Borrow request with ID " + id + " not found.");
        }
    }

    /**
     * Retrieves all borrow requests.
     *
     * @return A list of all borrow requests.
     */
    @GetMapping
    public ResponseEntity<List<BorrowRequestDto>> getAllBorrowRequests() {
        return ResponseEntity.ok(borrowRequestService.getAllBorrowRequests());
    }

    /**
     * Updates the status of a borrow request.
     *
     * @param id The ID of the borrow request to update.
     * @param requestDto The updated borrow request details.
     * @return The updated borrow request.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BorrowRequestDto> updateBorrowRequestStatus(
            @PathVariable int id,
            @RequestBody BorrowRequestDto requestDto) {
        try {
            BorrowRequestStatus status;
            try {
                status = BorrowRequestStatus.valueOf(requestDto.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status value: " + requestDto.getStatus());
            }
           return ResponseEntity.ok(borrowRequestService.updateBorrowRequestStatus(id, status));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Borrow request with ID " + id + " not found.");
        }
    }

    /**
     * Deletes a borrow request by its ID.
     *
     * @param id The ID of the borrow request to delete.
     * @return HTTP 204 response if successful.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBorrowRequest(@PathVariable int id) {
        try {
            // Removed test environment special handling
            // Normal flow: call service, which now handles auth
            borrowRequestService.deleteBorrowRequest(id);
            return ResponseEntity.noContent().build(); // Return 204 No Content on success
        } catch (IllegalArgumentException e) {
             // Let GlobalExceptionHandler handle this (typically 404 or 400)
             // Consider logging e.getMessage()
             throw e;
        } catch (ForbiddenException e) {
             // Let GlobalExceptionHandler handle this (typically 403)
             // Consider logging e.getMessage()
             throw e;
        } catch (UnauthedException e) {
             // Let GlobalExceptionHandler handle this (typically 401)
             // Consider logging e.getMessage()
             throw e;
        }
        // Other potential exceptions will also be caught by GlobalExceptionHandler
    }

    /**
     * Retrieve borrow requests filtered by status.
     *
     * @param status The status to filter by (e.g., "PENDING", "APPROVED", etc.).
     * @return A list of borrow requests with the specified status.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<BorrowRequestDto>> getBorrowRequestsByStatus(@PathVariable String status) {
        List<BorrowRequestDto> filteredRequests = borrowRequestService.getAllBorrowRequests().stream()
                .filter(request -> request.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());
        return ResponseEntity.ok(filteredRequests);
    }

    /**
     * Retrieve all borrow requests for a particular requester.
     *
     * @param requesterId The ID of the user who initiated the borrow request.
     * @return A list of borrow requests for the specified requester.
     */
    /**
     * Retrieve all borrow requests for a particular requester, visible to the current user.
     *
     * @param requesterId The ID of the user who initiated the borrow request.
     * @return A list of borrow requests for the specified requester that the current user is allowed to see.
     */
    @GetMapping("/requester/{requesterId}")
    public ResponseEntity<List<BorrowRequestDto>> getBorrowRequestsByRequester(
            @PathVariable int requesterId) { // Removed unused @RequestParam

        // Log authentication information (optional, for debugging)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            System.out.println("Auth principal: " + auth.getPrincipal());
            System.out.println("Auth authorities: " + auth.getAuthorities());
            System.out.println("Auth name: " + auth.getName());
        } else {
            System.out.println("No authentication found in SecurityContextHolder");
        }

        System.out.println("Fetching borrow requests visible to current user, filtered for requesterId: " + requesterId);

        try {
            // 1. Get all requests visible to the current user (service handles auth)
            List<BorrowRequestDto> visibleRequests = borrowRequestService.getAllBorrowRequests();

            // 2. Filter those requests by the specific requesterId from the path
            List<BorrowRequestDto> filteredRequests = visibleRequests.stream()
                    .filter(request -> request.getRequesterId() == requesterId) // Compare primitive int directly
                    .collect(Collectors.toList());

            System.out.println("Found " + filteredRequests.size() + " requests for requester " + requesterId + " visible to current user.");
            return ResponseEntity.ok(filteredRequests);
        } catch (UnauthedException | ForbiddenException e) {
             // Let GlobalExceptionHandler handle auth errors from getAllBorrowRequests
             throw e;
        } catch (Exception e) {
            System.err.println("Error retrieving borrow requests for requester: " + e.getMessage());
            e.printStackTrace();
            // Let GlobalExceptionHandler handle other errors
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving borrow requests: " + e.getMessage());
        }
    }

    /**
     * Retrieve all borrow requests for a specific game owner.
     *
     * @param ownerId The ID of the game owner.
     * @return A list of borrow requests associated with the specified game owner.
     */
    @GetMapping("/by-owner/{ownerId}") // Changed mapping for troubleshooting
    public ResponseEntity<List<BorrowRequestDto>> getBorrowRequestsByOwnerId(@PathVariable int ownerId) {
        try {
            // Use service method from origin/dev-Yessine-D3
            return ResponseEntity.ok(borrowRequestService.getBorrowRequestsByOwnerId(ownerId));
        } catch (IllegalArgumentException e) {
            // Service might throw this if owner not found or has no requests
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No borrow requests found for owner with ID " + ownerId);
        } catch (Exception e) {
             System.err.println("Error retrieving borrow requests for owner: " + e.getMessage());
             e.printStackTrace();
             // Let GlobalExceptionHandler handle this
             throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving borrow requests for owner: " + e.getMessage());
        }
    }

    /**
     * Updates a user's own borrow request with new details.
     * Only allows the requester who created the request to update it.
     * Only pending requests can be updated.
     *
     * @param id The ID of the borrow request to update.
     * @param updatedRequestDto The updated borrow request details.
     * @return The updated borrow request.
     */
    @PutMapping("/{id}/user-update")
    public ResponseEntity<BorrowRequestDto> updateUserBorrowRequest(
            @PathVariable int id,
            @RequestBody BorrowRequestDto updatedRequestDto) {
        
        // Log request for debugging
        System.out.println("Received request to update borrow request: " + id);
        
        try {
            // Get current authentication
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                throw new UnauthedException("Authentication required to update borrow request.");
            }
            
            // Get the original request to verify ownership
            BorrowRequestDto existingRequest = borrowRequestService.getBorrowRequestById(id);
            
            // Check if the current user is the requester
            String username = auth.getName();
            if (!borrowRequestService.isRequesterForRequest(id, username)) {
                throw new ForbiddenException("You can only update your own borrow requests.");
            }
            
            // Check if the request is in 'PENDING' status
            if (!existingRequest.getStatus().equalsIgnoreCase("PENDING")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Only pending requests can be modified. This request is " + existingRequest.getStatus());
            }
            
            // Update the request details
            return ResponseEntity.ok(borrowRequestService.updateBorrowRequestDetails(id, updatedRequestDto));
            
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (UnauthedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (ForbiddenException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            System.err.println("Error updating borrow request: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error updating borrow request: " + e.getMessage());
        }
    }
}