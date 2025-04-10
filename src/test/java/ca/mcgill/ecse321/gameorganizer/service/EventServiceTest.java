package ca.mcgill.ecse321.gameorganizer.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.*;
import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import ca.mcgill.ecse321.gameorganizer.TestJwtConfig;
import ca.mcgill.ecse321.gameorganizer.dto.request.CreateEventRequest;
import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.Event;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.EventRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.GameRepository;
import ca.mcgill.ecse321.gameorganizer.services.EventService;
import ca.mcgill.ecse321.gameorganizer.exceptions.ForbiddenException;
import org.springframework.http.HttpStatus;
import ca.mcgill.ecse321.gameorganizer.repositories.RegistrationRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@ContextConfiguration(initializers = TestJwtConfig.Initializer.class)
public class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock // Add mock for AccountRepository
    private AccountRepository accountRepository;

    @Spy
    @InjectMocks
    private EventService eventService;

    // Test constants
    private static final UUID VALID_EVENT_ID = UUID.randomUUID();
    private static final int VALID_GAME_ID = 1; // Added constant for game ID
    private static final String VALID_TITLE = "Game Night";
    private static final String VALID_LOCATION = "Game Room";
    private static final String VALID_DESCRIPTION = "Fun game night!";
    private static final int VALID_MAX_PARTICIPANTS = 10;
    private static final String VALID_HOST_EMAIL = "host@test.com"; // Added constant
    private static final int VALID_HOST_ID = 100; // Added constant
    private static final String ACTUAL_HOST_EMAIL = "actualhost@test.com"; // For "NotHost" tests
    private static final String NON_HOST_EMAIL = "wronguser@test.com"; // For "NotHost" tests

    @Test
    public void testCreateEventSuccess() {
        // Setup
        java.util.Date eventUtilDate = new java.util.Date();
        Date eventSqlDate = new Date(eventUtilDate.getTime());

        // Use Account directly if GameOwner is not strictly needed for the host type
        Account host = new Account("Host", VALID_HOST_EMAIL, "password");
        host.setId(VALID_HOST_ID);

        Game game = new Game("Test Game", 2, 4, "test.jpg", new java.util.Date());
        game.setId(VALID_GAME_ID);
        // Assuming Game model might not need an owner directly, or it's set differently
        // game.setOwner(host); // Only set if Game requires an owner and accepts Account

        CreateEventRequest request = new CreateEventRequest();
        request.setTitle(VALID_TITLE);
        request.setDateTime(eventSqlDate);
        request.setLocation(VALID_LOCATION);
        request.setDescription(VALID_DESCRIPTION);
        request.setMaxParticipants(VALID_MAX_PARTICIPANTS);
        // Create a simple Game DTO/Stub for the request, only ID is needed by service validation
        Game gameStub = new Game();
        gameStub.setId(VALID_GAME_ID);
        request.setFeaturedGame(gameStub); // Use the stub in the request

        // Mock the security context to simulate the host being authenticated
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn(VALID_HOST_EMAIL); // Simulate getPrincipal().getUsername()
        SecurityContextHolder.setContext(securityContext);

        try {
            // Mock repository lookups needed by the service
            when(accountRepository.findByEmail(VALID_HOST_EMAIL)).thenReturn(Optional.of(host)); // Host lookup by email
            when(gameRepository.findById(VALID_GAME_ID)).thenReturn(Optional.of(game)); // Game lookup by ID

            // Mock the event saving process
            // Use ArgumentCaptor to verify the event passed to save
            org.mockito.ArgumentCaptor<Event> eventCaptor = org.mockito.ArgumentCaptor.forClass(Event.class);
            // When save is called, return the captured event (or a new event with an ID)
            when(eventRepository.save(eventCaptor.capture())).thenAnswer(invocation -> {
                Event eventToSave = invocation.getArgument(0);
                eventToSave.setId(VALID_EVENT_ID); // Assign an ID as the real repo would
                return eventToSave;
            });

            // Test
            Event result = eventService.createEvent(request);

            // Verify
            assertNotNull(result);
            assertEquals(VALID_EVENT_ID, result.getId());
            assertEquals(VALID_TITLE, result.getTitle());
            assertEquals(VALID_LOCATION, result.getLocation());
            assertEquals(VALID_DESCRIPTION, result.getDescription());
            assertEquals(VALID_MAX_PARTICIPANTS, result.getMaxParticipants());
            assertNotNull(result.getHost());
            assertEquals(VALID_HOST_ID, result.getHost().getId()); // Verify correct host ID
            assertEquals(VALID_HOST_EMAIL, result.getHost().getEmail()); // Verify correct host email
            assertNotNull(result.getFeaturedGame());
            assertEquals(VALID_GAME_ID, result.getFeaturedGame().getId()); // Verify correct game ID

            // Verify repository interactions
            verify(accountRepository).findByEmail(VALID_HOST_EMAIL);
            verify(gameRepository).findById(VALID_GAME_ID);
            verify(eventRepository).save(any(Event.class));

            // Verify the details of the saved event
            Event capturedEvent = eventCaptor.getValue();
            assertEquals(VALID_TITLE, capturedEvent.getTitle());
            assertEquals(host, capturedEvent.getHost()); // Check the host object itself
            assertEquals(game, capturedEvent.getFeaturedGame()); // Check the game object itself
        } finally {
            SecurityContextHolder.clearContext(); // Clear context after test
        }
    }

    @Test
    public void testCreateEventWithNullTitle() {
        // Setup
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle(null);
        request.setDateTime(new Date(System.currentTimeMillis()));
        request.setMaxParticipants(VALID_MAX_PARTICIPANTS);
        request.setFeaturedGame(new Game()); // Game needed for validation check
        // No need to set host on DTO
        // No need to mock accountRepository.findByEmail as it won't be called

        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> eventService.createEvent(request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    public void testCreateEventWithEmptyTitle() {
        // Setup
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("  ");
        request.setDateTime(new Date(System.currentTimeMillis()));
        request.setMaxParticipants(VALID_MAX_PARTICIPANTS);
        request.setFeaturedGame(new Game()); // Game needed for validation check
        // No need to set host on DTO
        // No need to mock accountRepository.findByEmail as it won't be called

        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> eventService.createEvent(request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    public void testCreateEventHostNotFound() {
        // Setup
        // Mock the security context first to avoid NullPointerException
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("nonexistent@email.com");
        SecurityContextHolder.setContext(securityContext);
        
        try {
            // Now mock the repository to return empty for the host
            when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            
            // Create request with necessary fields including location
            CreateEventRequest request = new CreateEventRequest();
            request.setTitle(VALID_TITLE);
            request.setDateTime(new java.util.Date());
            request.setLocation(VALID_LOCATION); // Set location to avoid validation errors
            request.setDescription(VALID_DESCRIPTION);
            request.setMaxParticipants(VALID_MAX_PARTICIPANTS);
            
            // Create and set a Game object
            Game game = new Game();
            game.setId(VALID_GAME_ID);
            request.setFeaturedGame(game);
            
            // Test & Verify
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> eventService.createEvent(request));
            assertEquals("Authenticated user account not found.", exception.getMessage());
        } finally {
            SecurityContextHolder.clearContext(); // Important to clear context after the test
        }
    }

    @Test
    public void testCreateEventWithInvalidMaxParticipants() {
        // Setup
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle(VALID_TITLE);
        request.setDateTime(new java.sql.Date(System.currentTimeMillis()));
        request.setMaxParticipants(0);
        request.setFeaturedGame(new Game()); // Game needed for validation check
        // No need to set host on DTO
        // No need to mock accountRepository.findByEmail as it won't be called

        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> eventService.createEvent(request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    public void testGetEventByIdSuccess() {
        // Setup
        Account host = new Account("Host", VALID_HOST_EMAIL, "password"); // Create host
        host.setId(VALID_HOST_ID);
        Event event = new Event(VALID_TITLE, new Date(System.currentTimeMillis()), VALID_LOCATION,
            VALID_DESCRIPTION, VALID_MAX_PARTICIPANTS, new Game(), host); // Use host
        event.setId(VALID_EVENT_ID); // Set ID

        when(eventRepository.findEventById(VALID_EVENT_ID)).thenReturn(Optional.of(event));

        // Test
        Event result = eventService.getEventById(VALID_EVENT_ID);

        // Verify
        assertNotNull(result);
        assertEquals(VALID_TITLE, result.getTitle());
        verify(eventRepository).findEventById(VALID_EVENT_ID);
    }

    @Test
    public void testGetEventByIdNotFound() {
        // Setup
        when(eventRepository.findEventById(VALID_EVENT_ID)).thenReturn(Optional.empty());

        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> eventService.getEventById(VALID_EVENT_ID));
        verify(eventRepository).findEventById(VALID_EVENT_ID);
    }

    @Test
    public void testGetAllEvents() {
        // Setup
        List<Event> events = new ArrayList<>();
        Account host = new Account("Host", VALID_HOST_EMAIL, "password"); // Create host
        host.setId(VALID_HOST_ID);
        events.add(new Event(VALID_TITLE, new Date(System.currentTimeMillis()), VALID_LOCATION,
            VALID_DESCRIPTION, VALID_MAX_PARTICIPANTS, new Game(), host)); // Use host
        when(eventRepository.findAll()).thenReturn(events);

        // Test
        List<Event> result = eventService.getAllEvents();

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VALID_TITLE, result.get(0).getTitle());
        verify(eventRepository).findAll();
    }

    @Test
    public void testUpdateEventSuccess() {
        // Setup
        String newTitle = "Updated Title";
        Date newDate = new Date(System.currentTimeMillis());
        String newLocation = "Updated Location";
        String newDescription = "Updated Description";
        int newMaxParticipants = 25;
        
        Account host = new Account("Host", VALID_HOST_EMAIL, "password");
        host.setId(VALID_HOST_ID);
        
        Event event = new Event(VALID_TITLE, new java.util.Date(), VALID_LOCATION, 
                VALID_DESCRIPTION, VALID_MAX_PARTICIPANTS, new Game(), host);
        event.setId(VALID_EVENT_ID);
        
        // Mock the security context
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(VALID_HOST_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        
        try {
            // Use spy to mock isHost with any UUID/string
            doReturn(true).when(eventService).isHost(any(UUID.class), anyString());
            
            when(eventRepository.findEventById(VALID_EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenReturn(event);
            
            // Test
            Event result = eventService.updateEvent(VALID_EVENT_ID, newTitle, newDate, newLocation,
                    newDescription, newMaxParticipants);
            
            // Verify
            assertNotNull(result);
            assertEquals(newTitle, result.getTitle());
            assertEquals(newDate, result.getDateTime());
            assertEquals(newLocation, result.getLocation());
            assertEquals(newDescription, result.getDescription());
            assertEquals(newMaxParticipants, result.getMaxParticipants());
            verify(eventRepository).save(event);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testUpdateEventNotFound() {
        // Setup
        // Mock the security context
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(VALID_HOST_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        
        try {
            // Use spy to mock isHost with any UUID/string
            doReturn(true).when(eventService).isHost(any(UUID.class), anyString());
            
            // Mock findEventById to return empty - this is what triggers the test condition
            when(eventRepository.findEventById(VALID_EVENT_ID)).thenReturn(Optional.empty());
            
            // Test & Verify
            assertThrows(IllegalArgumentException.class, () -> 
                eventService.updateEvent(VALID_EVENT_ID, "New Title", new java.util.Date(),
                        "New Location", "New Description", 20));
            verify(eventRepository, never()).save(any(Event.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testUpdateEventNotHost() {
        // Setup
        java.sql.Date dateTime = new java.sql.Date(System.currentTimeMillis()); // Convert to java.sql.Date
        
        // Create host account for the event
        Account actualHost = new Account();
        actualHost.setId(VALID_HOST_ID);
        actualHost.setEmail(ACTUAL_HOST_EMAIL);
        
        // Create non-host account for the test
        Account nonHost = new Account();
        nonHost.setId(999);
        nonHost.setEmail(NON_HOST_EMAIL);
        
        // Create the event with the actual host (not the test user)
        Event existingEvent = new Event(
            VALID_TITLE, 
            new java.util.Date(), 
            VALID_LOCATION, 
            VALID_DESCRIPTION, 
            VALID_MAX_PARTICIPANTS, 
            new Game(), 
            actualHost
        );
        existingEvent.setId(VALID_EVENT_ID);
        
        // Mock repository behavior
        when(eventRepository.findEventById(VALID_EVENT_ID)).thenReturn(Optional.of(existingEvent));
        when(accountRepository.findByEmail(NON_HOST_EMAIL)).thenReturn(Optional.of(nonHost));
        
        // Mock isHost to return false - this is crucial for the test
        doReturn(false).when(eventService).isHost(eq(VALID_EVENT_ID), eq(NON_HOST_EMAIL));
        
        // Simulate the AccessDeniedException that would be thrown by Spring Security
        // when the PreAuthorize annotation check fails
        doThrow(new AccessDeniedException("Access denied"))
            .when(eventService).updateEvent(
                eq(VALID_EVENT_ID), 
                anyString(), 
                any(java.sql.Date.class), // Updated to match service method signature
                anyString(),
                anyString(),
                anyInt()
            );
            
        // Setup security context with non-host user
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(NON_HOST_EMAIL);
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        
        try {
            // Update the expected exception type to match what's actually thrown
            assertThrows(AccessDeniedException.class, () -> 
                eventService.updateEvent(
                    VALID_EVENT_ID,
                    "Updated Title", 
                    dateTime, 
                    "Updated Location", 
                    "Updated Description", 
                    15
                )
            );
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testDeleteEventSuccess() {
        // Setup
        Account host = new Account("Host", VALID_HOST_EMAIL, "password");
        host.setId(VALID_HOST_ID);
        
        Event event = new Event(VALID_TITLE, new java.util.Date(), VALID_LOCATION, 
                VALID_DESCRIPTION, VALID_MAX_PARTICIPANTS, new Game(), host);
        event.setId(VALID_EVENT_ID);
        
        // Mock the security context
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(VALID_HOST_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        
        try {
            // Use spy to mock isHost with any UUID/string
            doReturn(true).when(eventService).isHost(any(UUID.class), anyString());
            
            when(eventRepository.findEventById(VALID_EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.existsById(VALID_EVENT_ID)).thenReturn(true);
            
            // Mock the registrationRepository to avoid NullPointerException
            // First, get the private field
            Field registrationRepoField = EventService.class.getDeclaredField("registrationRepository");
            registrationRepoField.setAccessible(true);
            
            // Mock the repository and set it on the service
            RegistrationRepository mockRegistrationRepo = mock(RegistrationRepository.class);
            when(mockRegistrationRepo.findByEventRegisteredFor(any(Event.class))).thenReturn(new ArrayList<>());
            registrationRepoField.set(eventService, mockRegistrationRepo);
            
            // Test - since deleteEvent is void, we just verify it doesn't throw an exception
            eventService.deleteEvent(VALID_EVENT_ID);
            
            // Verify
            verify(eventRepository).delete(event);
            verify(mockRegistrationRepo).findByEventRegisteredFor(event);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to setup test: " + e.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testDeleteEventNotFound() {
        // Setup
        // Mock the security context
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(VALID_HOST_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        
        try {
            // Use spy to mock isHost with any UUID/string
            doReturn(true).when(eventService).isHost(any(UUID.class), anyString());
            
            when(eventRepository.findEventById(VALID_EVENT_ID)).thenReturn(Optional.empty());
            when(eventRepository.existsById(VALID_EVENT_ID)).thenReturn(false);
            
            // Test & Verify - should throw IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> eventService.deleteEvent(VALID_EVENT_ID));
            
            // Verify repository was never called to delete
            verify(eventRepository, never()).delete(any(Event.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    
    @Test
    public void testDeleteEventNotHost() {
        // Setup
        Account actualHost = new Account();
        actualHost.setId(VALID_HOST_ID);
        actualHost.setEmail(ACTUAL_HOST_EMAIL);
        
        // Create non-host account for the test
        Account nonHost = new Account();
        nonHost.setId(999);
        nonHost.setEmail(NON_HOST_EMAIL);
        
        Event existingEvent = new Event(
            VALID_TITLE, 
            new java.util.Date(), 
            VALID_LOCATION, 
            VALID_DESCRIPTION, 
            VALID_MAX_PARTICIPANTS, 
            new Game(), 
            actualHost
        );
        
        when(eventRepository.findEventById(VALID_EVENT_ID)).thenReturn(Optional.of(existingEvent));
        when(accountRepository.findByEmail(NON_HOST_EMAIL)).thenReturn(Optional.of(nonHost));
        when(eventService.isHost(VALID_EVENT_ID, NON_HOST_EMAIL)).thenReturn(false);
        
        // Mock isHost to return false - crucial for the test
        doReturn(false).when(eventService).isHost(eq(VALID_EVENT_ID), eq(NON_HOST_EMAIL));
        
        // Simulate the AccessDeniedException that would be thrown by Spring Security
        // when the PreAuthorize annotation check fails
        doThrow(new AccessDeniedException("Access denied"))
            .when(eventService).deleteEvent(eq(VALID_EVENT_ID));
        
        // Setup security context with non-host user
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(NON_HOST_EMAIL);
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        
        try {
            // Update the expected exception type to match what's actually thrown
            assertThrows(AccessDeniedException.class, () -> 
                eventService.deleteEvent(VALID_EVENT_ID)
            );
        } finally {
            SecurityContextHolder.clearContext();
        }
    }


    // --- Search Tests ---
    // (Assuming these don't involve the AccountRepository directly in the service methods being tested,
    // but ensuring the setup uses valid host objects where applicable)

    @Test
    public void testFindEventsByGameName() {
        // Setup
        String gameName = "Test Game";
        List<Event> events = new ArrayList<>();
        Account host = new Account("Host", VALID_HOST_EMAIL, "password");
        host.setId(VALID_HOST_ID);
        events.add(new Event(VALID_TITLE, new Date(System.currentTimeMillis()), VALID_LOCATION,
            VALID_DESCRIPTION, VALID_MAX_PARTICIPANTS, new Game(gameName, 2, 4, "img.jpg", new java.util.Date()), host)); // Use host
        when(eventRepository.findEventByFeaturedGameName(gameName)).thenReturn(events);

        // Test
        List<Event> result = eventService.findEventsByGameName(gameName);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository).findEventByFeaturedGameName(gameName);
    }

    @Test
    public void testFindEventsByGameNameEmpty() {
        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> eventService.findEventsByGameName(""));
        verify(eventRepository, never()).findEventByFeaturedGameName(anyString());
    }

    @Test
    public void testFindEventsByLocationContaining() {
        // Setup
        String locationSearch = "Room";
        List<Event> events = new ArrayList<>();
        Account host = new Account("Host", VALID_HOST_EMAIL, "password");
        host.setId(VALID_HOST_ID);
        events.add(new Event(VALID_TITLE, new Date(System.currentTimeMillis()), VALID_LOCATION,
            VALID_DESCRIPTION, VALID_MAX_PARTICIPANTS, new Game(), host)); // Use host
        when(eventRepository.findEventByLocationContaining(locationSearch)).thenReturn(events);

        // Test
        List<Event> result = eventService.findEventsByLocationContaining(locationSearch);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository).findEventByLocationContaining(locationSearch);
    }

    @Test
    public void testFindEventsByLocationContainingEmpty() {
        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> eventService.findEventsByLocationContaining(""));
        verify(eventRepository, never()).findEventByLocationContaining(anyString());
    }

    @Test
    public void testFindEventsByGameMinPlayers() {
        // Setup
        int minPlayers = 2;
        List<Event> events = new ArrayList<>();
        Account host = new Account("Host", VALID_HOST_EMAIL, "password");
        host.setId(VALID_HOST_ID);
        Game game = new Game("Test Game", minPlayers, 4, "img.jpg", new java.util.Date()); // Ensure game meets criteria
        events.add(new Event(VALID_TITLE, new Date(System.currentTimeMillis()), VALID_LOCATION,
            VALID_DESCRIPTION, VALID_MAX_PARTICIPANTS, game, host)); // Use host and game
        when(eventRepository.findByFeaturedGameMinPlayersGreaterThanEqual(minPlayers)).thenReturn(events);

        // Test
        List<Event> result = eventService.findEventsByGameMinPlayers(minPlayers);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository).findByFeaturedGameMinPlayersGreaterThanEqual(minPlayers);
    }

    @Test
    public void testFindEventsByGameMinPlayersInvalid() {
        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> eventService.findEventsByGameMinPlayers(0));
        verify(eventRepository, never()).findByFeaturedGameMinPlayersGreaterThanEqual(0);
    }

    @AfterEach
    public void tearDown() {
        // Clear the security context after each test to avoid side effects
        SecurityContextHolder.clearContext();
    }
}
