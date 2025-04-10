/**
 * API Service
 *
 * This file provides API functions for the application.
 * Includes both mock data and real API endpoints.
 */

// Define API_BASE_URL centrally (or import if moved)
// const API_BASE_URL = "http://localhost:8080/"; // Not needed if using apiClient exclusively

import apiClient from './apiClient'; // Import the centralized API client

// === EVENT API FUNCTIONS ===

/**
 * Fetches all events.
 * Requires authentication (via HttpOnly cookie).
 * @returns {Promise<Array>} A promise that resolves to an array of event objects.
 * @throws {UnauthorizedError} If the user is not authenticated.
 * @throws {ApiError} For other API-related errors.
 */
export const getAllEvents = async () => {
  try {
    // Add a response type param to help with large or potentially invalid JSON responses
    const events = await apiClient("/events", {
      method: "GET",
      skipPrefix: false, // Assuming /api/events
      responseType: 'text' // Get as text first to better handle parsing errors
    });
    
    // Try to safely parse the response
    let parsedEvents = [];
    if (typeof events === 'string') {
      try {
        parsedEvents = JSON.parse(events);
      } catch (parseError) {
        console.error("Error parsing events JSON:", parseError);
        console.log("First 100 chars of response:", events.substring(0, 100) + "...");
        throw new Error("Server returned invalid JSON. Please contact the administrator.");
      }
    } else {
      // Already parsed by apiClient
      parsedEvents = events;
    }
    
    return Array.isArray(parsedEvents) ? parsedEvents : [];
  } catch (error) {
    console.error("Failed to fetch events:", error);
    // Return empty array instead of throwing to prevent UI from breaking
    return [];
  }
};

/**
 * Fetches details for a single event by its ID.
 * Requires authentication (via HttpOnly cookie).
 * @param {string} eventId - The UUID of the event.
 * @returns {Promise<object>} A promise that resolves to the event object.
 * @throws {UnauthorizedError} If the user is not authenticated.
 * @throws {ApiError} For other API-related errors.
 */
export const getEventById = async (eventId) => {
  if (!eventId) {
    throw new Error("Event ID is required to fetch event details.");
  }

  try {
    const event = await apiClient(`/events/${eventId}`, {
      method: "GET",
      skipPrefix: false // Assuming /api/events/:id
    });
    return event;
  } catch (error) {
    console.error(`Failed to fetch event with ID ${eventId}:`, error);
    throw error; // Re-throw the specific error from apiClient
  }
};

/**
 * Registers the current authenticated user for an event.
 * Requires authentication (via HttpOnly cookie). Backend identifies attendee from session.
 * @param {string|number} eventId - The UUID or ID of the event to register for.
 * @returns {Promise<object>} A promise that resolves to the created registration object.
 * @throws {UnauthorizedError} If the user is not authenticated.
 * @throws {ForbiddenError} If registration is not allowed (e.g., event full, already registered).
 * @throws {ApiError} For other API-related errors.
 */
export const registerForEvent = async (eventId) => {
  if (!eventId) throw new Error("Event ID is required.");

  // Convert eventId to string if it's a number or other type
  const eventIdString = String(eventId);
  console.log(`[API] Registering for event with ID: ${eventIdString} (original type: ${typeof eventId})`);

  // Payload might only need the eventId if backend identifies user from cookie.
  const payload = {
    eventId: eventIdString
    // attendeeId might not be needed if derived from session on backend
  };

  try {
    const registration = await apiClient("/registrations", {
      method: "POST",
      body: payload, // apiClient handles stringification
      skipPrefix: false, // Assuming /api/registrations
      headers: {
        'Content-Type': 'application/json'
      }
    });
    console.log(`[API] Successfully registered for event ${eventIdString}`, registration);
    return registration; // Return the created registration object
  } catch (error) {
    // Clean up error message if possible
    let cleanedError = error;
    
    if (error.message) {
      // Check for specific known error conditions and clean up the message
      if (error.message.includes("already exists")) {
        console.warn(`[API] User already registered for event ${eventIdString}`);
        cleanedError = new Error("You are already registered for this event");
      } else if (error.message.includes("You cannot register for your own event")) {
        cleanedError = new Error("You cannot register for your own event");
      } else if (error.message.includes("full capacity")) {
        cleanedError = new Error("Event is already at full capacity");
      } else if (error.message.includes("detail")) {
        // Try to extract 'detail' from JSON error response
        try {
          const jsonStart = error.message.indexOf("{");
          if (jsonStart !== -1) {
            const jsonPart = error.message.substring(jsonStart);
            const errorObj = JSON.parse(jsonPart);
            if (errorObj.detail) {
              cleanedError = new Error(errorObj.detail);
            }
          }
        } catch (e) {
          // If parsing fails, keep the original error
          console.log("[API] Error parsing error message JSON:", e);
        }
      }
    }
    
    console.error(`[API] Failed to register for event ${eventIdString}:`, error);
    throw cleanedError; // Throw the cleaned up error
  }
};

/**
 * Creates a new event
 * 
 * @param {Object} eventData - Data for the event
 * @param {string} eventData.title - Event title
 * @param {string} eventData.dateTime - Event date and time (ISO string)
 * @param {number} eventData.maxParticipants - Maximum number of participants
 * @param {string} eventData.location - Event location
 * @param {number|string} eventData.featuredGameId - ID of the featured game
 * @param {number} [eventData.gameInstanceId] - ID of the game instance (for borrowed games)
 * @param {boolean} [eventData.isBorrowed] - Whether the game is borrowed
 * @param {string} [eventData.description] - Event description
 * @returns {Promise<Object>} Created event data
 */
export const createEvent = async (eventData) => {
  if (!eventData.title) {
    throw new Error("Event title is required");
  }
  if (!eventData.dateTime) {
    throw new Error("Event date and time are required");
  }
  if (!eventData.maxParticipants) {
    throw new Error("Maximum participants is required");
  }
  
  // Ensure featuredGameId or featuredGame.id is provided and valid
  if (!eventData.featuredGameId && !(eventData.featuredGame && eventData.featuredGame.id)) {
    throw new Error("A featured game must be selected");
  }
  
  // Get the game ID from either source
  const gameId = eventData.featuredGameId || (eventData.featuredGame && eventData.featuredGame.id);
  
  // Check if gameId is NaN when it's supposed to be a number
  if (typeof gameId === 'number' && isNaN(gameId)) {
    throw new Error("Invalid featured game ID");
  }
  
  console.log("Creating event with data:", JSON.stringify(eventData, null, 2));
  
  try {
    // Ensure featuredGameId is a number or properly formatted string
    let formattedFeaturedGameId = gameId;
    if (typeof formattedFeaturedGameId === 'string' && !isNaN(formattedFeaturedGameId)) {
      formattedFeaturedGameId = parseInt(formattedFeaturedGameId, 10);
    }
    
    // Make sure it's not NaN after conversion
    if (typeof formattedFeaturedGameId === 'number' && isNaN(formattedFeaturedGameId)) {
      throw new Error("Failed to parse featured game ID");
    }
    
    // Build a URL with the game ID as a query parameter
    const endpoint = `/events?featuredGameId=${formattedFeaturedGameId}`;
    
    // Create a new payload with explicit key names that match what the backend expects
    // Try multiple naming variations for the game ID
    const payload = {
      title: eventData.title,
      dateTime: eventData.dateTime,
      maxParticipants: Number(eventData.maxParticipants),
      location: eventData.location || "",
      description: eventData.description || "",
      // Try multiple field name variations for the game ID
      featuredGameId: formattedFeaturedGameId,
      gameId: formattedFeaturedGameId,
      game_id: formattedFeaturedGameId,
      FeaturedGameId: formattedFeaturedGameId,
      GameId: formattedFeaturedGameId,
      featured_game_id: formattedFeaturedGameId,
      "featured-game-id": formattedFeaturedGameId,
      createdBy: localStorage.getItem('userId') || null
    };
    
    // Add gameInstanceId if present and this is a borrowed game
    if (eventData.isBorrowed && eventData.gameInstanceId) {
      payload.gameInstanceId = eventData.gameInstanceId;
      // Also try other naming variants for gameInstanceId
      payload.game_instance_id = eventData.gameInstanceId;
      payload.GameInstanceId = eventData.gameInstanceId;
    }
    
    // Log the final payload being sent to the server
    console.log("Final event payload:", JSON.stringify(payload, null, 2));
    
    let response;
    try {
      // Create a properly formatted request body with featuredGame as an object with ID
      const properPayload = {
        title: payload.title,
        dateTime: payload.dateTime,
        maxParticipants: Number(payload.maxParticipants),
        location: payload.location || "",
        description: payload.description || "",
        featuredGame: {
          id: formattedFeaturedGameId
        },
        // Add gameInstanceId if present for borrowed games
        ...(payload.gameInstanceId && { gameInstanceId: payload.gameInstanceId })
      };
      
      console.log("Properly formatted payload:", JSON.stringify(properPayload, null, 2));
      
      // Remove featuredGameId from URL - put it in the body instead
      response = await apiClient("/events", {
        method: "POST",
        body: properPayload,
        skipPrefix: false
      });
    } catch (apiClientError) {
      console.log("Failed with apiClient, trying direct fetch:", apiClientError);
      
      // Fallback to direct fetch as a backup
      const API_URL = "http://localhost:8080/api"; // Use your actual API URL
      
      // Get auth tokens from localStorage
      const userId = localStorage.getItem('userId');
      const isAuthenticated = localStorage.getItem('isAuthenticated');
      
      // Create headers
      const headers = {
        'Content-Type': 'application/json'
      };
      
      // Add auth headers if available
      if (userId) headers['X-User-Id'] = userId;
      if (isAuthenticated) headers['X-Remember-Me'] = 'true';
      
      // Add a single game ID parameter for simplicity
      const directPayload = {
        title: payload.title,
        dateTime: payload.dateTime,
        maxParticipants: Number(payload.maxParticipants),
        location: payload.location || "",
        description: payload.description || "",
        featuredGame: {
          id: formattedFeaturedGameId
        },
        // Also include gameInstanceId for borrowed games if present
        ...(payload.gameInstanceId && { gameInstanceId: payload.gameInstanceId })
      };
      
      console.log("Trying direct fetch with payload:", JSON.stringify(directPayload, null, 2));
      
      const directResponse = await fetch(`${API_URL}/events`, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(directPayload),
        credentials: 'include'
      });
      
      if (!directResponse.ok) {
        const errorText = await directResponse.text();
        console.error("Direct API call failed:", directResponse.status, errorText);
        throw new Error(`API error: ${errorText}`);
      }
      
      response = await directResponse.json();
    }
    
    console.log("Event created successfully:", response);
    return response;
  } catch (error) {
    console.error("Error creating event:", error);
    
    // Try to extract more information from the error
    console.log("Error details:", error);
    if (error.response) {
      console.log("Error response:", error.response);
    }
    
    // Provide more user-friendly error messages based on error type
    if (error.message && error.message.includes("borrow period")) {
      throw new Error("Event must occur within the borrow period for borrowed games.");
    } else if (error.name === 'UnauthorizedError') {
      throw new Error("You must be logged in to create events.");
    } else if (error.name === 'ForbiddenError') {
      throw new Error("You don't have permission to create events with this game.");
    } else if (error.message && (error.message.includes("Featured game ID") || error.message.includes("invalid") || error.message.includes("featured") || error.message.includes("game"))) {
      throw new Error("Featured game ID is missing or invalid. Please try a different game or contact support.");
    }
    
    throw error;
  }
};

/**
 * Searches for events by title.
 * Requires authentication (via HttpOnly cookie).
 * @param {string} title - The title (or part of it) to search for.
 * @returns {Promise<Array>} A promise that resolves to an array of matching event objects.
 * @throws {UnauthorizedError} If the user is not authenticated.
 * @throws {ApiError} For other API-related errors.
 */
export const searchEventsByTitle = async (title) => {
  try {
    const events = await apiClient(`/events/by-title?title=${encodeURIComponent(title)}`, {
      method: "GET",
      skipPrefix: false // Assuming /api/events/by-title
    });
    return events;
  } catch (error) {
    console.error(`Failed to search events by title "${title}":`, error);
    throw error; // Re-throw the specific error from apiClient
  }
};

/**
 * Fetches all events hosted by a specific user email.
 * Requires authentication.
 * @param {string} hostEmail - The email of the host user.
 * @returns {Promise<Array>} A promise that resolves to an array of event objects.
 * @throws {UnauthorizedError} If the user is not authenticated.
 * @throws {ApiError} For other API-related errors.
 */
export const getEventsByHostEmail = async (hostEmail) => {
  if (!hostEmail) {
     throw new Error("Host email is required to fetch hosted events.");
  }

  try {
    // Using the correct endpoint based on backend API
    const events = await apiClient(`/events/by-host?email=${encodeURIComponent(hostEmail)}`, {
      method: "GET",
      skipPrefix: false, // Assuming /api/events/by-host
      responseType: 'text' // Get as text first to better handle parsing errors
    });
    
    // Try to safely parse the response
    let parsedEvents = [];
    if (typeof events === 'string') {
      try {
        parsedEvents = JSON.parse(events);
      } catch (parseError) {
        console.error(`Error parsing events JSON for host ${hostEmail}:`, parseError);
        console.log("First 100 chars of response:", events.substring(0, 100) + "...");
        // Return empty array instead of throwing
        return [];
      }
    } else {
      // Already parsed by apiClient
      parsedEvents = events;
    }
    
    return Array.isArray(parsedEvents) ? parsedEvents : [];
  } catch (error) {
    console.error(`Failed to fetch events for host ${hostEmail}:`, error);
    // Return empty array instead of throwing to prevent UI from breaking
    return [];
  }
};

/**
 * Unregisters the current authenticated user from an event using the registration ID.
 * Requires authentication (via HttpOnly cookie).
 * @param {string|number} registrationId - The ID of the registration record to delete.
 * @returns {Promise<object|null>} A promise that resolves (often with no content) on success.
 * @throws {UnauthorizedError} If the user is not authenticated.
 * @throws {ForbiddenError} If the user is not allowed to delete this registration.
 * @throws {ApiError} For other API-related errors (e.g., 404 Not Found).
 */
export const unregisterFromEvent = async (registrationId) => {
  if (!registrationId) {
    throw new Error("Registration ID is required to unregister from an event");
  }

  try {
    // DELETE request to remove the registration using apiClient
    const result = await apiClient(`/registrations/${registrationId}`, {
      method: "DELETE",
      skipPrefix: false, // Assuming /api/registrations/:id
      responseType: 'text' // Get as text first to better handle parsing errors
    });
    
    // If the result is a string, try to parse it as JSON
    let parsedResult = result;
    if (typeof result === 'string') {
      try {
        // Only try to parse if it looks like JSON
        if (result.trim().startsWith('{') || result.trim().startsWith('[')) {
          parsedResult = JSON.parse(result);
        }
      } catch (parseError) {
        console.error("Error parsing unregister response:", parseError);
        // Return the original string result on parse error
      }
    }
    
    return parsedResult; // Return the result (parsed or original)
  } catch (error) {
    console.error(`Failed to unregister from event (registration ${registrationId}):`, error);
    throw error; // Re-throw the specific error from apiClient
  }
};

/**
 * Deletes an event by its ID.
 * Requires authentication. Host/Admin privileges likely checked by backend.
 * @param {string|number} eventId - The ID of the event to delete.
 * @returns {Promise<object|string|null>} A promise resolving to success message or null.
 * @throws {UnauthorizedError} If not authenticated.
 * @throws {ForbiddenError} If not allowed to delete.
 * @throws {ApiError} For other errors.
 */
export const deleteEvent = async (eventId) => {
  if (!eventId) {
    throw new Error("Event ID is required for deleteEvent");
  }

  try {
    // Use apiClient for consistency
    const result = await apiClient(`/events/${eventId}`, {
      method: "DELETE",
      skipPrefix: false // Assuming /api/events/:id
      // apiClient handles authentication automatically
    });
    // apiClient might return null or parsed JSON depending on response
    // If backend sends a text message, apiClient might need adjustment or we handle it here
    // For now, just return whatever apiClient gives back on success
    return result;
  } catch (error) {
    console.error(`Error deleting event ${eventId}:`, error);
    // apiClient throws specific error types (UnauthorizedError, ForbiddenError, ApiError)
    throw error; // Re-throw for caller to handle
  }
};

/**
 * Updates an existing event by its ID.
 * Requires authentication. Host/Admin privileges likely checked by backend.
 * @param {string|number} eventId - The ID of the event to update.
 * @param {object} eventData - Updated data for the event.
 * @returns {Promise<object>} A promise that resolves to the updated event object.
 */
export const updateEvent = async (eventId, eventData) => {
  if (!eventId) {
    throw new Error("Event ID is required for updateEvent");
  }
  
  // The backend expects individual URL parameters for each field
  // NOT a JSON body like in the POST/create method
  
  // Prepare URL parameters
  const params = new URLSearchParams();
  
  // Add each parameter if it exists
  if (eventData.title) {
    params.append('title', eventData.title);
  }
  
  if (eventData.dateTime) {
    // Convert JS Date to ISO datetime format (YYYY-MM-DDTHH:MM:SS.sssZ)
    // The backend expects a full ISO datetime string, not just a date
    const date = new Date(eventData.dateTime);
    const formattedDate = date.toISOString(); 
    params.append('dateTime', formattedDate);
  }
  
  if (eventData.location) {
    params.append('location', eventData.location);
  }
  
  if (eventData.description || eventData.description === '') {
    // Allow empty string to clear description
    params.append('description', eventData.description);
  }
  
  // Always send maxParticipants
  const maxParticipants = parseInt(eventData.maxParticipants, 10) || 1;
  params.append('maxParticipants', maxParticipants);
  
  // Note: The featuredGame is not updated through this endpoint based on the 
  // backend controller implementation

  console.log("Update event URL parameters:", params.toString());
  
  try {
    // Make a PUT request with URL parameters instead of a JSON body
    const updatedEvent = await apiClient(`/events/${eventId}?${params.toString()}`, {
      method: "PUT",
      skipPrefix: false,
      // No body for this request
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    });
    
    console.log("Event updated successfully:", updatedEvent);
    return updatedEvent;
  } catch (error) {
    console.error(`Failed to update event ${eventId}:`, error);
    
    // Check for specific error messages from the backend
    if (error.message && error.message.toLowerCase().includes("not found")) {
      throw new Error("Event not found. It may have been deleted.");
    }
    
    // Check for authorization issues
    if (error.message && error.message.toLowerCase().includes("forbidden")) {
      throw new Error("You don't have permission to update this event.");
    }
    
    // Provide a more specific error message
    throw new Error(`Failed to update event: ${error.message || "Unknown error"}`);
  }
};

/**
 * Alternative approach to update an event using POST instead of PUT.
 * Some backends implement updates differently.
 * @param {string|number} eventId - The ID of the event to update.
 * @param {object} eventData - Updated data for the event.
 * @returns {Promise<object>} A promise that resolves to the updated event object.
 */
export const updateEventAlternative = async (eventId, eventData) => {
  if (!eventId) {
    throw new Error("Event ID is required for updateEvent");
  }
  
  // Create a simplified payload that matches what the create function sends
  const payload = {
    title: eventData.title,
    dateTime: new Date(eventData.dateTime).toISOString(),
    location: eventData.location || "",
    description: eventData.description || "",
    maxParticipants: parseInt(eventData.maxParticipants, 10),
    featuredGameId: parseInt(eventData.featuredGameId, 10)
  };

  console.log("Alternative update approach - payload:", JSON.stringify(payload, null, 2));

  try {
    // Try an alternative endpoint pattern that some backends use
    const updatedEvent = await apiClient(`/events/${eventId}/update`, {
      method: "POST", // Using POST instead of PUT
      body: payload,
      skipPrefix: false,
      headers: {
        'Content-Type': 'application/json'
      }
    });
    console.log("Event updated successfully with alternative approach:", updatedEvent);
    return updatedEvent;
  } catch (error) {
    console.error(`Alternative update approach failed for event ${eventId}:`, error);
    
    // Try another endpoint pattern
    try {
      console.log("Trying second alternative endpoint pattern...");
      const updatedEvent = await apiClient(`/events/update/${eventId}`, {
        method: "POST",
        body: payload,
        skipPrefix: false,
        headers: {
          'Content-Type': 'application/json'
        }
      });
      console.log("Event updated successfully with second alternative approach:", updatedEvent);
      return updatedEvent;
    } catch (error2) {
      console.error(`Second alternative update approach also failed:`, error2);
      throw error; // Throw the original error
    }
  }
};

/**
 * Another alternative update approach that focuses on proper UUID handling.
 * Some backends are very strict about UUID format and validation.
 * @param {string} eventId - The UUID of the event to update.
 * @param {object} eventData - The updated event data.
 */
export const updateEventWithValidatedId = async (eventId, eventData) => {
  if (!eventId) {
    throw new Error("Event ID is required");
  }
  
  // Make sure the UUID is properly formatted
  let formattedId = eventId;
  
  // If it doesn't have dashes, try to format it as a standard UUID
  if (eventId.length === 32 && !eventId.includes('-')) {
    formattedId = `${eventId.substring(0, 8)}-${eventId.substring(8, 12)}-${eventId.substring(12, 16)}-${eventId.substring(16, 20)}-${eventId.substring(20)}`;
  }
  
  console.log(`Trying special UUID format: ${formattedId}`);
  
  // Prepare a minimal payload with clean, primitive types
  const payload = {
    id: formattedId, // Include ID in the payload
    title: String(eventData.title || "").trim(),
    dateTime: new Date(eventData.dateTime).toISOString(),
    location: String(eventData.location || "").trim(),
    description: String(eventData.description || "").trim(),
    maxParticipants: Number(eventData.maxParticipants),
    // Just send the raw ID as a number
    featuredGameId: Number(eventData.featuredGameId)
  };
  
  console.log("UUID validation approach payload:", JSON.stringify(payload, null, 2));
  
  try {
    // Try a PATCH request which is sometimes used for partial updates
    const updatedEvent = await apiClient(`/events/${formattedId}`, {
      method: "PATCH",
      body: payload,
      skipPrefix: false,
      headers: {
        'Content-Type': 'application/json'
      }
    });
    console.log("Event updated successfully with UUID validation approach:", updatedEvent);
    return updatedEvent;
  } catch (error) {
    console.error(`UUID validation approach failed:`, error);
    
    // Try using POST to the base /events endpoint with ID in the payload
    try {
      console.log("Trying POST to base endpoint with ID in payload");
      const updatedEvent = await apiClient(`/events`, {
        method: "POST",
        body: payload,
        skipPrefix: false,
        headers: {
          'Content-Type': 'application/json',
          'X-HTTP-Method-Override': 'PUT' // Some backends use this header
        }
      });
      console.log("Event updated successfully with POST+header approach:", updatedEvent);
      return updatedEvent;
    } catch (error2) {
      console.error("POST+header approach also failed:", error2);
      throw error; // Throw the original error
    }
  }
};

/**
 * Special function that allows updating an event INCLUDING the featured game
 * by deleting and recreating the event with the new data.
 * This works around the backend limitation where the PUT endpoint doesn't allow changing the game.
 * 
 * @param {string} eventId - The ID of the event to update
 * @param {object} eventData - The updated event data including featuredGameId
 * @returns {Promise<object>} The newly created event that replaces the old one
 */
export const updateEventWithGameChange = async (eventId, eventData) => {
  if (!eventId) {
    throw new Error("Event ID is required");
  }
  
  if (!eventData.featuredGameId) {
    throw new Error("Featured Game ID is required for event update with game change");
  }
  
  try {
    // 1. Get the existing event with all its details
    console.log("Getting existing event details for reconstruction");
    const existingEvent = await getEventById(eventId);
    
    if (!existingEvent) {
      throw new Error("Event not found");
    }
    
    // 2. Prepare the create payload, using existing data as fallback
    const createPayload = {
      title: eventData.title || existingEvent.title,
      dateTime: eventData.dateTime || existingEvent.dateTime,
      location: eventData.location || existingEvent.location,
      description: eventData.description || existingEvent.description,
      maxParticipants: parseInt(eventData.maxParticipants || existingEvent.maxParticipants, 10),
      featuredGameId: parseInt(eventData.featuredGameId, 10)
    };
    
    console.log("Recreating event with new game - payload:", JSON.stringify(createPayload, null, 2));

    // 3. Create the new event first so we don't lose data if creation fails
    const newEvent = await createEvent(createPayload);
    
    if (!newEvent || !newEvent.eventId) {
      throw new Error("Failed to create replacement event");
    }
    
    console.log("Successfully created replacement event:", newEvent);
    
    // 4. Delete the old event only after successful creation
    await deleteEvent(eventId);
    console.log("Successfully deleted original event:", eventId);
    
    // 5. Return the newly created event
    return newEvent;
  } catch (error) {
    console.error("Failed to update event with game change:", error);
    throw new Error(`Failed to update event with game change: ${error.message}`);
  }
};
