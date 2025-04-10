package ca.mcgill.ecse321.gameorganizer.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UpdateAccountRequest {
    @NotEmpty(message = "Email address is required")
    @Email(message = "Email address is not in a valid format")
    private String email;

    @NotEmpty(message = "Username is required")
    private String username;

    @NotEmpty(message = "Password is required")
    private String password;

    // Optional
    private String newPassword;

}
