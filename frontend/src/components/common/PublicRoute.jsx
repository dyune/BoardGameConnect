import React from 'react';
import { useAuth } from '../../context/AuthContext';

/**
 * Public route component that renders different content based on authentication status.
 * This allows pages to be accessible to all users but with different functionality
 * depending on whether they're logged in.
 * 
 * @param {Object} props - Component props
 * @param {React.ReactNode} props.children - Default content for authenticated users
 * @param {React.ReactNode} props.guestContent - Content for unauthenticated users
 * @param {boolean} props.restrictContent - Whether to show restricted content for guests
 * @returns {React.ReactNode} - The appropriate content based on auth status
 */
const PublicRoute = ({ children, guestContent, restrictContent = false }) => {
  const { isAuthenticated, loading } = useAuth();

  // If authentication is still loading, show the default content
  // This prevents flashing between guest/auth content during auth check
  if (loading) {
    return children;
  }

  // Authenticated users always see the full content
  if (isAuthenticated) {
    return children;
  }

  // For guests, show either restricted content or the full content
  // depending on the restrictContent flag
  if (restrictContent && guestContent) {
    return guestContent;
  }

  // If no restriction or no guest content provided, show the default content
  return children;
};

export default PublicRoute; 