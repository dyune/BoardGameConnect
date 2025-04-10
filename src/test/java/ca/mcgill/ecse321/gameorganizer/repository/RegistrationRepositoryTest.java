package ca.mcgill.ecse321.gameorganizer.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.Event;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.Registration;
import ca.mcgill.ecse321.gameorganizer.repositories.RegistrationRepository;

@DataJpaTest
public class RegistrationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RegistrationRepository registrationRepository;

    private Account testAttendee;
    private Event testEvent;
    private Game testGame;
    private Registration testRegistration;
    private Date testDate;

    @BeforeEach
    public void setUp() {
        // Create test data
        testDate = new Date();
        
        // Create and save test attendee
        testAttendee = new Account("Test User", "test@example.com", "password123");
        testAttendee = entityManager.persist(testAttendee);

        // Create and save test game
        testGame = new Game("Test Game", 2, 4, "test.jpg", new Date());
        testGame = entityManager.persist(testGame);

        // Create and save test event
        testEvent = new Event("Test Event", new Date(), "Test Location", "Test Description", 10, testGame);
        testEvent = entityManager.persist(testEvent);

        // Create and save test registration
        testRegistration = new Registration(testDate);
        testRegistration.setAttendee(testAttendee);
        testRegistration.setEventRegisteredFor(testEvent);
        testRegistration = entityManager.persistAndFlush(testRegistration);

        entityManager.clear();
    }

    @AfterEach
    public void cleanUp() {
        registrationRepository.deleteAll();
        entityManager.flush();
    }

    @Test
    public void testPersistAndLoadRegistration() {
        // Create a new registration
        Registration registration = new Registration(testDate);
        registration.setAttendee(testAttendee);
        registration.setEventRegisteredFor(testEvent);

        // Save the registration
        registration = entityManager.persistAndFlush(registration);
        int id = registration.getId();

        entityManager.clear();

        // Retrieve the registration
        Optional<Registration> retrievedOpt = registrationRepository.findRegistrationById(id);
        assertTrue(retrievedOpt.isPresent(), "The registration should be present in the database");

        Registration retrieved = retrievedOpt.get();
        assertEquals(testDate, retrieved.getRegistrationDate());
        assertEquals(testAttendee.getId(), retrieved.getAttendee().getId());
        assertEquals(testEvent.getId(), retrieved.getEventRegisteredFor().getId());
    }

    @Test
    public void testFindByAttendeeName() {
        // Find registrations by attendee name
        final String attendeeName = testAttendee.getName();
        List<Registration> registrations = registrationRepository.findRegistrationByAttendeeName(attendeeName);
        
        assertFalse(registrations.isEmpty(), "Should find registrations for the attendee");
        assertEquals(1, registrations.size(), "Should find exactly one registration");
        assertEquals(testAttendee.getId(), registrations.get(0).getAttendee().getId());
        assertEquals(testEvent.getId(), registrations.get(0).getEventRegisteredFor().getId());
    }

    @Test
    public void testFindByNonExistentAttendeeName() {
        // Find registrations for non-existent attendee
        List<Registration> registrations = registrationRepository.findRegistrationByAttendeeName("NonExistentUser");
        assertTrue(registrations.isEmpty(), "Should not find any registrations for non-existent attendee");
    }

    @Test
    public void testDeleteRegistration() {
        // Get the ID before deletion
        int id = testRegistration.getId();

        // Delete the registration
        registrationRepository.delete(testRegistration);
        entityManager.flush();

        // Try to find the deleted registration
        Optional<Registration> deletedOpt = registrationRepository.findRegistrationById(id);
        assertFalse(deletedOpt.isPresent(), "The registration should be deleted from the database");
    }

    @Test
    public void testUpdateRegistration() {
        // Get the registration to update
        Optional<Registration> registrationOpt = registrationRepository.findRegistrationById(testRegistration.getId());
        assertTrue(registrationOpt.isPresent());
        Registration registration = registrationOpt.get();

        // Create a new date for update
        Date newDate = new Date(testDate.getTime() + 86400000); // One day later
        registration.setRegistrationDate(newDate);

        // Save the updated registration
        entityManager.persistAndFlush(registration);
        entityManager.clear();

        // Verify the update
        Optional<Registration> updatedOpt = registrationRepository.findRegistrationById(registration.getId());
        assertTrue(updatedOpt.isPresent());
        assertEquals(newDate, updatedOpt.get().getRegistrationDate());
    }

    @Test
    public void testMultipleRegistrationsForAttendee() {
        // Create another event
        Event secondEvent = new Event("Second Event", new Date(), "Another Location", "Another Description", 20, testGame);
        secondEvent = entityManager.persist(secondEvent);

        // Create another registration for the same attendee
        Registration secondRegistration = new Registration(new Date());
        secondRegistration.setAttendee(testAttendee);
        secondRegistration.setEventRegisteredFor(secondEvent);
        entityManager.persistAndFlush(secondRegistration);
        entityManager.clear();

        // Find all registrations for the attendee
        final String attendeeName = testAttendee.getName();
        List<Registration> registrations = registrationRepository.findRegistrationByAttendeeName(attendeeName);
        assertEquals(2, registrations.size(), "Should find two registrations for the attendee");
    }

    @Test
    public void testCascadeOnDelete() {
        // Store IDs for verification
        final Account storedAttendee = entityManager.find(Account.class, testAttendee.getId());
        final Event storedEvent = entityManager.find(Event.class, testEvent.getId());
        final Game storedGame = entityManager.find(Game.class, testGame.getId());

        // Delete the registration
        registrationRepository.delete(testRegistration);
        entityManager.flush();
        entityManager.clear();

        // Verify that the related entities still exist
        assertTrue(entityManager.find(Account.class, storedAttendee.getId()) != null, "Attendee should still exist");
        assertTrue(entityManager.find(Event.class, storedEvent.getId()) != null, "Event should still exist");
        assertTrue(entityManager.find(Game.class, storedGame.getId()) != null, "Game should still exist");
    }
}
