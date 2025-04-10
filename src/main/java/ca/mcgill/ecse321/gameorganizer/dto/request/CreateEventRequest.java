package ca.mcgill.ecse321.gameorganizer.dto.request;

import java.util.Date; // Changed from java.sql.Date

import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import lombok.Data;

@Data
public class CreateEventRequest {
    private String title;
    private Date dateTime; // Changed from java.sql.Date
    private String location;
    private String description;
    private int maxParticipants;
    private Game featuredGame; // Keep as Game object for ID
    private Account host; // Keep as Account object for ID
    private Integer gameInstanceId; // Added for borrowed games

    // Remove the problematic setDateTime method
    // public void setDateTime(java.util.Date date) {
    //     if (date == null) {
    //         this.dateTime = null;
    //     } else {
    //         // THIS IS THE PROBLEM - converting to java.sql.Date loses time
    //         // this.dateTime = new java.sql.Date(date.getTime()); 
    //         this.dateTime = date; // Keep as java.util.Date
    //     }
    // }
}