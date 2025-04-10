/**
 * Borrow Request Service
 * 
 * This file provides API functions for interacting with the borrow request controller endpoints.
 */

const API_URL = '/api/v1';

/**
 * Create a new borrow request
 * @param {Object} requestData - The borrow request data
 * @returns {Promise<Object>} Created borrow request
 */
export const createBorrowRequest = async (requestData) => {
  try {
    const response = await fetch(`${API_URL}/borrowrequests`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestData),
    });
    
    if (!response.ok) {
      throw new Error(`Error creating borrow request: ${response.statusText}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Failed to create borrow request:', error);
    throw error;
  }
};

/**
 * Get a borrow request by ID
 * @param {number} id - The borrow request ID
 * @returns {Promise<Object>} Borrow request data
 */
export const getBorrowRequestById = async (id) => {
  try {
    const response = await fetch(`${API_URL}/borrowrequests/${id}`);
    if (!response.ok) {
      throw new Error(`Error fetching borrow request: ${response.statusText}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Failed to fetch borrow request:', error);
    throw error;
  }
};

/**
 * Get all borrow requests
 * @returns {Promise<Array>} Array of borrow requests
 */
export const getAllBorrowRequests = async () => {
  try {
    const response = await fetch(`${API_URL}/borrowrequests`);
    if (!response.ok) {
      throw new Error(`Error fetching borrow requests: ${response.statusText}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Failed to fetch borrow requests:', error);
    throw error;
  }
};

/**
 * Update a borrow request's status
 * @param {number} id - The borrow request ID
 * @param {Object} requestData - Updated request data (status)
 * @returns {Promise<Object>} Updated borrow request
 */
export const updateBorrowRequestStatus = async (id, requestData) => {
  try {
    const response = await fetch(`${API_URL}/borrowrequests/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestData),
    });
    
    if (!response.ok) {
      throw new Error(`Error updating borrow request: ${response.statusText}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Failed to update borrow request:', error);
    throw error;
  }
};

/**
 * Delete a borrow request
 * @param {number} id - The borrow request ID
 * @returns {Promise<void>}
 */
export const deleteBorrowRequest = async (id) => {
  try {
    const response = await fetch(`${API_URL}/borrowrequests/${id}`, {
      method: 'DELETE',
    });
    
    if (!response.ok) {
      throw new Error(`Error deleting borrow request: ${response.statusText}`);
    }
  } catch (error) {
    console.error('Failed to delete borrow request:', error);
    throw error;
  }
};

/**
 * Get borrow requests by status
 * @param {string} status - The status to filter by (e.g., "PENDING", "APPROVED")
 * @returns {Promise<Array>} Array of filtered borrow requests
 */
export const getBorrowRequestsByStatus = async (status) => {
  try {
    const response = await fetch(`${API_URL}/borrowrequests/status/${status}`);
    if (!response.ok) {
      throw new Error(`Error fetching borrow requests by status: ${response.statusText}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Failed to fetch borrow requests by status:', error);
    throw error;
  }
};

/**
 * Get borrow requests for a specific requester
 * @param {number} requesterId - The requester's ID
 * @returns {Promise<Array>} Array of requester's borrow requests
 */
export const getBorrowRequestsByRequester = async (requesterId) => {
  try {
    const response = await fetch(`${API_URL}/borrowrequests/requester/${requesterId}`);
    if (!response.ok) {
      throw new Error(`Error fetching requester's borrow requests: ${response.statusText}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Failed to fetch requester\'s borrow requests:', error);
    throw error;
  }
}; 