/**
 * Game Service
 * 
 * This file provides API functions for interacting with the game controller endpoints.
 */

const API_URL = '/api/v1';

/**
 * Get a game by its ID
 * @param {number} id - The game ID
 * @returns {Promise<Object>} Game data
 */
export const getGameById = async (id) => {
  try {
    const response = await fetch(`${API_URL}/games/${id}`);
    if (!response.ok) {
      throw new Error(`Error fetching game: ${response.statusText}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Failed to fetch game:', error);
    throw error;
  }
};

/**
 * Get all games with optional filtering
 * @param {Object} options - Filter options
 * @returns {Promise<Array>} Array of games
 */
export const getAllGames = async (options = {}) => {
  try {
    const { ownerId, category, namePart } = options;
    let url = `${API_URL}/games`;
    
    const params = new URLSearchParams();
    if (ownerId) params.append('ownerId', ownerId);
    if (category) params.append('category', category);
    if (namePart) params.append('namePart', namePart);
    
    if (params.toString()) {
      url += `?${params.toString()}`;
    }
    
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Error fetching games: ${response.statusText}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Failed to fetch games:', error);
    throw error;
  }
};

/**
 * Get all reviews for a specific game
 * @param {number} gameId - The game ID
 * @returns {Promise<Array>} Array of reviews
 */
export const getGameReviews = async (gameId) => {
  try {
    const response = await fetch(`${API_URL}/games/${gameId}/reviews`);
    if (!response.ok) {
      throw new Error(`Error fetching reviews: ${response.statusText}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Failed to fetch reviews:', error);
    throw error;
  }
};

/**
 * Get average rating for a game
 * @param {number} gameId - The game ID
 * @returns {Promise<number>} Average rating
 */
export const getGameRating = async (gameId) => {
  try {
    const response = await fetch(`${API_URL}/games/${gameId}/rating`);
    if (!response.ok) {
      throw new Error(`Error fetching rating: ${response.statusText}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Failed to fetch rating:', error);
    throw error;
  }
};

/**
 * Submit a new review for a game
 * @param {number} gameId - The game ID
 * @param {Object} reviewData - Review details
 * @returns {Promise<Object>} Created review
 */
export const submitGameReview = async (gameId, reviewData) => {
  try {
    const response = await fetch(`${API_URL}/games/${gameId}/reviews`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(reviewData),
    });
    
    if (!response.ok) {
      throw new Error(`Error submitting review: ${response.statusText}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Failed to submit review:', error);
    throw error;
  }
};

/**
 * Get game instances by game ID (all copies of a specific game)
 * This function assumes copies are returned by getting games with the same name
 * @param {number} gameId - The game ID
 * @returns {Promise<Array>} Array of game instances
 */
export const getGameInstances = async (gameId) => {
  try {
    // First get the game to find its name
    const game = await getGameById(gameId);
    
    // Then find all games with this name (different copies)
    const instances = await getAllGames({ namePart: game.name });
    
    // Return all instances except the original one
    return instances.filter(instance => instance.id !== gameId);
  } catch (error) {
    console.error('Failed to fetch game instances:', error);
    throw error;
  }
};

/**
 * Get all games owned by a specific owner
 * @param {string} ownerId - The owner ID (email)
 * @returns {Promise<Array>} Array of games
 */
export const getGamesByOwner = async (ownerId) => {
  try {
    const response = await fetch(`${API_URL}/users/${ownerId}/games`);
    if (!response.ok) {
      throw new Error(`Error fetching owner's games: ${response.statusText}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Failed to fetch owner games:', error);
    throw error;
  }
}; 