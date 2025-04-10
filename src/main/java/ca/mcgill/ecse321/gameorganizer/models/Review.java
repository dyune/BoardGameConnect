package ca.mcgill.ecse321.gameorganizer.models;

import java.util.Date;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a review for a game in the system.
 * Reviews contain ratings, comments, and track who made the review and when.
 * Reviews are associated with specific games and can persist even if the game is deleted.
 * 
 * @author @jiwoong0815
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
public class Review {

    /** Unique identifier for the review */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    /** Numerical rating given to the game (typically 1-5) */
    private int rating;

    /** Textual feedback about the game */
    private String comment;

    /** Date when the review was submitted */
    private Date dateSubmitted;

    /** The game being reviewed, can be null if game is deleted */
    @ManyToOne
    @JoinColumn(name = "game_reviewed_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Game gameReviewed;

    /** The account of the user who wrote the review */
    @ManyToOne
    private Account reviewer;

    /**
     * Creates a new review with the specified details.
     *
     * @param aRating The numerical rating given to the game
     * @param aComment The textual feedback about the game
     * @param aDateSubmitted The date when the review was submitted
     */
    public Review(int aRating, String aComment, Date aDateSubmitted) {
        rating = aRating;
        comment = aComment;
        dateSubmitted = aDateSubmitted;
    }

    /**
     * Returns a string representation of the Review.
     *
     * @return A string containing the review's ID, rating, comment, and submission date
     */
    @Override
    public String toString() {
        return super.toString() + "[" +
                "id" + ":" + getId() + "," +
                "rating" + ":" + getRating() + "," +
                "comment" + ":" + getComment() + "]" + System.getProperties().getProperty("line.separator") +
                "  " + "dateSubmitted" + "=" + (getDateSubmitted() != null ? !getDateSubmitted().equals(this) ? getDateSubmitted().toString().replaceAll("  ", "    ") : "this" : "null") + System.getProperties().getProperty("line.separator");
    }
}
