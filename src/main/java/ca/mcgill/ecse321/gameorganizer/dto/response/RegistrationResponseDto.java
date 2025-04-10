package ca.mcgill.ecse321.gameorganizer.dto.response;

import java.util.Date;
import java.util.UUID;

import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.Event;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.Registration;
import lombok.Getter;
import lombok.Data;

/**
 * DTO for returning registration details.
 */
@Data
public class RegistrationResponseDto {
    private int id;
    private Date registrationDate;
    private int attendeeId;
    private String attendeeName;
    private UUID eventId;
    private EventDto event;

    public RegistrationResponseDto(Registration registration) {
        this.id = registration.getId();
        this.registrationDate = registration.getRegistrationDate();
        
        // Extract attendee info safely
        if (registration.getAttendee() != null) {
            this.attendeeId = registration.getAttendee().getId();
            this.attendeeName = registration.getAttendee().getName();
        }
        
        // Extract event info safely
        if (registration.getEventRegisteredFor() != null) {
            Event eventEntity = registration.getEventRegisteredFor();
            this.eventId = eventEntity.getId();
            this.event = new EventDto(eventEntity);
        }
    }
    
    // Inner DTO class for Event to avoid circular references
    @Data
    public static class EventDto {
        private UUID eventId;
        private String title;
        private Date dateTime;
        private String location;
        private String description;
        private int currentNumberParticipants;
        private int maxParticipants;
        private GameDto featuredGame;
        private AccountDto host;
        
        public EventDto(Event event) {
            this.eventId = event.getId();
            this.title = event.getTitle();
            this.dateTime = event.getDateTime();
            this.location = event.getLocation();
            this.description = event.getDescription();
            this.currentNumberParticipants = event.getCurrentNumberParticipants();
            this.maxParticipants = event.getMaxParticipants();
            
            if (event.getFeaturedGame() != null) {
                this.featuredGame = new GameDto(event.getFeaturedGame());
            }
            
            if (event.getHost() != null) {
                this.host = new AccountDto(event.getHost());
            }
        }
    }
    
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
