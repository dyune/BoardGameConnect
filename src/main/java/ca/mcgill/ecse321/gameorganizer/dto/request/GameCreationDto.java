package ca.mcgill.ecse321.gameorganizer.dto.request;

/**
 * DTO for game creation and update requests
 */
public class GameCreationDto {
    private String name;
    private int minPlayers;
    private int maxPlayers;
    private String image;
    private String category;
    private String description; // Add description field
    private String ownerId;
    
    // Instance-specific fields
    private String condition;
    private String location;
    private String instanceName; // Optional name for the game instance

    // Default constructor
    public GameCreationDto() {
    }

    // Constructor with all fields
    public GameCreationDto(String name, int minPlayers, int maxPlayers, String image, String category, String ownerId,
                          String condition, String location) {
        this.name = name;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.image = image;
        this.category = category;
        this.ownerId = ownerId;
        this.condition = condition;
        this.location = location;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
    
    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    // Getter and Setter for description
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
