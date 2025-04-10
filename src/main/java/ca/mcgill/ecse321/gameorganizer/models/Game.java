package ca.mcgill.ecse321.gameorganizer.models;

import java.util.Date;
import java.util.Set; // Import Set

import com.fasterxml.jackson.annotation.JsonIdentityInfo; // Import CascadeType
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import jakarta.persistence.CascadeType; // Import FetchType
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType; // Import OneToMany
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a board game in the system.
 * Games can be owned by users and borrowed by others through borrow requests.
 * Each game has information about player counts, an image, and when it was added to the system.
 *
 * @author @PlazmaMamba
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Game {

    /** Unique identifier for the game */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    /** Name of the game */
    private String name;

    /** Minimum number of players required to play */
    private int minPlayers;

    /** Maximum number of players that can play */
    private int maxPlayers;

    /** URL or path to the game's image */
    private String image;

    /** Date when the game was added to the system */
    private Date dateAdded;

    /** Category or genre of the game */
    private String category;

    /** Description of the game */
    @Column(length = 1000) // Optional: Allow longer descriptions
    private String description;

    /** Owner of the game */
    @ManyToOne // Assuming owner is mandatory, add (optional = false) if needed
    @JsonIgnoreProperties("games")
    private GameOwner owner;

    /** Reviews associated with this game */
    @OneToMany(mappedBy = "gameReviewed", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("gameReviewed")
    private Set<Review> reviews;


    /**
     * Creates a new game with the specified details.
     *
     * @param aName The name of the game
     * @param aMinPlayers The minimum number of players required
     * @param aMaxPlayers The maximum number of players allowed
     * @param aImage The URL or path to the game's image
     * @param aDateAdded The date when the game was added
     */
    public Game(String aName, int aMinPlayers, int aMaxPlayers, String aImage, Date aDateAdded) {
        name = aName;
        minPlayers = aMinPlayers;
        maxPlayers = aMaxPlayers;
        image = aImage;
        dateAdded = aDateAdded;
        this.category = "Uncategorized"; // Default category
    }

    /**
     * Returns a string representation of the Game.
     *
     * @return A string containing the game's details including ID, name, player counts,
     *         image path, and date added
     */
    public String toString() {
        return super.toString() + "[" +
                "id" + ":" + getId() + "," +
                "name" + ":" + getName() + "," +
                "minPlayers" + ":" + getMinPlayers() + "," +
                "maxPlayers" + ":" + getMaxPlayers() + "," +
                "image" + ":" + getImage() + "]" + System.getProperties().getProperty("line.separator") +
                "  " + "dateAdded" + "=" + (getDateAdded() != null ? !getDateAdded().equals(this) ? getDateAdded().toString().replaceAll("  ", "    ") : "this" : "null") + System.getProperties().getProperty("line.separator");
    }

    /**
     * Checks if this game is equal to another object.
     * Games are considered equal if they have the same ID, name, player counts,
     * image path, and date added.
     *
     * @param obj The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true; 
        }
        if (!(obj instanceof Game)) {
            return false; 
        }
        Game game = (Game) obj;
        
        return this.id == game.id &&
               this.minPlayers == game.minPlayers &&
               this.maxPlayers == game.maxPlayers &&
               (this.name != null ? this.name.equals(game.name) : game.name == null) &&
               (this.image != null ? this.image.equals(game.image) : game.image == null) &&
               (this.dateAdded != null ? this.dateAdded.equals(game.dateAdded) : game.dateAdded == null) &&
               (this.category != null ? this.category.equals(game.category) : game.category == null);
    }
}
