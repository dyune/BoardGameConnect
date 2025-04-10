import { clsx } from "clsx"
import { twMerge } from "tailwind-merge"

/**
 * A utility function for conditionally joining class names together.
 * It uses clsx to conditionally merge classes and tailwind-merge to handle
 * Tailwind CSS class conflicts.
 *
 * @param {...any} inputs - Class names or conditional objects
 * @returns {string} - Merged class names
 */
export function cn(...inputs) {
  return twMerge(clsx(inputs))
}

/**
 * Format date to a readable string
 * @param {string|Date} date - Date to format
 * @param {object} options - Intl.DateTimeFormat options
 * @returns {string} - Formatted date string
 */
export function formatDate(date, options = {}) {
  const defaultOptions = {
    year: "numeric",
    month: "short",
    day: "numeric"
  };
  
  try {
    return new Intl.DateTimeFormat(
      "en-US", 
      { ...defaultOptions, ...options }
    ).format(new Date(date));
  } catch (error) {
    console.error("Date formatting error:", error);
    return "Invalid date";
  }
}

/**
 * Truncate text with ellipsis
 * @param {string} text - Text to truncate
 * @param {number} maxLength - Maximum length
 * @returns {string} - Truncated text
 */
export function truncateText(text, maxLength = 100) {
  if (!text) return "";
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength) + "...";
}

/**
 * Safely access nested object properties
 * @param {object} obj - The object to access
 * @param {string} path - Path to the property (e.g., "user.address.street")
 * @param {any} defaultValue - Default value if property doesn't exist
 * @returns {any} - Value at path or default value
 */
export function getNestedValue(obj, path, defaultValue = null) {
  if (!obj || !path) return defaultValue;
  
  const keys = path.split('.');
  let current = obj;
  
  for (const key of keys) {
    if (current === undefined || current === null || !Object.prototype.hasOwnProperty.call(current, key)) {
      return defaultValue;
    }
    current = current[key];
  }
  
  return current === undefined ? defaultValue : current;
} 