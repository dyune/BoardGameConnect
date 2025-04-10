package ca.mcgill.ecse321.gameorganizer.controllers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ca.mcgill.ecse321.gameorganizer.dto.request.RegistrationRequestDto;
import ca.mcgill.ecse321.gameorganizer.dto.response.RegistrationResponseDto;
import ca.mcgill.ecse321.gameorganizer.exceptions.ForbiddenException;
import ca.mcgill.ecse321.gameorganizer.exceptions.ResourceNotFoundException;
import ca.mcgill.ecse321.gameorganizer.exceptions.UnauthedException;
import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.Event;
import ca.mcgill.ecse321.gameorganizer.models.Registration;
import ca.mcgill.ecse321.gameorganizer.services.AccountService;
import ca.mcgill.ecse321.gameorganizer.services.EventService;
import ca.mcgill.ecse321.gameorganizer.services.RegistrationService;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private EventService eventService;

    /**
     * Creates a new registration.
     */
    @PostMapping
    public ResponseEntity<RegistrationResponseDto> createRegistration(@RequestBody RegistrationRequestDto dto) {
        try {
            // Attendee is now determined by the service based on authentication context
            // Account attendee = accountService.getAccountById(dto.getAttendeeId()); // Removed
            Event event = eventService.getEventById(dto.getEventId()); // Still need the event

            // Call service without attendee parameter
            Registration registration = registrationService.createRegistration(dto.getRegistrationDate(), event);
            return new ResponseEntity<>(new RegistrationResponseDto(registration), HttpStatus.CREATED);
        } catch (ForbiddenException | UnauthedException e) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage()); // Or UNAUTHORIZED
        } catch (ResourceNotFoundException e) { // Catch if event not found
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) { // Catch validation errors (e.g., already registered, event full)
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Retrieves a registration by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RegistrationResponseDto> getRegistration(@PathVariable int id) {
        Optional<Registration> registration = registrationService.getRegistration(id);
        return registration.map(value -> ResponseEntity.ok(new RegistrationResponseDto(value)))
                           .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all registrations.
     */
    @GetMapping
    public ResponseEntity<List<RegistrationResponseDto>> getAllRegistrations() {
        List<RegistrationResponseDto> registrations = ((List<Registration>) registrationService.getAllRegistrations())
                .stream()
                .map(RegistrationResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(registrations);
    }

    // Changed path to avoid ambiguity with getRegistration by ID
    @GetMapping("/user/{email}")
    public ResponseEntity<List<RegistrationResponseDto>> getAllRegistrationsByUserEmail(@PathVariable String email) {
        List<RegistrationResponseDto> response = registrationService.getAllRegistrationsByUserEmail(email);
        return ResponseEntity.ok(response);
    }
    /**
     * Updates an existing registration.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RegistrationResponseDto> updateRegistration(@PathVariable int id,
                                                                       @RequestBody RegistrationRequestDto dto) {
       try {
            // Service now only allows updating limited fields (e.g., date) and handles auth.
            // We don't need to fetch attendee or event here.
            // Account attendee = accountService.getAccountById(dto.getAttendeeId()); // Removed
            // Event event = eventService.getEventById(dto.getEventId()); // Removed

            // Call service only with ID and potentially updatable fields (just date for now)
            Registration updatedRegistration = registrationService.updateRegistration(id, dto.getRegistrationDate());
            return ResponseEntity.ok(new RegistrationResponseDto(updatedRegistration));
        } catch (ForbiddenException | UnauthedException e) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage()); // Or UNAUTHORIZED
        } catch (ResourceNotFoundException e) {
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Deletes a registration by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRegistration(@PathVariable int id) {
        try {
            // Service now handles auth check and finding the registration
            registrationService.deleteRegistration(id);
            return ResponseEntity.ok("Registration deleted successfully.");
        } catch (ForbiddenException | UnauthedException e) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage()); // Or UNAUTHORIZED
        } catch (ResourceNotFoundException e) {
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
