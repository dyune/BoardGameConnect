package ca.mcgill.ecse321.gameorganizer.dto.request;

import java.util.Date;

/**
 * Data Transfer Object for filtering lending history.
 * Used to specify criteria for filtering lending records when viewing lending history.
 * 
 * @author @YoussGm3o8
 */
public class LendingHistoryFilterDto {
    private Date fromDate;
    private Date toDate;
    private String status;
    private Integer gameId;
    private Integer borrowerId;
    
    /**
     * Default constructor for deserialization
     */
    public LendingHistoryFilterDto() {
    }
    
    /**
     * Constructs a new LendingHistoryFilterDto with the specified criteria.
     *
     * @param fromDate The start date for filtering (inclusive)
     * @param toDate The end date for filtering (inclusive)
     * @param status The lending status to filter by
     * @param gameId The game ID to filter by
     * @param borrowerId The borrower ID to filter by
     */
    public LendingHistoryFilterDto(Date fromDate, Date toDate, String status, Integer gameId, Integer borrowerId) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.status = status;
        this.gameId = gameId;
        this.borrowerId = borrowerId;
    }
    
    // Getters and Setters
    
    /**
     * Gets the from date for filtering.
     *
     * @return The from date
     */
    public Date getFromDate() {
        return fromDate;
    }
    
    /**
     * Sets the from date for filtering.
     *
     * @param fromDate The from date to set
     */
    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }
    
    /**
     * Gets the to date for filtering.
     *
     * @return The to date
     */
    public Date getToDate() {
        return toDate;
    }
    
    /**
     * Sets the to date for filtering.
     *
     * @param toDate The to date to set
     */
    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }
    
    /**
     * Gets the status for filtering.
     *
     * @return The status
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * Sets the status for filtering.
     *
     * @param status The status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Gets the game ID for filtering.
     *
     * @return The game ID
     */
    public Integer getGameId() {
        return gameId;
    }
    
    /**
     * Sets the game ID for filtering.
     *
     * @param gameId The game ID to set
     */
    public void setGameId(Integer gameId) {
        this.gameId = gameId;
    }
    
    /**
     * Gets the borrower ID for filtering.
     *
     * @return The borrower ID
     */
    public Integer getBorrowerId() {
        return borrowerId;
    }
    
    /**
     * Sets the borrower ID for filtering.
     *
     * @param borrowerId The borrower ID to set
     */
    public void setBorrowerId(Integer borrowerId) {
        this.borrowerId = borrowerId;
    }
} 