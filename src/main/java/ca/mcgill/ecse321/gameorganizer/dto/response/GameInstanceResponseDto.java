package ca.mcgill.ecse321.gameorganizer.dto.response;

import java.util.Date;

/**
 * Data Transfer Object for Game Instance responses in the API.
 * Represents a physical copy of a game that can be borrowed.
 */
public class GameInstanceResponseDto {
    private int id;
    private int gameId;
    private String gameName;
    private String gameImage; // Add field for game image
    private String condition;
    private boolean available;
    private String location;
    private String name;
    private Date acquiredDate;
    private AccountDto owner;

    /**
     * Default constructor required for Jackson serialization/deserialization
     */
    public GameInstanceResponseDto() {
    }

    /**
     * Constructor with all fields for creating DTOs programmatically
     */
    public GameInstanceResponseDto(int id, int gameId, String gameName, String condition, 
                                  boolean available, String location, String name, Date acquiredDate, 
                                  AccountDto owner) {
        this.id = id;
        this.gameId = gameId;
        this.gameName = gameName;
        // No gameImage in this constructor, maybe add later if needed
        this.condition = condition;
        this.available = available;
        this.location = location;
        this.name = name;
        this.acquiredDate = acquiredDate;
        this.owner = owner;
    }

    /**
     * Constructs a DTO from a GameInstance entity
     * @param instance The GameInstance entity
     */
    public GameInstanceResponseDto(ca.mcgill.ecse321.gameorganizer.models.GameInstance instance) {
        this.id = instance.getId();
        this.gameId = instance.getGame().getId();
        this.gameName = instance.getGame().getName();
        this.gameImage = instance.getGame().getImage(); // Get image from parent Game
        this.condition = instance.getCondition();
        this.available = instance.isAvailable();
        this.location = instance.getLocation();
        this.name = instance.getName();
        this.acquiredDate = instance.getAcquiredDate();
        
        // Create owner DTO
        ca.mcgill.ecse321.gameorganizer.models.GameOwner gameOwner = instance.getOwner();
        this.owner = new AccountDto(
            gameOwner.getId(),
            gameOwner.getName(),
            gameOwner.getEmail()
        );
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getGameImage() { // Add getter for gameImage
        return gameImage;
    }

    public void setGameImage(String gameImage) { // Add setter for gameImage
        this.gameImage = gameImage;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getAcquiredDate() {
        return acquiredDate;
    }

    public void setAcquiredDate(Date acquiredDate) {
        this.acquiredDate = acquiredDate;
    }

    public AccountDto getOwner() {
        return owner;
    }

    public void setOwner(AccountDto owner) {
        this.owner = owner;
    }

    /**
     * Nested DTO for Account information
     */
    public static class AccountDto {
        private int id;
        private String name;
        private String email;

        // Default constructor
        public AccountDto() {}

        public AccountDto(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
