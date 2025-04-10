/**
 * Date formatting utilities
 */

/**
 * Formats an ISO date string into "Month of Year" format
 * Example: "2024-07-17T01:05:01.499Z" -> "July 2024"
 * 
 * @param {string} dateString - ISO date string
 * @param {object} options - Formatting options
 * @returns {string} Formatted date string
 */
export const formatJoinDate = (dateString, options = {}) => {
  if (!dateString) return 'Unknown date';
  
  try {
    const date = new Date(dateString);
    
    // Check for invalid date
    if (isNaN(date.getTime())) {
      return 'Invalid date';
    }
    
    const formatter = new Intl.DateTimeFormat('en-US', {
      month: 'long',
      year: 'numeric',
      ...options
    });
    
    return formatter.format(date);
  } catch (error) {
    console.error('Date formatting error:', error);
    return 'Error formatting date';
  }
};

/**
 * Formats an ISO date string into a relative time format (e.g., "3 months ago")
 * 
 * @param {string} dateString - ISO date string
 * @returns {string} Relative time string
 */
export const formatRelativeTime = (dateString) => {
  if (!dateString) return 'Unknown time';
  
  try {
    const date = new Date(dateString);
    
    // Check for invalid date
    if (isNaN(date.getTime())) {
      return 'Invalid date';
    }
    
    const now = new Date();
    const diffInMs = now - date;
    const diffInSecs = Math.floor(diffInMs / 1000);
    const diffInMins = Math.floor(diffInSecs / 60);
    const diffInHours = Math.floor(diffInMins / 60);
    const diffInDays = Math.floor(diffInHours / 24);
    const diffInMonths = Math.floor(diffInDays / 30);
    const diffInYears = Math.floor(diffInMonths / 12);
    
    if (diffInYears > 0) {
      return `${diffInYears} year${diffInYears > 1 ? 's' : ''} ago`;
    } else if (diffInMonths > 0) {
      return `${diffInMonths} month${diffInMonths > 1 ? 's' : ''} ago`;
    } else if (diffInDays > 0) {
      return `${diffInDays} day${diffInDays > 1 ? 's' : ''} ago`;
    } else if (diffInHours > 0) {
      return `${diffInHours} hour${diffInHours > 1 ? 's' : ''} ago`;
    } else if (diffInMins > 0) {
      return `${diffInMins} minute${diffInMins > 1 ? 's' : ''} ago`;
    } else {
      return 'Just now';
    }
  } catch (error) {
    console.error('Date formatting error:', error);
    return 'Error formatting date';
  }
};

/**
 * Formats an ISO date string into a custom format
 * 
 * @param {string} dateString - ISO date string
 * @param {object} options - Intl.DateTimeFormat options
 * @returns {string} Formatted date string
 */
export const formatDate = (dateString, options = {}) => {
  if (!dateString) return 'Unknown date';
  
  try {
    const date = new Date(dateString);
    
    // Check for invalid date
    if (isNaN(date.getTime())) {
      return 'Invalid date';
    }
    
    const defaultOptions = {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    };
    
    const formatter = new Intl.DateTimeFormat('en-US', {
      ...defaultOptions,
      ...options
    });
    
    return formatter.format(date);
  } catch (error) {
    console.error('Date formatting error:', error);
    return 'Error formatting date';
  }
}; 