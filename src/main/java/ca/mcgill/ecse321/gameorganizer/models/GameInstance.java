package ca.mcgill.ecse321.gameorganizer.models;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a physical copy of a game that can be borrowed.
 * Each game can have multiple instances owned by different users.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
public class GameInstance {

    /** Unique identifier for the game instance */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    /** The game this instance is a copy of */
    @ManyToOne(optional = false)
    private Game game;

    /** Owner of this copy */
    @ManyToOne(optional = false)
    private GameOwner owner;

    /** Physical condition of the game (e.g., "Excellent", "Good", "Fair") */
    private String condition;

    /** Whether this copy is currently available for borrowing */
    private boolean available;

    /** Physical location or address where the game is stored */
    private String location;

    /** Optional name for this copy of the game */
    private String name;

    /** Date when this copy was acquired */
    private Date acquiredDate;

    /**
     * Creates a new game instance with the specified details.
     *
     * @param game The game this is an instance of
     * @param owner The owner of this copy
     * @param condition The physical condition of the copy
     * @param location The location where the copy is stored
     * @param name Optional name for this copy
     */
    public GameInstance(Game game, GameOwner owner, String condition, String location, String name) {
        this.game = game;
        this.owner = owner;
        this.condition = condition;
        this.location = location;
        this.name = name;
        this.available = true;
        this.acquiredDate = new Date();
    }

    /**
     * Returns a string representation of the GameInstance.
     *
     * @return A string containing the instance's details
     */
    @Override
    public String toString() {
        return "GameInstance [id=" + id + ", game=" + game.getName() + ", owner=" + owner.getName()
                + ", condition=" + condition + ", available=" + available + ", location=" + location
                + ", acquiredDate=" + acquiredDate + "]";
    }
} 