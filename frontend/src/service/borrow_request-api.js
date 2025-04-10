/**
 * BorrowRequest API Module
 *
 * This file provides API functions for managing borrow requests.
 * Follows the application's established API patterns.
 */

import apiClient from './apiClient'; // Import the centralized API client
// Use the same base URL as other API modules
// Use apiClient which handles base URL and prefix
const BORROW_REQUESTS_ENDPOINT = '/borrowrequests'; // Relative path for apiClient

/**
 * Creates a new borrow request
 * @param {Object} requestData - Contains requesterId, requestedGameId, startDate, endDate
 * @returns {Promise<Object>} The created borrow request
 */
export const createBorrowRequest = async (requestData) => {
  // Remove manual token check and Authorization header
  // Removed extra brace

  // Use apiClient
  return apiClient(BORROW_REQUESTS_ENDPOINT, {
    method: "POST",
    body: requestData,
    requiresAuth: true,
    skipPrefix: false // Use /api prefix
  });
};

/**
 * Gets a borrow request by its ID
 * @param {number} id - The ID of the borrow request
 * @returns {Promise<Object>} The borrow request
 */
export const getBorrowRequestById = async (id) => {
  if (!id) {
    throw new Error("Borrow request ID is required.");
  }
  // Remove manual token check and Authorization header
  // Removed extra brace

  // Use apiClient
  return apiClient(`${BORROW_REQUESTS_ENDPOINT}/${id}`, {
    method: "GET",
    requiresAuth: true,
    skipPrefix: false // Use /api prefix
  });
};

/**
 * Gets all borrow requests
 * @returns {Promise<Array>} List of all borrow requests
 */
export const getAllBorrowRequests = async () => {
  // Remove manual token check and Authorization header
  // Removed extra brace

  // Use apiClient
  return apiClient(BORROW_REQUESTS_ENDPOINT, {
    method: "GET",
    requiresAuth: true, // Assuming this needs auth, adjust if not
    skipPrefix: false // Use /api prefix
  });
};

/**
 * Updates a borrow request's status
 * @param {number} id - The ID of the borrow request to update
 * @param {string} status - The new status (e.g., 'APPROVED', 'DECLINED')
 * @returns {Promise<Object>} The updated borrow request
 */
export const updateBorrowRequestStatus = async (id, status) => {
  if (!id) {
    throw new Error("Borrow request ID is required.");
  }
  if (!status) {
    throw new Error("Status is required.");
  }
  // Remove manual token check and Authorization header
  
  // Note: Sending the full existingRequest might not be ideal for a status update.
  // Ideally, the backend endpoint should accept just the status.
  // Assuming the backend PUT /api/borrowrequests/{id} expects the full object for now.
  
  // Fetch existing request using the refactored function
  const existingRequest = await getBorrowRequestById(id);
  // Update status
  existingRequest.status = status;
  // Removed extra brace

  // Use apiClient
  return apiClient(`${BORROW_REQUESTS_ENDPOINT}/${id}`, {
    method: "PUT",
    body: existingRequest, // Sending full object as per original logic
    requiresAuth: true,
    skipPrefix: false // Use /api prefix
  });
};

/**
 * Deletes a borrow request
 * @param {number} id - The ID of the borrow request to delete
 * @returns {Promise<boolean>} True if deletion was successful
 */
export const deleteBorrowRequest = async (id) => {
  if (!id) {
    throw new Error("Borrow request ID is required.");
  }
  // Remove manual token check and Authorization header
  // Removed extra brace

  // Use apiClient
  // DELETE often returns 204 No Content, apiClient handles this
  await apiClient(`${BORROW_REQUESTS_ENDPOINT}/${id}`, {
    method: "DELETE",
    requiresAuth: true,
    skipPrefix: false // Use /api prefix
  });
  return true; // Assume success if apiClient doesn't throw
};

/**
 * Gets borrow requests by status
 * @param {string} status - The status to filter by (e.g., 'PENDING', 'APPROVED')
 * @returns {Promise<Array>} List of borrow requests with the specified status
 */
export const getBorrowRequestsByStatus = async (status) => {
  if (!status) {
    throw new Error("Status parameter is required.");
  }
  // Remove manual token check and Authorization header
  // Removed extra brace

  // Use apiClient
  return apiClient(`${BORROW_REQUESTS_ENDPOINT}/status/${encodeURIComponent(status)}`, {
    method: "GET",
    requiresAuth: true,
    skipPrefix: false // Use /api prefix
  });
};

/**
 * Gets borrow requests by requester ID
 * @param {string|number} requesterId - The ID or email of the requester
 * @returns {Promise<Array>} List of borrow requests associated with the specified requester
 */
export const getBorrowRequestsByRequester = async (requesterId) => {
  if (!requesterId) {
    throw new Error("Requester ID is required.");
  }
  
  // Determine if requesterId is an email
  const isEmail = typeof requesterId === 'string' && requesterId.includes('@');
  
  try {
    let endpoint;
    if (isEmail) {
      // If it's an email, use the user endpoint instead
      // This assumes the backend has an endpoint to get requests by user email
      endpoint = `/borrowrequests/user/${encodeURIComponent(requesterId)}`;
    } else {
      // Otherwise use the regular endpoint
      endpoint = `/borrowrequests/requester/${requesterId}`;
    }
    
    // Use apiClient
    return await apiClient(endpoint, {
      method: "GET",
      requiresAuth: true,
      skipPrefix: false // Use /api prefix
    });
  } catch (error) {
    console.error(`Error fetching borrow requests for requester ${requesterId}:`, error);
    // Return empty array on error to prevent UI crashes
    return [];
  }
};

/**
 * Gets borrow requests by game owner ID
 * @param {number} ownerId - The ID of the game owner
 * @returns {Promise<Array>} List of borrow requests associated with the specified game owner
 */
export const getBorrowRequestsByOwner = async (ownerId) => {
    if (!ownerId) {
        throw new Error("Owner ID is required.");
    }
    // Remove manual token check and Authorization header
    // Removed extra brace

    // Use apiClient
    return apiClient(`${BORROW_REQUESTS_ENDPOINT}/by-owner/${ownerId}`, { // Changed path for troubleshooting
        method: "GET",
        requiresAuth: true,
        skipPrefix: false // Use /api prefix
    });
};

/**
 * Gets games that a user is currently borrowing with approved status
 * that can be used for creating events
 * 
 * @param {string} userId - The ID of the user
 * @returns {Promise<Array>} List of borrowed games that can be used for events
 */
export const getActiveBorrowedGames = async (userId) => {
    if (!userId) {
        throw new Error("User ID is required.");
    }
    
    try {
        // Get user's borrow requests
        const requests = await getBorrowRequestsByRequester(userId);
        
        console.log("Active borrow requests:", requests);
        
        // Filter for approved requests where the end date is in the future
        const activeRequests = requests.filter(req => 
            req.status === 'APPROVED' && 
            new Date(req.endDate) > new Date()
        );
        
        console.log("Filtered active requests:", activeRequests);
        
        // Get game details for each request
        const borrowedGames = await Promise.all(
            activeRequests.map(async (req) => {
                try {
                    // Import the game-api functions to avoid circular dependencies
                    const { getGameById } = await import('./game-api.js');
                    
                    // Validate the requestedGameId
                    if (!req.requestedGameId || req.requestedGameId <= 0) {
                        console.error(`Invalid requestedGameId in borrow request:`, req);
                        return null;
                    }
                    
                    const game = await getGameById(req.requestedGameId);
                    
                    console.log(`Got game details for borrowed game ${req.requestedGameId}:`, game);
                    
                    // Ensure we have valid game data
                    if (!game || !game.id) {
                        console.error(`Invalid game data returned for ID ${req.requestedGameId}`);
                        return null;
                    }
                    
                    // Add borrow request information to the game
                    // Making sure to include the requestedGameId as gameId for event creation
                    return {
                        ...game,
                        isBorrowed: true,
                        borrowStartDate: req.startDate,
                        borrowEndDate: req.endDate,
                        gameInstanceId: req.gameInstanceId || req.id, // Fallback to request id if instance id is missing
                        borrowRequestId: req.id,
                        requestedGameId: req.requestedGameId // This is critical for event creation
                    };
                } catch (error) {
                    console.error(`Error fetching game details for borrowed game ID ${req.requestedGameId}:`, error);
                    return null;
                }
            })
        );
        
        // Filter out any failed game fetch attempts
        const filteredGames = borrowedGames.filter(game => game !== null);
        console.log(`Found ${filteredGames.length} active borrowed games for user ${userId}:`, filteredGames);
        
        return filteredGames;
    } catch (error) {
        console.error(`Error fetching active borrowed games for user ${userId}:`, error);
        return [];
    }
};

/**
 * Gets a borrow request by ID with additional details about the game instance
 * @param {number} requestId - The ID of the borrow request
 * @returns {Promise<Object>} Borrow request data with extra instance details
 */
export const getBorrowRequestWithInstanceDetails = async (requestId) => {
  if (!requestId) {
    throw new Error("Request ID is required to fetch borrow request details.");
  }
  
  console.log("Fetching detailed borrow request:", requestId);
  
  try {
    // First get the basic borrow request
    const borrowRequest = await apiClient(`/borrowrequests/${requestId}`, {
      method: "GET",
      skipPrefix: false
    });
    
    console.log("Got basic borrow request:", borrowRequest);
    
    // If request has a gameInstanceId, get instance details
    if (borrowRequest.gameInstanceId) {
      try {
        // Import the relevant functions to avoid circular dependencies
        const { getGameById, getGameInstances } = await import('./game-api.js');
        
        // Get the game data
        const game = await getGameById(borrowRequest.requestedGameId);
        console.log("Got game data for request:", game);
        
        // Get all instances for this game to find the specific one
        const instances = await getGameInstances(borrowRequest.requestedGameId);
        console.log(`Got ${instances.length} instances for game:`, instances);
        
        // Find the specific instance
        const targetInstance = instances.find(inst => inst.id === borrowRequest.gameInstanceId);
        
        if (targetInstance) {
          console.log("Found the specific instance:", targetInstance);
          
          // Merge the instance data into the borrow request
          return {
            ...borrowRequest,
            gameInstance: targetInstance,
            instanceOwner: targetInstance.owner || null
          };
        }
      } catch (instanceError) {
        console.error("Error fetching instance details:", instanceError);
        // Continue with the basic borrow request info
      }
    }
    
    // Return the original borrow request if we couldn't enhance it
    return borrowRequest;
  } catch (error) {
    console.error(`Error fetching borrow request ${requestId}:`, error);
    throw error;
  }
};