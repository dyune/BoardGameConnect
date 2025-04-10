import React, { useEffect, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Loader2 } from 'lucide-react';

/**
 * Protected route component that ensures users are authenticated.
 * Redirects to login page if not authenticated with return URL.
 * 
 * @param {Object} props - Component props
 * @param {React.ReactNode} props.children - Child components to render if authenticated
 * @param {Array<string>} [props.roles] - Optional roles required to access this route
 * @returns {React.ReactNode} - The protected children or redirect
 */
const ProtectedRoute = ({ children, roles }) => {
  const { user, isAuthenticated, loading, error, refreshUser, isSessionExpired } = useAuth();
  const location = useLocation();
  const [loadingTimer, setLoadingTimer] = useState(0);
  
  const EXTENDED_LOADING_THRESHOLD = 3; // Show extended message after 3 seconds

  // Increment loading timer every second when in loading state
  useEffect(() => {
    let timerId;
    if (loading) {
      timerId = setInterval(() => {
        setLoadingTimer(prev => prev + 1);
      }, 1000);
    } else {
      setLoadingTimer(0);
    }
    
    return () => {
      clearInterval(timerId);
    };
  }, [loading]);

  // Show loading state during initial authentication
  if (loading && loadingTimer > 1) {
    return (
      <div className="flex flex-col h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary mb-4" />
        
        {loadingTimer < EXTENDED_LOADING_THRESHOLD ? (
          <span className="text-lg">Loading...</span>
        ) : (
          <div className="text-center max-w-md px-4">
            <span className="text-lg mb-2">Still working on it...</span>
            <p className="text-sm text-muted-foreground mt-2">
              This is taking longer than expected. If this persists,
              try refreshing the page or clearing your browser cache.
            </p>
            {loadingTimer > 8 && (
              <button 
                onClick={() => window.location.reload()}
                className="mt-4 px-4 py-2 bg-primary text-white rounded hover:bg-primary/80"
              >
                Refresh Page
              </button>
            )}
          </div>
        )}
      </div>
    );
  }

  // Show session expired message if session has expired
  if (isSessionExpired) {
    return (
      <div className="flex h-screen flex-col items-center justify-center text-center p-4">
        <h2 className="text-xl font-bold mb-2">Session Expired</h2>
        <p className="mb-4 max-w-md">Your session has expired due to inactivity. Please log in again to continue.</p>
        <div className="flex gap-2">
          <button 
            onClick={() => window.location.href = `/login?redirect=${encodeURIComponent(location.pathname)}`}
            className="px-4 py-2 bg-primary text-white rounded hover:bg-primary/80"
          >
            Log In Again
          </button>
        </div>
      </div>
    );
  }

  // If not authenticated, redirect to login with return URL
  if (!isAuthenticated) {
    return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname)}`} replace />;
  }

  // If there's a connection error, show error message
  if (error) {
    return (
      <div className="flex h-screen flex-col items-center justify-center text-center p-4">
        <h2 className="text-xl font-bold mb-2">Connection Error</h2>
        <p className="mb-4 max-w-md">{error || "Unable to connect to the server. Please check your internet connection."}</p>
        <div className="flex gap-2">
          <button 
            onClick={() => refreshUser()}
            className="px-4 py-2 bg-primary text-white rounded hover:bg-primary/80"
          >
            Retry Connection
          </button>
          <button 
            onClick={() => window.location.reload()}
            className="px-4 py-2 bg-secondary text-white rounded hover:bg-secondary/80"
          >
            Refresh Page
          </button>
        </div>
      </div>
    );
  }

  // If roles are specified, check if user has required role
  if (roles && roles.length > 0 && user) {
    const userRoles = user.roles || [];
    const hasRequiredRole = roles.some(role => userRoles.includes(role));
    
    if (!hasRequiredRole) {
      // User doesn't have required role, redirect to unauthorized page
      return <Navigate to="/unauthorized" replace />;
    }
  }

  // User is authenticated and has required roles (if specified)
  return children;
};

export default ProtectedRoute;
