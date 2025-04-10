package ca.mcgill.ecse321.gameorganizer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times; // Keep one import
import static org.mockito.Mockito.never;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Collections; // Keep one import
import java.util.UUID; // Added import

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.ArgumentCaptor;

// Imports for Security Context Mocking
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.Event;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.Registration;
import ca.mcgill.ecse321.gameorganizer.repositories.RegistrationRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository; // Keep one import
import ca.mcgill.ecse321.gameorganizer.repositories.EventRepository; // Add EventRepository mock
import ca.mcgill.ecse321.gameorganizer.services.RegistrationService;
import ca.mcgill.ecse321.gameorganizer.exceptions.ResourceNotFoundException;

// Add ContextConfiguration and import TestJwtConfig
import org.springframework.test.context.ContextConfiguration;
import ca.mcgill.ecse321.gameorganizer.TestJwtConfig;
import org.springframework.security.authentication.TestingAuthenticationToken;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@ContextConfiguration(initializers = TestJwtConfig.Initializer.class)
public class RegistrationServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private AccountRepository accountRepository; // Keep one mock
    
    @Mock
    private EventRepository eventRepository; // Add EventRepository mock

    @InjectMocks
    private RegistrationService registrationService;

    // Test constants
    private static final int VALID_REGISTRATION_ID = 1;

    @Test
    public void testCreateRegistrationSuccess() {
        // Setup
        Date registrationDate = new Date();
        
        // Create attendee with proper email
        Account attendee = new Account();
        attendee.setName("Attendee");
        attendee.setEmail("attendee@test.com"); // Explicitly set email to ensure it's not null
        attendee.setPassword("password");
        
        Game game = new Game("Test Game", 2, 4, "test.jpg", new Date());
        Account host = new Account();
        host.setName("Host");
        host.setEmail("host@test.com");
        
        Event event = new Event("Game Night", new Date(), "Location", "Description", 10, game, host);
        UUID eventId = UUID.randomUUID(); // Generate UUID
        event.setId(eventId); // Set UUID on event
        event.setCurrentNumberParticipants(5); // Set current participants

        Registration registration = new Registration(registrationDate);
        registration.setId(VALID_REGISTRATION_ID);
        registration.setAttendee(attendee);
        registration.setEventRegisteredFor(event);

        // Setup Security Context properly
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(attendee.getEmail());
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        try {
            // Setup mocks
            when(registrationRepository.save(any(Registration.class))).thenReturn(registration);
            when(accountRepository.findByEmail(attendee.getEmail())).thenReturn(Optional.of(attendee));
            when(eventRepository.findEventById(any(UUID.class))).thenReturn(Optional.of(event));
            // Add mock for exists check
            when(registrationRepository.existsByAttendeeAndEventRegisteredFor(attendee, event)).thenReturn(false);

            // Test
            Registration result = registrationService.createRegistration(registrationDate, event);

            // Verify
            assertNotNull(result);
            assertEquals(VALID_REGISTRATION_ID, result.getId());
            assertEquals(attendee, result.getAttendee());
            assertEquals(event, result.getEventRegisteredFor());
            verify(registrationRepository).save(any(Registration.class));
        } finally {
            SecurityContextHolder.clearContext(); // Clear context after test
        }
    }

    @Test
    public void testGetRegistrationByIdSuccess() {
        // Setup
        Registration registration = new Registration(new Date());
        registration.setId(VALID_REGISTRATION_ID);
        when(registrationRepository.findRegistrationById(VALID_REGISTRATION_ID))
            .thenReturn(Optional.of(registration));

        // Test
        Optional<Registration> result = registrationService.getRegistration(VALID_REGISTRATION_ID);

        // Verify
        assertTrue(result.isPresent());
        assertEquals(VALID_REGISTRATION_ID, result.get().getId());
        verify(registrationRepository).findRegistrationById(VALID_REGISTRATION_ID);
    }

    @Test
    public void testGetRegistrationByIdNotFound() {
        // Setup
        when(registrationRepository.findRegistrationById(anyInt())).thenReturn(Optional.empty());

        // Test
        Optional<Registration> result = registrationService.getRegistration(VALID_REGISTRATION_ID);

        // Verify
        assertTrue(result.isEmpty());
        verify(registrationRepository).findRegistrationById(VALID_REGISTRATION_ID);
    }

    @Test
    public void testGetAllRegistrations() {
        // Setup
        List<Registration> registrations = new ArrayList<>();
        Registration registration = new Registration(new Date());
        registration.setId(VALID_REGISTRATION_ID);
        registrations.add(registration);

        when(registrationRepository.findAll()).thenReturn(registrations);

        // Test
        Iterable<Registration> result = registrationService.getAllRegistrations();

        // Verify
        assertNotNull(result);
        assertEquals(1, ((List<Registration>) result).size());
        assertEquals(VALID_REGISTRATION_ID, ((List<Registration>) result).get(0).getId());
        verify(registrationRepository).findAll();
    }

    @Test
    public void testUpdateRegistrationSuccess() {
        // Setup Attendee and Security Context
        Account attendee = new Account("Attendee", "attendee@test.com", "password");
        attendee.setId(99); // Assign an ID
        Authentication auth = new UsernamePasswordAuthenticationToken(attendee.getEmail(), "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        try {
            // Setup Mocks and other test data
            Date newRegistrationDate = new Date();
            Account newAttendee = new Account("New Attendee", "new@test.com", "password");
            newAttendee.setId(100);
            // Define original event
            Game game = new Game("Original Game", 1, 5, "orig.jpg", new Date());
            Account host = new Account("Host", "host@test.com", "hostpwd");
            Event event = new Event("Original Event", new Date(), "Original Location", "Original Desc", 5, game, host);
            // event.setId(VALID_EVENT_ID); // ID not needed for this test logic

            // Define unused new event data (kept for reference, but not used in update call)
            Game newGame = new Game("New Game", 2, 4, "new.jpg", new Date());
            Event newEvent = new Event("New Event", new Date(), "New Location", "New Description", 10, newGame, new Account());

            Registration existingRegistration = new Registration(new Date());
            existingRegistration.setId(VALID_REGISTRATION_ID);
            existingRegistration.setAttendee(attendee); // Set the attendee who is authenticated
            existingRegistration.setEventRegisteredFor(event); // Set the original event

            when(accountRepository.findByEmail(attendee.getEmail())).thenReturn(Optional.of(attendee)); // Mock finding authenticated user
            when(registrationRepository.findRegistrationById(VALID_REGISTRATION_ID))
                .thenReturn(Optional.of(existingRegistration));
            when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Test
            Registration result = registrationService.updateRegistration(VALID_REGISTRATION_ID,
                newRegistrationDate); // Ensure signature: (int, Date)

            // Verify
            assertNotNull(result);
            assertEquals(VALID_REGISTRATION_ID, result.getId());
            assertEquals(newRegistrationDate, result.getRegistrationDate());
            // Verify that attendee and event were NOT changed
            assertEquals(attendee, result.getAttendee());
            assertEquals(event, result.getEventRegisteredFor());
            verify(registrationRepository).save(any(Registration.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testUpdateRegistrationNotFound() {
        // Setup
        when(registrationRepository.findRegistrationById(VALID_REGISTRATION_ID))
            .thenReturn(Optional.empty());

        // Test & Verify
        assertThrows(ResourceNotFoundException.class, () ->
            registrationService.updateRegistration(VALID_REGISTRATION_ID, new Date())); // Ensure signature: (int, Date)
    }

    @Test
    public void testDeleteRegistration() {
        // Setup Authentication
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Account attendee = new Account("test@example.com", "testUser", "password123");
        Authentication auth = new TestingAuthenticationToken(attendee.getEmail(), null);
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        try {
            // Setup Mocks and other test data
            Game game = new Game("Test Game", 2, 4, "test.jpg", new Date());
            Event event = new Event("Game Night", new Date(), "Location", "Description", 10, game, new Account());
            event.setCurrentNumberParticipants(5); // Ensure participants > 0

            Registration registration = new Registration(new Date());
            registration.setId(VALID_REGISTRATION_ID);
            registration.setEventRegisteredFor(event);
            registration.setAttendee(attendee); // Set the attendee who is authenticated

            when(accountRepository.findByEmail(attendee.getEmail())).thenReturn(Optional.of(attendee)); // Mock finding authenticated user
            when(registrationRepository.findRegistrationById(VALID_REGISTRATION_ID))
                .thenReturn(Optional.of(registration));
            // Add event repository mock
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            // Test
            registrationService.deleteRegistration(VALID_REGISTRATION_ID);

            // Verify
            verify(registrationRepository).deleteById(VALID_REGISTRATION_ID);
            verify(eventRepository).save(event); // Verify event was saved
            assertEquals(4, event.getCurrentNumberParticipants()); // Verify participant count decreased
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
