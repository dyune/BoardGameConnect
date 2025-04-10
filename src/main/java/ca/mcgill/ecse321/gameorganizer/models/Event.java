package ca.mcgill.ecse321.gameorganizer.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date; // Changed from java.sql.Date
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

/**
 * Represents a gaming event in the system.
 * Events are organized gatherings where users can meet to play games.
 * Each event has a featured game, a host, and can accommodate a maximum number of participants.
 * 
 * @author @Yessine-glitch
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Event {

    /**
     * Unique identifier for the event.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The title or name of the event.
     */
    private String title;

    /**
     * The date and time when the event will take place.
     * Use java.util.Date or java.time.Instant/OffsetDateTime for timestamp.
     */
    @Temporal(TemporalType.TIMESTAMP) // Ensure DB stores timestamp
    private Date dateTime; // Changed from java.sql.Date

    /**
     * The physical location where the event will be held.
     */
    private String location;

    /**
     * A detailed description of the event.
     */
    @Column(length = 1000) // Allow longer descriptions
    private String description;

    /**
     * The current number of participants attending the event.
     */
    private int currentNumberParticipants;

    /**
     * The maximum number of participants that can attend the event.
     */
    private int maxParticipants;

    /**
     * The main game that will be featured at this event.
     */
    @ManyToOne // Event must have a featured game
    @JsonIgnoreProperties("owner")
    private Game featuredGame;

    /**
     * The account of the user who is hosting the event.
     */
    @ManyToOne // Event must have a host
    @JsonIgnoreProperties({"password", "resetPasswordToken", "resetPasswordTokenExpiry"})
    private Account host;
    
    /**
     * The specific game instance being used for the event (especially for borrowed games).
     * Optional: Can be null if the host uses their general owned game without specifying a copy.
     */
    @ManyToOne(optional = true)
    private GameInstance gameInstance;

    /**
     * Creates a new event with the specified details. (except host)
     *
     * @param aTitle The title of the event
     * @param aDateTime The date and time when the event will occur (use java.util.Date)
     * @param aLocation The location where the event will be held
     * @param aDescription A description of the event
     * @param aMaxParticipants The maximum number of participants allowed
     * @param aFeaturedGame The game that will be featured at the event
     */
    public Event(String aTitle, Date aDateTime, String aLocation, String aDescription, int aMaxParticipants, Game aFeaturedGame) {
        title = aTitle;
        dateTime = aDateTime; // Use java.util.Date directly
        location = aLocation;
        description = aDescription;
        maxParticipants = aMaxParticipants;
        featuredGame = aFeaturedGame;
        currentNumberParticipants = 0; 
    }

    /**
     * Creates a new event with the specified details.
     *
     * @param aTitle The title of the event
     * @param aDateTime The date and time when the event will occur (use java.util.Date)
     * @param aLocation The location where the event will be held
     * @param aDescription A description of the event
     * @param aMaxParticipants The maximum number of participants allowed
     * @param aFeaturedGame The game that will be featured at the event
     * @param host The account of the user hosting the event
     */
    public Event(String aTitle, Date aDateTime, String aLocation, String aDescription, int aMaxParticipants, Game aFeaturedGame, Account aHost) {
        title = aTitle;
        dateTime = aDateTime; // Use java.util.Date directly
        location = aLocation;
        description = aDescription;
        maxParticipants = aMaxParticipants;
        featuredGame = aFeaturedGame;
        this.host = aHost;
        currentNumberParticipants = 0; 
    }

    /**
     * Returns a string representation of the Event.
     *
     * @return A string containing the event's details including ID, title, location, description,
     *         maximum participants, date/time, and featured game reference
     */
    public String toString() {
        return super.toString() + "[" +
            "id" + ":" + getId() + "," +
            "title" + ":" + getTitle() + "," +
            "location" + ":" + getLocation() + "," +
            "description" + ":" + getDescription() + "," +
            "maxParticipants" + ":" + getMaxParticipants() + "," +
            "currentNumberParticipants" + ":" + getCurrentNumberParticipants() + "]" + System.getProperties().getProperty("line.separator") +
            "  " + "dateTime" + "=" + (getDateTime() != null ? !getDateTime().equals(this) ? getDateTime().toString().replaceAll("  ", "    ") : "this" : "null") + System.getProperties().getProperty("line.separator") +
            "  " + "featuredGame = " + (getFeaturedGame() != null ? Integer.toHexString(System.identityHashCode(getFeaturedGame())) : "null");
    }
}
