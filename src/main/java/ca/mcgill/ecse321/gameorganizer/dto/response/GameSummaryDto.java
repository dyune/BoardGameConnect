package ca.mcgill.ecse321.gameorganizer.dto.response;

public class GameSummaryDto {
    private int id; // Changed type from Integer to int
    private String name;
    private String image; // Changed field from coverLink to image

    // Default constructor for frameworks
    public GameSummaryDto() {
    }

    public GameSummaryDto(int id, String name, String image) { // Updated constructor signature
        this.id = id;
        this.name = name;
        this.image = image; // Updated assignment
    }

    // Getters
    public int getId() { // Changed return type
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImage() { // Changed method name and getter target
        return image;
    }

    // Setters (optional, depending on usage)
    public void setId(int id) { // Changed parameter type
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImage(String image) { // Changed method name and parameter
        this.image = image; // Updated assignment
    }
}