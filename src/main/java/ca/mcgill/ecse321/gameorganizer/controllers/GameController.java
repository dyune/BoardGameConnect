package ca.mcgill.ecse321.gameorganizer.controllers;

import java.util.List;
import java.util.Map;
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

import ca.mcgill.ecse321.gameorganizer.dto.request.GameCreationDto;
import ca.mcgill.ecse321.gameorganizer.dto.request.GameSearchCriteria;
import ca.mcgill.ecse321.gameorganizer.dto.request.ReviewSubmissionDto; // Import
import ca.mcgill.ecse321.gameorganizer.dto.response.GameInstanceResponseDto; // Import
import ca.mcgill.ecse321.gameorganizer.dto.response.GameResponseDto; // Import
import ca.mcgill.ecse321.gameorganizer.dto.response.ReviewResponseDto; // Import
import ca.mcgill.ecse321.gameorganizer.exceptions.ForbiddenException;
import ca.mcgill.ecse321.gameorganizer.exceptions.ResourceNotFoundException;
import ca.mcgill.ecse321.gameorganizer.exceptions.UnauthedException;
import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import ca.mcgill.ecse321.gameorganizer.services.AccountService;
import ca.mcgill.ecse321.gameorganizer.services.GameService;

/**
 * Controller that handles API endpoints for game operations.
 * Provides endpoints for creating, retrieving, updating, and deleting games,
 * as well as various filtering options.
 *
 * @author Alexander
 */

@RestController
@RequestMapping("/api/games")

public class GameController {
    @Autowired
    private GameService service;

    @Autowired
    private AccountService accountService;

    /**
     * Retrieves all games in the system, with optional filtering.
     *
     * @param ownerId Optional parameter to filter games by owner's email
     * @param category Optional parameter to filter games by category
     * @param namePart Optional parameter to filter games by name containing text
     * @return List of games matching the filter criteria
     */
    @GetMapping
    public ResponseEntity<List<GameResponseDto>> getAllGames(
            @RequestParam(required = false) String ownerId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String namePart) {

        List<Game> games;

        if (ownerId != null && !ownerId.isEmpty()) {
            // If owner email is provided, get games by owner
            Account account = accountService.getAccountByEmail(ownerId);
            if (account instanceof GameOwner) {
                games = service.getGamesByOwner((GameOwner) account);
            } else {
                throw new IllegalArgumentException("Account is not a game owner");
            }
        } else if (category != null && !category.isEmpty()) {
            // If category is provided, filter by category
            games = service.getGamesByCategory(category);
        } else if (namePart != null && !namePart.isEmpty()) {
            // If name part is provided, search by name containing
            games = service.getGamesByNameContaining(namePart);
        } else {
            // Otherwise, get all games
            games = service.getAllGames();
        }

        List<GameResponseDto> gameResponseDtos = games.stream()
                .map(GameResponseDto::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(gameResponseDtos);
    }

    /**
     * Retrieves a specific game by ID.
     *
     * @param id ID of the game to retrieve
     * @return The requested game
     */
    @GetMapping("/{id}")
    public ResponseEntity<GameResponseDto> findGameById(@PathVariable int id) {
        Game game = service.getGameById(id);
        return ResponseEntity.ok(new GameResponseDto(game));
    }

    /**
     * Creates a new game.
     *
     * @param gameCreationDto Data for the new game
     * @return The created game
     */
    @PostMapping
    public ResponseEntity<GameResponseDto> createGame(@RequestBody GameCreationDto gameCreationDto) {
        try {
            // Service now uses authenticated principal for owner
            GameResponseDto createdGame = service.createGame(gameCreationDto);
            return new ResponseEntity<>(createdGame, HttpStatus.CREATED);
        } catch (UnauthedException e) {
            // When owner doesn't exist, return 400 Bad Request
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (ForbiddenException e) {
            // When permission denied, return 403 Forbidden
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            // Handle validation errors
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Updates an existing game.
     *
     * @param id ID of the game to update
     * @param gameDto Updated game data
     * @return The updated game
     */
    @PutMapping("/{id}")
    public ResponseEntity<GameResponseDto> updateGame(@PathVariable int id, @RequestBody GameCreationDto gameDto) {
        try {
            GameResponseDto updatedGame = service.updateGame(id, gameDto);
            return ResponseEntity.ok(updatedGame);
        } catch (ForbiddenException | UnauthedException e) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage()); // Or UNAUTHORIZED
        } catch (ResourceNotFoundException e) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalArgumentException e) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Deletes a game by ID.
     *
     * @param id ID of the game to delete
     * @return Confirmation message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteGame(@PathVariable int id) {
        try {
            return service.deleteGame(id);
        } catch (ForbiddenException | UnauthedException e) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage()); // Or UNAUTHORIZED
        } catch (ResourceNotFoundException e) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Retrieves games that can be played with the specified number of players.
     *
     * @param players Number of players
     * @return List of games compatible with the player count
     */
    @GetMapping("/players")
    public ResponseEntity<List<GameResponseDto>> getGamesByPlayerCount(@RequestParam int players) {
        List<Game> games = service.getGamesByPlayerRange(players, players);
        List<GameResponseDto> gameResponseDtos = games.stream()
                .map(GameResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(gameResponseDtos);
    }

    /**
     * Advanced search endpoint for games with multiple criteria
     */
    @GetMapping("/search")
    public ResponseEntity<List<GameResponseDto>> searchGames(GameSearchCriteria criteria) {
        List<Game> games = service.searchGames(criteria);
        List<GameResponseDto> gameResponseDtos = games.stream()
                .map(GameResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(gameResponseDtos);
    }

    /**
     * Get all games owned by a specific user
     */
    @GetMapping("/users/{ownerId}/games")
    public ResponseEntity<List<GameResponseDto>> getGamesByOwner(@PathVariable String ownerId) {
        Account account = accountService.getAccountByEmail(ownerId);
        if (!(account instanceof GameOwner)) {
            throw new IllegalArgumentException("Account is not a game owner");
        }
        List<Game> games = service.getGamesByOwner((GameOwner) account);
        List<GameResponseDto> gameResponseDtos = games.stream()
                .map(GameResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(gameResponseDtos);
    }

    /**
     * Get all reviews for a specific game
     */
    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewResponseDto>> getGameReviews(@PathVariable int id) {
        List<ReviewResponseDto> reviews = service.getReviewsByGameId(id);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Submit a new review for a game
     */
    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewResponseDto> submitGameReview(
            @PathVariable int id,
            @RequestBody ReviewSubmissionDto reviewDto) {
        try {
            reviewDto.setGameId(id);
            // Service now uses authenticated principal for reviewer
            ReviewResponseDto review = service.submitReview(reviewDto);
            return new ResponseEntity<>(review, HttpStatus.CREATED);
        } catch (ForbiddenException | UnauthedException e) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage()); // Or UNAUTHORIZED
        } catch (ResourceNotFoundException e) {
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Get average rating for a game
     */
    @GetMapping("/{id}/rating")
    public ResponseEntity<Double> getGameRating(@PathVariable int id) {
        double rating = service.getAverageRatingForGame(id);
        return ResponseEntity.ok(rating);
    }

    /**
     * Get all instances for a specific game OR owned by a specific user.
     * If 'id' path variable is present, gets instances for that game.
     * If 'ownerId' request parameter is present, gets instances owned by that user.
     * If 'my' request parameter is true, gets instances for the current user.
     * If none are present, potentially returns all instances (or could be restricted).
     * 
     * @param id (Optional) Path variable for the game ID.
     * @param ownerId (Optional) Request parameter for the owner's email.
     * @param my (Optional) Request parameter to fetch current user's instances.
     * @return List of game instances.
     */
    @GetMapping({"/instances", "/{id}/instances"}) // Combine paths
    public ResponseEntity<List<GameInstanceResponseDto>> getGameInstances(
            @PathVariable(required = false) Integer id,
            @RequestParam(required = false) String ownerId,
            @RequestParam(required = false) Boolean my) {
        try {
            List<GameInstanceResponseDto> instances;
            if (id != null) {
                // Fetch by game ID (original functionality)
                instances = service.getInstancesByGameId(id);
            } else if (ownerId != null && !ownerId.isEmpty()) {
                // Fetch by owner email
                Account account = accountService.getAccountByEmail(ownerId);
                if (account instanceof GameOwner) {
                    instances = service.getGameInstancesByOwner((GameOwner) account);
                } else {
                    throw new IllegalArgumentException("Specified ownerId does not belong to a GameOwner account.");
                }
            } else if (my != null && my) {
                 // Fetch for current authenticated user
                 instances = service.getGameInstancesByCurrentUser();
            }
            else {
                // Optional: Decide what to do if no filter is provided.
                // Could return all instances, or throw an error, or return empty list.
                // Returning empty list for now to avoid exposing all instances unintentionally.
                 instances = java.util.Collections.emptyList();
                 // Alternatively, throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please specify a game ID, owner ID, or 'my=true' to fetch instances.");
            }
            return ResponseEntity.ok(instances);
        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (UnauthedException e) { // Needed for getGameInstancesByCurrentUser
             throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            // Log unexpected errors
            // logger.error("Unexpected error retrieving game instances: {}", e.getMessage(), e); // Assuming logger is available
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving game instances: " + e.getMessage());
        }
    }

    /**
     * Check if a game is available for a specific date range
     * 
     * @param id The ID of the game to check
     * @param startDate The start date of the borrowing period (in milliseconds since epoch)
     * @param endDate The end date of the borrowing period (in milliseconds since epoch)
     * @return Boolean indicating whether the game is available for the specified period
     */
    @GetMapping("/{id}/availability")
    public ResponseEntity<Boolean> checkGameAvailability(
            @PathVariable int id,
            @RequestParam long startDate,
            @RequestParam long endDate) {
        try {
            // Convert milliseconds to Date objects
            java.util.Date start = new java.util.Date(startDate);
            java.util.Date end = new java.util.Date(endDate);
            
            boolean isAvailable = service.isGameAvailableForPeriod(id, start, end);
            return ResponseEntity.ok(isAvailable);
        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error checking game availability");
        }
    }

    /**
     * Creates a new game instance (copy)
     * 
     * @param id ID of the game
     * @param instanceData Data for the new instance
     * @return The created game instance
     */
    @PostMapping("/{id}/instances")
    public ResponseEntity<GameInstanceResponseDto> createGameInstance(
            @PathVariable int id,
            @RequestBody Map<String, Object> instanceData) {
        try {
            // Add gameId to the instance data
            instanceData.put("gameId", id);
            
            // Call service method to create the instance
            GameInstanceResponseDto createdInstance = service.createGameInstance(instanceData);
            return new ResponseEntity<>(createdInstance, HttpStatus.CREATED);
        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (ForbiddenException | UnauthedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Updates a specific game instance
     * 
     * @param id ID of the game
     * @param instanceId ID of the instance to update
     * @param instanceData Updated instance data
     * @return The updated game instance
     */
    @PutMapping("/{id}/instances/{instanceId}")
    public ResponseEntity<GameInstanceResponseDto> updateGameInstance(
            @PathVariable int id,
            @PathVariable int instanceId,
            @RequestBody Map<String, Object> instanceData) {
        try {
            // Add gameId to the instance data
            instanceData.put("gameId", id);
            
            // Call service method to update the instance
            GameInstanceResponseDto updatedInstance = service.updateGameInstance(instanceId, instanceData);
            return ResponseEntity.ok(updatedInstance);
        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (ForbiddenException | UnauthedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Deletes a specific game instance
     * 
     * @param id ID of the game
     * @param instanceId ID of the instance to delete
     * @return Success message
     */
    @DeleteMapping("/{id}/instances/{instanceId}")
    public ResponseEntity<String> deleteGameInstance(
            @PathVariable int id,
            @PathVariable int instanceId) {
        try {
            service.deleteGameInstance(instanceId);
            return ResponseEntity.ok("Game instance deleted successfully");
        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (ForbiddenException | UnauthedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Creates a copy of a game in the user's collection
     * @param gameId ID of the game to copy
     * @param instanceData Additional data for the game instance (condition, location, etc.)
     * @return The created game instance
     */
    @PostMapping("/{gameId}/copy")
    public ResponseEntity<GameInstanceResponseDto> copyGame(
            @PathVariable int gameId,
            @RequestBody(required = false) Map<String, Object> instanceData) {
        
        try {
            // Create instance data map if not provided
            Map<String, Object> data = instanceData != null ? instanceData : new java.util.HashMap<>();
            // Set the game ID in the instance data
            data.put("gameId", gameId);
            
            // Use the existing createGameInstance logic to create a copy
            GameInstanceResponseDto createdInstance = service.createGameInstance(data);
            return new ResponseEntity<>(createdInstance, HttpStatus.CREATED);
        } catch (UnauthedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (ForbiddenException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "An unexpected error occurred while copying the game: " + e.getMessage());
        }
    }

    /**
     * Retrieves all game instances owned by the current authenticated user.
     * This allows users to see their collection regardless of who created the original games.
     *
     * @return List of game instances owned by the current user
     */
    @GetMapping("/instances/my")
    public ResponseEntity<List<GameInstanceResponseDto>> getCurrentUserGameInstances() {
        try {
            List<GameInstanceResponseDto> instances = service.getGameInstancesByCurrentUser();
            return ResponseEntity.ok(instances);
        } catch (UnauthedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "An unexpected error occurred while fetching your game instances: " + e.getMessage());
        }
    }

    // Note: Removed the separate /instances/my endpoint as its functionality is merged into the combined /instances endpoint.
}
