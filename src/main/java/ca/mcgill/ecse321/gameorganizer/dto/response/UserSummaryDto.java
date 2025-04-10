package ca.mcgill.ecse321.gameorganizer.dto.response;

import java.util.List;
import java.util.ArrayList;

public class UserSummaryDto {
    private int id; // Changed type from Long to int
    private String name; // Changed field from username to name
    private String email; // Added email field
    private boolean gameOwner; // Added gameOwner field
    private List<RegistrationResponseDto> events; // Added events list
    // Consider adding 'name' if it exists in Account and is needed

    // Default constructor for frameworks
    public UserSummaryDto() {
        this.events = new ArrayList<>();
    }

    public UserSummaryDto(int id, String name) { // Updated constructor signature
        this.id = id;
        this.name = name; // Updated assignment
        this.gameOwner = false; // Default to false
        this.events = new ArrayList<>();
    }
    
    // New constructor with email
    public UserSummaryDto(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.gameOwner = false; // Default to false
        this.events = new ArrayList<>();
    }
    
    // New constructor with all fields including gameOwner
    public UserSummaryDto(int id, String name, String email, boolean gameOwner) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.gameOwner = gameOwner;
        this.events = new ArrayList<>();
    }

    // Getters
    public int getId() { // Changed return type
        return id;
    }

    public String getName() { // Changed method name and getter target
        return name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public boolean isGameOwner() {
        return gameOwner;
    }
    
    public List<RegistrationResponseDto> getEvents() {
        return events;
    }

    // Setters (optional, depending on usage)
    public void setId(int id) { // Changed parameter type
        this.id = id;
    }

    public void setName(String name) { // Changed method name and parameter
        this.name = name; // Updated assignment
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public void setGameOwner(boolean gameOwner) {
        this.gameOwner = gameOwner;
    }
    
    public void setEvents(List<RegistrationResponseDto> events) {
        this.events = events;
    }
}