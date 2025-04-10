import apiClient from './apiClient'; // Import the centralized API client

/**
 * Searches for games based on the provided criteria.
 * Authentication may or may not be required depending on backend implementation.
 * @param {object} criteria - The search criteria.
 * @param {string} [criteria.name] - Part of the game name to search for.
 * @param {string} [criteria.category] - The category to filter by.
 * @param {string|number} [criteria.minPlayers] - Minimum number of players.
 * @param {string|number} [criteria.maxPlayers] - Maximum number of players.
 * @returns {Promise<Array>} A promise that resolves to an array of game objects.
 * @throws {ApiError} For API-related errors.
 */
export const searchGames = async (criteria) => {
  const queryParams = new URLSearchParams();

  // Map frontend criteria names to backend parameter names
  if (criteria.name) queryParams.append('name', criteria.name);
  if (criteria.category) queryParams.append('category', criteria.category);
  if (criteria.minPlayers) queryParams.append('minPlayers', criteria.minPlayers);
  if (criteria.maxPlayers) queryParams.append('maxPlayers', criteria.maxPlayers);
  // Add other potential criteria here if needed

  const endpoint = `/games/search?${queryParams.toString()}`;

  try {
    // Use apiClient - it handles credentials automatically if needed
    const games = await apiClient(endpoint, { 
      method: "GET",
      skipPrefix: false // Should now use the /api prefix
    });
    return games;
  } catch (error) {
    console.error("Failed to fetch games:", error);
    // Re-throw the error (could be ApiError, UnauthorizedError, etc.)
    throw error;
  }
};

/**
 * Creates a new game. Requires authentication (via HttpOnly cookie).
 * The backend identifies the owner based on the authenticated user session and the provided ownerId.
 * 
 * NOTE: The backend will always create a default instance regardless of the createInstance flag.
 * This is backend behavior that cannot be disabled. The createInstance flag is kept for API compatibility.
 * 
 * @param {object} gameData - The game data.
 * @param {string} gameData.name - Game name.
 * @param {number} gameData.minPlayers - Min players.
 * @param {number} gameData.maxPlayers - Max players.
 * @param {string} [gameData.image] - Image URL (optional).
 * @param {string} [gameData.category] - Category (optional).
 * @param {string} [gameData.ownerId] - Owner email (required).
 * @param {string} [gameData.condition] - Physical condition of the game copy if creating instance.
 * @param {string} [gameData.location] - Location where the game is stored if creating instance.
 * @param {boolean} [gameData.createInstance=false] - When true, the backend will automatically create a game instance.
 *                                                  No need to call createGameInstance separately when this is true.
 * @returns {Promise<object>} A promise that resolves to the created game object.
 * @throws {UnauthorizedError} If the user is not authenticated.
 * @throws {ForbiddenError} If the user is not allowed (e.g., not a game owner account type).
 * @throws {ApiError} For other API-related errors.
 */
export const createGame = async (gameData) => {
  // Ensure ownerId is included in the payload
  if (!gameData.ownerId) {
    console.error("createGame: ownerId is required");
    throw new Error("Owner ID is required to create a game");
  }
  
  // Prepare payload with all required fields
  const payload = {
    ...gameData,
    minPlayers: parseInt(gameData.minPlayers, 10), // Ensure numbers are integers
    maxPlayers: parseInt(gameData.maxPlayers, 10),
    // Include instance-specific fields if instance should be created
    condition: gameData.createInstance ? (gameData.condition || "Excellent") : undefined, // Only include if creating instance
    location: gameData.createInstance ? (gameData.location || "Home") : undefined, // Only include if creating instance
    createInstance: gameData.createInstance || false // Explicitly set createInstance flag
  };

  console.log("createGame: Attempting to create game:", payload);
  
  try {
    // Use apiClient for the POST request
    const createdGame = await apiClient("/games", {
      method: "POST",
      body: payload,
      skipPrefix: false // Should now use the /api prefix
    });
    
    console.log("createGame: Successfully created game:", createdGame);
    return createdGame; // Return the created game object from backend
  } catch (error) {
    console.error("createGame: Failed to create game:", error);
    // Check if this is an authentication issue
    if (error.name === 'UnauthorizedError') {
      console.error("createGame: Authentication error - user not logged in or session expired");
    } else if (error.name === 'ForbiddenError') {
      console.error("createGame: Permission error - user does not have GAME_OWNER role");
    }
    
    // Log cookies state to help debug
    console.log("createGame: Cookie state at time of error:", {
      isAuthenticated: document.cookie.includes('isAuthenticated=true'),
      hasAccessToken: document.cookie.includes('accessToken='),
      allCookies: document.cookie
    });
    
    // Re-throw the specific error from apiClient
    throw error;
  }
};

/**
 * Fetches all games owned by a specific user (identified by email).
 * Requires authentication (via HttpOnly cookie).
 * @param {string} ownerEmail - The email of the owner whose games are to be fetched.
 * @param {boolean} [filterByInstances=true] - Whether to filter by games that have instances
 * @returns {Promise<Array>} A promise that resolves to an array of game objects owned by the user.
 * @throws {UnauthorizedError} If the user is not authenticated.
 * @throws {ForbiddenError} If the user is not allowed to view these games.
 * @throws {ApiError} For other API-related errors.
 */
export const getGamesByOwner = async (ownerEmail, filterByInstances = true) => {
  if (!ownerEmail) {
     console.warn("getGamesByOwner: Owner email is missing");
     return []; // Return empty array instead of throwing
  }

  console.log("getGamesByOwner: Fetching games for owner:", ownerEmail);
  
  // Handle if email is accidentally passed as an email object or with unnecessary formats
  const cleanEmail = ownerEmail.toString().trim();
  if (!cleanEmail) {
    console.warn("getGamesByOwner: Owner email is empty after cleaning");
    return []; // Return empty array instead of throwing
  }

  // Create endpoint for fetching games by owner
  let endpoint = `/games?ownerId=${encodeURIComponent(cleanEmail)}`;
  
  // Optionally add parameter for filtering by instances
  if (!filterByInstances) {
    endpoint += "&includeAllGames=true";
  }
  
  console.log("getGamesByOwner: Using endpoint:", endpoint);

  try {
    // Log cookie state before making the request
    console.log("getGamesByOwner: Cookie state before fetch:", {
      isAuthenticated: document.cookie.includes('isAuthenticated=true'),
      hasAccessToken: document.cookie.includes('accessToken='),
      allCookies: document.cookie
    });
    
    // Use apiClient for the GET request with skipPrefix option
    const games = await apiClient(endpoint, { 
      method: "GET",
      skipPrefix: false,
      timeout: 8000 // Set a reasonable timeout to prevent hanging
    });
    
    // Ensure we return an array
    if (!Array.isArray(games)) {
      console.warn(`getGamesByOwner: Response is not an array for ${cleanEmail}:`, games);
      return [];
    }
    
    console.log(`getGamesByOwner: Successfully fetched ${games.length} games for owner ${cleanEmail}:`, games);
    return games;
  } catch (error) {
    console.error(`getGamesByOwner: Failed to fetch games for owner ${cleanEmail}:`, error);
    // Check specific error types
    if (error.name === 'UnauthorizedError') {
      console.error("getGamesByOwner: Authentication error - user not logged in or session expired");
    } else if (error.name === 'ForbiddenError') {
      console.error("getGamesByOwner: Permission error accessing games");
    } else if (error.name === 'TimeoutError') {
      console.error("getGamesByOwner: Request timed out, possible server issue");
    }
    
    // Return empty array instead of throwing to prevent UI from breaking
    return [];
  }
};

/**
 * Fetches a single game by its ID.
 * @param {number} id - Game ID
 * @returns {Promise<object>} Game object
 */
export const getGameById = async (id) => {
  if (!id) {
    throw new Error("Game ID is required.");
  }
  
  const endpoint = `/games/${id}`;
  console.log(`getGameById: Fetching game with ID ${id}`);
  
  try {
    const game = await apiClient(endpoint, { 
      method: "GET",
      skipPrefix: false // Use /api prefix
    });
    console.log(`getGameById: Successfully fetched game ${id}:`, game);
    return game;
  } catch (error) {
    console.error(`getGameById: Failed to fetch game ${id}:`, error);
    throw error;
  }
};

/**
 * Fetches all instances (physical copies) of a specific game.
 * Authentication might be required depending on backend setup.
 * @param {string|number} gameId - The ID of the game.
 * @returns {Promise<Array>} A promise that resolves to an array of game instance objects.
 * @throws {ApiError} For API-related errors.
 */
export const getGameInstances = async (gameId) => {
  if (!gameId) {
    throw new Error("Game ID is required to fetch instances.");
  }
  const endpoint = `/games/${gameId}/instances`;
  console.log("getGameInstances: Fetching instances for game:", gameId);

  try {
    const instances = await apiClient(endpoint, { 
      method: "GET",
      skipPrefix: false // Should now use the /api prefix
    });
    console.log(`getGameInstances: Successfully fetched ${instances.length} instances for game ${gameId}:`, instances);
    return instances;
  } catch (error) {
    console.error(`getGameInstances: Failed to fetch instances for game ${gameId}:`, error);
    throw error;
  }
};

/**
 * Fetches all reviews for a specific game.
 * Authentication might be required depending on backend setup.
 * @param {string|number} gameId - The ID of the game.
 * @returns {Promise<Array>} A promise that resolves to an array of review objects.
 * @throws {ApiError} For API-related errors.
 */
export const getGameReviews = async (gameId) => {
  if (!gameId) {
    throw new Error("Game ID is required to fetch reviews.");
  }
  const endpoint = `/games/${gameId}/reviews`;
  console.log("getGameReviews: Fetching reviews for game:", gameId);

  try {
    const reviews = await apiClient(endpoint, { 
      method: "GET",
      skipPrefix: false // Should now use the /api prefix
    });
    console.log(`getGameReviews: Successfully fetched ${reviews.length} reviews for game ${gameId}:`, reviews);
    return reviews;
  } catch (error) {
    console.error(`getGameReviews: Failed to fetch reviews for game ${gameId}:`, error);
    throw error;
  }
};

/**
 * Submits a new review for a game.
 * @param {Object} reviewData - The review data to submit
 * @param {number} reviewData.rating - Rating from 1-5
 * @param {string} reviewData.comment - Review comment text
 * @param {number} reviewData.gameId - ID of the game being reviewed
 * @param {string} [reviewData.reviewerId] - Email of the reviewer (optional, uses current user if not provided)
 * @returns {Promise<Object>} A promise that resolves to the created review
 * @throws {ApiError} For API-related errors
 */
export const submitReview = async (reviewData) => {
  if (!reviewData.gameId) {
    throw new Error("Game ID is required to submit a review.");
  }

  if (!reviewData.rating) {
    throw new Error("Rating is required to submit a review.");
  }

  console.log("submitReview: Submitting review:", reviewData);

  try {
    const response = await apiClient('/reviews', {
      method: "POST",
      skipPrefix: false,
      body: reviewData
    });
    console.log("submitReview: Successfully submitted review:", response);
    return response;
  } catch (error) {
    console.error("submitReview: Failed to submit review:", error);
    throw error;
  }
};

/**
 * Updates an existing review.
 * @param {number} reviewId - ID of the review to update
 * @param {Object} reviewData - Updated review data
 * @returns {Promise<Object>} A promise that resolves to the updated review
 * @throws {ApiError} For API-related errors
 */
export const updateReview = async (reviewId, reviewData) => {
  if (!reviewId) {
    throw new Error("Review ID is required to update a review.");
  }

  console.log(`updateReview: Updating review ${reviewId}:`, reviewData);

  try {
    const response = await apiClient(`/reviews/${reviewId}`, {
      method: "PUT",
      skipPrefix: false,
      body: reviewData
    });
    console.log(`updateReview: Successfully updated review ${reviewId}:`, response);
    return response;
  } catch (error) {
    console.error(`updateReview: Failed to update review ${reviewId}:`, error);
    throw error;
  }
};

/**
 * Deletes a review by its ID.
 * @param {number} reviewId - ID of the review to delete
 * @returns {Promise<Object>} A promise that resolves when the review is deleted
 * @throws {ApiError} For API-related errors
 */
export const deleteReview = async (reviewId) => {
  if (!reviewId) {
    throw new Error("Review ID is required to delete a review.");
  }

  console.log(`deleteReview: Deleting review ${reviewId}`);

  try {
    const response = await apiClient(`/reviews/${reviewId}`, {
      method: "DELETE",
      skipPrefix: false
    });
    console.log(`deleteReview: Successfully deleted review ${reviewId}`);
    return response;
  } catch (error) {
    console.error(`deleteReview: Failed to delete review ${reviewId}:`, error);
    throw error;
  }
};

/**
 * Deletes a game by its ID. Requires authentication.
 * @param {string|number} gameId - The ID of the game to delete.
 * @returns {Promise<void>} A promise that resolves when the game is deleted.
 * @throws {UnauthorizedError} If the user is not authenticated.
 * @throws {ForbiddenError} If the user is not allowed to delete the game (e.g., not the owner).
 * @throws {ApiError} For other API-related errors.
 */
export const deleteGame = async (gameId) => {
  if (!gameId) {
    throw new Error("Game ID is required to delete the game.");
  }
  const endpoint = `/games/${gameId}`;
  console.log("deleteGame: Attempting to delete game:", gameId);

  try {
    await apiClient(endpoint, {
      method: "DELETE",
      skipPrefix: false // Should now use the /api prefix
    });
    console.log(`deleteGame: Successfully deleted game ${gameId}`);
  } catch (error) {
    console.error(`deleteGame: Failed to delete game ${gameId}:`, error);
    // Re-throw the specific error from apiClient
    throw error;
  }
};

/**
 * Updates a game instance
 * @param {number} instanceId - ID of the instance to update
 * @param {object} data - Updated instance data
 * @returns {Promise<Object>} - Updated instance data
 */
export const updateGameInstance = async (instanceId, data) => {
  if (!instanceId) {
    throw new Error("Instance ID is required to update game instance.");
  }
  
  if (!data.gameId) {
    throw new Error("Game ID is required to update game instance.");
  }
  
  const endpoint = `/games/${data.gameId}/instances/${instanceId}`;
  console.log(`updateGameInstance: Updating instance ${instanceId}:`, data);
  
  try {
    const response = await apiClient(endpoint, { 
      method: "PUT",
      skipPrefix: false,
      body: data
    });
    console.log(`updateGameInstance: Successfully updated instance ${instanceId}:`, response);
    return response;
  } catch (error) {
    console.error(`updateGameInstance: Failed to update instance ${instanceId}:`, error);
    throw error;
  }
};

/**
 * Creates a new game instance (physical copy) for a specific game.
 * Authentication is required (via HttpOnly cookie).
 * @param {string|number} gameId - The ID of the game
 * @param {object} data - The instance data
 * @param {string} [data.name] - Optional name for this specific copy (e.g., "Deluxe Edition")
 * @param {string} data.condition - Physical condition (e.g., "New", "Excellent", "Good", "Fair", "Poor")
 * @param {string} data.location - Where the game is stored
 * @param {boolean} data.available - Whether the copy is available for borrowing
 * @param {string|number} [data.ownerId] - Owner ID (typically derived from authenticated user)
 * @returns {Promise<object>} A promise that resolves to the created game instance object
 * @throws {UnauthorizedError} If the user is not authenticated
 * @throws {ForbiddenError} If the user is not allowed
 * @throws {ApiError} For other API-related errors
 */
export const createGameInstance = async (gameId, data) => {
  if (!gameId) {
    throw new Error("Game ID is required to create an instance");
  }
  
  console.log(`createGameInstance: Creating instance for game ${gameId}:`, data);
  
  try {
    // Ensure proper formatting of data
    const instanceData = {
      ...data,
      gameId, // Ensure gameId is included
      available: data.available !== false // Default to true if not specified
    };
    
    const response = await apiClient(`/games/${gameId}/instances`, {
      method: "POST",
      body: instanceData,
      skipPrefix: false // Use /api prefix
    });
    
    console.log(`createGameInstance: Successfully created instance for game ${gameId}:`, response);
    return response;
  } catch (error) {
    console.error(`createGameInstance: Failed to create instance for game ${gameId}:`, error);
    throw error;
  }
};

/**
 * Updates an existing game by ID. Requires authentication.
 * @param {number} gameId - The ID of the game to update
 * @param {object} gameData - The updated game data
 * @param {string} gameData.name - Game name
 * @param {number} gameData.minPlayers - Min players
 * @param {number} gameData.maxPlayers - Max players
 * @param {string} [gameData.image] - Image URL (optional)
 * @param {string} [gameData.category] - Category (optional)
 * @returns {Promise<object>} A promise that resolves to the updated game object
 * @throws {UnauthorizedError} If the user is not authenticated
 * @throws {ForbiddenError} If the user is not allowed to update this game
 * @throws {ApiError} For other API-related errors
 */
export const updateGame = async (gameId, gameData) => {
  if (!gameId) {
    throw new Error("Game ID is required for updating a game");
  }
  
  const payload = {
    ...gameData,
    minPlayers: parseInt(gameData.minPlayers, 10),
    maxPlayers: parseInt(gameData.maxPlayers, 10)
  };
  
  console.log(`updateGame: Attempting to update game ${gameId}:`, payload);
  
  try {
    const endpoint = `/games/${gameId}`;
    const updatedGame = await apiClient(endpoint, {
      method: "PUT",
      body: payload,
      skipPrefix: false
    });
    
    console.log(`updateGame: Successfully updated game ${gameId}:`, updatedGame);
    return updatedGame;
  } catch (error) {
    console.error(`updateGame: Failed to update game ${gameId}:`, error);
    throw error;
  }
};

/**
 * Deletes a game instance by its ID. Requires authentication.
 * @param {number} gameId - The ID of the game.
 * @param {number} instanceId - The ID of the instance to delete.
 * @returns {Promise<void>} A promise that resolves when the instance is deleted.
 * @throws {UnauthorizedError} If the user is not authenticated.
 * @throws {ForbiddenError} If the user is not allowed to delete the instance (e.g., not the owner).
 * @throws {ApiError} For other API-related errors.
 */
export const deleteGameInstance = async (gameId, instanceId) => {
  if (!gameId || !instanceId) {
    throw new Error("Game ID and Instance ID are required to delete the instance.");
  }
  const endpoint = `/games/${gameId}/instances/${instanceId}`;
  console.log("deleteGameInstance: Attempting to delete instance:", instanceId);

  try {
    await apiClient(endpoint, {
      method: "DELETE",
      skipPrefix: false
    });
    console.log(`deleteGameInstance: Successfully deleted instance ${instanceId}`);
  } catch (error) {
    console.error(`deleteGameInstance: Failed to delete instance ${instanceId}:`, error);
    throw error;
  }
};

/**
 * Checks if a game is available for a specific date range.
 * @param {number} gameId - The ID of the game to check.
 * @param {Date} startDate - The start date of the borrowing period.
 * @param {Date} endDate - The end date of the borrowing period.
 * @returns {Promise<boolean>} A promise that resolves to a boolean indicating whether the game is available.
 * @throws {ApiError} For API-related errors.
 */
export const checkGameAvailability = async (gameId, startDate, endDate) => {
  if (!gameId) {
    throw new Error("Game ID is required to check availability.");
  }
  
  if (!startDate || !endDate) {
    throw new Error("Start date and end date are required to check availability.");
  }
  
  // Convert dates to milliseconds for API call
  const startTimestamp = startDate.getTime();
  const endTimestamp = endDate.getTime();
  
  const endpoint = `/games/${gameId}/availability?startDate=${startTimestamp}&endDate=${endTimestamp}`;
  console.log(`checkGameAvailability: Checking availability for game ${gameId} from ${startDate} to ${endDate}`);

  try {
    const isAvailable = await apiClient(endpoint, { 
      method: "GET",
      skipPrefix: false
    });
    console.log(`checkGameAvailability: Game ${gameId} is ${isAvailable ? 'available' : 'not available'} for the requested period`);
    return isAvailable;
  } catch (error) {
    console.error(`checkGameAvailability: Failed to check availability for game ${gameId}:`, error);
    // Default to false (unavailable) on error to be safe
    return false;
  }
};

/**
 * Creates a copy of an existing game in the user's collection.
 * Requires authentication.
 * 
 * @param {number} gameId - ID of the game to copy
 * @param {object} [instanceData] - Optional data for the game instance
 * @param {string} [instanceData.condition] - Physical condition of the game copy
 * @param {string} [instanceData.location] - Location where the game is stored
 * @param {string} [instanceData.name] - Optional custom name for this copy
 * @returns {Promise<object>} A promise that resolves to the created game instance
 * @throws {UnauthorizedError} If the user is not authenticated
 * @throws {ForbiddenError} If the user is not a game owner
 * @throws {ApiError} For other API-related errors
 */
export const copyGame = async (gameId, instanceData = {}) => {
  if (!gameId) {
    throw new Error("Game ID is required to copy a game");
  }
  
  console.log(`copyGame: Copying game with ID ${gameId}`);
  
  try {
    // Use apiClient for the POST request
    const createdInstance = await apiClient(`/games/${gameId}/copy`, {
      method: "POST",
      body: instanceData,
      skipPrefix: false
    });
    
    console.log("copyGame: Successfully created game copy:", createdInstance);
    return createdInstance;
  } catch (error) {
    console.error(`copyGame: Failed to copy game ${gameId}:`, error);
    
    if (error.name === 'UnauthorizedError') {
      console.error("copyGame: Authentication error - user not logged in or session expired");
    } else if (error.name === 'ForbiddenError') {
      console.error("copyGame: Permission error - user does not have GAME_OWNER role");
    } else if (error.name === 'ApiError' && error.status === 404) {
      console.error(`copyGame: Game with ID ${gameId} not found`);
    }
    
    throw error;
  }
};

/**
 * Fetches all game instances owned by a specific user email.
 * @param {string} ownerEmail - The email of the owner.
 * @returns {Promise<Array>} A promise that resolves to an array of game instance objects.
 * @throws {ApiError} For API-related errors.
 */
export const getInstancesByOwnerEmail = async (ownerEmail) => {
  if (!ownerEmail) {
    throw new Error("Owner email is required to fetch game instances.");
  }
  const endpoint = `/games/instances?ownerId=${encodeURIComponent(ownerEmail)}`;
  console.log("getInstancesByOwnerEmail: Fetching instances for owner:", ownerEmail);

  try {
    const instances = await apiClient(endpoint, { 
      method: "GET",
      skipPrefix: false // Use /api prefix
    });
    console.log(`getInstancesByOwnerEmail: Successfully fetched ${instances.length} instances for owner ${ownerEmail}:`, instances);
    // Ensure the response includes gameImage if the backend DTO was updated
    return instances.map(inst => ({
      ...inst,
      // Attempt to map gameImage if it exists, otherwise keep original structure
      image: inst.gameImage || inst.image || null 
    }));
  } catch (error) {
    console.error(`getInstancesByOwnerEmail: Failed to fetch instances for owner ${ownerEmail}:`, error);
    // Return empty array on error to prevent UI break, but log the error
    return []; 
  }
};


/**
 * Fetches all game instances owned by the current authenticated user.
 * Uses the combined /api/games/instances?my=true endpoint.
 * 
 * @returns {Promise<Array>} A promise that resolves to an array of game instance objects
 * @throws {UnauthorizedError} If the user is not authenticated
 * @throws {ApiError} For other API-related errors
 */
export const getUserGameInstances = async () => {
  console.log("getUserGameInstances: Fetching all game instances for current authenticated user");
  
  try {
    // Use the combined endpoint with my=true parameter
    const instances = await apiClient("/games/instances?my=true", { 
      method: "GET",
      skipPrefix: false 
    });
    console.log(`getUserGameInstances: Successfully fetched ${instances.length} instances:`, instances);
    // Ensure the response includes gameImage if the backend DTO was updated
    return instances.map(inst => ({
      ...inst,
      // Attempt to map gameImage if it exists, otherwise keep original structure
      image: inst.gameImage || inst.image || null 
    }));
  } catch (error) {
    console.error("getUserGameInstances: Failed to fetch current user's game instances:", error);
    
    if (error.name === 'UnauthorizedError') {
      console.error("getUserGameInstances: Authentication error - user not logged in or session expired");
    }
    
    // Return empty array instead of throwing to prevent UI from breaking
    return [];
  }
};

/**
 * Fetches games that a user can include in events.
 * This includes both games they own and games they're currently borrowing
 * that meet the time criteria for events.
 * 
 * @param {string} userId - ID or email of the user
 * @returns {Promise<Array>} Array of games available for events
 */
export const getGamesAvailableForEvents = async (userId) => {
  if (!userId) {
    throw new Error("User ID is required.");
  }

  console.log(`getGamesAvailableForEvents: Fetching games for user ${userId}`);

  try {
    // First try to get all games from global library
    let allAccessibleGames = [];

    // Step 1: Try to get owned games (this may fail if user is not a game owner)
    let ownedGames = [];
    try {
      ownedGames = await getGamesByOwner(userId);
      console.log(`getGamesAvailableForEvents: Found ${ownedGames.length} owned games`);
      // Mark these as owned
      ownedGames = ownedGames.map(game => ({
        ...game,
        isOwned: true
      }));
      allAccessibleGames.push(...ownedGames);
    } catch (error) {
      // If user is not a game owner, this API call will fail with 400
      console.log("getGamesAvailableForEvents: User may not be a game owner:", error.message);
      // Continue with other sources of games
    }

    // Step 2: Try to get borrowed games by different means
    try {
      // First approach: Try to get active borrow requests through user service
      // This is more resilient if email is provided instead of numeric ID
      if (typeof userId === 'string' && userId.includes('@')) {
        // Get user ID from auth service or localStorage if available
        const numericUserId = localStorage.getItem('userId');
        if (numericUserId) {
          try {
            // Import the function to avoid circular dependencies
            const {getActiveBorrowedGames} = await import('./borrow_request-api.js');
            const borrowedGames = await getActiveBorrowedGames(numericUserId);
            console.log(`getGamesAvailableForEvents: Found ${borrowedGames.length} borrowed games using numeric ID`);

            // Add borrowed games to the list, avoiding duplicates
            for (const borrowedGame of borrowedGames) {
              if (!allAccessibleGames.some(game => game.id === borrowedGame.id)) {
                allAccessibleGames.push({
                  ...borrowedGame,
                  isBorrowed: true
                });
              }
            }
          } catch (error) {
            console.log("getGamesAvailableForEvents: Could not get borrowed games using numeric ID:", error.message);
          }
        }

        // Second approach: Try to get games through user API
        try {
          // Implement this if your backend has a user-games endpoint
          // const userGames = await getUserGames(userId);
          // allAccessibleGames.push(...userGames);
        } catch (error) {
          console.log("getGamesAvailableForEvents: Could not get user games:", error.message);
        }
      } else {
        // Numeric ID provided, use the standard method
        const {getActiveBorrowedGames} = await import('./borrow_request-api.js');
        const borrowedGames = await getActiveBorrowedGames(userId);
        console.log(`getGamesAvailableForEvents: Found ${borrowedGames.length} borrowed games`);

        // Add borrowed games to the list, avoiding duplicates
        for (const borrowedGame of borrowedGames) {
          if (!allAccessibleGames.some(game => game.id === borrowedGame.id)) {
            allAccessibleGames.push({
              ...borrowedGame,
              isBorrowed: true
            });
          }
        }
      }
    } catch (error) {
      console.error("getGamesAvailableForEvents: Error fetching borrowed games:", error);
      // Continue with whatever games we have
    }
    return allAccessibleGames;
  } catch (error) {
    console.error("getGamesAvailableForEvents: Error fetching games:", error);
  }
}

/**
 * Fetches a specific game instance by its ID.
 * @param {number} instanceId - The ID of the instance to fetch.
 * @returns {Promise<Object>} A promise that resolves to the game instance object.
 * @throws {ApiError} For API-related errors.
 */
export const getGameInstanceById = async (instanceId) => {
  if (!instanceId) {
    throw new Error("Instance ID is required to fetch the game instance.");
  }

  const parsedInstanceId = parseInt(instanceId);
  console.log("getGameInstanceById: Fetching instance with ID:", parsedInstanceId);

  try {
    // Try first approach: Get all user's game instances and filter
    const userInstances = await getUserGameInstances();
    console.log(`getGameInstanceById: Checking among ${userInstances.length} user instances`);

    const matchingInstance = userInstances.find(inst => inst.id === parsedInstanceId);
    if (matchingInstance) {
      console.log(`getGameInstanceById: Found instance ${parsedInstanceId} in user's instances:`, matchingInstance);
      return matchingInstance;
    }

    // Second approach: Try to get all instances of all games and search through them
    // This is less efficient but might find instances not owned by the current user
    console.log("getGameInstanceById: Attempting to find instance in all games");
    // Get all games the user can see (this could be a lot)
    const games = await apiClient("/games", {
      method: "GET",
      skipPrefix: false
    });

    // For each game, check if it has the instance we're looking for
    for (const game of games) {
      if (!game.instances || game.instances.length === 0) continue;

      const instance = game.instances.find(inst => inst.id === parsedInstanceId);
      if (instance) {
        console.log(`getGameInstanceById: Found instance ${parsedInstanceId} in game ${game.id}:`, instance);
        return instance;
      }

      // If the game doesn't have instances yet, try to fetch them specifically
      try {
        const instances = await getGameInstances(game.id);
        const matchedInstance = instances.find(inst => inst.id === parsedInstanceId);
        if (matchedInstance) {
          console.log(`getGameInstanceById: Found instance ${parsedInstanceId} in game ${game.id}'s instances:`, matchedInstance);
          return matchedInstance;
        }
      } catch (e) {
        console.error(`getGameInstanceById: Error fetching instances for game ${game.id}:`, e);
      }
    }

    // If all attempts fail, throw an error
    throw new Error(`Instance with ID ${parsedInstanceId} not found`);
  } catch (error) {
    console.error(`getGameInstanceById: Failed to fetch instance ${parsedInstanceId}:`, error);
    throw error;
  }
}


// Add other game-related API functions here as needed, using apiClient
