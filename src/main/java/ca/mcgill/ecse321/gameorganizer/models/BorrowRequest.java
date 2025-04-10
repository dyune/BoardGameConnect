package ca.mcgill.ecse321.gameorganizer.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * Represents a request to borrow a game from its owner.
 * Tracks the request details including dates, status, and involved parties.
 *
 * @author @rayanBaida
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class BorrowRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private Date startDate;

    private Date endDate;

    @Enumerated(EnumType.STRING)
    private BorrowRequestStatus status;

    private Date requestDate;

    // Associations

    @ManyToOne
    private Game requestedGame;

    @ManyToOne
    private GameInstance gameInstance;

    @ManyToOne
    private Account requester;

    @ManyToOne(optional = true) // Allow responder to be null initially
    private GameOwner responder;

    // Methods

    /**
     * Creates a new borrow request with the specified details.
     *
     * @param aStartDate    The date when the borrowing period starts
     * @param aEndDate      The date when the borrowing period ends
     * @param aStatus       The current status of the request
     * @param aRequestDate  The date when the request was made
     * @param aRequestedGame The game being requested to borrow
     * @param aGameInstance The specific game instance being requested
     */
    public BorrowRequest(Date aStartDate, Date aEndDate, BorrowRequestStatus aStatus, Date aRequestDate, Game aRequestedGame, GameInstance aGameInstance) {
        startDate = aStartDate;
        endDate = aEndDate;
        status = aStatus;
        requestDate = aRequestDate;
        requestedGame = aRequestedGame;
        gameInstance = aGameInstance;
    }

    /**
     * Gets the game associated with this borrow request.
     *
     * @return The requested Game object
     */
    public Game getRequestedGame() {
        return this.requestedGame;
    }

    /**
     * Returns a string representation of the BorrowRequest.
     *
     * @return A string containing the request's details including ID, status, dates, and game reference
     */
    public String toString() {
        return super.toString() + "[" +
                "id" + ":" + getId() + "," +
                "status" + ":" + getStatus() + "]" + System.getProperties().getProperty("line.separator") +
                "  " + "startDate" + "=" + (getStartDate() != null ? !getStartDate().equals(this) ? getStartDate().toString().replaceAll("  ", "    ") : "this" : "null") + System.getProperties().getProperty("line.separator") +
                "  " + "endDate" + "=" + (getEndDate() != null ? !getEndDate().equals(this) ? getEndDate().toString().replaceAll("  ", "    ") : "this" : "null") + System.getProperties().getProperty("line.separator") +
                "  " + "requestDate" + "=" + (getRequestDate() != null ? !getRequestDate().equals(this) ? getRequestDate().toString().replaceAll("  ", "    ") : "this" : "null") + System.getProperties().getProperty("line.separator") +
                "  " + "requestedGame = " + (getRequestedGame() != null ? Integer.toHexString(System.identityHashCode(getRequestedGame())) : "null");
    }
}