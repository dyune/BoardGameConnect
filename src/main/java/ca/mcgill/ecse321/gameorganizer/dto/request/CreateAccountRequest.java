package ca.mcgill.ecse321.gameorganizer.dto.request;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

@Data
public class CreateAccountRequest {

    @NotEmpty(message = "Email address is required")
    @Email(message = "Email address is not in a valid format")
    private String email;

    @NotEmpty(message = "Username is required")
    private String username;

    @NotEmpty(message = "Password is required")
    private String password;

    private boolean gameOwner;

    public CreateAccountRequest(String email, String username, String password, boolean gameOwner) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.gameOwner = gameOwner;
    }

    // Default constructor needed for JSON deserialization
    public CreateAccountRequest() {
        this.gameOwner = false;
    }
}
