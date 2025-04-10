package ca.mcgill.ecse321.gameorganizer.repositories;

import java.util.Date; // Changed from java.sql.Date
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ca.mcgill.ecse321.gameorganizer.models.Event;

/**
 * Repository interface for managing Event entities.
 * Provides CRUD operations and custom queries for gaming events.
 * 
 * @author @Yessine-glitch
 */
@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    /**
     * Finds an event by its unique identifier.
     *
     * @param id the UUID of the event to find
     * @return Optional containing the event if found, empty otherwise
     */
    Optional<Event> findEventById(UUID id);

    /**
     * Finds events by their title.
     *
     * @param title the exact title to search for
     * @return list of events matching the title
     */
    List<Event> findEventByTitle(String title);

    /**
     * Finds events containing the given text in their title.
     *
     * @param title the text to search for in titles
     * @return list of events with matching title parts
     */
    List<Event> findEventByTitleContaining(String title);

    List<Event> findEventByDateTime(Date dateTime); // Now expects java.util.Date
    List<Event> findEventByLocation(String location);
    List<Event> findEventByLocationContaining(String location);
    List<Event> findEventByDescription(String description);
    List<Event> findEventByMaxParticipants(int maxParticipants);
    List<Event> findByFeaturedGameMinPlayers(int minPlayers);
    List<Event> findByFeaturedGameMinPlayersGreaterThanEqual(int minPlayers);
    //List<Event> findByFeaturedGameMinPlayersGreaterThanEqualAndMaxParticipantsLessThanEqual(int minPlayers, int maxParticipants);
    List<Event> findEventByFeaturedGameId(int featuredGameId);
    List<Event> findEventByFeaturedGameName(String featuredGameName);
    List<Event> findEventByHostId(int hostId);
    List<Event> findEventByHostName(String hostUsername); // Keep for potential future use?
    List<Event> findEventByHostEmail(String hostEmail); // Add method to find by host email

    void deleteAllByFeaturedGameId(int gameId); // Delete all events associated with a specific game ID
}
