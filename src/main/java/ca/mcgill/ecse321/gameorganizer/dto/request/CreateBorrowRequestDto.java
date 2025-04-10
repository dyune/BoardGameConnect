package ca.mcgill.ecse321.gameorganizer.dto.request;

import java.util.Date;

/**
 * DTO for creating a borrow request.
 * Contains only the necessary details needed to submit a new request.
 * 
 * @autor Rayan Baida
 */
public class CreateBorrowRequestDto {
    private int requesterId;
    private int requestedGameId;
    private int gameInstanceId;
    private Date startDate;
    private Date endDate;

    // Default constructor (required for Jackson)
    public CreateBorrowRequestDto() {
    }

    /**
     * Constructs a new create borrow request DTO.
     * 
     * @param requesterId ID of the user making the request.
     * @param requestedGameId ID of the game being requested.
     * @param gameInstanceId ID of the specific game instance being requested.
     * @param startDate Start date of the borrow period.
     * @param endDate End date of the borrow period.
     */
    public CreateBorrowRequestDto(int requesterId, int requestedGameId, int gameInstanceId, Date startDate, Date endDate) {
        this.requesterId = requesterId;
        this.requestedGameId = requestedGameId;
        this.gameInstanceId = gameInstanceId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

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
}