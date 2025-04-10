import apiClient, { UnauthorizedError } from './apiClient'; // Import the centralized API client and UnauthorizedError

/**
 * Fetches all event registrations for a given user email.
 * Requires authentication (via HttpOnly cookie).
 * @param {string} email - The email of the user.
 * @param {number} [retryCount=0] - Number of times this request has been retried
 * @returns {Promise<Array>} A promise that resolves to an array of registration objects.
 * @throws {UnauthorizedError} If the user is not authenticated.
 * @throws {ApiError} For other API-related errors.
 */
export const getRegistrationsByEmail = async (email, retryCount = 0) => {
  if (!email) {
     throw new Error("Email is required to fetch registrations.");
  }

  // Max retry count to prevent infinite loops
  const MAX_RETRIES = 2;
  
  // Add a small delay to allow authentication to complete if this is a retry
  if (retryCount > 0) {
    await new Promise(resolve => setTimeout(resolve, 800));
  }

  try {
    console.log(`[RegistrationAPI] Fetching registrations for ${email}`);
    
    // Use the correct API endpoint path - ensure it matches the backend controller
    const registrations = await apiClient(`/api/registrations/user/${encodeURIComponent(email)}`, {
      method: "GET",
      skipPrefix: false
    });
    
    if (!Array.isArray(registrations)) {
      console.warn(`[RegistrationAPI] Registration response is not an array:`, registrations);
      return [];
    }
    
    console.log(`[RegistrationAPI] Retrieved ${registrations.length} registrations for ${email}`);
    
    return registrations;
  } catch (error) {
    // If unauthorized error and we haven't exceeded max retries, try again
    if (error instanceof UnauthorizedError && retryCount < MAX_RETRIES) {
      console.log(`Auth not ready, retrying registration fetch for ${email} (attempt ${retryCount + 1})`);
      return getRegistrationsByEmail(email, retryCount + 1);
    }
    
    console.error(`Failed to fetch registrations for user ${email}:`, error);
    return []; // Return empty array instead of throwing to prevent UI breaking
  }
};


/**
 * Unregisters a user from an event.
 * Requires authentication.
 * @param {string} registrationId - The ID of the registration to delete.
 * @returns {Promise<Object>} A promise that resolves to the response data (likely empty or confirmation).
 * @throws {UnauthorizedError} If the user is not authenticated.
 * @throws {ApiError} For other API-related errors.
 */
export const unregisterFromEvent = async (registrationId) => {
  if (!registrationId) {
    throw new Error("Registration ID is required to unregister.");
  }

  // Convert registrationId to string if it's a number
  const registrationIdString = String(registrationId);
  console.log(`[API] Unregistering from event with registration ID: ${registrationIdString}`);

  try {
    // Make sure we have a valid registration ID
    if (isNaN(parseInt(registrationIdString))) {
      throw new Error(`Invalid registration ID format: ${registrationIdString}`);
    }

    const response = await apiClient(`/registrations/${registrationIdString}`, {
      method: "DELETE",
      skipPrefix: false, // Assuming '/registrations/{id}' is the full path
      headers: {
        'Content-Type': 'application/json'
      }
    });
    console.log(`[API] Successfully unregistered with ID ${registrationIdString}`);
    return response; // Or handle specific success response if needed
  } catch (error) {
    console.error(`[API] Failed to unregister registration ${registrationIdString}:`, error);
    throw error; // Re-throw the specific error from apiClient
  }
};

// TODO: Add other registration-related API functions if needed
// e.g., deleteRegistration (unregister) - might already be in event-api.js? Check consistency.
