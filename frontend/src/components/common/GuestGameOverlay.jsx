import React from 'react';
import { Link } from 'react-router-dom';
import { UserPlus, LogIn } from 'lucide-react';
import { Button } from "../ui/button";

/**
 * Component that displays an overlay on game items for unauthenticated users,
 * prompting them to sign up or log in to interact with games.
 * 
 * @param {Object} props - Component props
 * @param {string} props.title - The title of the game (optional)
 * @returns {React.ReactNode} - The overlay component
 */
const GuestGameOverlay = ({ title }) => {
  return (
    <div className="absolute inset-0 bg-black/70 backdrop-blur-sm flex flex-col items-center justify-center text-white p-4 rounded-md">
      <h3 className="text-xl font-bold mb-4">
        {title 
          ? `Want to play ${title}?` 
          : "Want to explore our games?"}
      </h3>
      
      <p className="text-center mb-6 max-w-xs">
        Sign up or log in to view game details, join events, and connect with other players.
      </p>
      
      <div className="flex flex-col sm:flex-row gap-3">
        <Button asChild variant="default" className="flex items-center gap-2">
          <Link to="/register">
            <UserPlus className="w-4 h-4" />
            Sign Up
          </Link>
        </Button>
        
        <Button asChild variant="outline" className="flex items-center gap-2 bg-transparent text-white border-white hover:bg-white/10">
          <Link to="/login">
            <LogIn className="w-4 h-4" />
            Log In
          </Link>
        </Button>
      </div>
    </div>
  );
};

export default GuestGameOverlay; 