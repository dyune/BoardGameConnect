package ca.mcgill.ecse321.gameorganizer.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;

import ca.mcgill.ecse321.gameorganizer.dto.response.RegistrationResponseDto;
import ca.mcgill.ecse321.gameorganizer.exceptions.ForbiddenException;
import ca.mcgill.ecse321.gameorganizer.exceptions.ResourceNotFoundException;
import ca.mcgill.ecse321.gameorganizer.exceptions.UnauthedException;
import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.Event;
import ca.mcgill.ecse321.gameorganizer.models.Registration;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.EventRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.RegistrationRepository;

/**
 * Service class that handles business logic for event registration operations.
 * Provides methods for creating, retrieving, updating, and deleting event registrations.
 * 
 * @author @Shine111111
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class); // Add logger instance
    private final RegistrationRepository registrationRepository;
    private final AccountRepository accountRepository;
    private final EventRepository eventRepository; // Add EventRepository field

    @Autowired
    public RegistrationService(RegistrationRepository registrationRepository, AccountRepository accountRepository, EventRepository eventRepository) { // Inject EventRepository
        this.registrationRepository = registrationRepository;
        this.accountRepository = accountRepository;
        this.eventRepository = eventRepository; // Assign injected repository
    }

    /**
     * Creates a new registration for an event.
     *
     * @param registrationDate The date of registration
     * @param attendee The account of the person registering
     * @param eventRegisteredFor The event being registered for
     * @return The created Registration object
     */
    @Transactional
    @PreAuthorize("isAuthenticated()") // Ensure user is logged in
    public Registration createRegistration(Date registrationDate, Event eventRegisteredFor) { // Removed attendee parameter
        try {
            // Get attendee from authenticated context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String attendeeEmail = authentication.getName();
            Account attendee = accountRepository.findByEmail(attendeeEmail)
                    .orElseThrow(() -> new UnauthedException("Authenticated attendee account not found in database."));

            // Check if the user is trying to register for their own event
            if (eventRegisteredFor.getHost() != null && 
                eventRegisteredFor.getHost().getEmail().equals(attendeeEmail)) {
                throw new IllegalArgumentException("You cannot register for your own event.");
            }

            Registration registration = new Registration(registrationDate);
            if (registrationRepository.existsByAttendeeAndEventRegisteredFor(attendee, eventRegisteredFor)) {
                throw new IllegalArgumentException("Registration already exists for this account and event.");
            }
            if (eventRegisteredFor.getCurrentNumberParticipants() >= eventRegisteredFor.getMaxParticipants()) {
                throw new IllegalArgumentException("Event is already at full capacity.");
            }
            registration.setAttendee(attendee); // Set attendee from context
            eventRegisteredFor.setCurrentNumberParticipants(eventRegisteredFor.getCurrentNumberParticipants() + 1);
            registration.setEventRegisteredFor(eventRegisteredFor);
            return registrationRepository.save(registration);
            
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (org.springframework.security.access.AccessDeniedException e) {
             throw new ForbiddenException("Authentication required to register for an event.");
        } catch (UnauthedException e) {
             throw e; // Handle case where authenticated user somehow isn't in DB
        } catch (Exception e) {
             // Log unexpected errors
             throw new RuntimeException("An unexpected error occurred while creating the registration.", e);
        }
    }

    /**
     * Retrieves a registration by its ID.
     *
     * @param id The ID of the registration to retrieve
     * @return Optional containing the Registration if found
     */
    public Optional<Registration> getRegistration(int id) {
        return registrationRepository.findRegistrationById(id);
    }

    /**
     * Retrieves all registrations in the system.
     *
     * @return Iterable of all Registration objects
     */
    public Iterable<Registration> getAllRegistrations() {
        return registrationRepository.findAll();
    }
    /**
     * Retrieves all registrations in the system for a selected user.
     * @param email the user's email
     *
     * @return List of all RegistrationResponseDTO objects
     */
    public List<RegistrationResponseDto> getAllRegistrationsByUserEmail(String email) {
        List<Registration> registrations = registrationRepository.findRegistrationByAttendeeEmail(email);
        List<RegistrationResponseDto> response = new ArrayList<>();
        for (Registration registration : registrations) {
            response.add(new RegistrationResponseDto(registration));
        }
        return response;
    }
    /**
     * Updates an existing registration.
     *
     * @param id The ID of the registration to update
     * @param registrationDate The new registration date
     * @param attendee The new attendee account
     * @param eventRegisteredFor The new event
     * @return The updated Registration object
     * @throws IllegalArgumentException if the registration is not found
     */
    @Transactional
    @PreAuthorize("@registrationService.isAttendee(#id, authentication.principal.username)")
    public Registration updateRegistration(int id, Date registrationDate) { // Removed attendee and event parameters
        try {
            Registration registration = registrationRepository.findRegistrationById(id)
                 .orElseThrow(() -> new ResourceNotFoundException("Registration with id " + id + " not found"));

            // Authorization handled by @PreAuthorize

            // Update only allowed fields (e.g., registrationDate)
            // Preventing updates to attendee or eventRegisteredFor as it doesn't make logical sense
            // and was previously insecure.
            if (registrationDate != null) {
                 registration.setRegistrationDate(registrationDate);
            } else {
                 throw new IllegalArgumentException("Registration date cannot be null for update.");
            }
            
            // If other fields were meant to be updatable, add logic here, but changing attendee/event is disallowed.

            return registrationRepository.save(registration);
            
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            throw e; // Re-throw validation/not found errors
        } catch (org.springframework.security.access.AccessDeniedException e) {
             throw new ForbiddenException("Access denied: You can only update your own registration.");
        } catch (UnauthedException e) {
             // Handle case where authenticated user somehow isn't in DB (shouldn't happen)
             throw e;
        } catch (Exception e) {
             // Log unexpected errors
             throw new RuntimeException("An unexpected error occurred while updating the registration.", e);
        } // End catch (Exception e)
    } // End try block

    /**
     * Deletes a registration by its ID and updates the event's participant count.
     *
     * @param id The ID of the registration to delete
     * @param event The event associated with the registration
     * @throws IllegalArgumentException if the registration or event is not valid
     */
    @Transactional
    @PreAuthorize("@registrationService.isAttendee(#id, authentication.principal.username)")
    public void deleteRegistration(int id) {
        try {
            Registration registration = registrationRepository.findRegistrationById(id)
                 .orElseThrow(() -> new ResourceNotFoundException("Registration with id " + id + " not found"));

            // Authorization handled by @PreAuthorize
            
            // Decrement participant count before deleting
            Event event = registration.getEventRegisteredFor();
            if (event != null) {
                int currentCount = event.getCurrentNumberParticipants();
                if (currentCount > 0) { // Prevent going below zero
                    event.setCurrentNumberParticipants(currentCount - 1);
                    eventRepository.save(event); // Save the updated event
                } else {
                    // Log a warning if count is already zero
                    log.warn("Attempted to decrement participant count for event {} which was already zero.", event.getId());
                }
            } else {
                log.warn("Registration with ID {} did not have an associated event.", id);
            }

            registrationRepository.deleteById(id);
            
        } catch (ResourceNotFoundException e) {
            throw e; // Re-throw not found error
        } catch (org.springframework.security.access.AccessDeniedException e) {
             throw new ForbiddenException("Access denied: You can only delete your own registration.");
        } catch (UnauthedException e) {
             // Handle case where authenticated user somehow isn't in DB
             throw e;
        } catch (Exception e) {
             // Log unexpected errors
             throw new RuntimeException("An unexpected error occurred while deleting the registration.", e);
        }
    }
 
    // --- Helper methods for @PreAuthorize --- 

    /**
     * Checks if the given username corresponds to the attendee of the registration.
     */
    public boolean isAttendee(int registrationId, String username) {
        if (username == null) return false;
        try {
            Registration registration = registrationRepository.findRegistrationById(registrationId).orElse(null);
            Account user = accountRepository.findByEmail(username).orElse(null);

            if (registration == null || user == null || registration.getAttendee() == null) {
                return false; // Cannot determine attendee
            }

            return registration.getAttendee().getId() == user.getId();
        } catch (Exception e) {
            // Log error
            return false; // Deny on error
        }
    }
}