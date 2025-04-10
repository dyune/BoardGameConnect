package ca.mcgill.ecse321.gameorganizer.dto.request;

import java.util.Date;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for creating or updating a Registration.
 */
@Getter
@Setter
public class RegistrationRequestDto {
    private Date registrationDate;
    private int attendeeId;
    private UUID eventId;
}
