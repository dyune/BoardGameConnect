package ca.mcgill.ecse321.gameorganizer.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.Event;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.repositories.EventRepository;

@DataJpaTest
public class EventRepositoryTests {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventRepository eventRepository;

    @AfterEach
    public void clearDatabase() {
        eventRepository.deleteAll();
        entityManager.flush();
    }

    @Test
    public void testPersistAndLoadEvent() {
        // Create event
        String title = "D&D Night";
        java.util.Date utilDate = new Date();
        java.sql.Date dateTime = new java.sql.Date(utilDate.getTime());
        String location = "Trottier 3rd floor";
        String description = "Dungeons and Dragons night";
        int maxParticipants = 10;
        Game featuredGame = new Game("Dungeons and Dragons", 3, 6, "D&D.jpg", new Date());

        // Save game first
        featuredGame = entityManager.persist(featuredGame);

        // Create and save event
        Event event = new Event(title, dateTime, location, description, maxParticipants, featuredGame);
        event = entityManager.persistAndFlush(event);
        UUID eventId = event.getId();

        // Clear persistence context
        entityManager.clear();

        // Read event from database
        Optional<Event> eventFromDB = eventRepository.findEventById(eventId);

        // Assert correct response
        assertTrue(eventFromDB.isPresent(), "Event should be found");
        Event retrieved = eventFromDB.get();
        assertEquals(title, retrieved.getTitle());
        assertEquals(dateTime, retrieved.getDateTime());
        assertEquals(location, retrieved.getLocation());
        assertEquals(description, retrieved.getDescription());
        assertEquals(maxParticipants, retrieved.getMaxParticipants());
        assertEquals(featuredGame.getId(), retrieved.getFeaturedGame().getId());
    }

    @Test
    public void testLoadNonexistentEvent() {
        // Create event
        String title = "D&D Night";
        java.util.Date utilDate = new Date();
        java.sql.Date dateTime = new java.sql.Date(utilDate.getTime());
        String location = "Trottier 3rd floor";
        String description = "Dungeons and Dragons night";
        int maxParticipants = 10;
        Game featuredGame = new Game("Dungeons and Dragons", 3, 6, "D&D.jpg", new Date());

        // Save game first
        featuredGame = entityManager.persist(featuredGame);

        // Create and save event
        Event event = new Event(title, dateTime, location, description, maxParticipants, featuredGame);
        event = entityManager.persistAndFlush(event);
        UUID eventId = event.getId();

        // Delete the event
        entityManager.remove(event);
        entityManager.flush();
        entityManager.clear();

        // Try to read deleted event
        Optional<Event> eventFromDB = eventRepository.findEventById(eventId);

        // Assert correct response
        assertFalse(eventFromDB.isPresent(), "Event should not be found after deletion");
    }

    @Test
    public void testFindByTitle() {
        // Create test data
        Game featuredGame = new Game("Dungeons and Dragons", 3, 6, "D&D.jpg", new Date());
        featuredGame = entityManager.persist(featuredGame);

        String title1 = "D&D Night";
        String title2 = "Monopoly Night";
        String title3 = "Werewolf";
        Date testDateTime = new Date();
        String testLocation = "Trottier 3rd floor";
        String testDescription = "Game night";
        int testMaxParticipants = 10;

        Event event1 = new Event(title1, testDateTime, testLocation, testDescription, testMaxParticipants, featuredGame);
        Event event2 = new Event(title2, testDateTime, testLocation, testDescription, testMaxParticipants, featuredGame);
        Event event3 = new Event(title3, testDateTime, testLocation, testDescription, testMaxParticipants, featuredGame);

        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.persist(event3);
        entityManager.flush();
        entityManager.clear();

        // Test finding by title
        List<Event> events = eventRepository.findEventByTitle(title1);

        // Assertions
        assertNotNull(events);
        assertEquals(1, events.size());
        Event found = events.get(0);
        assertEquals(title1, found.getTitle());
        assertEquals(testDateTime, found.getDateTime());
        assertEquals(testLocation, found.getLocation());
        assertEquals(testDescription, found.getDescription());
        assertEquals(testMaxParticipants, found.getMaxParticipants());
        assertEquals(featuredGame.getId(), found.getFeaturedGame().getId());
    }

    @Test
    public void testFindByTitleContaining() {
        // Create test data
        Game featuredGame = new Game("Dungeons and Dragons", 3, 6, "D&D.jpg", new Date());
        featuredGame = entityManager.persist(featuredGame);

        final String title1 = "D&D Night";
        final String title2 = "Monopoly Night";
        final String title3 = "Werewolf";
        Date testDateTime = new Date();
        String testLocation = "Trottier 3rd floor";
        String testDescription = "Game night";
        int testMaxParticipants = 10;

        Event event1 = new Event(title1, testDateTime, testLocation, testDescription, testMaxParticipants, featuredGame);
        Event event2 = new Event(title2, testDateTime, testLocation, testDescription, testMaxParticipants, featuredGame);
        Event event3 = new Event(title3, testDateTime, testLocation, testDescription, testMaxParticipants, featuredGame);

        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.persist(event3);
        entityManager.flush();
        entityManager.clear();

        // Test finding by title containing
        List<Event> events = eventRepository.findEventByTitleContaining("Night");

        // Assertions
        assertNotNull(events);
        assertEquals(2, events.size());
        assertTrue(events.stream().anyMatch(e -> e.getTitle().equals(title1)));
        assertTrue(events.stream().anyMatch(e -> e.getTitle().equals(title2)));
    }

    @Test
    public void testFindByLocation() {
        // Create test data
        Game featuredGame = new Game("Dungeons and Dragons", 3, 6, "D&D.jpg", new Date());
        featuredGame = entityManager.persist(featuredGame);

        String testTitle = "Game Night";
        Date testDateTime = new Date();
        final String location1 = "Trottier 3rd floor";
        final String location2 = "Trottier 4th floor";
        String testDescription = "Game night";
        int testMaxParticipants = 10;

        Event event1 = new Event(testTitle, testDateTime, location1, testDescription, testMaxParticipants, featuredGame);
        Event event2 = new Event(testTitle, testDateTime, location2, testDescription, testMaxParticipants, featuredGame);
        Event event3 = new Event(testTitle, testDateTime, location2, testDescription, testMaxParticipants, featuredGame);

        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.persist(event3);
        entityManager.flush();
        entityManager.clear();

        // Test finding by location
        List<Event> events = eventRepository.findEventByLocation(location2);

        // Assertions
        assertNotNull(events);
        assertEquals(2, events.size());
        final String expectedLocation = location2;
        events.forEach(e -> assertEquals(expectedLocation, e.getLocation()));
    }

    @Test
    public void testFindByLocationContaining() {
        // Create test data
        Game featuredGame = new Game("Dungeons and Dragons", 3, 6, "D&D.jpg", new Date());
        featuredGame = entityManager.persist(featuredGame);

        String testTitle = "Game Night";
        Date testDateTime = new Date();
        final String location1 = "Trottier 3rd floor";
        final String location2 = "Trottier 4th floor";
        final String location3 = "McConnell basement";
        String testDescription = "Game night";
        int testMaxParticipants = 10;

        Event event1 = new Event(testTitle, testDateTime, location1, testDescription, testMaxParticipants, featuredGame);
        Event event2 = new Event(testTitle, testDateTime, location2, testDescription, testMaxParticipants, featuredGame);
        Event event3 = new Event(testTitle, testDateTime, location3, testDescription, testMaxParticipants, featuredGame);

        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.persist(event3);
        entityManager.flush();
        entityManager.clear();

        // Test finding by location containing
        List<Event> events = eventRepository.findEventByLocationContaining("Trottier");

        // Assertions
        assertNotNull(events);
        assertEquals(2, events.size());
        events.forEach(e -> assertTrue(e.getLocation().contains("Trottier")));
    }

    @Test
    public void testFindByFeaturedGameMinPlayers() {
        // Create test data
        Game game1 = new Game("D&D", 3, 6, "D&D.jpg", new Date());
        Game game2 = new Game("Monopoly", 2, 4, "monopoly.jpg", new Date());
        Game game3 = new Game("Werewolf", 5, 10, "werewolf.jpg", new Date());

        game1 = entityManager.persist(game1);
        game2 = entityManager.persist(game2);
        game3 = entityManager.persist(game3);

        Event event1 = new Event("Event 1", new Date(), "Location 1", "Description 1", 10, game1);
        Event event2 = new Event("Event 2", new Date(), "Location 2", "Description 2", 10, game2);
        Event event3 = new Event("Event 3", new Date(), "Location 3", "Description 3", 10, game3);

        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.persist(event3);
        entityManager.flush();
        entityManager.clear();

        // Test finding by min players
        List<Event> events = eventRepository.findByFeaturedGameMinPlayers(2);

        // Assertions
        assertNotNull(events);
        assertEquals(1, events.size());
        assertEquals(game2.getId(), events.get(0).getFeaturedGame().getId());
    }

    @Test
    public void testFindByHostId() {
        // Create test data
        final Account host1 = entityManager.persist(new Account("Host One", "host1@test.com", "password1"));
        final Account host2 = entityManager.persist(new Account("Host Two", "host2@test.com", "password2"));

        Game game = new Game("Test Game", 2, 4, "game.jpg", new Date());
        game = entityManager.persist(game);

        Event event1 = new Event("Event 1", new Date(), "Location 1", "Description 1", 10, game);
        event1.setHost(host1);
        Event event2 = new Event("Event 2", new Date(), "Location 2", "Description 2", 10, game);
        event2.setHost(host2);
        Event event3 = new Event("Event 3", new Date(), "Location 3", "Description 3", 10, game);
        event3.setHost(host2);

        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.persist(event3);
        entityManager.flush();
        entityManager.clear();

        // Test finding by host ID
        List<Event> events = eventRepository.findEventByHostId(host2.getId());

        // Assertions
        assertNotNull(events);
        assertEquals(2, events.size());
        final int expectedHostId = host2.getId();
        events.forEach(e -> assertEquals(expectedHostId, e.getHost().getId()));
    }

    @Test
    public void testFindByHostName() {
        // Create test data
        final Account host1 = entityManager.persist(new Account("Host One", "host1@test.com", "password1"));
        final Account host2 = entityManager.persist(new Account("Host Two", "host2@test.com", "password2"));

        Game game = new Game("Test Game", 2, 4, "game.jpg", new Date());
        game = entityManager.persist(game);

        Event event1 = new Event("Event 1", new Date(), "Location 1", "Description 1", 10, game);
        event1.setHost(host1);
        Event event2 = new Event("Event 2", new Date(), "Location 2", "Description 2", 10, game);
        event2.setHost(host2);
        Event event3 = new Event("Event 3", new Date(), "Location 3", "Description 3", 10, game);
        event3.setHost(host2);

        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.persist(event3);
        entityManager.flush();
        entityManager.clear();

        // Test finding by host name
        List<Event> events = eventRepository.findEventByHostName(host2.getName());

        // Assertions
        assertNotNull(events);
        assertEquals(2, events.size());
        final String expectedHostName = host2.getName();
        events.forEach(e -> assertEquals(expectedHostName, e.getHost().getName()));
    }
}
