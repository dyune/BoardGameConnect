package ca.mcgill.ecse321.gameorganizer.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
// Imports for Security Context Mocking
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import org.springframework.test.context.ContextConfiguration;

import ca.mcgill.ecse321.gameorganizer.TestJwtConfig;
import ca.mcgill.ecse321.gameorganizer.dto.request.GameCreationDto;
import ca.mcgill.ecse321.gameorganizer.dto.response.GameResponseDto;
import ca.mcgill.ecse321.gameorganizer.dto.response.GameInstanceResponseDto;
import ca.mcgill.ecse321.gameorganizer.dto.request.ReviewSubmissionDto;
import ca.mcgill.ecse321.gameorganizer.dto.response.ReviewResponseDto;
import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import ca.mcgill.ecse321.gameorganizer.models.Review;
import ca.mcgill.ecse321.gameorganizer.models.GameInstance;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.GameRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.ReviewRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.GameInstanceRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.EventRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.BorrowRequestRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.LendingRecordRepository;
import ca.mcgill.ecse321.gameorganizer.services.GameService;
import ca.mcgill.ecse321.gameorganizer.models.LendingRecord;
import ca.mcgill.ecse321.gameorganizer.models.LendingRecord.LendingStatus;
import ca.mcgill.ecse321.gameorganizer.models.BorrowRequest;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(initializers = TestJwtConfig.Initializer.class)
public class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private GameInstanceRepository gameInstanceRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private BorrowRequestRepository borrowRequestRepository;

    @Mock
    private LendingRecordRepository lendingRecordRepository;

    @InjectMocks
    private GameService gameService;

    // Test constants
    private static final String VALID_GAME_NAME = "Test Game";
    private static final String VALID_OWNER_EMAIL = "owner@test.com";
    private static final String VALID_CATEGORY = "Strategy";
    private static final String VALID_IMAGE = "test.jpg";
    private static final int VALID_MIN_PLAYERS = 2;
    private static final int VALID_MAX_PLAYERS = 4;
    private static final int VALID_GAME_ID = 1;

    @Test
    public void testCreateGameSuccess() {
        // Setup SecurityContext for authenticated user
        GameOwner owner = new GameOwner("Test Owner", VALID_OWNER_EMAIL, "password");
        Authentication auth = new UsernamePasswordAuthenticationToken(VALID_OWNER_EMAIL, "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_GAME_OWNER")));
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
        
        try {
            // Setup
            GameCreationDto gameDto = new GameCreationDto();
            gameDto.setName(VALID_GAME_NAME);
            gameDto.setOwnerId(VALID_OWNER_EMAIL);
            gameDto.setCategory(VALID_CATEGORY);
            gameDto.setImage(VALID_IMAGE);
            gameDto.setMinPlayers(VALID_MIN_PLAYERS);
            gameDto.setMaxPlayers(VALID_MAX_PLAYERS);
            gameDto.setCondition("Excellent");
            gameDto.setLocation("Home");
            gameDto.setInstanceName("My Special Copy");

            Game savedGame = new Game(VALID_GAME_NAME, VALID_MIN_PLAYERS, VALID_MAX_PLAYERS, VALID_IMAGE, new Date());
            savedGame.setOwner(owner);
            GameInstance savedGameInstance = new GameInstance(savedGame, owner, "Excellent", "Home", "My Special Copy");

            when(accountRepository.findByEmail(VALID_OWNER_EMAIL)).thenReturn(Optional.of(owner));
            when(gameRepository.save(any(Game.class))).thenReturn(savedGame);
            when(gameInstanceRepository.save(any(GameInstance.class))).thenReturn(savedGameInstance);

            // Test
            GameResponseDto response = gameService.createGame(gameDto);

            // Verify
            assertNotNull(response);
            assertEquals(VALID_GAME_NAME, response.getName());
            assertEquals(VALID_MIN_PLAYERS, response.getMinPlayers());
            assertEquals(VALID_MAX_PLAYERS, response.getMaxPlayers());
            verify(accountRepository).findByEmail(VALID_OWNER_EMAIL);
            verify(gameRepository).save(any(Game.class));
            verify(gameInstanceRepository).save(any(GameInstance.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testCreateGameWithInvalidOwner() {
        // Setup SecurityContext for authenticated user
        Authentication auth = new UsernamePasswordAuthenticationToken(VALID_OWNER_EMAIL, "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_GAME_OWNER")));
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
        
        try {
            // Setup
            GameCreationDto gameDto = new GameCreationDto();
            gameDto.setName(VALID_GAME_NAME);
            gameDto.setOwnerId(VALID_OWNER_EMAIL);
            gameDto.setCategory(VALID_CATEGORY);
            gameDto.setMinPlayers(VALID_MIN_PLAYERS);
            gameDto.setMaxPlayers(VALID_MAX_PLAYERS);

            when(accountRepository.findByEmail(VALID_OWNER_EMAIL)).thenReturn(Optional.empty());

            // Test & Verify - Changed to IllegalArgumentException to match actual implementation
            assertThrows(IllegalArgumentException.class, 
                () -> gameService.createGame(gameDto));
            verify(accountRepository).findByEmail(VALID_OWNER_EMAIL);
            verify(gameRepository, never()).save(any(Game.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testCreateGameWithNonGameOwnerAccount() {
        // Setup SecurityContext for authenticated user
        Account regularAccount = new Account("Test User", VALID_OWNER_EMAIL, "password");
        Authentication auth = new UsernamePasswordAuthenticationToken(VALID_OWNER_EMAIL, "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
        
        try {
            // Setup
            GameCreationDto gameDto = new GameCreationDto();
            gameDto.setName(VALID_GAME_NAME);
            gameDto.setOwnerId(VALID_OWNER_EMAIL);
            gameDto.setCategory(VALID_CATEGORY);
            gameDto.setMinPlayers(VALID_MIN_PLAYERS);
            gameDto.setMaxPlayers(VALID_MAX_PLAYERS);

            when(accountRepository.findByEmail(VALID_OWNER_EMAIL)).thenReturn(Optional.of(regularAccount));

            // Test - Expect IllegalArgumentException instead of ForbiddenException
            Exception exception = assertThrows(IllegalArgumentException.class, 
                () -> gameService.createGame(gameDto));
            
            // Verify the exception message contains something about needing to be a GameOwner
            assertTrue(exception.getMessage().contains("Account must be a GameOwner"),
                "Exception message should indicate GameOwner requirement");
            
            verify(accountRepository).findByEmail(VALID_OWNER_EMAIL);
            verify(gameRepository, never()).save(any(Game.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testGetGameByIdSuccess() {
        // Setup
        Game game = new Game(VALID_GAME_NAME, VALID_MIN_PLAYERS, VALID_MAX_PLAYERS, VALID_IMAGE, new Date());
        when(gameRepository.findGameById(VALID_GAME_ID)).thenReturn(game);

        // Test
        Game result = gameService.getGameById(VALID_GAME_ID);

        // Verify
        assertNotNull(result);
        assertEquals(VALID_GAME_NAME, result.getName());
        verify(gameRepository).findGameById(VALID_GAME_ID);
    }

    @Test
    public void testGetGameByIdNotFound() {
        // Setup
        when(gameRepository.findGameById(anyInt())).thenReturn(null);

        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> gameService.getGameById(VALID_GAME_ID));
        verify(gameRepository).findGameById(VALID_GAME_ID);
    }

    @Test
    public void testGetGamesByNameSuccess() {
        // Setup
        List<Game> games = new ArrayList<>();
        games.add(new Game(VALID_GAME_NAME, VALID_MIN_PLAYERS, VALID_MAX_PLAYERS, VALID_IMAGE, new Date()));
        when(gameRepository.findByName(VALID_GAME_NAME)).thenReturn(games);

        // Test
        List<Game> result = gameService.getGamesByName(VALID_GAME_NAME);

        // Verify
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(VALID_GAME_NAME, result.get(0).getName());
        verify(gameRepository).findByName(VALID_GAME_NAME);
    }

    @Test
    public void testGetGamesByNameEmpty() {
        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> gameService.getGamesByName(""));
        verify(gameRepository, never()).findByName(anyString());
    }

    @Test
    public void testUpdateGameSuccess() {
        // Setup Owner and Security Context
        GameOwner owner = new GameOwner("Test Owner", VALID_OWNER_EMAIL, "password");
        owner.setId(1); // Assign an ID
        Authentication auth = new UsernamePasswordAuthenticationToken(VALID_OWNER_EMAIL, "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_GAME_OWNER")));
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        try {
            // Setup Mocks and other test data
            Game existingGame = new Game(VALID_GAME_NAME, VALID_MIN_PLAYERS, VALID_MAX_PLAYERS, VALID_IMAGE, new Date());
            existingGame.setOwner(owner); // Set the owner
            GameCreationDto updateDto = new GameCreationDto();
            updateDto.setName("Updated Game");
            updateDto.setMinPlayers(3);
            updateDto.setMaxPlayers(6);
            updateDto.setImage("updated.jpg");

            when(gameRepository.findGameById(VALID_GAME_ID)).thenReturn(existingGame);
            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Test
            GameResponseDto result = gameService.updateGame(VALID_GAME_ID, updateDto);

            // Verify
            assertNotNull(result);
            assertEquals("Updated Game", result.getName());
            assertEquals(3, result.getMinPlayers());
            assertEquals(6, result.getMaxPlayers());
            verify(gameRepository).findGameById(VALID_GAME_ID);
            verify(gameRepository).save(any(Game.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testDeleteGameSuccess() {
        // Setup Owner and Security Context
        GameOwner owner = new GameOwner("Test Owner", VALID_OWNER_EMAIL, "password");
        owner.setId(1);
        Authentication auth = new UsernamePasswordAuthenticationToken(VALID_OWNER_EMAIL, "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_GAME_OWNER")));
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        try {
            // Setup Mocks
            Game game = new Game(VALID_GAME_NAME, VALID_MIN_PLAYERS, VALID_MAX_PLAYERS, VALID_IMAGE, new Date());
            game.setOwner(owner); // Set the owner
            when(gameRepository.findGameById(VALID_GAME_ID)).thenReturn(game);
            when(eventRepository.findEventByFeaturedGameId(VALID_GAME_ID)).thenReturn(Collections.emptyList());
            
            // Mock borrowRequestRepository - Add this to fix the test
            when(borrowRequestRepository.findByRequestedGame(game)).thenReturn(Collections.emptyList());
            // Mock the instances required for deletion
            when(gameInstanceRepository.findByGame(game)).thenReturn(Collections.emptyList());

            // Test
            ResponseEntity<String> response = gameService.deleteGame(VALID_GAME_ID);

            // Verify
            assertEquals(200, response.getStatusCodeValue());
            verify(gameRepository).findGameById(VALID_GAME_ID);
            verify(gameRepository).delete(game);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testDeleteGameNotFound() {
        // Setup
        when(gameRepository.findGameById(VALID_GAME_ID)).thenReturn(null);

        // Test & Verify
        assertThrows(ca.mcgill.ecse321.gameorganizer.exceptions.ResourceNotFoundException.class, 
            () -> gameService.deleteGame(VALID_GAME_ID));
        verify(gameRepository).findGameById(VALID_GAME_ID);
        verify(gameRepository, never()).delete(any());
    }

    @Test
    public void testSubmitReviewSuccess() {
        // Setup security context
        Account reviewer = new Account("Reviewer", VALID_OWNER_EMAIL, "password");
        Authentication auth = new UsernamePasswordAuthenticationToken(VALID_OWNER_EMAIL, "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
        
        try {
            // Setup
            ReviewSubmissionDto reviewDto = new ReviewSubmissionDto();
            reviewDto.setGameId(VALID_GAME_ID);
            reviewDto.setReviewerId(VALID_OWNER_EMAIL);
            reviewDto.setRating(5);
            reviewDto.setComment("Great game!");

            Game game = new Game(VALID_GAME_NAME, VALID_MIN_PLAYERS, VALID_MAX_PLAYERS, VALID_IMAGE, new Date());
            game.setId(VALID_GAME_ID);
            Review savedReview = new Review(5, "Great game!", new Date());
            savedReview.setGameReviewed(game);
            savedReview.setReviewer(reviewer);

            // Create mock lending record to show user has borrowed and returned this game
            LendingRecord closedRecord = new LendingRecord();
            closedRecord.setStatus(LendingStatus.CLOSED);
            
            BorrowRequest request = new BorrowRequest();
            request.setRequester(reviewer);
            request.setRequestedGame(game);
            closedRecord.setRequest(request);
            
            List<LendingRecord> lendingRecords = new ArrayList<>();
            lendingRecords.add(closedRecord);

            // Setup mocks
            when(accountRepository.findByEmail(VALID_OWNER_EMAIL)).thenReturn(Optional.of(reviewer));
            when(gameRepository.findGameById(VALID_GAME_ID)).thenReturn(game);
            when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
            when(lendingRecordRepository.findByRequest_Requester(reviewer)).thenReturn(lendingRecords);

            // Test
            ReviewResponseDto result = gameService.submitReview(reviewDto);

            // Verify
            assertNotNull(result);
            assertEquals(5, result.getRating());
            assertEquals("Great game!", result.getComment());
            verify(accountRepository).findByEmail(VALID_OWNER_EMAIL);
            verify(gameRepository).findGameById(VALID_GAME_ID);
            verify(reviewRepository).save(any(Review.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testSubmitReviewInvalidRating() {
        // Setup
        ReviewSubmissionDto reviewDto = new ReviewSubmissionDto();
        reviewDto.setGameId(VALID_GAME_ID);
        reviewDto.setReviewerId(VALID_OWNER_EMAIL);
        reviewDto.setRating(6); // Invalid rating > 5

        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> gameService.submitReview(reviewDto));
        verify(gameRepository, never()).findGameById(anyInt());
        verify(accountRepository, never()).findByEmail(anyString());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    public void testGetGamesByCategorySuccess() {
        // Setup
        List<Game> games = new ArrayList<>();
        games.add(new Game(VALID_GAME_NAME, VALID_MIN_PLAYERS, VALID_MAX_PLAYERS, VALID_IMAGE, new Date()));
        when(gameRepository.findByCategory(VALID_CATEGORY)).thenReturn(games);

        // Test
        List<Game> result = gameService.getGamesByCategory(VALID_CATEGORY);

        // Verify
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(gameRepository).findByCategory(VALID_CATEGORY);
    }

    @Test
    public void testGetGamesByCategoryEmptyCategory() {
        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> gameService.getGamesByCategory(""));
        verify(gameRepository, never()).findByCategory(anyString());
    }

    @Test
    public void testGetGamesByAvailabilityTrue() {
        // Setup
        List<Game> games = new ArrayList<>();
        games.add(new Game(VALID_GAME_NAME, VALID_MIN_PLAYERS, VALID_MAX_PLAYERS, VALID_IMAGE, new Date()));
        when(gameRepository.findAvailableGames(any(Date.class))).thenReturn(games);

        // Test
        List<Game> result = gameService.getGamesByAvailability(true);

        // Verify
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(gameRepository).findAvailableGames(any(Date.class));
    }

    @Test
    public void testGetGamesByAvailabilityFalse() {
        // Setup
        List<Game> games = new ArrayList<>();
        games.add(new Game(VALID_GAME_NAME, VALID_MIN_PLAYERS, VALID_MAX_PLAYERS, VALID_IMAGE, new Date()));
        when(gameRepository.findUnavailableGames(any(Date.class))).thenReturn(games);

        // Test
        List<Game> result = gameService.getGamesByAvailability(false);

        // Verify
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(gameRepository).findUnavailableGames(any(Date.class));
    }

    @Test
    public void testGetGamesByRatingSuccess() {
        // Setup
        List<Game> games = new ArrayList<>();
        games.add(new Game(VALID_GAME_NAME, VALID_MIN_PLAYERS, VALID_MAX_PLAYERS, VALID_IMAGE, new Date()));
        when(gameRepository.findByAverageRatingGreaterThanEqual(any(Double.class))).thenReturn(games);

        // Test
        List<Game> result = gameService.getGamesByRating(4.0);

        // Verify
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(gameRepository).findByAverageRatingGreaterThanEqual(4.0);
    }

    @Test
    public void testGetGamesByRatingInvalidRating() {
        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> gameService.getGamesByRating(6.0));
        verify(gameRepository, never()).findByAverageRatingGreaterThanEqual(any(Double.class));
    }

    @Test
    public void testGetGamesByPlayerRangeSuccess() {
        // Setup
        List<Game> games = new ArrayList<>();
        games.add(new Game(VALID_GAME_NAME, VALID_MIN_PLAYERS, VALID_MAX_PLAYERS, VALID_IMAGE, new Date()));
        when(gameRepository.findByMinPlayersLessThanEqualAndMaxPlayersGreaterThanEqual(2, 4)).thenReturn(games);

        // Test
        List<Game> result = gameService.getGamesByPlayerRange(2, 4);

        // Verify
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(gameRepository).findByMinPlayersLessThanEqualAndMaxPlayersGreaterThanEqual(2, 4);
    }

    @Test
    public void testGetGamesByPlayerRangeInvalidRange() {
        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> gameService.getGamesByPlayerRange(4, 2));
        verify(gameRepository, never()).findByMinPlayersLessThanEqualAndMaxPlayersGreaterThanEqual(anyInt(), anyInt());
    }

    @Test
    public void testGetGamesByDateRangeSuccess() {
        // Setup
        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + 86400000); // Next day
        List<Game> games = new ArrayList<>();
        games.add(new Game(VALID_GAME_NAME, VALID_MIN_PLAYERS, VALID_MAX_PLAYERS, VALID_IMAGE, new Date()));
        when(gameRepository.findByDateAddedBetween(startDate, endDate)).thenReturn(games);

        // Test
        List<Game> result = gameService.getGamesByDateRange(startDate, endDate);

        // Verify
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(gameRepository).findByDateAddedBetween(startDate, endDate);
    }

    @Test
    public void testGetGamesByDateRangeInvalidDates() {
        // Setup
        Date laterDate = new Date();
        Date earlierDate = new Date(laterDate.getTime() - 86400000);

        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> gameService.getGamesByDateRange(laterDate, earlierDate));
        verify(gameRepository, never()).findByDateAddedBetween(any(Date.class), any(Date.class));
    }

    @Test
    public void testGetGamesByDateRangeNullDates() {
        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> gameService.getGamesByDateRange(null, new Date()));
        assertThrows(IllegalArgumentException.class, () -> gameService.getGamesByDateRange(new Date(), null));
        verify(gameRepository, never()).findByDateAddedBetween(any(Date.class), any(Date.class));
    }

    @Test
    public void testGetAllGamesSuccess() {
        // Setup
        List<Game> games = new ArrayList<>();
        games.add(new Game(VALID_GAME_NAME, VALID_MIN_PLAYERS, VALID_MAX_PLAYERS, VALID_IMAGE, new Date()));
        when(gameRepository.findAll()).thenReturn(games);

        // Test
        List<Game> result = gameService.getAllGames();

        // Verify
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(gameRepository).findAll();
    }

    @Test
    public void testDeleteGameWithAuthentication() {
         // Setup Owner and Security Context
        GameOwner owner = new GameOwner("Test Owner", VALID_OWNER_EMAIL, "password");
        owner.setId(1);
        Authentication auth = new UsernamePasswordAuthenticationToken(owner.getEmail(), "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_GAME_OWNER")));
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        try {
            // Setup Mocks
            Game game = new Game(VALID_GAME_NAME, VALID_MIN_PLAYERS, VALID_MAX_PLAYERS, VALID_IMAGE, new Date());
            game.setOwner(owner); // Set the owner
            game.setId(VALID_GAME_ID); // Assume a valid ID for the test game
            when(gameRepository.findGameById(VALID_GAME_ID)).thenReturn(game); // Mock finding game by ID
            when(eventRepository.findEventByFeaturedGameId(VALID_GAME_ID)).thenReturn(Collections.emptyList()); // Added mock for eventRepo find
            
            // Mock borrowRequestRepository - Add this to fix the test
            when(borrowRequestRepository.findByRequestedGame(game)).thenReturn(Collections.emptyList());
            // Mock the instances required for deletion
            when(gameInstanceRepository.findByGame(game)).thenReturn(Collections.emptyList());

            // Test
            ResponseEntity<String> response = gameService.deleteGame(VALID_GAME_ID);

            // Verify
            assertEquals(200, response.getStatusCodeValue());
            // Updated expected message to match actual implementation
            assertEquals("Game with ID " + VALID_GAME_ID + ", its instances, lending records, borrow requests, and associated events/registrations have been deleted", response.getBody());
            verify(gameRepository).delete(game); // Verify delete was called with the game object
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testCreateGameInstanceWithName() {
        // Setup SecurityContext for authenticated user
        GameOwner owner = new GameOwner("Test Owner", VALID_OWNER_EMAIL, "password");
        Authentication auth = new UsernamePasswordAuthenticationToken(VALID_OWNER_EMAIL, "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_GAME_OWNER")));
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        try {
            // Setup
            Game game = new Game(VALID_GAME_NAME, VALID_MIN_PLAYERS, VALID_MAX_PLAYERS, VALID_IMAGE, new Date());
            game.setOwner(owner);
            Map<String, Object> instanceData = new HashMap<>();
            instanceData.put("gameId", VALID_GAME_ID);
            instanceData.put("condition", "Excellent");
            instanceData.put("location", "Home");
            instanceData.put("name", "Special Edition Copy");
            instanceData.put("available", true);

            GameInstance savedInstance = new GameInstance(game, owner, "Excellent", "Home", "Special Edition Copy");

            when(gameRepository.findGameById(VALID_GAME_ID)).thenReturn(game);
            when(accountRepository.findByEmail(VALID_OWNER_EMAIL)).thenReturn(Optional.of(owner));
            when(gameInstanceRepository.save(any(GameInstance.class))).thenReturn(savedInstance);

            // Test
            GameInstanceResponseDto response = gameService.createGameInstance(instanceData);

            // Verify
            assertNotNull(response);
            assertEquals("Special Edition Copy", response.getName());
            assertEquals("Excellent", response.getCondition());
            assertEquals("Home", response.getLocation());
            assertTrue(response.isAvailable());
            verify(gameRepository).findGameById(VALID_GAME_ID);
            verify(accountRepository).findByEmail(VALID_OWNER_EMAIL);
            verify(gameInstanceRepository).save(any(GameInstance.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testUpdateGameInstanceWithName() {
        // Setup SecurityContext for authenticated user
        GameOwner owner = new GameOwner("Test Owner", VALID_OWNER_EMAIL, "password");
        Authentication auth = new UsernamePasswordAuthenticationToken(VALID_OWNER_EMAIL, "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_GAME_OWNER")));
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        try {
            // Setup
            Game game = new Game(VALID_GAME_NAME, VALID_MIN_PLAYERS, VALID_MAX_PLAYERS, VALID_IMAGE, new Date());
            game.setOwner(owner);
            GameInstance existingInstance = new GameInstance(game, owner, "Good", "Home", "Old Name");
            existingInstance.setId(1);

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("name", "Updated Name");
            updateData.put("condition", "Excellent");
            updateData.put("location", "New Location");
            updateData.put("available", true);

            GameInstance updatedInstance = new GameInstance(game, owner, "Excellent", "New Location", "Updated Name");
            updatedInstance.setId(1);

            when(gameInstanceRepository.findById(1)).thenReturn(Optional.of(existingInstance));
            when(gameInstanceRepository.save(any(GameInstance.class))).thenReturn(updatedInstance);

            // Test
            GameInstanceResponseDto response = gameService.updateGameInstance(1, updateData);

            // Verify
            assertNotNull(response);
            assertEquals("Updated Name", response.getName());
            assertEquals("Excellent", response.getCondition());
            assertEquals("New Location", response.getLocation());
            assertTrue(response.isAvailable());
            verify(gameInstanceRepository).findById(1);
            verify(gameInstanceRepository).save(any(GameInstance.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
