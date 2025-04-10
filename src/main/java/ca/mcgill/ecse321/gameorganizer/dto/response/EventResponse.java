package ca.mcgill.ecse321.gameorganizer.dto.response;

import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.Event;
import ca.mcgill.ecse321.gameorganizer.models.Game;

import java.util.Date; // Changed from java.sql.Date
import java.util.UUID;
import lombok.Data;

@Data
public class EventResponse {

    public EventResponse(Event event) {
        this.eventId = event.getId();
        this.title = event.getTitle();
        this.dateTime = event.getDateTime(); // Use java.util.Date directly
        this.location = event.getLocation();
        this.description = event.getDescription();
        this.currentNumberParticipants = event.getCurrentNumberParticipants();
        this.maxParticipants = event.getMaxParticipants();
        
        // Create simplified DTO for nested objects to prevent circular references
        if (event.getFeaturedGame() != null) {
            this.featuredGame = new GameDto(event.getFeaturedGame());
        }
        
        if (event.getHost() != null) {
            this.host = new AccountDto(event.getHost());
        }
    }

    private UUID eventId;
    private String title;
    private Date dateTime; // Changed from java.sql.Date
    private String location;
    private String description;
    private int currentNumberParticipants;
    private int maxParticipants;
    private GameDto featuredGame;
    private AccountDto host;
    
    // Inner DTO class for Game to avoid circular references
    @Data
    public static class GameDto {
        private int id;
        private String name;
        private String image;
        
        public GameDto(Game game) {
            this.id = game.getId();
            this.name = game.getName();
            this.image = game.getImage();
        }
    }
    
    // Inner DTO class for Account to avoid circular references
    @Data
    public static class AccountDto {
        private int id;
        private String name;
        private String email;
        
        public AccountDto(Account account) {
            this.id = account.getId();
            this.name = account.getName();
            this.email = account.getEmail();
        }
    }
}