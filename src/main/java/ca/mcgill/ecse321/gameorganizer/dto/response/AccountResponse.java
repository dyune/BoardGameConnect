package ca.mcgill.ecse321.gameorganizer.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class AccountResponse {

    private String username;

    private List<EventResponse> events;

    private boolean isGameOwner;

    public AccountResponse(
            String username,
            List<EventResponse> events,
            boolean isGameOwner)
    {
        this.username = username;
        this.events = events;
        this.isGameOwner = isGameOwner;
    }

}
