package ca.mcgill.ecse321.gameorganizer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor // Add a no-args constructor for Jackson
public class LoginResponse {
    private Integer userId; // Remove 'final' to allow Jackson to set the field
    private String email;   // Remove 'final' to allow Jackson to set the field
}
