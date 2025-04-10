import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';
import { checkAuthStatus, getUserProfile, logoutUser } from '../service/auth-api';
import { getGamesByOwner } from '../service/game-api'; // Import game fetching service
import { setAuthInProgress, getCookieAuthState } from '../service/apiClient';

// Create the auth context
const AuthContext = createContext(null);

// Public paths that don't need authenticated state
const PUBLIC_PATHS = ['/', '/login', '/register', '/about', '/contact'];

// Inactive timeout - 30 minutes
const INACTIVE_TIMEOUT = 30 * 60 * 1000;

// Auth context provider component
export const AuthProvider = ({ children }) => {
  // User state - contains user data when authenticated
  const [user, setUser] = useState(null);
  // Loading state for async operations
  const [loading, setLoading] = useState(true);
  // Authentication state
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  // Auth ready state - indicates when authentication process is fully complete
  const [authReady, setAuthReady] = useState(false);
  // Error state
  const [error, setError] = useState(null);
  const [currentUserGames, setCurrentUserGames] = useState([]); // State for user's games (game names/IDs)
  const [currentUserGamesError, setCurrentUserGamesError] = useState(null); // State for game fetch error
  // Session expired flag
  const [isSessionExpired, setIsSessionExpired] = useState(false);
  // Remember me state
  const [rememberMe, setRememberMe] = useState(false);
  // Last activity timestamp
  const [lastActivity, setLastActivity] = useState(Date.now());
  // Current path
  const [currentPath, setCurrentPath] = useState(window.location.pathname);

  const [isInitialCheckComplete, setIsInitialCheckComplete] = useState(false); // Added state
  // Update current path when location changes
  useEffect(() => {
    const handleLocationChange = () => {
      setCurrentPath(window.location.pathname);
    };
    
    window.addEventListener('popstate', handleLocationChange);
    
    return () => {
      window.removeEventListener('popstate', handleLocationChange);
    };
  }, []);

  // Check if the current path is public
  const isPublicPath = PUBLIC_PATHS.includes(currentPath);

  // Function to update last activity timestamp
  const updateActivity = useCallback(() => {
    setLastActivity(Date.now());
    // If session was expired but user is active, attempt to revalidate
    if (isSessionExpired) {
      checkAuthStatus()
        .then(isAuth => {
          if (isAuth) {
            setIsSessionExpired(false);
          }
        })
        .catch(() => {
          // Ignore errors - will remain in expired state
        });
    }
  }, [isSessionExpired]);

  // Fetch user games function
  const fetchUserGames = useCallback(async (email) => {
    if (!email) {
      setCurrentUserGamesError(null); // Clear error if no email
      return;
    }
    setCurrentUserGamesError(null); // Clear previous error on new attempt
    try {
      const gamesData = await getGamesByOwner(email);
      const gameNames = gamesData.map(game => game.name);
      setCurrentUserGames(gameNames);
    } catch (err) {
      const errorMsg = err.message || "Could not load your games for comparison.";
      console.error("Error fetching current user games in AuthContext:", err);
      setCurrentUserGamesError(errorMsg); // Set the error state
      setCurrentUserGames([]); // Reset games on error
    }
  }, []);
  
  // Helper function to clear auth data
  const clearAuthData = useCallback(() => {
    setUser(null);
    setIsAuthenticated(false);
    
    // Clear localStorage
    localStorage.removeItem('rememberMe');
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('user');
    
    // Clear cookies with multiple approaches to ensure they're properly removed
    // Method 1: Simple path
    document.cookie = "isAuthenticated=false; path=/; max-age=0";
    document.cookie = "accessToken=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; max-age=0";
    
    // Method 2: With domain (in case domain was set)
    const domain = window.location.hostname;
    document.cookie = `isAuthenticated=false; path=/; domain=${domain}; max-age=0`;
    document.cookie = `accessToken=; path=/; domain=${domain}; expires=Thu, 01 Jan 1970 00:00:00 GMT; max-age=0`;
    
    // Method 3: Root path
    document.cookie = "isAuthenticated=false; path=/; max-age=0";
    document.cookie = "accessToken=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; max-age=0";
    
    console.log('AuthContext: Cleared all auth data, cookies and localStorage');
    
    setCurrentUserGamesError(null); // Clear game error on clear auth
  }, []);


  // Initialize authentication state
  const initAuth = useCallback(async () => {
    try {
      setLoading(true);
      setAuthReady(false);
      setAuthInProgress(true); // Set auth in progress flag
      setError(null);

      // Check for rememberMe in localStorage first, as we need this information
      // before making API calls to determine appropriate session handling
      const savedRememberMe = localStorage.getItem('rememberMe') === 'true';
      setRememberMe(savedRememberMe);
      console.log('AuthContext init: RememberMe setting loaded:', savedRememberMe);

      // Check if we have isAuthenticated cookie already (from previous sessions)
      const hasAuthCookie = document.cookie.includes('isAuthenticated=true');
      console.log('AuthContext init: Found isAuthenticated cookie:', hasAuthCookie);

      // Give a small delay to allow browser to initialize cookies if needed
      if (hasAuthCookie) {
        console.log('AuthContext init: Waiting briefly for cookies to be fully initialized...');
        await new Promise(resolve => setTimeout(resolve, 50));
      }

      // Always try to verify the session with the backend using checkAuthStatus.
      // This relies on the browser sending the HttpOnly accessToken cookie.
      console.log('AuthContext init: Attempting to verify session with backend via checkAuthStatus...');
      
      let userProfile = null;
      try {
        // Make sure to pass the saved rememberMe setting indirectly through the API call
        // checkAuthStatus will set the X-Remember-Me header based on localStorage
        userProfile = await checkAuthStatus(); // checkAuthStatus now returns user profile or null
      } catch (authCheckError) {
        console.error('Error checking auth status:', authCheckError);
        // Continue with null userProfile
      }

      if (userProfile) {
        // Backend confirmed authentication is valid
        console.log('AuthContext init: Session verified by backend. User:', userProfile.email);
        setUser(userProfile);
        setIsAuthenticated(true);
        setIsSessionExpired(false);

        // Update localStorage with potentially fresh data from the profile check
        // This ensures localStorage is consistent even if cookies were the primary source.
        localStorage.setItem('userId', userProfile.id);
        localStorage.setItem('userEmail', userProfile.email);
        localStorage.setItem('user', JSON.stringify(userProfile));
        localStorage.setItem('rememberMe', savedRememberMe ? 'true' : 'false');

        fetchUserGames(userProfile.email); // Fetch games for the authenticated user
      } else {
        // Backend indicated no valid session (e.g., 401, error, or null response from checkAuthStatus)
        console.log('AuthContext init: No valid session confirmed by backend.');
        clearAuthData(); // Ensure client state reflects unauthenticated
      }
    } catch (err) {
      console.error('Error initializing auth:', err);
      setError('Failed to initialize authentication');
      clearAuthData();
    } finally {
      setLoading(false);
      setIsInitialCheckComplete(true); // Mark initial check as complete
      // Add a small delay before setting authReady to true
      // This gives the browser time to process state updates
      setTimeout(() => {
        setAuthReady(true); // Authentication flow is now complete
        setAuthInProgress(false); // Auth is no longer in progress
      }, 100); // Increased delay for better browser processing
    }
  }, [fetchUserGames, clearAuthData]);
  

  // Initialize auth when component mounts
  useEffect(() => {
    initAuth();
  }, [initAuth]);

  // Setup inactivity monitoring based on remember me
  useEffect(() => {
    // Only monitor inactivity if user is authenticated and remember me is not set
    if (!isAuthenticated || rememberMe) {
      console.log('AuthContext: Inactivity monitoring disabled:', 
        !isAuthenticated ? 'Not authenticated' : 'RememberMe is true');
      return;
    }

    console.log('AuthContext: Setting up inactivity monitoring (rememberMe is false)');
    
    // Check for inactivity
    const checkInactivity = () => {
      const now = Date.now();
      const inactiveTime = now - lastActivity;

      // If user has been inactive for too long, expire session
      if (inactiveTime >= INACTIVE_TIMEOUT) {
        console.log(`AuthContext: User inactive for ${inactiveTime}ms, expiring session`);
        setIsSessionExpired(true);
      }
    };

    // Set up interval to check inactivity
    const intervalId = setInterval(checkInactivity, 60000); // Check every minute

    // Listen for user activity
    const activityEvents = ['mousedown', 'keypress', 'scroll', 'touchstart'];
    
    const handleActivity = () => {
      updateActivity();
    };

    // Add event listeners for user activity
    activityEvents.forEach(event => {
      window.addEventListener(event, handleActivity);
    });

    // Cleanup function
    return () => {
      console.log('AuthContext: Cleaning up inactivity monitoring');
      clearInterval(intervalId);
      activityEvents.forEach(event => {
        window.removeEventListener(event, handleActivity);
      });
    };
  }, [isAuthenticated, lastActivity, rememberMe, updateActivity]);

  // Login function
  const login = useCallback(async (userData, remember = false) => {
    try {
      // Ensure remember is a boolean
      remember = Boolean(remember);
      console.log('[AuthContext] Login called with rememberMe:', remember);
      
      setLoading(true);
      setAuthReady(false);
      setAuthInProgress(true); // Set auth in progress flag

      setUser(userData);
      setIsAuthenticated(true);
      setIsSessionExpired(false);
      setRememberMe(remember);
      setLastActivity(Date.now());

      // Check if we have a token in the userData, which should come from the API response
      const token = userData.token || userData.accessToken;

      // Save user data and token to localStorage
      if (userData) {
        if (token) {
          localStorage.setItem('token', token); // Note: Storing token in localStorage is generally discouraged with HttpOnly cookies
        }
        localStorage.setItem('userId', userData.id);
        localStorage.setItem('userEmail', userData.email);
        localStorage.setItem('user', JSON.stringify(userData));
      }

      // Save remember me preference
      localStorage.setItem('rememberMe', remember ? 'true' : 'false');
      console.log('[AuthContext] RememberMe saved to localStorage:', remember);

      // Set auth state immediately *before* fetching games
      setLoading(false);
      setAuthReady(true);
      setAuthInProgress(false); // Set to false *before* potentially long-running fetch
      console.log('[AuthContext Login] Auth state updated, authInProgress: false');

      // Now fetch games
      if (userData && userData.email) {
        fetchUserGames(userData.email);
      }

      // Return the user data after successful state updates
      return userData;

    } catch (error) {
      console.error('Error during login:', error);
      // Ensure state is reset on error
      setLoading(false);
      setAuthReady(true); // Set authReady even on error to unblock UI
      setAuthInProgress(false);
      // Consider clearing partial auth data here if necessary
      throw error; // Re-throw for the caller
    }
  }, [fetchUserGames]);

  // Logout function
  const logout = useCallback(async () => {
    try {
      setAuthReady(false);
      setAuthInProgress(true); // Set auth in progress flag
      await logoutUser();
    } catch (err) {
      // Log error but proceed with client-side logout
      console.error('Error during logout:', err);
    } finally {
      // Clear auth state
      setUser(null);
      setIsAuthenticated(false);
      setIsSessionExpired(false);
      setRememberMe(false);
      
      // Clear all localStorage items
      localStorage.removeItem('token');
      localStorage.removeItem('userId');
      localStorage.removeItem('userEmail');
      localStorage.removeItem('user');
      localStorage.removeItem('rememberMe');
      setCurrentUserGames([]); // Clear games on logout
      setCurrentUserGamesError(null); // Clear game error on logout
      
      // Add a small delay for state updates to propagate
      setTimeout(() => {
        setAuthReady(true);
        setAuthInProgress(false); // Auth is no longer in progress
      }, 10); // Reduced delay
    }
  }, []);

  // Handle session expiration
  const handleSessionExpired = useCallback(() => {
    setIsSessionExpired(true);
  }, []);

  // Context value to be provided
  const value = {
    user,
    loading,
    isAuthenticated,
    isSessionExpired,
    error,
    authReady,
    login,
    logout,
    handleSessionExpired,
    rememberMe,
    setRememberMe,
    updateActivity,
    isPublicPath,
    currentUserGames, // Provide games through context
    currentUserGamesError // Provide game fetch error state
  };

  // Don't render children until the initial check is done
  if (!isInitialCheckComplete) {
    return null; // Or a loading spinner component
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// Hook to use the auth context
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === null) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

// Removed default export to avoid potential import confusion