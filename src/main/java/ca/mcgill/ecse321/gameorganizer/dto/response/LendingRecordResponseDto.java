package ca.mcgill.ecse321.gameorganizer.dto.response;

import java.util.Date;

/**
 * Comprehensive DTO for lending record responses that includes detailed information 
 * about the associated borrow request, game, and users.
 * Used for presenting detailed lending history information to clients.
 * 
 * @author @YoussGm3o8
 */
public class LendingRecordResponseDto {
    private int id;
    private Date startDate;
    private Date endDate;
    private String status;
    private GameInfo game;
    private UserInfo borrower;
    private UserInfo owner;
    private long durationInDays;
    private boolean isDamaged;
    private String damageNotes;
    private int damageSeverity;
    private Date damageAssessmentDate;

    /**
     * Default constructor required for Jackson deserialization
     */
    public LendingRecordResponseDto() {
        // Required for Jackson deserialization
    }

    /**
     * Constructs a new LendingRecordResponseDto with the specified properties.
     *
     * @param id The unique identifier of the lending record
     * @param startDate The start date of the lending period
     * @param endDate The end date of the lending period
     * @param status The current status of the lending record
     * @param game The game information
     * @param borrower The borrower information
     * @param owner The owner information
     * @param durationInDays The duration of the lending in days
     */
    public LendingRecordResponseDto(int id, Date startDate, Date endDate, String status, 
                                  GameInfo game, UserInfo borrower, UserInfo owner, long durationInDays) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.game = game;
        this.borrower = borrower;
        this.owner = owner;
        this.durationInDays = durationInDays;
        this.isDamaged = false;
        this.damageSeverity = 0;
    }
    
    /**
     * Constructs a new LendingRecordResponseDto with the specified properties including damage information.
     *
     * @param id The unique identifier of the lending record
     * @param startDate The start date of the lending period
     * @param endDate The end date of the lending period
     * @param status The current status of the lending record
     * @param game The game information
     * @param borrower The borrower information
     * @param owner The owner information
     * @param durationInDays The duration of the lending in days
     * @param isDamaged Flag indicating if the game was returned damaged
     * @param damageNotes Notes describing any damage
     * @param damageSeverity The severity of the damage (0-3)
     * @param damageAssessmentDate The date when damage was assessed
     */
    public LendingRecordResponseDto(int id, Date startDate, Date endDate, String status, 
                                  GameInfo game, UserInfo borrower, UserInfo owner, long durationInDays,
                                  boolean isDamaged, String damageNotes, int damageSeverity, Date damageAssessmentDate) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.game = game;
        this.borrower = borrower;
        this.owner = owner;
        this.durationInDays = durationInDays;
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
    public GameInfo getGame() { return game; }
    public UserInfo getBorrower() { return borrower; }
    public UserInfo getOwner() { return owner; }
    public long getDurationInDays() { return durationInDays; }
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
        return DamageSeverityUtils.getLabelForSeverity(damageSeverity);
    }

    /**
     * Utility class for damage severity operations.
     * Provides standardized labels and conversions for damage severity values.
     */
    public static class DamageSeverityUtils {
        /**
         * Gets a human-readable label for a damage severity level.
         * 
         * @param severity The numeric severity (0-3)
         * @return A descriptive label for the severity
         */
        public static String getLabelForSeverity(int severity) {
            switch (severity) {
                case 0: return "None";
                case 1: return "Minor";
                case 2: return "Moderate";
                case 3: return "Severe";
                default: return "Unknown";
            }
        }
    }

    /**
     * Inner class representing game information
     */
    public static class GameInfo {
        private int id;
        private String name;
        private String category;
        private String imageUrl;
        
        /**
         * Default constructor required for Jackson deserialization
         */
        public GameInfo() {
            // Required for Jackson deserialization
        }
        
        public GameInfo(int id, String name, String category, String imageUrl) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.imageUrl = imageUrl;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public String getImageUrl() { return imageUrl; }
    }
    
    /**
     * Inner class representing user information (either borrower or owner)
     */
    public static class UserInfo {
        private int id;
        private String name;
        private String email;
        
        /**
         * Default constructor required for Jackson deserialization
         */
        public UserInfo() {
            // Required for Jackson deserialization
        }
        
        public UserInfo(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
    }
} 