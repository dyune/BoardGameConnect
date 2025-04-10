package ca.mcgill.ecse321.gameorganizer.dto.request;

import java.util.Date;

/**
 * Represents a borrow request for a game.
 * Stores details about the request, including the requester, game, and status.
 * 
 * @author Rayan Baida
 */
public class BorrowRequestDto {
    private int id;
    private int requesterId;
    private int requestedGameId;
    private int gameInstanceId;
    private Date startDate;
    private Date endDate;
    private String status;
    private Date requestDate;

    /**
     * Constructs a new borrow request DTO.
     * 
     * @param id Unique ID of the request.
     * @param requesterId ID of the user making the request.
     * @param requestedGameId ID of the game being requested.
     * @param gameInstanceId ID of the specific game instance being requested.
     * @param startDate Start date of the borrow period.
     * @param endDate End date of the borrow period.
     * @param status Current status of the request (e.g., pending, approved, declined).
     * @param requestDate Date when the request was made.
     */
    public BorrowRequestDto(int id, int requesterId, int requestedGameId, int gameInstanceId, Date startDate, Date endDate, String status, Date requestDate) {
        this.id = id;
        this.requesterId = requesterId;
        this.requestedGameId = requestedGameId;
        this.gameInstanceId = gameInstanceId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.requestDate = requestDate;
    }

    /** @return The unique ID of the borrow request. */
    public int getId() { return id; }
    
    /** @return The ID of the requester. */
    public int getRequesterId() { return requesterId; }
    
    /** @return The ID of the requested game. */
    public int getRequestedGameId() { return requestedGameId; }
    
    /** @return The ID of the specific game instance being requested. */
    public int getGameInstanceId() { return gameInstanceId; }
    
    /** @return The start date of the borrow period. */
    public Date getStartDate() { return startDate; }
    
    /** @return The end date of the borrow period. */
    public Date getEndDate() { return endDate; }
    
    /** @return The status of the borrow request. */
    public String getStatus() { return status; }
    
    /** @return The date when the request was made. */
    public Date getRequestDate() { return requestDate; }
}