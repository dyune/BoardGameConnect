import apiClient, { UnauthorizedError } from './apiClient';

/**
 * Fetch incoming borrow requests with retry for auth issues
 */
export async function getIncomingBorrowRequests(gameOwnerId, retryCount = 0) {
  const MAX_RETRIES = 2;
  
  // Add delay for retry attempts
  if (retryCount > 0) {
    await new Promise(resolve => setTimeout(resolve, 800));
  }
  
  try {
    // Get userId for request headers
    const userId = localStorage.getItem('userId');
    
    // Debug auth state
    console.log(`[API] getIncomingBorrowRequests for owner: ${gameOwnerId}, userId: ${userId}`);
    
    // The backend uses cookies for authentication, we just need the user ID in headers
    const headers = {
      'X-User-Id': userId || gameOwnerId
    };
    
    // Assuming the backend endpoint for incoming requests is /api/borrowrequests/gameOwner/{gameOwnerId}
    return await apiClient(`/api/borrowrequests/gameOwner/${gameOwnerId}`, {
      skipPrefix: false,
      retryOnAuth: true,
      credentials: 'include', // Important for cookie-based auth
      headers
    });
  } catch (error) {
    if (error instanceof UnauthorizedError && retryCount < MAX_RETRIES) {
      console.log(`Auth not ready, retrying incoming requests fetch (attempt ${retryCount + 1})`);
      return getIncomingBorrowRequests(gameOwnerId, retryCount + 1);
    }
    throw error;
  }
}

/**
 * Search for users based on search criteria with retry for auth issues
 * @param {object} searchParams - Search parameters (name, email, etc.)
 * @param {number} retryCount - Current retry attempt (internal use)
 * @returns {Promise<Array>} - Array of matching user objects
 */
export async function searchUsers(searchParams, retryCount = 0) {
  const MAX_RETRIES = 3;
  
  // Add delay for retry attempts with increasing delay times
  if (retryCount > 0) {
    const delay = retryCount * 1000;  // Progressive delay: 1s, 2s, 3s
    console.log(`Waiting ${delay}ms before retry ${retryCount}/${MAX_RETRIES}`);
    await new Promise(resolve => setTimeout(resolve, delay));
  }
  
  try {
    console.log(`[API Request] Searching for users with params:`, searchParams);
    
    // Get userId from localStorage for consistent auth
    const userId = localStorage.getItem('userId');
    if (!userId) {
      console.warn('[API Request] No userId found in localStorage for user search');
    }
    
    // Make sure we have proper credentials and authentication
    return await apiClient(`/users/search`, {
      method: "POST",
      body: searchParams,
      skipPrefix: false,
      retryOnAuth: true,
      credentials: 'include',
      headers: {
        'X-User-Id': userId
      }
    });
  } catch (error) {
    if (error instanceof UnauthorizedError && retryCount < MAX_RETRIES) {
      console.log(`Auth not ready, retrying user search (attempt ${retryCount + 1})`);
      return searchUsers(searchParams, retryCount + 1);
    }
    throw error;
  }
}

/**
 * Fetch outgoing borrow requests with retry for auth issues
 */
export async function getOutgoingBorrowRequests(accountId, retryCount = 0) {
  const MAX_RETRIES = 3;  // Increase max retries
  
  // Add delay for retry attempts with increasing delay times
  if (retryCount > 0) {
    const delay = retryCount * 1000;  // Progressive delay: 1s, 2s, 3s
    console.log(`Waiting ${delay}ms before retry ${retryCount}/${MAX_RETRIES}`);
    await new Promise(resolve => setTimeout(resolve, delay));
  }
  
  try {
    console.log(`[API Request] Fetching borrow requests for user ${accountId}`);
    
    // Get userId from localStorage for consistent auth
    const userId = localStorage.getItem('userId') || accountId;
    
    // Make sure we have proper credentials and authentication
    return await apiClient(`/api/borrowrequests/requester/${accountId}`, {
      skipPrefix: false,
      retryOnAuth: true,
      credentials: 'include',
      headers: {
        'X-User-Id': userId
      }
    });
  } catch (error) {
    if (error instanceof UnauthorizedError && retryCount < MAX_RETRIES) {
      console.log(`Auth not ready, retrying outgoing requests fetch (attempt ${retryCount + 1})`);
      return getOutgoingBorrowRequests(accountId, retryCount + 1);
    }
    throw error;
  }
}

export async function actOnBorrowRequest(requestId, request) {
  const userId = localStorage.getItem('userId');
  
  console.log("[API] Acting on borrow request:", {
    requestId,
    request,
    userId
  });
  
  // The backend expects just a simple JSON object with the status
  // No need to add additional fields
  
  return apiClient(`/api/borrowrequests/${requestId}`, {
    method: "PUT",
    body: request, // Send the request object directly
    skipPrefix: false,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': userId
    }
  });
}

/**
 * Updates a user's own borrow request with new details
 * Different from actOnBorrowRequest which is meant for owners to approve/reject
 * 
 * @param {number} requestId - The ID of the borrow request to update
 * @param {object} updatedData - The updated request data
 * @returns {Promise<Object>} Updated borrow request
 */
export async function updateUserBorrowRequest(requestId, updatedData) {
  const userId = localStorage.getItem('userId');
  
  console.log("[API] Updating user's borrow request:", {
    requestId,
    updatedData,
    userId
  });
  
  // For now, since our backend doesn't have a separate endpoint,
  // we'll just ensure we're only sending the status as PENDING
  // along with the updated fields
  const simplifiedRequest = {
    ...updatedData,
    status: 'PENDING' // Always ensure we're keeping the status as PENDING
  };
  
  // Use the dedicated user-update endpoint that properly checks permissions for requesters
  return apiClient(`/api/borrowrequests/${requestId}/user-update`, {
    method: "PUT",
    body: simplifiedRequest,
    skipPrefix: false,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': userId
    }
  });
}

/**
 * Fetch lending history with retry for auth issues
 */
export async function getLendingHistory(accountId, isOwner, retryCount = 0) {
  const MAX_RETRIES = 2;  // Reduce max retries to prevent excessive attempts
  
  // Add delay for retry attempts with increasing delay times
  if (retryCount > 0) {
    const delay = retryCount * 1000;  // Progressive delay: 1s, 2s
    console.log(`Waiting ${delay}ms before retry ${retryCount}/${MAX_RETRIES}`);
    await new Promise(resolve => setTimeout(resolve, delay));
  }
  
  try {
    console.log(`[API Request] Fetching lending history for user ${accountId}`);
    
    // Get userId for headers
    const userId = localStorage.getItem('userId') || accountId;
    
    // Debug auth state
    console.log(`[API] getLendingHistory for userId: ${userId}, isOwner: ${isOwner}`);
    
    // Create a timeout promise to prevent hanging requests
    const timeoutPromise = new Promise((_, reject) => {
      setTimeout(() => reject(new Error('Request timed out')), 8000);
    });
    
    // The backend uses cookies for authentication, we just need the user ID in headers
    const headers = {
      'X-User-Id': userId
    };
    
    // Make sure we have proper credentials and authentication
    // Use Promise.race to implement timeout
    const response = await Promise.race([
      apiClient(`/api/lending-records/${isOwner ? "owner" : "borrower"}/${accountId}`, {
        skipPrefix: false,
        retryOnAuth: true,
        credentials: 'include',
        headers
      }),
      timeoutPromise
    ]);
    
    // Validate response
    if (!response) {
      console.error('[API] Empty response received from lending history endpoint');
      return [];
    }
    
    if (!Array.isArray(response)) {
      console.error('[API] Expected array response but got:', typeof response);
      // Try to convert to array if possible (e.g., if it's an object with data property)
      if (response && response.data && Array.isArray(response.data)) {
        return response.data;
      }
      return [];
    }
    
    console.log(`[API] Successfully fetched lending history with ${response.length} records`);
    return response;
  } catch (error) {
    if (error instanceof UnauthorizedError && retryCount < MAX_RETRIES) {
      console.log(`Auth not ready, retrying lending history fetch (attempt ${retryCount + 1})`);
      return getLendingHistory(accountId, isOwner, retryCount + 1);
    }
    
    if (error.message === 'Request timed out') {
      console.error('[API] Lending history request timed out after 8 seconds');
    } else {
      console.error('[API] Error fetching lending history:', error);
    }
    
    // Return empty array instead of throwing to prevent UI from breaking
    return [];
  }
}

export async function markAsReturned(lendingRecordId, data) {
  try {
    // Get userId from localStorage for the header
    const userId = localStorage.getItem('userId');
    
    console.log(`[API] Marking record ${lendingRecordId} as returned. User ID: ${userId}, Data:`, data);
    
    const response = await apiClient(`/api/lending-records/${lendingRecordId}/mark-returned`, {
      method: "POST",
      body: {}, // Empty object is required by backend - simplified from data parameter
      skipPrefix: false,
      credentials: 'include',  // Include cookies for authentication
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': userId   // Include user ID in header
      },
    });
    console.log(`[API] Successfully marked record ${lendingRecordId} as returned. Response:`, response);
    return response;
  } catch (error) {
    console.error(`Error marking record ${lendingRecordId} as returned:`, error);
    throw error;
  }
}

export async function updateUsernamePassword(request) {
  return apiClient(`/account`, {
    method: "PUT",
    body: request,
    skipPrefix: false,
    credentials: 'include',
  });
}

export async function upgradeAccountToGameOwner(email) {
  const userId = localStorage.getItem('userId');
  return apiClient(`/account/${email}?userId=${userId}`, {
    method: "PUT",
    skipPrefix: false,
    credentials: 'include',
    headers: {
      'X-User-Id': userId
    }
  });
}

export async function getHostedEvents(hostId) {
  const userId = localStorage.getItem('userId');
  return apiClient(`/events/by-host-id/${hostId}?userId=${hostId}`, {
    skipPrefix: false,
    credentials: 'include',
    headers: {
      'X-User-Id': userId || hostId
    }
  });
}

export async function getLendingRecordByRequestId(requestId) {
  return apiClient(`/api/lending-records/request/${requestId}`, {
    method: "GET",
    skipPrefix: false,
  });
}

/**
 * Checks if the current authenticated user can review a specific game.
 * A user can only review a game if they have borrowed and returned it.
 * 
 * @param {number} gameId - The ID of the game to check
 * @returns {Promise<boolean>} - Promise resolving to true if the user can review the game, false otherwise
 */
export async function checkUserCanReviewGame(gameId) {
  const userId = localStorage.getItem('userId');
  
  try {
    // Get user's lending records
    const response = await apiClient(`/api/lending-records/can-review?gameId=${gameId}`, {
      method: "GET",
      skipPrefix: false,
      credentials: 'include',
      headers: {
        'X-User-Id': userId
      }
    });
    
    // Backend will return a boolean indicating if user can review
    return response.canReview === true;
  } catch (error) {
    console.error("Error checking if user can review game:", error);
    return false; // Default to false if there's an error
  }
}

export async function deleteBorrowRequest(requestId) {
  const userId = localStorage.getItem('userId');
  
  console.log("[API] Deleting borrow request:", {
    requestId,
    userId
  });
  
  return apiClient(`/api/borrowrequests/${requestId}`, {
    method: "DELETE",
    skipPrefix: false,
    credentials: 'include',
    headers: {
      'X-User-Id': userId
    }
  });
}