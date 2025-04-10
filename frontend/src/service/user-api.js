import apiClient, { UnauthorizedError, ForbiddenError, NotFoundError } from './apiClient';

/**
 * Fetches the profile information of the currently logged-in user.
 * @returns {Promise<object>} User profile object
 * @throws {UnauthorizedError} If the user is not authenticated
 */
export const getUserProfile = async () => {
  return apiClient('/profile', {
    credentials: 'include'
  });
};

/**
 * Fetches user information by email (exact match)
 * @param {string} email - The email of the user to retrieve
 * @returns {Promise<Object>} - The user information
 */
export async function getUserInfoByEmail(email) {
  try {
    if (!email) {
      throw new Error("Email is required");
    }
    
    console.log(`[UserAPI] Fetching user info for ${email}`);
    
    // Use the correct API endpoint for finding a user by exact email
    const response = await apiClient(`/api/users/email/${email}`, {
      method: 'GET',
    });

    // If response is not what we expect, throw an error
    if (!response || (response.error && !response.name)) {
      throw new Error(response.error || "Invalid response from server");
    }

    console.log(`[UserAPI] Successfully fetched basic user data for ${email}`);
    
    // Fetch additional user game data and registrations
    const userData = await enrichUserWithGameData(response);
    
    // Check if events data is already present
    if (userData.events && Array.isArray(userData.events)) {
      console.log(`[UserAPI] User data already contains ${userData.events.length} events/registrations`);
    } else {
      console.log(`[UserAPI] No events found in user data, fetching registrations separately`);
      
      // Fetch registrations separately if they're not in the response
      const registrations = await fetchUserRegistrations(email);
      userData.events = registrations || [];
      
      console.log(`[UserAPI] Added ${userData.events.length} registrations to user data`);
    }
    
    return userData;
  } catch (error) {
    console.error(`Error fetching user info for ${email}:`, error);
    // Format the error appropriately
    const formattedError = new Error(`User with email ${email} not found`);
    formattedError.name = "NotFoundError";
    throw formattedError;
  }
}

/**
 * Enriches a user object with game data (played, borrowed, owned)
 * @param {Object} user - The user object to enrich
 * @returns {Promise<Object>} - The enriched user object with game data
 */
async function enrichUserWithGameData(user) {
  try {
    if (!user || !user.id) {
      return user; // Return as is if no valid user
    }
    
    // Fetch games played (now includes all borrowed games)
    const gamesPlayed = await getUserGamesPlayed(user.id);
    
    // Fetch games owned (only for game owners)
    const gamesOwned = user.gameOwner ? await getUserGamesOwned(user.id) : [];
    
    // Return enriched user object
    return {
      ...user,
      gamesPlayed,
      gamesOwned
    };
  } catch (error) {
    console.error(`Error enriching user data for user ID ${user.id}:`, error);
    // Return original user data if enrichment fails
    return user;
  }
}

/**
 * Fetch user registrations by email
 * @param {string} email - The email of the user
 * @returns {Promise<Array>} - Array of registration objects
 */
async function fetchUserRegistrations(email) {
  try {
    if (!email) {
      return [];
    }
    
    console.log(`[UserAPI] Fetching registrations for user ${email}`);
    
    // Use the registration API to get user's registrations
    const registrations = await apiClient(`/api/registrations/user/${encodeURIComponent(email)}`, {
      method: 'GET',
      skipPrefix: false // Explicitly set skipPrefix to false to match registration-api.js
    });
    
    console.log(`[UserAPI] Retrieved ${Array.isArray(registrations) ? registrations.length : 0} registrations for ${email}`);
    
    if (!Array.isArray(registrations)) {
      console.warn(`[UserAPI] Registrations response is not an array:`, registrations);
    }
    
    return Array.isArray(registrations) ? registrations : [];
  } catch (error) {
    console.error(`Error fetching registrations for ${email}:`, error);
    return [];
  }
}

/**
 * Fetches the games played by a user
 * @param {number} userId - The ID of the user
 * @returns {Promise<Array>} - Array of game names played by the user
 */
export async function getUserGamesPlayed(userId) {
  try {
    const response = await apiClient(`/api/users/${userId}/games/played`, {
      method: 'GET',
    });
    
    if (!response || !Array.isArray(response)) {
      return [];
    }
    
    // Depending on the response format, extract game names
    return response.map(game => typeof game === 'string' ? game : game.name || 'Unknown Game');
  } catch (error) {
    console.error(`Error fetching games played for user ${userId}:`, error);
    return [];
  }
}

/**
 * Fetches the games borrowed by a user
 * @param {number} userId - The ID of the user
 * @returns {Promise<Array>} - Array of game names borrowed by the user
 */
export async function getUserGamesBorrowed(userId) {
  try {
    const response = await apiClient(`/api/users/${userId}/games/borrowed`, {
      method: 'GET',
    });
    
    if (!response || !Array.isArray(response)) {
      return [];
    }
    
    // Depending on the response format, extract game names
    return response.map(game => typeof game === 'string' ? game : game.name || 'Unknown Game');
  } catch (error) {
    console.error(`Error fetching games borrowed for user ${userId}:`, error);
    return [];
  }
}

/**
 * Fetches the games owned by a user (for game owners)
 * @param {number} userId - The ID of the user
 * @returns {Promise<Array>} - Array of game names owned by the user
 */
export async function getUserGamesOwned(userId) {
  try {
    const response = await apiClient(`/api/users/${userId}/games/owned`, {
      method: 'GET',
    });
    
    if (!response || !Array.isArray(response)) {
      return [];
    }
    
    // Depending on the response format, extract game names
    return response.map(game => typeof game === 'string' ? game : game.name || 'Unknown Game');
  } catch (error) {
    console.error(`Error fetching games owned for user ${userId}:`, error);
    return [];
  }
}

/**
 * Searches for users by name, email, or both
 * @param {object} searchParams - Search parameters
 * @param {string} searchParams.term - The search term to look for in names or emails
 * @param {boolean} [searchParams.gameOwnerOnly=false] - Whether to only return game owners
 * @returns {Promise<Array>} Array of users matching search criteria
 */
export const searchUsers = async (searchParams) => {
  try {
    if (!searchParams || !searchParams.term || searchParams.term.trim() === '') {
      return [];
    }
    
    // Convert parameters to query string for GET request
    const queryParams = new URLSearchParams();
    queryParams.append('term', searchParams.term);
    
    if (searchParams.gameOwnerOnly) {
      queryParams.append('gameOwnerOnly', 'true');
    }
    
    const response = await apiClient(`/api/users/search?${queryParams.toString()}`, {
      method: 'GET',
    });
    
    if (!response || !Array.isArray(response)) {
      return [];
    }
    
    // Enrich each user with game data
    const enrichedUsers = await Promise.all(
      response.map(user => enrichUserWithGameData(user))
    );
    
    return enrichedUsers;
  } catch (error) {
    console.error('Error searching users:', error);
    return [];
  }
};

/**
 * Searches for users by name
 * @param {string} name - The name to search for
 * @returns {Promise<Array>} - Array of users matching the name
 */
export async function searchUsersByName(name) {
  try {
    if (!name || name.trim() === '') {
      return [];
    }
    
    const response = await apiClient(`/api/users/name/${name}`, {
      method: 'GET',
    });
    
    if (!response || !Array.isArray(response)) {
      return [];
    }
    
    // Enrich each user with game data
    const enrichedUsers = await Promise.all(
      response.map(user => enrichUserWithGameData(user))
    );
    
    return enrichedUsers;
  } catch (error) {
    console.error(`Error searching users by name ${name}:`, error);
    return [];
  }
}

/**
 * Searches for users by email (partial match)
 * @param {string} email - The email to search for
 * @returns {Promise<Array>} - Array of users matching the email
 */
export async function searchUsersByEmail(email) {
  try {
    if (!email || email.trim() === '') {
      return [];
    }
    
    const response = await apiClient(`/api/users/email/search/${email}`, {
      method: 'GET',
    });
    
    if (!response || !Array.isArray(response)) {
      return [];
    }
    
    // Enrich each user with game data
    const enrichedUsers = await Promise.all(
      response.map(user => enrichUserWithGameData(user))
    );
    
    return enrichedUsers;
  } catch (error) {
    console.error(`Error searching users by email ${email}:`, error);
    return [];
  }
}

/**
 * Logs out the current user
 * @returns {Promise<void>}
 */
export const logoutUser = async () => {
  try {
    await apiClient('/auth/logout', { 
      method: 'POST',
      credentials: 'include',
      skipRefresh: true // Skip token refresh for logout
    });
  } catch (error) {
    console.error("Error during logout:", error);
    // Continue with client-side logout even if API call fails
    throw error;
  }
};

/**
 * Updates user profile
 * @param {object} userData - User data to update
 * @returns {Promise<object>} Updated user profile
 */
export const updateUserProfile = async (userData) => {
  if (!userData) {
    throw new Error("User data is required");
  }
  
  return apiClient('/profile/update', {
    method: 'POST',
    body: userData
  });
};

/**
 * Changes user password
 * @param {string} currentPassword - Current password
 * @param {string} newPassword - New password
 * @returns {Promise<object>} Success response
 */
export const changePassword = async (currentPassword, newPassword) => {
  if (!currentPassword || !newPassword) {
    throw new Error("Current and new passwords are required");
  }
  
  return apiClient('/profile/password', {
    method: 'POST',
    body: { currentPassword, newPassword }
  });
};


/**
 * Sends a connection request to another user.
 * Requires authentication.
 * @param {string} targetUserEmail - The email of the user to connect with.
 * @returns {Promise<object>} Success response from the backend.
 * @throws {UnauthorizedError} If the user is not authenticated.
 * @throws {ApiError} For other API-related errors (e.g., user not found, already connected).
 */
export const sendConnectionRequest = async (targetUserEmail) => {
  if (!targetUserEmail) {
    throw new Error("Target user email is required to send a connection request.");
  }

  console.log("sendConnectionRequest: Sending request to:", targetUserEmail);

  try {
    // Use apiClient for the POST request. Assumes endpoint is /connections/request
    // and expects { email: targetUserEmail } in the body.
    const response = await apiClient('/connections/request', {
      method: 'POST',
      body: { email: targetUserEmail },
      // Assuming this endpoint is prefixed with /api like others
      skipPrefix: false,

    });
    console.log("sendConnectionRequest: Successfully sent request to", targetUserEmail, response);
    return response; // Return the success response
  } catch (error) {
    console.error(`sendConnectionRequest: Failed to send request to ${targetUserEmail}:`, error);
    // Re-throw the specific error from apiClient
    throw error;
  }
};

// TODO: Add other user-related API functions here if needed, using apiClient
// e.g., searchUsers, getUserById, updateUser, etc.
