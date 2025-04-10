import React, { useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '../ui/alert-dialog';

/**
 * Component that monitors session expiration and displays a modal
 * when the session expires, prompting the user to log in again.
 */
const SessionExpirationHandler = () => {
  const { isSessionExpired, logout } = useAuth();
  const navigate = useNavigate();
  
  // Handle session expiration
  const handleLogin = () => {
    // First logout to clear any existing session data
    logout().then(() => {
      // Then navigate to login page with current URL as redirect
      const currentPath = window.location.pathname;
      navigate(`/login?redirect=${encodeURIComponent(currentPath)}`, { replace: true });
    });
  };
  
  return (
    <AlertDialog open={isSessionExpired}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Session Expired</AlertDialogTitle>
          <AlertDialogDescription>
            Your session has expired due to inactivity. Please log in again to continue.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogAction onClick={handleLogin}>
            Log In Again
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
};

export default SessionExpirationHandler; 