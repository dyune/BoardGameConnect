package ca.mcgill.ecse321.gameorganizer.dto.response;

import java.util.Date;

/**
 * Data Transfer Object for LendingRecord entities.
 * Used to transfer lending record data between different layers of the application.
 * 
 * @author @YoussGm3o8
 */
public class LendingRecordDto {
    private int id;
    private Date startDate;
    private Date endDate;
    private String status;
    private int borrowRequestId;
    private int recordOwnerId;
    // Add damage-related fields
    private boolean isDamaged;
    private String damageNotes;
    private int damageSeverity;
    private Date damageAssessmentDate;

    /**
     * Constructs a new LendingRecordDto with the specified properties.
     *
     * @param id The unique identifier of the lending record
     * @param startDate The start date of the lending period
     * @param endDate The end date of the lending period
     * @param status The current status of the lending record
     * @param borrowRequestId The ID of the associated borrow request
     * @param recordOwnerId The ID of the game owner
     */
    public LendingRecordDto(int id, Date startDate, Date endDate, String status, int borrowRequestId, int recordOwnerId) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.borrowRequestId = borrowRequestId;
        this.recordOwnerId = recordOwnerId;
        this.isDamaged = false;
        this.damageSeverity = 0;
    }
    
    /**
     * Constructs a new LendingRecordDto with the specified properties including damage information.
     *
     * @param id The unique identifier of the lending record
     * @param startDate The start date of the lending period
     * @param endDate The end date of the lending period
     * @param status The current status of the lending record
     * @param borrowRequestId The ID of the associated borrow request
     * @param recordOwnerId The ID of the game owner
     * @param isDamaged Flag indicating if the game was returned damaged
     * @param damageNotes Notes describing any damage
     * @param damageSeverity The severity of the damage (0-3)
     * @param damageAssessmentDate The date when damage was assessed
     */
    public LendingRecordDto(int id, Date startDate, Date endDate, String status, int borrowRequestId, int recordOwnerId,
                            boolean isDamaged, String damageNotes, int damageSeverity, Date damageAssessmentDate) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.borrowRequestId = borrowRequestId;
        this.recordOwnerId = recordOwnerId;
        this.isDamaged = isDamaged;
        this.damageNotes = damageNotes;
        this.damageSeverity = damageSeverity;
        this.damageAssessmentDate = damageAssessmentDate;
    }

    // Getters
    public int getId() { return id; }
    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }
    public String getStatus() { return status; }
    public int getBorrowRequestId() { return borrowRequestId; }
    public int getRecordOwnerId() { return recordOwnerId; }
    public boolean isDamaged() { return isDamaged; }
    public String getDamageNotes() { return damageNotes; }
    public int getDamageSeverity() { return damageSeverity; }
    public Date getDamageAssessmentDate() { return damageAssessmentDate; }
    
    /**
     * Gets a human-readable label for the damage severity.
     * 
     * @return A descriptive label for the damage severity
     */
    public String getDamageSeverityLabel() {
        return LendingRecordResponseDto.DamageSeverityUtils.getLabelForSeverity(damageSeverity);
    }
} 