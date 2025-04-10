package ca.mcgill.ecse321.gameorganizer.dto.request;

/**
 * Data Transfer Object for updating the status of a lending record.
 * Used when a game owner needs to update the status of a lending record,
 * such as marking a game as returned.
 * 
 * @author @YoussGm3o8
 */
public class UpdateLendingRecordStatusDto {
    private String newStatus;
    private Integer userId;
    private String reason;
    
    /**
     * Default constructor for deserialization
     */
    public UpdateLendingRecordStatusDto() {
    }
    
    /**
     * Constructs a new UpdateLendingRecordStatusDto with the specified status.
     *
     * @param newStatus The new status to set for the lending record
     */
    public UpdateLendingRecordStatusDto(String newStatus) {
        this.newStatus = newStatus;
    }
    
    /**
     * Constructs a new UpdateLendingRecordStatusDto with all audit information.
     *
     * @param newStatus The new status to set for the lending record
     * @param userId The ID of the user making the change
     * @param reason The reason for the status change
     */
    public UpdateLendingRecordStatusDto(String newStatus, Integer userId, String reason) {
        this.newStatus = newStatus;
        this.userId = userId;
        this.reason = reason;
    }
    
    /**
     * Gets the new status for the lending record.
     *
     * @return The new status
     */
    public String getNewStatus() {
        return newStatus;
    }
    
    /**
     * Sets the new status for the lending record.
     *
     * @param newStatus The new status to set
     */
    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
    
    /**
     * Gets the ID of the user making the change.
     *
     * @return The user ID
     */
    public Integer getUserId() {
        return userId;
    }
    
    /**
     * Sets the ID of the user making the change.
     *
     * @param userId The user ID to set
     */
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    
    /**
     * Gets the reason for the status change.
     *
     * @return The reason
     */
    public String getReason() {
        return reason;
    }
    
    /**
     * Sets the reason for the status change.
     *
     * @param reason The reason to set
     */
    public void setReason(String reason) {
        this.reason = reason;
    }
} 