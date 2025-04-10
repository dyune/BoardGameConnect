package ca.mcgill.ecse321.gameorganizer.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * Represents a record of a game lending transaction between a game owner and a borrower.
 * This entity tracks the lending period, status, and associated request details.
 *
 * @author @YoussGm3o8
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
public class LendingRecord {

    /**
     * Enumeration of possible lending record statuses.
     * ACTIVE: The lending is currently ongoing
     * OVERDUE: The lending period has expired but the game hasn't been returned
     * CLOSED: The lending transaction has been completed
     */
    public enum LendingStatus {
        ACTIVE, OVERDUE, CLOSED
    }

    /** Unique identifier for the lending record */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /** The date when the lending period begins */
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDate;

    /** The date when the game is expected to be returned */
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDate;

    /** Current status of the lending record */
    @Enumerated(EnumType.STRING)
    private LendingStatus status;

    /** The owner who is lending the game */
    @ManyToOne
    private GameOwner recordOwner;

    /** The associated borrow request that initiated this lending */
    @OneToOne
    @JoinColumn(name = "request_id", unique = true)
    private BorrowRequest request;
    
    /** Flag indicating if the game was returned with damage */
    private boolean isDamaged = false;
    
    /** Notes describing any damage to the game when returned */
    @Column(length = 1000)
    private String damageNotes;
    
    /** Date when damage was assessed (typically on return) */
    @Temporal(TemporalType.TIMESTAMP)
    private Date damageAssessmentDate;
    
    /** The severity of the damage (can be used for fee calculation) */
    private int damageSeverity = 0; // 0=none, 1=minor, 2=moderate, 3=severe
    
    /** Date when the record was last modified */
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModifiedDate;
    
    /** ID of the user who last modified the record */
    private Integer lastModifiedBy;
    
    /** Reason for the status change */
    @Column(length = 500)
    private String statusChangeReason;
    
    /** ID of the user who closed the record */
    private Integer closedBy;
    
    /** Reason for closing the record */
    @Column(length = 500)
    private String closingReason;

    /**
     * Creates a new lending record with the specified details.
     *
     * @param aStartDate The start date of the lending period
     * @param aEndDate The end date of the lending period
     * @param aStatus The initial status of the lending
     * @param aRequest The associated borrow request
     * @param aOwner The game owner
     * @throws IllegalArgumentException if any parameter is null or if dates are invalid
     */
    public LendingRecord(Date aStartDate, Date aEndDate, LendingStatus aStatus, BorrowRequest aRequest, GameOwner aOwner) {
        if (aStartDate == null || aEndDate == null || aStatus == null || aRequest == null || aOwner == null) {
            throw new IllegalArgumentException("Required fields cannot be null");
        }
        if (aEndDate.before(aStartDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        if (!aRequest.getRequestedGame().getOwner().equals(aOwner)) {
            throw new IllegalArgumentException("The record owner must be the owner of the game in the borrow request");
        }
        startDate = aStartDate;
        endDate = aEndDate;
        status = aStatus;
        request = aRequest;
        recordOwner = aOwner;
        lastModifiedDate = new Date();
        statusChangeReason = "Initial record creation";
    }

    /**
     * Calculates the duration of the lending period in days.
     *
     * @return the number of days between start and end date
     */
    public long getDurationInDays() {
        return (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24);
    }
    
    /**
     * Records damage information when a game is returned.
     *
     * @param damaged flag indicating if the game was damaged
     * @param notes detailed description of any damage
     * @param severity the severity level of the damage (0-3)
     */
    public void recordDamage(boolean damaged, String notes, int severity) {
        this.isDamaged = damaged;
        this.damageNotes = notes;
        this.damageAssessmentDate = new Date();
        this.damageSeverity = Math.min(3, Math.max(0, severity)); // Ensure severity is between 0-3
    }

    /**
     * Records the closing of a lending record with audit information.
     * 
     * @param userId ID of the user who closed the record
     * @param reason Reason why the record was closed
     */
    public void recordClosing(Integer userId, String reason) {
        this.status = LendingStatus.CLOSED;
        this.endDate = new Date();
        this.closedBy = userId;
        this.closingReason = reason;
        this.lastModifiedDate = new Date();
        this.lastModifiedBy = userId;
        this.statusChangeReason = "Record closed: " + reason;
    }

    public String toString() {
        return super.toString() + "[" +
                "id" + ":" + getId() + "," +
                "status" + ":" + getStatus() + "]" + System.getProperties().getProperty("line.separator") +
                "  " + "startDate" + "=" + (getStartDate() != null ? !getStartDate().equals(this) ? getStartDate().toString().replaceAll("  ", "    ") : "this" : "null") + System.getProperties().getProperty("line.separator") +
                "  " + "endDate" + "=" + (getEndDate() != null ? !getEndDate().equals(this) ? getEndDate().toString().replaceAll("  ", "    ") : "this" : "null") + System.getProperties().getProperty("line.separator") +
                "  " + "request = " + (getRequest() != null ? Integer.toHexString(System.identityHashCode(getRequest())) : "null") + System.getProperties().getProperty("line.separator");
    }
}

