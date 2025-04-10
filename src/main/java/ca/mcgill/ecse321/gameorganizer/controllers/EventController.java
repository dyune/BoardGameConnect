package ca.mcgill.ecse321.gameorganizer.controllers;

// Keep most imports, but change Date import if needed
import java.util.Date; // Changed from java.sql.Date
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import ca.mcgill.ecse321.gameorganizer.dto.request.CreateEventRequest;
import ca.mcgill.ecse321.gameorganizer.dto.response.EventResponse;
import ca.mcgill.ecse321.gameorganizer.exceptions.ForbiddenException;
import ca.mcgill.ecse321.gameorganizer.exceptions.UnauthedException;
import ca.mcgill.ecse321.gameorganizer.models.Event;
import ca.mcgill.ecse321.gameorganizer.services.EventService;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    @Autowired
    private EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        log.info("Received request to get all events");
        List<Event> events = eventService.getAllEvents();
        List<EventResponse> eventResponses = events.stream()
            .map(EventResponse::new)
            .collect(Collectors.toList());
        log.info("Returning {} events", eventResponses.size());
        return ResponseEntity.ok(eventResponses);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable UUID eventId) {
        log.info("Received request to get event by ID: {}", eventId);
        Event event = eventService.getEventById(eventId);
        log.info("Returning event: {}", event.getTitle());
        return ResponseEntity.ok(new EventResponse(event));
    }
    
    @GetMapping("/by-host")
    public ResponseEntity<List<EventResponse>> getEventsByHostEmail(@RequestParam String email) {
        log.info("Received request to get events by host email: {}", email);
        List<Event> events = eventService.getEventsByHostEmail(email);
        List<EventResponse> eventResponses = events.stream()
            .map(EventResponse::new)
            .collect(Collectors.toList());
        log.info("Returning {} events hosted by {}", eventResponses.size(), email);
        return ResponseEntity.ok(eventResponses);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()") // Ensure user is logged in to create event
    public ResponseEntity<EventResponse> createEvent(@RequestBody CreateEventRequest request) { 
        log.info("Received request to create event: {}", request.getTitle());
        // The service now accepts java.util.Date directly from the DTO
        Event event = eventService.createEvent(request);
        log.info("Successfully created event with ID: {}", event.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new EventResponse(event));
    }

    @PutMapping("/{eventId}")
    @PreAuthorize("@eventService.isHost(#eventId, authentication.principal.username)") // Ensure only host can update
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable UUID eventId,
            @RequestParam(required = false) String title,
            // Use @DateTimeFormat to help Spring parse the string from the request param into java.util.Date
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) Date dateTime, 
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "0") int maxParticipants) { 
        
        log.info("Received request to update event ID: {}", eventId);
        
        // Validate maxParticipants if provided
        if (maxParticipants < 0) {
            log.error("Error updating event {}: maxParticipants cannot be negative", eventId);
            throw new IllegalArgumentException("maxParticipants cannot be negative");
        }
        
        try {
            // Pass java.util.Date to the service
            Event updatedEvent = eventService.updateEvent(
                   eventId, title, dateTime, location, description, maxParticipants);
            log.info("Successfully updated event ID: {}", eventId);
            return ResponseEntity.ok(new EventResponse(updatedEvent));
        } catch (IllegalArgumentException e) {
            log.error("Error updating event {}: {}", eventId, e.getMessage());
            throw e; 
        } catch (ForbiddenException e) {
            log.error("Authorization error updating event {}: {}", eventId, e.getMessage());
            throw e; 
        } catch (UnauthedException e) { // Should not happen due to @PreAuthorize, but good practice
            log.error("Authentication error updating event {}: {}", eventId, e.getMessage());
            throw e; 
        }
    }

    @DeleteMapping("/{eventId}")
    @PreAuthorize("@eventService.isHost(#eventId, authentication.principal.username)") // Ensure only host can delete
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId) {
        log.info("Received request to delete event ID: {}", eventId);
        try {
            eventService.deleteEvent(eventId);
            log.info("Successfully deleted event ID: {}", eventId);
            return ResponseEntity.noContent().build(); // Standard practice for successful DELETE
        } catch (IllegalArgumentException e) {
            log.error("Error deleting event {}: {}", eventId, e.getMessage());
            throw e; // Let global handler manage response
        } catch (ForbiddenException e) {
            log.error("Authorization error deleting event {}: {}", eventId, e.getMessage());
            throw e; // Let global handler manage response
        }
    }

    @GetMapping("/by-date")
    public ResponseEntity<List<EventResponse>> getEventsByDate(
        // Use @DateTimeFormat to parse the date string correctly into java.util.Date
        @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) Date date) {
        log.info("Received request to get events by date: {}", date);
        List<Event> events = eventService.findEventsByDate(date); // Service now handles java.util.Date
        List<EventResponse> eventResponses = events.stream()
            .map(EventResponse::new)
            .collect(Collectors.toList());
        log.info("Returning {} events for date {}", eventResponses.size(), date);
        return ResponseEntity.ok(eventResponses);
    }
    
    @GetMapping("/by-game-name")
    public ResponseEntity<List<EventResponse>> getEventsByGameName(@RequestParam String gameName) {
        log.info("Received request to get events by game name: {}", gameName);
        List<Event> events = eventService.findEventsByGameName(gameName);
        List<EventResponse> eventResponses = events.stream()
            .map(EventResponse::new)
            .collect(Collectors.toList());
        log.info("Returning {} events for game name {}", eventResponses.size(), gameName);
        return ResponseEntity.ok(eventResponses);
    }
    
    @GetMapping("/auth-test")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> authTest() {
        log.info("Received request to auth-test endpoint");
        return ResponseEntity.ok("Authentication test successful.");
    }
    
    @GetMapping("/auth-debug")
    public ResponseEntity<String> authDebug() {
        log.info("Received request to auth-debug endpoint");
        String authInfo = "Authentication present: " + 
            (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null);
        
        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
            authInfo += ", Username: " + 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName() +
                ", Authorities: " + 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        }
        
        return ResponseEntity.ok(authInfo);
    }
}
