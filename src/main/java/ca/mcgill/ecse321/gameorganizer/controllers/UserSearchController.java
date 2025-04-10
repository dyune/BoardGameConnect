package ca.mcgill.ecse321.gameorganizer.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ca.mcgill.ecse321.gameorganizer.dto.response.UserSummaryDto;
import ca.mcgill.ecse321.gameorganizer.services.UserSearchService;

import java.util.List;
import java.util.Map;

/**
 * Controller for user search endpoints.
 * Provides API endpoints for searching and retrieving user information.
 */
@RestController
@RequestMapping("/api/users")
public class UserSearchController {

    private final UserSearchService userSearchService;

    @Autowired
    public UserSearchController(UserSearchService userSearchService) {
        this.userSearchService = userSearchService;
    }

    /**
     * Get user by exact email match.
     * 
     * @param email The email to search for
     * @return UserSummaryDto if user is found, 404 Not Found otherwise
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        UserSummaryDto user = userSearchService.getUserByEmail(email);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User with email " + email + " not found");
        }
    }

    /**
     * Search for users by name pattern.
     * 
     * @param name The name pattern to search for
     * @return List of UserSummaryDto objects for matching users
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<List<UserSummaryDto>> searchUsersByName(@PathVariable String name) {
        List<UserSummaryDto> users = userSearchService.searchUsersByName(name);
        return ResponseEntity.ok(users);
    }

    /**
     * Search for users by email pattern.
     * 
     * @param email The email pattern to search for
     * @return List of UserSummaryDto objects for matching users
     */
    @GetMapping("/email/search/{email}")
    public ResponseEntity<List<UserSummaryDto>> searchUsersByEmail(@PathVariable String email) {
        List<UserSummaryDto> users = userSearchService.searchUsersByEmail(email);
        return ResponseEntity.ok(users);
    }

    /**
     * Search for users by either name or email containing the provided string.
     * Endpoint that handles both name and email search in a single request.
     * 
     * @param searchTerms Map containing search parameters:
     *                   - "term": The search term to find in name or email
     *                   - "gameOwnerOnly": (optional) Whether to only return game owners
     * @return List of UserSummaryDto objects for matching users
     */
    @PostMapping("/search")
    public ResponseEntity<List<UserSummaryDto>> searchUsers(@RequestBody Map<String, Object> searchTerms) {
        String searchTerm = (String) searchTerms.get("term");
        Boolean gameOwnerOnly = searchTerms.containsKey("gameOwnerOnly") ? 
                (Boolean) searchTerms.get("gameOwnerOnly") : false;
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        
        List<UserSummaryDto> users = userSearchService.searchUsers(searchTerm, gameOwnerOnly);
        return ResponseEntity.ok(users);
    }
    
    /**
     * Search for users by either name or email using GET method with query parameters.
     * Alternative endpoint that uses query parameters instead of POST body.
     * 
     * @param term The search term to find in name or email
     * @param gameOwnerOnly Whether to only return game owners
     * @return List of UserSummaryDto objects for matching users
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserSummaryDto>> searchUsersGet(
            @RequestParam String term,
            @RequestParam(required = false, defaultValue = "false") boolean gameOwnerOnly) {
        
        if (term == null || term.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        
        List<UserSummaryDto> users = userSearchService.searchUsers(term, gameOwnerOnly);
        return ResponseEntity.ok(users);
    }
} 