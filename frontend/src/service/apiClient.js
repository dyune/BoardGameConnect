// Base API client for making HTTP requests to the backend
// This is a simplified version that removes complex refresh token logic
// and focuses on clean error handling and proper URL construction

// Base URL for API requests - should be configured from environment in production
const BASE_URL = 'http://localhost:8080';
const API_PREFIX = '/api';
const DEFAULT_TIMEOUT_MS = 8000; // 8 second timeout

// Indicator for auth in progress
export let authInProgress = false;
export let lastAuthCheck = 0;

// Custom error classes for better error handling
export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export class ConnectionError extends ApiError {
  constructor(message = 'Could not connect to server') {
    super(message, 0);
    this.name = 'ConnectionError';
  }
}

export class UnauthorizedError extends ApiError {
  constructor(message = 'Authentication required') {
    super(message, 401);
    this.name = 'UnauthorizedError';
  }
}

export class ForbiddenError extends ApiError {
  constructor(message = 'Forbidden') {
    super(message, 403);
    this.name = 'ForbiddenError';
  }
}

export class NotFoundError extends ApiError {
  constructor(message = 'Resource not found') {
    super(message, 404);
    this.name = 'NotFoundError';
  }
}

/**
 * Set the authentication in progress state
 * @param {boolean} inProgress - Whether authentication is in progress
 */
export const setAuthInProgress = (inProgress) => {
  authInProgress = inProgress;
  // Record timestamp of last auth state change
  if (inProgress) {
    lastAuthCheck = Date.now();
  }
};

/**
 * Utility to build a complete URL from an endpoint
 * @param {string} endpoint - The API endpoint
 * @param {boolean} skipPrefix - Whether to skip adding the API prefix (defaults to true)
 * @returns {string} The complete URL
 */
const buildUrl = (endpoint, skipPrefix = true) => {
  // If endpoint already starts with http, it's already a full URL
  if (endpoint.startsWith('http')) {
    return endpoint;
  }

  // If endpoint already includes the base URL
  if (endpoint.includes(BASE_URL)) {
    return endpoint;
  }

  // Auth endpoints and some others don't need API prefix
  const noApiPrefixPaths = ['/auth', '/profile', '/users/me']; // Added /users/me
  const hasPrefix = endpoint.startsWith(API_PREFIX);
  const shouldNotPrefix = noApiPrefixPaths.some(path => endpoint.startsWith(path));

  // Skip prefix if explicitly requested or if it's a special path
  if (skipPrefix || shouldNotPrefix || hasPrefix) {
    // Ensure leading slash if missing after BASE_URL
    const path = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
    return `${BASE_URL}${path}`;
  }

  // Add API prefix for regular API endpoints
  // Ensure leading slash if missing
  const path = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  return `${BASE_URL}${API_PREFIX}${path}`;
};


/**
 * Check if an endpoint is an authentication endpoint
 * @param {string} endpoint - The API endpoint
 * @returns {boolean} - Whether this is an auth endpoint
 */
const isAuthEndpoint = (endpoint) => {
  return endpoint.includes('/auth/login') ||
         endpoint.includes('/auth/logout') ||
         endpoint.includes('/users/me') || // Match both /users/me and /api/users/me
         endpoint.includes('/api/users/me');
};

/**
 * Get all cookies as an object
 * @returns {Object} Object containing all cookies
 */
const getCookies = () => {
  if (typeof document === 'undefined' || !document.cookie) {
    return {}; // Return empty object if no cookies are set or not in browser
  }
  return document.cookie.split(';').reduce((prev, current) => {
    const [name, ...value] = current.split('=');
    // Trim whitespace from name and value
    const trimmedName = name ? name.trim() : '';
    const trimmedValue = value.join('=').trim();
    if (trimmedName) { // Ensure name is not empty after trimming
      prev[trimmedName] = trimmedValue;
    }
    return prev;
  }, {});
};

/**
 * Get the authentication token from storage (DEPRECATED for HttpOnly)
 * Rely on browser sending HttpOnly cookie automatically.
 * This function is kept for potential legacy/debugging but shouldn't be relied upon for auth checks.
 * @returns {string|null} The token or null if not found
 */
const getAuthToken = () => {
  // Primary check should be the backend verifying the HttpOnly cookie.
  // This function is now mostly for debugging or legacy compatibility.
  const cookies = getCookies();
  const hasAuthCookie = cookies.isAuthenticated === 'true';

  if (hasAuthCookie) {
    // Indicate that cookie-based authentication is *expected*.
    // Doesn't guarantee the HttpOnly cookie is actually present/valid.
    return 'cookie-auth-present';
  }

  // Fallback to localStorage (consider removing if fully migrated)
  const token = localStorage.getItem('token');
  if (token) {
    console.warn("Found token in localStorage. This is deprecated for HttpOnly cookie auth.");
    return token;
  }

  return null;
};


/**
 * Check if authentication state processing is complete.
 * This check is simplified and primarily focuses on whether an auth operation
 * (login/logout/refresh) is currently in progress. It does NOT guarantee
 * that the user *is* authenticated, only that the system isn't in a transitional auth state.
 * @returns {boolean} Whether authentication is ready for API requests
 */
const isAuthReady = () => {
  // The most reliable check is simply whether an auth operation is in progress.
  // Rely on the API calls themselves to fail with 401 if cookies aren't sent/valid.
  if (authInProgress) {
    console.log('[isAuthReady] Returning false (auth operation in progress)');
    return false;
  }
  console.log('[isAuthReady] Returning true (no auth operation in progress)');
  return true;
};


/**
 * Get cookie auth state for debugging
 * @returns {Object} Cookie authentication state
 */
export const getCookieAuthState = () => {
  const cookies = getCookies();
  // Simplify debugging state to only show isAuthenticated status
  return {
    isAuthenticated: cookies.isAuthenticated === 'true',
    // hasAccessToken check removed as it was unreliable and misleading
    allCookies: typeof document !== 'undefined' ? document.cookie : 'N/A (SSR/Worker?)'
  };
};

/**
 * Main API client function for making HTTP requests
 * @param {string} endpoint - The API endpoint
 * @param {Object} options - Request options
 * @returns {Promise<any>} - Response data
 */
const apiClient = async (endpoint, {
  body,
  method = 'GET',
  headers = {},
  timeout = DEFAULT_TIMEOUT_MS,
  skipPrefix = true, // Default skipPrefix to true as most auth/user endpoints don't use /api
  suppressErrors = false,
  retryOnAuth = true,
  requiresAuth = true,  // Assume endpoints require auth unless specified
  credentials = 'include', // Always include credentials by default
  returnHeaders = false, // Option to return headers along with data
  responseType = 'json', // Default to JSON, but allow 'text' for manual parsing
  ...customConfig
} = {}) => {
  // Special handling for /users/me endpoint - don't suppress errors by default
  const isUserMeEndpoint = endpoint.includes('/users/me');
  const effectiveSuppressErrors = isUserMeEndpoint ? false : suppressErrors;

  // Check if this is a login, logout, or user profile endpoint
  const isAuthActionEndpoint = endpoint.includes('/auth/login') || endpoint.includes('/auth/logout');
  const isProfileEndpoint = endpoint.includes('/users/me');
  
  // If auth is in progress and this isn't an auth action endpoint, wait or abort
  if (authInProgress && !isAuthActionEndpoint) {
    console.log(`[API] Auth in progress, delaying request to ${endpoint}`);
    
    // Special case for /users/me when checking auth status
    if (isProfileEndpoint && retryOnAuth === false) {
      console.log(`[API] Special handling for auth check to ${endpoint} - proceeding despite auth in progress`);
      // Continue with the request for this special case
    } else if (retryOnAuth && Date.now() - lastAuthCheck < 500) { // Increased wait time to 500ms
      console.log(`[API] Waiting up to 500ms for auth to complete for ${endpoint}`);
      await new Promise(resolve => setTimeout(resolve, 100));
      if (authInProgress) {
        await new Promise(resolve => setTimeout(resolve, 400)); // Wait longer
      }
      if (authInProgress) {
        console.error(`[API] Auth still in progress after waiting, aborting request to ${endpoint}`);
        throw new UnauthorizedError('Authentication in progress');
      }
      console.log(`[API] Auth completed, proceeding with request to ${endpoint}`);
    } else {
      console.error(`[API] Auth in progress, aborting immediate request to ${endpoint}`);
      throw new UnauthorizedError('Authentication in progress');
    }
  }

  // Check if auth is ready only for endpoints that require it and are not auth actions
  const shouldCheckAuth = requiresAuth && !isAuthActionEndpoint;

  // The isAuthReady check is now simplified. We rely more on the backend returning 401.
  // If an endpoint requires auth, we still ensure no auth operation is actively running.
  if (shouldCheckAuth && !isAuthReady()) {
     console.error(`[API] Auth not ready (operation in progress), aborting request to ${endpoint}`);
     // This path should ideally not be hit often if authInProgress logic above works.
     throw new UnauthorizedError('Authentication not ready (operation in progress)');
  }

  // Get the user ID from localStorage if available
  const userId = localStorage.getItem('userId');
  
  // Check for rememberMe status to adapt requests
  const rememberMeSetting = localStorage.getItem('rememberMe') === 'true';
  
  // Prepare headers
  const requestHeaders = { ...headers };
  
  // Add rememberMe flag as a header for ALL requests that require auth
  // This ensures the server always knows the current rememberMe preference for cookie handling
  if (requiresAuth && !requestHeaders['X-Remember-Me']) {
    requestHeaders['X-Remember-Me'] = rememberMeSetting ? 'true' : 'false';
    console.log(`[API] Adding X-Remember-Me header: ${rememberMeSetting} for ${endpoint}`);
  }
  
  // Add userId to headers if available (unless already specified)
  // This helps with user identification for logging/auditing on the backend
  if (userId && !requestHeaders['X-User-Id']) {
    requestHeaders['X-User-Id'] = userId;
  }

  // Request has a body, add appropriate content-type if not set
  if (body !== undefined && body !== null) {
    // If Content-Type not specified and body is an object that's not FormData, set to JSON
    if (!requestHeaders['Content-Type'] && 
        typeof body === 'object' && 
        !(body instanceof FormData)) {
      requestHeaders['Content-Type'] = 'application/json';
    }
  }

  // Prepare request config - always include credentials for cookie-based auth
  const config = {
    method,
    headers: requestHeaders,
    credentials: 'include', // Always include credentials to ensure cookies are sent
    ...customConfig,
  };

  // Add the body to the request if it exists
  if (body) {
    // If body is an object and Content-Type is application/json, stringify it
    if (typeof body === 'object' && 
        !(body instanceof FormData) && 
        requestHeaders['Content-Type'] === 'application/json') {
      config.body = JSON.stringify(body);
    } else {
      config.body = body;
    }
  }

  // Build the complete URL
  const url = buildUrl(endpoint, skipPrefix);

  // Setup request timeout
  const controller = new AbortController();
  config.signal = customConfig.signal || controller.signal;
  const timeoutId = setTimeout(() => controller.abort(), timeout);

  try {
    // Log cookie state before making request
    console.log(`[API] Making ${config.method} request to ${url} with credentials: ${config.credentials}`);
    console.log(`[API] Cookie state before request:`, getCookieAuthState());
    console.log(`[API] Request headers:`, config.headers);
    
    // Make the request
    const response = await fetch(url, config);

    // Clear the timeout
    clearTimeout(timeoutId);
    
    // Log cookie state after response
    console.log(`[API] Response status: ${response.status} for ${url}`);
    console.log(`[API] Cookie state after response:`, getCookieAuthState());

    // Handle different response statuses
    if (!response.ok) {
      // Pass the original endpoint for better error context
      await handleErrorResponse(response, endpoint, effectiveSuppressErrors);
    }

    // Check for no content responses
    if (response.status === 204) {
      return returnHeaders ? { data: null, headers: response.headers } : null;
    }

    let responseData;
    
    // Handle response based on requested responseType
    if (responseType === 'text') {
      // Return raw text for manual parsing
      responseData = await response.text();
    } else {
      // Parse response based on content type
      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        try {
          responseData = await response.json();
        } catch (parseError) {
          console.error(`[API] JSON parse error for ${url}:`, parseError);
          // If JSON parsing fails, return the raw text and let the caller handle it
          responseData = await response.text();
        }
      } else {
        // Return text for non-JSON responses
        responseData = await response.text();
      }
    }
    
    // Return data with headers if requested
    return returnHeaders ? { data: responseData, headers: response.headers } : responseData;
  } catch (error) {
    // Clear the timeout
    clearTimeout(timeoutId);

    // Handle aborted requests (timeout)
    if (error.name === 'AbortError') {
      console.error(`[API] Request timeout after ${timeout}ms: ${url}`);
      throw new TimeoutError(`Request to ${url} timed out after ${timeout}ms`);
    }

    // Handle network errors
    if (error instanceof TypeError && error.message.includes('fetch')) {
       console.error(`[API] Network error for ${url}:`, error);
       throw new ConnectionError(`Could not connect to ${url}. Check network or server status.`);
    }

    // Rethrow API errors (UnauthorizedError, ForbiddenError, etc.)
    if (error instanceof ApiError) {
      throw error;
    }

    // Handle other errors
    if (!effectiveSuppressErrors) {
      console.error(`[API] Unexpected request error for ${url}:`, error);
    }
    // Throw a generic ApiError for unexpected issues
    throw new ApiError(error.message || 'Unknown error occurred during API request', 500);
  }
};

/**
 * Handle error responses from the API
 * @param {Response} response - The fetch response object
 * @param {string} endpoint - The original endpoint requested
 * @param {boolean} suppressErrors - Whether to suppress error logging
 * @throws {ApiError} - Different types of API errors based on status code
 */
async function handleErrorResponse(response, endpoint, suppressErrors = false) {
  let errorMessage = `Request failed with status ${response.status}`;
  let errorData = null;

  // Try to parse error response body
  try {
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      errorData = await response.json();
      
      // Priority order for error message fields
      if (errorData) {
        if (typeof errorData.detail === 'string') {
          // Spring Boot error responses typically use 'detail'
          errorMessage = errorData.detail;
        } else if (typeof errorData.message === 'string') {
          errorMessage = errorData.message;
        } else if (typeof errorData.error === 'string') {
          errorMessage = errorData.error;
        } else if (typeof errorData.errorMessage === 'string') {
          errorMessage = errorData.errorMessage;
        }
      }
    } else {
      const text = await response.text();
      if (text) {
        errorMessage = text; // Use text response if not JSON
        
        // Try to extract JSON from text response (some errors might be JSON embedded in text)
        if (text.includes('{') && text.includes('}')) {
          try {
            const jsonStart = text.indexOf('{');
            const jsonEnd = text.lastIndexOf('}') + 1;
            const jsonStr = text.substring(jsonStart, jsonEnd);
            const jsonData = JSON.parse(jsonStr);
            
            if (jsonData.detail) {
              errorMessage = jsonData.detail;
            }
          } catch (jsonParseErr) {
            // Ignore JSON parsing errors in text
            console.debug(`[API] Could not extract JSON from text response: ${jsonParseErr.message}`);
          }
        }
      }
    }
  } catch (err) {
    // Ignore error parsing errors, use default message
    console.warn(`[API] Could not parse error response body for ${endpoint}`, err);
  }

  // Log error unless suppressed
  if (!suppressErrors) {
     console.error(`[API] Error response for ${endpoint}: Status ${response.status}, Message: ${errorMessage}`, errorData || '');
  }

  // Throw appropriate error based on status code
  switch (response.status) {
    case 400:
      throw new ApiError(errorMessage || 'Bad request', 400);
    case 401:
      // A 401 means the request was not authenticated.
      console.warn(`[API] Unauthorized (401) received for ${endpoint}. Auth state might be invalid.`);
      throw new UnauthorizedError(errorMessage || 'Authentication required. Please log in.');
    case 403:
      throw new ForbiddenError(errorMessage || 'Forbidden: You do not have permission to access this resource.');
    case 404:
      throw new NotFoundError(errorMessage || `Resource not found at ${endpoint}.`);
    default:
      // Include status in the generic ApiError message, but only if the error message doesn't already contain it
      const statusSuffix = errorMessage.includes(response.status.toString()) ? '' : ` (Status: ${response.status})`;
      throw new ApiError(`${errorMessage}${statusSuffix}`, response.status);
  }
}

// Export TimeoutError class
export class TimeoutError extends ApiError {
  constructor(message = 'Request timed out') {
    super(message, 408); // Use 408 status code for timeout
    this.name = 'TimeoutError';
  }
}

/**
 * Extracts authentication token from a response
 * @param {Response} response - The fetch Response object 
 * @returns {string|null} - The token or null if not found
 */
export const extractAuthToken = (response) => {
  if (!response || !response.headers) {
    return null;
  }
  
  // Try various possible auth header names
  const token = 
    response.headers.get('x-auth-token') || 
    response.headers.get('X-Auth-Token') || 
    response.headers.get('authorization') ||
    response.headers.get('Authorization');
    
  if (token) {
    const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
    localStorage.setItem('token', cleanToken);
    console.log('[API] Saved authentication token from response headers');
    return cleanToken;
  }
  
  return null;
};

export default apiClient;