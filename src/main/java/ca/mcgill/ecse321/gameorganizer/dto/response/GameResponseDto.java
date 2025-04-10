package ca.mcgill.ecse321.gameorganizer.dto.response;

import java.util.Date;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;

/**
 * Data Transfer Object for Game responses in the API
 */

public class GameResponseDto {
    private int id;
    private String name;
    private int minPlayers;
    private int maxPlayers;
    private String image;
    private Date dateAdded;
    private String category;
    private String description; // Add description field
    private AccountDto owner;

    // Default constructor
    public GameResponseDto() {}

    /**
     * Constructs a GameResponseDto from a Game entity
     *
     * @param game The game entity to convert to DTO
     */
    public GameResponseDto(Game game) {
        this.id = game.getId();
        this.name = game.getName();
        this.minPlayers = game.getMinPlayers();
        this.maxPlayers = game.getMaxPlayers();
        this.image = game.getImage();
        this.dateAdded = game.getDateAdded();
        this.category = game.getCategory();
        this.description = game.getDescription(); // Map description from Game entity
        if (game.getOwner() != null) {
            this.owner = new AccountDto(game.getOwner());
        }
    }

    // Getters and setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getMinPlayers() { return minPlayers; }
    public void setMinPlayers(int minPlayers) { this.minPlayers = minPlayers; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public Date getDateAdded() { return dateAdded; }
    public void setDateAdded(Date dateAdded) { this.dateAdded = dateAdded; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; } // Add getter
    public void setDescription(String description) { this.description = description; } // Add setter

    public AccountDto getOwner() { return owner; }
    public void setOwner(AccountDto owner) { this.owner = owner; }

    /**
     * Nested DTO for Account information
     */
    public static class AccountDto {
        private int id;
        private String name;
        private String email;

        // Default constructor
        public AccountDto() {}

        public AccountDto(GameOwner owner) {
            this.id = owner.getId();
            this.name = owner.getName();
            this.email = owner.getEmail();
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
