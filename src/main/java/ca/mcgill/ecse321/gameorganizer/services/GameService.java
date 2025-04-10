package ca.mcgill.ecse321.gameorganizer.services;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger; // Added Logger import
import org.slf4j.LoggerFactory; // Added LoggerFactory import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import PreAuthorize

import ca.mcgill.ecse321.gameorganizer.dto.request.GameCreationDto;
import ca.mcgill.ecse321.gameorganizer.dto.request.GameSearchCriteria;
import ca.mcgill.ecse321.gameorganizer.dto.request.ReviewSubmissionDto;
import ca.mcgill.ecse321.gameorganizer.dto.response.GameInstanceResponseDto;
import ca.mcgill.ecse321.gameorganizer.dto.response.GameResponseDto;
import ca.mcgill.ecse321.gameorganizer.dto.response.ReviewResponseDto;
import ca.mcgill.ecse321.gameorganizer.exceptions.ForbiddenException; // Import ForbiddenException
import ca.mcgill.ecse321.gameorganizer.exceptions.ResourceNotFoundException;
import ca.mcgill.ecse321.gameorganizer.exceptions.UnauthedException;
import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.BorrowRequest; // Import added
import ca.mcgill.ecse321.gameorganizer.models.Event;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.GameInstance;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import ca.mcgill.ecse321.gameorganizer.models.LendingRecord; // Import added
import ca.mcgill.ecse321.gameorganizer.models.Review;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.BorrowRequestRepository; // Import added
import ca.mcgill.ecse321.gameorganizer.repositories.EventRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.GameInstanceRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.GameRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.LendingRecordRepository; // Import added
import ca.mcgill.ecse321.gameorganizer.repositories.RegistrationRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.ReviewRepository;


/**
 * Service class that handles business logic for game management operations.
 * Provides methods for creating, retrieving, updating, and deleting games,
 * as well as various search functionalities.
 *
 * @author @PlazmaMamba
 */
@Service
public class GameService {

    private static final Logger logger = LoggerFactory.getLogger(GameService.class); // Added logger

    private GameRepository gameRepository;
    private ReviewRepository reviewRepository;
    private AccountRepository accountRepository;
    private RegistrationRepository  registrationRepository;
    private EventRepository eventRepository;
    private GameInstanceRepository gameInstanceRepository;
    private BorrowRequestRepository borrowRequestRepository;
    private LendingRecordRepository lendingRecordRepository;

    @Autowired
    public GameService(GameRepository gameRepository, ReviewRepository reviewRepository, AccountRepository accountRepository, RegistrationRepository registrationRepository, EventRepository eventRepository, GameInstanceRepository gameInstanceRepository, LendingRecordRepository lendingRecordRepository, BorrowRequestRepository borrowRequestRepository) {

        this.gameRepository = gameRepository;
        this.reviewRepository = reviewRepository;
        this.accountRepository = accountRepository;
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.gameInstanceRepository = gameInstanceRepository;
        this.lendingRecordRepository = lendingRecordRepository;
        this.borrowRequestRepository = borrowRequestRepository;

    }

    /**
     * Submits a new review for a game.
     * Validates the submitted review data, creates a new Review entity,
     * associates it with the specified game and reviewer, and saves it to the database.
     *
     * @param submittedReview DTO containing review information including rating,
     *                        optional comment, game ID, and reviewer email
     * @return ReviewResponseDto containing the saved review information with associated entities
     * @throws IllegalArgumentException if rating is invalid (not 1-5),
     *                                 if the game does not exist,
     *                                 or if the reviewer account does not exist
     */
    @Transactional
    @PreAuthorize("isAuthenticated()") // Ensure user is logged in to submit review
    public ReviewResponseDto submitReview(ReviewSubmissionDto submittedReview) {
        try {
            int rating = submittedReview.getRating();
            if (rating < 1 || rating > 5){
                throw new IllegalArgumentException("Rating must be between 1 and 5");
            }
            String comment = submittedReview.getComment() != null ? submittedReview.getComment() : "";
            int gameId = submittedReview.getGameId();

            // Validate game exists
            Game reviewedGame = gameRepository.findGameById(gameId);
            if (reviewedGame == null){
                throw new IllegalArgumentException("Game with ID " + gameId + " does not exist");
            }

            // Get reviewer from authenticated context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String reviewerEmail = authentication.getName();

            // Validate reviewerId in DTO matches authenticated user
            String dtoReviewerId = submittedReview.getReviewerId();
            if (dtoReviewerId != null && !dtoReviewerId.isEmpty() && !dtoReviewerId.equals(reviewerEmail)) {
                throw new IllegalArgumentException("Reviewer email in request does not match authenticated user");
            }

            Account reviewer = accountRepository.findByEmail(reviewerEmail)
                    .orElseThrow(() -> new UnauthedException("Authenticated reviewer account not found in database."));

            // Verify that the user has borrowed this game before
            List<LendingRecord> userLendingRecords = lendingRecordRepository.findByRequest_Requester(reviewer);
            boolean hasBorrowedGame = userLendingRecords.stream()
                .anyMatch(record -> record.getRequest() != null 
                    && record.getRequest().getRequestedGame() != null 
                    && record.getRequest().getRequestedGame().getId() == gameId
                    && record.getStatus() == LendingRecord.LendingStatus.CLOSED);
                    
            if (!hasBorrowedGame) {
                throw new ForbiddenException("You can only review games that you have borrowed and returned");
            }

            Review review = new Review(rating, comment, new Date());
            review.setReviewer(reviewer);
            review.setGameReviewed(reviewedGame);

            reviewRepository.save(review);
            return new ReviewResponseDto(review);
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (ResourceNotFoundException e) {
            throw new IllegalArgumentException(e.getMessage()); // Convert to IllegalArgumentException
        } catch (org.springframework.security.access.AccessDeniedException e) {
             // Should not happen with isAuthenticated(), but good practice
             throw new ForbiddenException("Authentication required to submit a review.");
        } catch (UnauthedException e) {
            // Handle case where authenticated user somehow isn't in DB
            throw e;
        } catch (Exception e) {
             // Log unexpected errors
             logger.error("Unexpected error submitting review: {}", e.getMessage(), e);
             throw new RuntimeException("An unexpected error occurred while submitting the review.", e);
        }
    }

    /**
     * Creates a new game in the system.
     *
     * @param aNewGame The game object to create
     * @return ResponseEntity with creation confirmation message
     * @throws IllegalArgumentException if game details are invalid
     */

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_GAME_OWNER')") // Ensure only game owners can create games
    public GameResponseDto createGame(GameCreationDto gameDto) {
        // Validate input
        if (gameDto == null || gameDto.getName() == null || gameDto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Game name cannot be empty");
        }

        // Get the owner
        Account ownerAccount = accountRepository.findByEmail(gameDto.getOwnerId())
            .orElseThrow(() -> new IllegalArgumentException("Owner not found"));
        
        if (!(ownerAccount instanceof GameOwner)) {
            throw new IllegalArgumentException("Account must be a GameOwner to create games");
        }
        
        GameOwner owner = (GameOwner) ownerAccount;

        // Create the game
        Game game = new Game(
            gameDto.getName(),
            gameDto.getMinPlayers(),
            gameDto.getMaxPlayers(),
            gameDto.getImage(),
            new Date()
        );
        game.setOwner(owner);
        game.setCategory(gameDto.getCategory());
        game.setDescription(gameDto.getDescription()); // Set description during creation
        game = gameRepository.save(game);

        // Create the initial game instance
        GameInstance gameInstance = new GameInstance(
            game,
            owner,
            gameDto.getCondition(),
            gameDto.getLocation(),
            gameDto.getInstanceName() // Pass the instance name
        );
        gameInstanceRepository.save(gameInstance);

        return new GameResponseDto(game);
    }

    /**
     * Retrieves a game by its ID.
     *
     * @param id The ID of the game to retrieve
     * @return The Game object
     * @throws IllegalArgumentException if no game is found with the given ID
     */
    @Transactional
    public Game getGameById(int id) {
        Game game = gameRepository.findGameById(id);
        if (game == null) {
            throw new IllegalArgumentException("Game with ID " + id + " does not exist");
        }
        return game;
    }

    /**
     * Retrieves games by their exact name.
     *
     * @param name The exact name to search for
     * @return List of games matching the name
     * @throws IllegalArgumentException if name is null or empty
     */
    @Transactional
    public List<Game> getGamesByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        return gameRepository.findByName(name);
    }

    /**
     * Retrieves games containing the given text in their name.
     *
     * @param namePart The text to search for in game names
     * @return List of games with matching name parts
     * @throws IllegalArgumentException if search pattern is null or empty
     */
    @Transactional
    public List<Game> getGamesByNameContaining(String namePart) {
        if (namePart == null || namePart.trim().isEmpty()) {
            throw new IllegalArgumentException("Search pattern cannot be empty");
        }
        return gameRepository.findByNameContaining(namePart);
    }

    /**
     * Finds games that can be played with the specified number of players or fewer.
     *
     * @param players The maximum number of players to search for
     * @return List of games playable with the specified number of players or fewer
     * @throws IllegalArgumentException if player count is less than 1
     */
    @Transactional
    public List<Game> getGamesByMinPlayers(int players) {
        if (players < 1) {
            throw new IllegalArgumentException("Player count must be at least 1");
        }
        return gameRepository.findByMinPlayersLessThanEqual(players);
    }

    /**
     * Finds games that can be played with the specified number of players or more.
     *
     * @param players The minimum number of players to search for
     * @return List of games playable with the specified number of players or more
     * @throws IllegalArgumentException if player count is less than 1
     */
    @Transactional
    public List<Game> getGamesByMaxPlayers(int players) {
        if (players < 1) {
            throw new IllegalArgumentException("Player count must be at least 1");
        }
        return gameRepository.findByMaxPlayersGreaterThanEqual(players);
    }

    /**
     * Finds games that can be played with a number of players within the specified range.
     *
     * @param minPlayers The minimum number of players required
     * @param maxPlayers The maximum number of players allowed
     * @return List of games playable within the specified player range
     * @throws IllegalArgumentException if minPlayers is less than 1 or maxPlayers is less than minPlayers
     */
    @Transactional
    public List<Game> getGamesByPlayerRange(int minPlayers, int maxPlayers) {
        if (minPlayers < 1) {
            throw new IllegalArgumentException("Minimum players must be at least 1");
        }
        if (maxPlayers < minPlayers) {
            throw new IllegalArgumentException("Maximum players must be greater than or equal to minimum players");
        }
        return gameRepository.findByMinPlayersLessThanEqualAndMaxPlayersGreaterThanEqual(minPlayers, maxPlayers);
    }

    @Transactional
    public List<Game> getGamesByDateAddedBefore(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        return gameRepository.findByDateAddedBefore(date);
    }

    @Transactional
    public List<Game> getGamesByDateAddedAfter(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        return gameRepository.findByDateAddedAfter(date);
    }

    @Transactional
    public List<Game> getGamesByDateRange(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates cannot be null");
        }
        if (endDate.before(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        return gameRepository.findByDateAddedBetween(startDate, endDate);
    }

    @Transactional
    public List<Game> getGamesByOwner(GameOwner owner) {
        if (owner == null) {
            throw new IllegalArgumentException("Owner cannot be null");
        }
        return gameRepository.findByOwner(owner);
    }

    @Transactional
    public List<Game> getGamesByOwnerAndName(GameOwner owner, String namePart) {
        if (owner == null) {
            throw new IllegalArgumentException("Owner cannot be null");
        }
        if (namePart == null || namePart.trim().isEmpty()) {
            throw new IllegalArgumentException("Search pattern cannot be empty");
        }
        return gameRepository.findByOwnerAndNameContaining(owner, namePart);
    }

    /**
     * Updates an existing game's information.
     *
     * @param id The ID of the game to update
     * @param updateDto The game dto object containing updated information
     * @return ResponseEntity with update confirmation message
     * @throws IllegalArgumentException if no game is found with the given ID
     */
    @Transactional
    @PreAuthorize("@gameService.hasAccessToGame(#id, authentication.principal.username)")
    public GameResponseDto updateGame(int id, GameCreationDto updateDto) {
       try {
            Game game = gameRepository.findGameById(id);
            if (game == null) {
                throw new ResourceNotFoundException("Game with ID " + id + " does not exist");
            }

            // Authorization handled by @PreAuthorize

        // Validate the update data
        if (updateDto.getName() == null || updateDto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Game name cannot be empty");
        }
        if (updateDto.getMinPlayers() < 1) {
            throw new IllegalArgumentException("Minimum players must be at least 1");
        }
        if (updateDto.getMaxPlayers() < updateDto.getMinPlayers()) {
            throw new IllegalArgumentException("Maximum players must be greater than or equal to minimum players");
        }

        // Update only the fields you want to change
        game.setName(updateDto.getName());
        game.setMinPlayers(updateDto.getMinPlayers());
        game.setMaxPlayers(updateDto.getMaxPlayers());
        game.setImage(updateDto.getImage());
        game.setCategory(updateDto.getCategory());
        game.setDescription(updateDto.getDescription()); // Set description during update

        // Save the updated game
            gameRepository.save(game);

            // Return the updated game as DTO
            return new GameResponseDto(game);

        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            throw e; // Re-throw validation/not found errors
        } catch (org.springframework.security.access.AccessDeniedException e) {
             throw new ForbiddenException("Access denied: You need to either be the creator of this game or have a copy of it in your collection to edit it.");
        } catch (UnauthedException e) {
             // Handle case where authenticated user somehow isn't in DB (shouldn't happen if @PreAuthorize works)
             throw e;
        } catch (Exception e) {
             // Log unexpected errors
             logger.error("Unexpected error updating game {}: {}", id, e.getMessage(), e);
             throw new RuntimeException("An unexpected error occurred while updating the game.", e);
        }
    }

    /**
     * Deletes a game and its associated events and registrations.
     *
     * @param id The ID of the game to delete.
     * @return ResponseEntity indicating success or failure.
     * @throws ResourceNotFoundException If the game with the specified ID does not exist.
     * @throws ForbiddenException If the authenticated user is not the owner of the game.
     * @throws UnauthedException If the user is not authenticated.
     * @throws RuntimeException For any other unexpected errors during deletion.
     */
    @Transactional
    @PreAuthorize("@gameService.hasAccessToGame(#id, authentication.principal.username)")
    public ResponseEntity<String> deleteGame(int id) {
        try {
            Game gameToDelete = gameRepository.findGameById(id);
            if (gameToDelete == null) {
                 throw new ResourceNotFoundException("Game with ID " + id + " does not exist");
            }

            // Authorization handled by @PreAuthorize

            // --- Cascade Delete Logic from origin/dev-Yessine-D3 ---
            logger.info("Deleting game {}. Finding associated events...", id);
            List<Event> events = eventRepository.findEventByFeaturedGameId(id);
            logger.info("Found {} associated events for game {}.", events.size(), id);

            // Step 2: For each event, delete all registrations
            for (Event event : events) {
                logger.info("Deleting registrations for event {}...", event.getId());
                registrationRepository.deleteAllByEventRegisteredForId(event.getId()); // Delete all registrations associated with the event
                logger.info("Deleted registrations for event {}.", event.getId());
            }

            // Step 3: Delete all associated events
            if (!events.isEmpty()) {
                logger.info("Deleting {} associated events for game {}...", events.size(), id);
                eventRepository.deleteAll(events);
                logger.info("Deleted associated events for game {}.", id);
            }
            // --- End Cascade Delete Logic ---

            // Step 4: Delete associated LendingRecords and BorrowRequests
            logger.info("Finding associated borrow requests for game {}...", id);
            List<BorrowRequest> borrowRequestsToDelete = borrowRequestRepository.findByRequestedGame(gameToDelete);
            if (!borrowRequestsToDelete.isEmpty()) {
                logger.info("Found {} associated borrow requests. Checking for linked lending records...", borrowRequestsToDelete.size(), id);
                // Delete associated LendingRecords first
                for (BorrowRequest br : borrowRequestsToDelete) {
                    Optional<LendingRecord> lendingRecordOpt = lendingRecordRepository.findByRequest(br);
                    if (lendingRecordOpt.isPresent()) {
                        logger.info("Deleting lending record {} linked to borrow request {}...", lendingRecordOpt.get().getId(), br.getId());
                        lendingRecordRepository.delete(lendingRecordOpt.get());
                        logger.info("Deleted lending record {}.", lendingRecordOpt.get().getId());
                    }
                }
                // Now delete the BorrowRequests
                logger.info("Deleting {} associated borrow requests for game {}...", borrowRequestsToDelete.size(), id);
                borrowRequestRepository.deleteAll(borrowRequestsToDelete);
                logger.info("Deleted associated borrow requests for game {}.", id);
            } else {
                logger.info("No associated borrow requests found for game {}.", id);
            }

            // Step 5: Delete associated GameInstances
            logger.info("Finding associated game instances for game {}...", id);
            List<GameInstance> instancesToDelete = gameInstanceRepository.findByGame(gameToDelete);
            if (!instancesToDelete.isEmpty()) {
                logger.info("Deleting {} associated game instances for game {}...", instancesToDelete.size(), id);
                gameInstanceRepository.deleteAll(instancesToDelete);
                logger.info("Deleted associated game instances for game {}.", id);
            } else {
                logger.info("No associated game instances found for game {}.", id);
            }

            // Step 6: Delete the game itself
            logger.info("Deleting game {}...", id);
            gameRepository.delete(gameToDelete);
            logger.info("Successfully deleted game {}.", id);
            return ResponseEntity.ok("Game with ID " + id + ", its instances, lending records, borrow requests, and associated events/registrations have been deleted"); // Updated message again

        } catch (ResourceNotFoundException e) {
            throw e; // Re-throw not found error
        } catch (org.springframework.security.access.AccessDeniedException e) {
             throw new ForbiddenException("Access denied: You need to either be the creator of this game or have a copy of it in your collection to delete it.");
        } catch (UnauthedException e) { // Although PreAuthorize should handle this, keep for safety
             throw e;
        } catch (Exception e) {
             // Log unexpected errors during cascade or final delete
             logger.error("Unexpected error deleting game {}: {}", id, e.getMessage(), e);
             throw new RuntimeException("An unexpected error occurred while deleting the game and its associations.", e);
        }
    }

    /**
     * Retrieves all games in the system.
     *
     * @return List of all Game objects
     */
    @Transactional
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    /**
     * Finds games based on their current availability status.
     *
     * @param isAvailable true to find available games, false for unavailable games
     * @return List of games matching the availability criteria
     */
    @Transactional
    public List<Game> getGamesByAvailability(boolean isAvailable) {
        Date currentDate = new Date();
        if (isAvailable) {
            return gameRepository.findAvailableGames(currentDate);
        } else {
            return gameRepository.findUnavailableGames(currentDate);
        }
    }

    /**
     * Finds games with an average rating at or above the specified minimum.
     *
     * @param minRating The minimum average rating to search for (0-5)
     * @return List of games meeting the rating criteria
     * @throws IllegalArgumentException if rating is not between 0 and 5
     */
    @Transactional
    public List<Game> getGamesByRating(double minRating) {
        if (minRating < 0 || minRating > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        }
        return gameRepository.findByAverageRatingGreaterThanEqual(minRating);
    }

    /**
     * Finds games belonging to a specific category.
     *
     * @param category The category to search for
     * @return List of games in the specified category
     * @throws IllegalArgumentException if category is null or empty
     */
    @Transactional
    public List<Game> getGamesByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be empty");
        }
        return gameRepository.findByCategory(category);
    }

    /**
     * Retrieves all instances of a specific game.
     *
     * @param gameId The ID of the game to get instances for
     * @return A list of DTOs representing the game instances
     * @throws ResourceNotFoundException if the game does not exist
     * @throws IllegalArgumentException if the game ID is invalid
     */
    @Transactional
    public List<GameInstanceResponseDto> getInstancesByGameId(int gameId) {
        // Validate game exists
        Game game = gameRepository.findGameById(gameId);
        if (game == null) {
            throw new ResourceNotFoundException("Game with ID " + gameId + " not found");
        }
        
        // Get instances from repository
        List<GameInstance> instances = gameInstanceRepository.findByGame(game);
        
        // Convert to DTOs
        return instances.stream()
            .map(instance -> {
                GameInstanceResponseDto dto = new GameInstanceResponseDto();
                dto.setId(instance.getId());
                dto.setGameId(game.getId());
                dto.setGameName(game.getName());
                dto.setCondition(instance.getCondition());
                dto.setAvailable(instance.isAvailable());
                dto.setLocation(instance.getLocation());
                dto.setAcquiredDate(instance.getAcquiredDate());
                dto.setName(instance.getName());
                
                if (instance.getOwner() != null) {
                    GameInstanceResponseDto.AccountDto ownerDto = 
                        new GameInstanceResponseDto.AccountDto(
                            instance.getOwner().getId(),
                            instance.getOwner().getName(),
                            instance.getOwner().getEmail()
                        );
                    dto.setOwner(ownerDto);
                }
                
                return dto;
            })
            .collect(Collectors.toList());
    }

    /**
     * Creates a new game instance (copy) for a game
     * @param instanceData Map containing instance data (gameId, condition, location)
     * @return GameInstanceResponseDto with created instance data
     * @throws ResourceNotFoundException if game not found
     */
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_GAME_OWNER')")
    public GameInstanceResponseDto createGameInstance(Map<String, Object> instanceData) {
        try {
            // 1. Validate and extract data
            if (instanceData == null) {
                throw new IllegalArgumentException("Instance data cannot be null");
            }
            
            // Parse the game ID
            if (!instanceData.containsKey("gameId")) {
                throw new IllegalArgumentException("Game ID must be provided");
            }
            int gameId;
            try {
                gameId = Integer.valueOf(instanceData.get("gameId").toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid game ID format");
            }
            
            // Get the game entity - this will throw exception if game doesn't exist
            Game game = getGameById(gameId);
            
            // 2. Get the authenticated user as the instance owner
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String ownerEmail = authentication.getName();
            
            Account ownerAccount = accountRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UnauthedException("Authenticated user account not found in database."));
            
            if (!(ownerAccount instanceof GameOwner)) {
                throw new ForbiddenException("Only game owners can create game instances");
            }
            
            GameOwner instanceOwner = (GameOwner) ownerAccount;
            
            // 3. Extract other fields with validations and defaults
            String condition = instanceData.containsKey("condition") && instanceData.get("condition") != null
                ? instanceData.get("condition").toString()
                : "Good"; // Default condition
                
            String location = instanceData.containsKey("location") && instanceData.get("location") != null
                ? instanceData.get("location").toString()
                : "Home"; // Default location
                
            String instanceName = instanceData.containsKey("name") && instanceData.get("name") != null
                ? instanceData.get("name").toString()
                : null; // Optional name for this copy
            
            // 4. Create the game instance
            GameInstance instance = new GameInstance(game, instanceOwner, condition, location, instanceName);
            instance = gameInstanceRepository.save(instance);
            
            // 5. Return response DTO
            return new GameInstanceResponseDto(instance);
            
        } catch (ResourceNotFoundException e) {
            // Rethrow resource not found as-is
            throw e;
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors
            throw e;
        } catch (UnauthedException | ForbiddenException e) {
            // Rethrow auth errors as-is
            throw e;
        } catch (Exception e) {
            // Log unexpected errors
            logger.error("Unexpected error creating game instance: {}", e.getMessage(), e);
            throw new RuntimeException("An unexpected error occurred while creating the game instance.", e);
        }
    }

    /**
     * Retrieves a review by its ID.
     *
     * @param id The ID of the review to retrieve
     * @return ReviewResponseDto containing the review details
     * @throws IllegalArgumentException if no review is found with the given ID
     */
    public ReviewResponseDto getReviewById(int id) { // Removed duplicate @Transactional
        Optional<Review> review = reviewRepository.findReviewById(id);
        if (review.isEmpty()) {
            throw new IllegalArgumentException("Review with ID " + id + " does not exist");
        }
        return new ReviewResponseDto(review.get());
    }

    /**
     * Retrieves all reviews for a specific game by game ID.
     *
     * @param gameId The ID of the game to get reviews for
     * @return List of ReviewResponseDto objects containing review details
     * @throws IllegalArgumentException if no game is found with the given ID
     */
    public List<ReviewResponseDto> getReviewsByGameId(int gameId) { // Removed duplicate @Transactional
        Game game = gameRepository.findGameById(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game with ID " + gameId + " does not exist");
        }

        List<Review> reviews = reviewRepository.findByGameReviewed(game);
        return reviews.stream()
                .map(ReviewResponseDto::new)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Retrieves all reviews for games with a specific name.
     * This allows retrieving reviews across different instances of games with the same name.
     *
     * @param gameName The name of the game(s) to get reviews for
     * @return List of ReviewResponseDto objects containing review details
     * @throws IllegalArgumentException if the game name is null or empty
     */
    @Transactional
    public List<ReviewResponseDto> getReviewsByGameName(String gameName) {
        if (gameName == null || gameName.trim().isEmpty()) {
            throw new IllegalArgumentException("Game name cannot be empty");
        }

        List<Game> games = gameRepository.findByNameContaining(gameName);
        List<Review> reviews = new java.util.ArrayList<>();

        for (Game game : games) {
            reviews.addAll(reviewRepository.findByGameReviewed(game));
        }

        return reviews.stream()
                .map(ReviewResponseDto::new)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Updates an existing review.
     *
     * @param id The ID of the review to update
     * @param reviewDto DTO containing updated review information
     * @return ReviewResponseDto containing the updated review details
     * @throws IllegalArgumentException if no review is found with the given ID,
     *                                 if rating is invalid (not 1-5),
     *                                 or if the reviewer cannot be authenticated
     */
    @Transactional
    public ReviewResponseDto updateReview(int id, ReviewSubmissionDto reviewDto) {
       try {
            // Validate rating must be between 1 and 5.
            if (reviewDto.getRating() < 1 || reviewDto.getRating() > 5) {
                throw new IllegalArgumentException("Rating must be between 1 and 5");
            }

            // First check if review exists
            Review review = reviewRepository.findReviewById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Review with id " + id + " not found"));

            // Now check authorization
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            if (!isReviewer(id, username)) {
                throw new ForbiddenException("Access denied: You can only update your own reviews.");
            }

            // Update review details.
            review.setRating(reviewDto.getRating());
            review.setComment(reviewDto.getComment());
            // (Add any other field updates as necessary)
            reviewRepository.save(review);
            return new ReviewResponseDto(review);

        } catch (IllegalArgumentException | ResourceNotFoundException | ForbiddenException | UnauthedException e) {
            throw e; // Re-throw these directly
        } catch (Exception e) {
             // Log unexpected errors
             logger.error("Unexpected error updating review {}: {}", id, e.getMessage(), e);
             throw new RuntimeException("An unexpected error occurred while updating the review.", e);
        }
    }

    /**
     * Deletes a review by ID.
     *
     * @param id The ID of the review to delete
     * @return ResponseEntity with deletion confirmation message
     * @throws IllegalArgumentException if no review is found with the given ID
     */
    @Transactional
    public ResponseEntity<String> deleteReview(int id) {
        try {
            // First check if review exists
            Review review = reviewRepository.findReviewById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Review with id " + id + " not found"));

            // Now check authorization
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            if (!isReviewer(id, username)) {
                throw new ForbiddenException("Access denied: You can only delete your own reviews.");
            }

            reviewRepository.delete(review);
            return ResponseEntity.ok("Review deleted successfully");

        } catch (ResourceNotFoundException | ForbiddenException | UnauthedException e) {
            throw e; // Re-throw these directly
        } catch (Exception e) {
             // Log unexpected errors
             logger.error("Unexpected error deleting review {}: {}", id, e.getMessage(), e);
             throw new RuntimeException("An unexpected error occurred while deleting the review.", e);
        }
    }

    /**
     * Advanced search for games based on multiple criteria
     */
    @Transactional
    public List<Game> searchGames(GameSearchCriteria criteria) {
        List<Game> games = getAllGames();

        // Apply filters based on criteria
        if (criteria.getName() != null && !criteria.getName().trim().isEmpty()) {
            games = games.stream()
                    .filter(game -> game.getName().toLowerCase().contains(criteria.getName().toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (criteria.getMinPlayers() != null) {
            games = games.stream()
                    .filter(game -> game.getMinPlayers() >= criteria.getMinPlayers())
                    .collect(Collectors.toList());
        }

        if (criteria.getMaxPlayers() != null) {
            games = games.stream()
                    .filter(game -> game.getMaxPlayers() <= criteria.getMaxPlayers())
                    .collect(Collectors.toList());
        }

        if (criteria.getCategory() != null && !criteria.getCategory().trim().isEmpty()) {
            games = games.stream()
                    .filter(game -> game.getCategory().equalsIgnoreCase(criteria.getCategory()))
                    .collect(Collectors.toList());
        }

        if (criteria.getMinRating() != null) {
            games = games.stream()
                    .filter(game -> getAverageRatingForGame(game.getId()) >= criteria.getMinRating())
                    .collect(Collectors.toList());
        }

        if (criteria.getAvailable() != null) {
            games = games.stream()
                    .filter(game -> isGameAvailable(game.getId()) == criteria.getAvailable())
                    .collect(Collectors.toList());
        }

        if (criteria.getOwnerId() != null && !criteria.getOwnerId().trim().isEmpty()) {
            Account owner = accountRepository.findByEmail(criteria.getOwnerId())
                    .orElseThrow(() -> new IllegalArgumentException("Owner not found"));
            if (owner instanceof GameOwner) {
                games = games.stream()
                        .filter(game -> game.getOwner() != null && game.getOwner().getId() == owner.getId())
                        .collect(Collectors.toList());
            }
        }

        // Apply sorting if specified
        if (criteria.getSort() != null && !criteria.getSort().trim().isEmpty()) {
            boolean ascending = "asc".equalsIgnoreCase(criteria.getOrder());
            games = games.stream()
                    .sorted((g1, g2) -> {
                int comparison = 0;
                switch (criteria.getSort().toLowerCase()) {
                    case "name":
                        comparison = g1.getName().compareTo(g2.getName());
                        break;
                    case "rating":
                        comparison = Double.compare(getAverageRatingForGame(g1.getId()), getAverageRatingForGame(g2.getId()));
                        break;
                    case "date":
                        comparison = g1.getDateAdded().compareTo(g2.getDateAdded());
                        break;
                    default:
                        return 0;
                }
                return ascending ? comparison : -comparison;
                    })
                    .collect(Collectors.toList());
        }

        return games;
    }

    /**
     * Checks if the given username corresponds to the owner of the game.
     */
    public boolean isOwnerOfGame(int gameId, String username) {
        if (username == null) return false;
        try {
            Game game = gameRepository.findGameById(gameId);
            if (game == null) {
                throw new ResourceNotFoundException("Game with ID " + gameId + " does not exist");
            }

            Account user = accountRepository.findByEmail(username).orElse(null);
            if (user == null) {
                return false; // Cannot determine ownership
            }

            if (game.getOwner() == null) {
                return false; // Game has no owner
            }

            return game.getOwner().getId() == user.getId();
        } catch (ResourceNotFoundException e) {
            // Re-throw resource not found exception
            throw e;
        } catch (Exception e) {
            // Log error
            logger.error("Error during isOwnerOfGame check for game {}: {}", gameId, e.getMessage(), e);
            return false; // Deny on error
        }
    }

    /**
     * Checks if the given user has access to edit a game.
     * A user has access if they either:
     * 1. Are the original creator/owner of the game, OR
     * 2. Have at least one instance (copy) of the game in their collection
     * 
     * @param gameId ID of the game to check
     * @param username Email of the user to check
     * @return true if the user has edit access, false otherwise
     */
    public boolean hasAccessToGame(int gameId, String username) {
        if (username == null) return false;
        try {
            // First check if they're the owner (faster check)
            if (isOwnerOfGame(gameId, username)) {
                return true;
            }
            
            // If not the owner, check if they have an instance of this game
            Game game = gameRepository.findGameById(gameId);
            if (game == null) {
                throw new ResourceNotFoundException("Game with ID " + gameId + " does not exist");
            }
            
            Account user = accountRepository.findByEmail(username).orElse(null);
            if (user == null || !(user instanceof GameOwner)) {
                return false; // User doesn't exist or isn't a game owner
            }
            
            GameOwner gameOwner = (GameOwner) user;
            
            // Check if the user has any instances of this game
            List<GameInstance> userInstances = gameInstanceRepository.findByGameAndOwner(game, gameOwner);
            return !userInstances.isEmpty(); // Has access if they have at least one instance
            
        } catch (ResourceNotFoundException e) {
            // Re-throw resource not found exception
            throw e;
        } catch (Exception e) {
            // Log error
            logger.error("Error during hasAccessToGame check for game {}: {}", gameId, e.getMessage(), e);
            return false; // Deny on error
        }
    }

    /**
     * Checks if the given username corresponds to the reviewer of the review.
     */
    public boolean isReviewer(int reviewId, String username) {
        if (username == null) return false;
        try {
            Review review = reviewRepository.findReviewById(reviewId).orElse(null);
            if (review == null) {
                throw new ResourceNotFoundException("Review with ID " + reviewId + " does not exist");
            }

            Account user = accountRepository.findByEmail(username).orElse(null);
            if (user == null) {
                return false; // Cannot determine reviewer
            }

            if (review.getReviewer() == null) {
                return false; // Review has no reviewer
            }

            return review.getReviewer().getId() == user.getId();
        } catch (ResourceNotFoundException e) {
            // Re-throw resource not found exception
            throw e;
        } catch (Exception e) {
            // Log error
            logger.error("Error during isReviewer check for review {}: {}", reviewId, e.getMessage(), e);
            return false; // Deny on error
        }
    }

    /**
     * Get the average rating for a game
     */
    @Transactional
    public double getAverageRatingForGame(int gameId) {
        Game game = getGameById(gameId);
        List<Review> reviews = reviewRepository.findByGameReviewed(game);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }

    /**
     * Check if a game is currently available
     */
    private boolean isGameAvailable(int gameId) {
        Game game = getGameById(gameId);
        Date currentDate = new Date();
        return gameRepository.findAvailableGames(currentDate).contains(game);
    }

    /**
     * Check if a game is available for a specific date range
     * @param gameId The ID of the game to check
     * @param startDate The start date of the borrowing period
     * @param endDate The end date of the borrowing period
     * @return true if the game is available for the specified period, false otherwise
     */
    @Transactional
    public boolean isGameAvailableForPeriod(int gameId, Date startDate, Date endDate) {
        // Get the game
        Game game = getGameById(gameId);
        
        // Find any approved requests that overlap with the requested period
        List<BorrowRequest> overlappingRequests = borrowRequestRepository
            .findOverlappingApprovedRequests(gameId, startDate, endDate);
        
        // No overlapping approved requests means the game is available for this period
        return overlappingRequests.isEmpty();
    }

    /**
     * Updates a game instance with the provided data
     * @param instanceId The ID of the instance to update
     * @param instanceData Map containing the update data (condition, location, available)
     * @return GameInstanceResponseDto with updated instance data
     * @throws ResourceNotFoundException if instance not found
     * @throws ForbiddenException if user is not the owner
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public GameInstanceResponseDto updateGameInstance(int instanceId, Map<String, Object> instanceData) {
        // Find the instance
        GameInstance instance = gameInstanceRepository.findById(instanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Game instance with ID " + instanceId + " not found"));
        
        // Check authorization - only owner can update
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        if (instance.getOwner() == null || !instance.getOwner().getEmail().equals(username)) {
            throw new ForbiddenException("You can only update your own game instances");
        }
        
        // Update instance fields
        if (instanceData.containsKey("condition") && instanceData.get("condition") != null) {
            instance.setCondition((String) instanceData.get("condition"));
        }
        
        if (instanceData.containsKey("location") && instanceData.get("location") != null) {
            instance.setLocation((String) instanceData.get("location"));
        }
        
        if (instanceData.containsKey("name") && instanceData.get("name") != null) {
            instance.setName((String) instanceData.get("name"));
        }
        
        if (instanceData.containsKey("available") && instanceData.get("available") != null) {
            instance.setAvailable((Boolean) instanceData.get("available"));
        }
        
        // Save updated instance
        gameInstanceRepository.save(instance);
        
        // Create and return DTO
        GameInstanceResponseDto dto = new GameInstanceResponseDto();
        dto.setId(instance.getId());
        dto.setGameId(instance.getGame().getId());
        dto.setGameName(instance.getGame().getName());
        dto.setCondition(instance.getCondition());
        dto.setAvailable(instance.isAvailable());
        dto.setLocation(instance.getLocation());
        dto.setAcquiredDate(instance.getAcquiredDate());
        dto.setName(instance.getName());
        
        if (instance.getOwner() != null) {
            GameInstanceResponseDto.AccountDto ownerDto = 
                new GameInstanceResponseDto.AccountDto(
                    instance.getOwner().getId(),
                    instance.getOwner().getName(),
                    instance.getOwner().getEmail()
                );
            dto.setOwner(ownerDto);
        }
        
        return dto;
    }

    /**
     * Deletes a specific game instance
     * 
     * @param instanceId ID of the instance to delete
     * @throws ResourceNotFoundException if the instance doesn't exist
     * @throws ForbiddenException if the user is not the owner of the instance
     * @throws UnauthedException if the user is not authenticated
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void deleteGameInstance(int instanceId) {
        // Find the instance
        GameInstance instance = gameInstanceRepository.findById(instanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Game instance with ID " + instanceId + " not found"));
        
        // Check authorization - only owner can delete
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        if (instance.getOwner() == null || !instance.getOwner().getEmail().equals(username)) {
            throw new ForbiddenException("You can only delete your own game instances");
        }
        
        // Delete the instance
        gameInstanceRepository.delete(instance);
        logger.info("Successfully deleted game instance {}.", instanceId);
    }

    /**
     * Retrieves all game instances owned by the currently authenticated user.
     * This is useful for showing a user's collection regardless of who created the original game.
     *
     * @return List of game instance DTOs owned by the current user
     * @throws UnauthedException if no authenticated user is found
     */
    @Transactional
    public List<GameInstanceResponseDto> getGameInstancesByCurrentUser() {
        // Get the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        
        Account account = accountRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UnauthedException("Authenticated user account not found"));
        
        if (!(account instanceof GameOwner)) {
            return Collections.emptyList(); // Return empty list for non-owner accounts
        }
        
        GameOwner owner = (GameOwner) account;
        
        // Get all instances owned by this user
        List<GameInstance> instances = gameInstanceRepository.findByOwner(owner);
        
        // Convert to DTOs
        return instances.stream()
                .map(GameInstanceResponseDto::new)
                 .collect(Collectors.toList());
    }

    /**
     * Retrieves all game instances owned by a specific GameOwner.
     *
     * @param owner The GameOwner whose instances are to be retrieved.
     * @return List of game instance DTOs owned by the specified user.
     * @throws IllegalArgumentException if the owner is null.
     */
    @Transactional
    public List<GameInstanceResponseDto> getGameInstancesByOwner(GameOwner owner) {
        if (owner == null) {
            throw new IllegalArgumentException("Owner cannot be null when fetching game instances.");
        }
        
        // Get all instances owned by this user
        List<GameInstance> instances = gameInstanceRepository.findByOwner(owner);
        
        // Convert to DTOs
        return instances.stream()
                .map(GameInstanceResponseDto::new)
                .collect(Collectors.toList());
    }

}
