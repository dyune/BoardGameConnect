import React, { useEffect, useState, useCallback } from 'react'; // Added useCallback
import { useNavigate } from 'react-router-dom';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog.jsx';
import { Button } from '@/components/ui/button.jsx';
import { Avatar, AvatarFallback, AvatarImage } from './avatar.jsx';
import Tag from '../common/Tag.jsx';
import GameOwnerTag from '../common/GameOwnerTag.jsx';
import { motion, AnimatePresence } from 'framer-motion';
import { Users, GamepadIcon, Mail, Loader2 } from 'lucide-react';
import { formatJoinDate, formatRelativeTime } from '../lib/dateUtils.js';
import { sendConnectionRequest } from '@/service/user-api.js'; // Added connection API import
import { useAuth } from '@/context/AuthContext';
import { toast } from 'sonner'; // Added toast import

const UserPreviewOverlay = ({ user, isOpen, onClose }) => {
  const navigate = useNavigate();
  const { user: currentUser, currentUserGames, currentUserGamesError } = useAuth(); // Get games and error from context
  // Removed: const [currentUserGames, setCurrentUserGames] = useState([]);
  const [isConnecting, setIsConnecting] = useState(false);
  const [connectStatus, setConnectStatus] = useState('idle'); // 'idle', 'success', 'error'
  const [connectError, setConnectError] = useState('');

  // Reset connection state when the dialog opens or user changes
  useEffect(() => {
    if (isOpen) {
      setIsConnecting(false);
      setConnectStatus('idle');
      setConnectError('');
    }
  }, [isOpen, user?.id]);

  if (!user) return null;
  
  // Find common games between current user and viewed profile
  // TODO: [Refactor] Compare games by ID if both currentUserGames (from AuthContext)
  // and user.gamesPlayed (from API) consistently provide unique game IDs.
  // Currently comparing by name due to uncertainty about ID availability in user.gamesPlayed.

  const commonGames = user?.gamesPlayed?.filter(game => currentUserGames.includes(game)) || [];

  // Format join date
  const joinDateFormatted = formatJoinDate(user.joinDate);
  const joinTimeRelative = formatRelativeTime(user.joinDate);

  // Animation variants
  const overlayVariants = {
    hidden: { opacity: 0, scale: 0.95 },
    visible: { 
      opacity: 1, 
      scale: 1,
      transition: {
        duration: 0.2,
        ease: "easeOut",
        staggerChildren: 0.05
      }
    },
    exit: { 
      opacity: 0, 
      scale: 0.95,
      transition: { duration: 0.15 }
    }
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 10 },
    visible: { opacity: 1, y: 0 }
  };

  // Render game tag directly without motion wrapper to ensure visibility
  const renderGameTag = (game, index) => (
    <Tag 
      key={index}
      text={game} 
      variant="primary" 
      interactive={true}
      searchable={true}
      fromUserId={user.id}
      className="transition-all duration-200"
    />
  );

  const handleConnect = useCallback(async () => {
    if (!user?.email || connectStatus === 'success' || isConnecting) return;

    setIsConnecting(true);
    setConnectStatus('idle');
    setConnectError('');

    try {
      await sendConnectionRequest(user.email);
      setConnectStatus('success');
      toast.success(`Connection request sent to ${user.username || 'user'}.`);
      // Keep button disabled after success
    } catch (error) {
      console.error("Failed to send connection request:", error);
      const errorMsg = error.message || "Could not send connection request. Please try again.";
      setConnectError(errorMsg);
      setConnectStatus('error');
      toast.error(errorMsg);
      // Re-enable button on error to allow retry? Or keep disabled? Let's re-enable for now.
      // If we want to keep disabled, remove the setIsConnecting(false) below.
    } finally {
      // Only set connecting to false on error if we want to allow retries
      if (connectStatus !== 'success') {
         setIsConnecting(false);
      }
    }
  }, [user?.email, user?.username, isConnecting, connectStatus]);

  return (
    <AnimatePresence>
      {isOpen && (
        <Dialog open={isOpen} onOpenChange={onClose}>
          <DialogContent className="sm:max-w-[450px] p-0 overflow-hidden" aria-describedby="user-profile-description">
            <DialogDescription id="user-profile-description" className="sr-only">
              User profile information for {user.username || 'User'}
            </DialogDescription>
            <motion.div
              variants={overlayVariants}
              initial="hidden"
              animate="visible"
              exit="exit"
            >
              {/* Banner/Header Section */}
              <div className="bg-gradient-to-r from-primary/10 to-primary/5 h-24 relative">
                <motion.div 
                  className="absolute -bottom-12 left-6"
                  variants={itemVariants}
                >
                  <Avatar className="h-20 w-20 border-4 border-background shadow-md">
                    <AvatarImage src={user.avatarUrl} alt={user.username} />
                    <AvatarFallback className="text-2xl bg-primary/10 text-primary">
                      {user.username ? user.username[0].toUpperCase() : 'U'}
                    </AvatarFallback>
                  </Avatar>
                </motion.div>
              </div>

              {/* Main Content Section */}
              <div className="pt-16 px-6">
                <motion.div variants={itemVariants} className="flex items-center gap-2 mb-1">
                  <DialogTitle className="text-xl">{user.username || 'User'}</DialogTitle>
                  {user.isGameOwner && (
                    <GameOwnerTag />
                  )}
                </motion.div>

                <motion.div variants={itemVariants} className="flex items-center text-muted-foreground text-sm mb-4 gap-2">
                  <Users className="h-3 w-3" />
                  <span>
                    Member since {joinDateFormatted}
                    <span className="text-xs ml-1 opacity-70">({joinTimeRelative})</span>
                  </span>
                </motion.div>

                {/* Bio Section */}
                <motion.div variants={itemVariants} className="mb-6">
                  <p className="text-sm text-muted-foreground">
                    {user.bio || "This user hasn't added a bio yet."}
                  </p>
                </motion.div>

                {/* All Games Section - Simplified rendering without complex animations */}
                {user?.gamesPlayed?.length > 0 && (
                  <div className="mb-6">
                    <div className="flex items-center gap-2 mb-2">
                      <GamepadIcon className="h-4 w-4 text-primary" />
                      <h4 className="text-sm font-medium">Games Played:</h4>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      {user.gamesPlayed.map((game, index) => renderGameTag(game, index))}
                    </div>
                  </div>
                )}

                {/* Common Games Section */}
                <div className="mb-6">
                  <div className="flex items-center gap-2 mb-2">
                    <GamepadIcon className="h-4 w-4 text-primary" />
                    <h4 className="text-sm font-medium">Games Played in Common:</h4>
                  </div>
                  {/* Display error if fetching current user's games failed */}
                  {currentUserGamesError && (
                    <motion.div variants={itemVariants} className="text-xs text-destructive bg-destructive/10 p-2 rounded-md mb-2">
                      Could not load common games info: {currentUserGamesError}
                    </motion.div>
                  )}
                  {/* Only show common games list if there's no error and common games exist */}
                  {!currentUserGamesError && commonGames.length > 0 && (
                    <div className="flex flex-wrap gap-2">
                      {commonGames.map((game, index) => renderGameTag(game, index + 100))}
                    </div>
                  )}
                  {/* Show message if no common games and no error */}
                  {!currentUserGamesError && commonGames.length === 0 && user?.gamesPlayed?.length > 0 && (
                     <motion.p variants={itemVariants} className="text-xs text-muted-foreground italic">
                       No games in common found.
                     </motion.p>
                  )}
                </div>
              </div>

              {/* Footer Section */}
              <DialogFooter className="px-6 py-4 bg-muted/10 border-t mt-2">
                <Button
                  variant="outline"
                  onClick={() => {
                    navigate(`/profile/${user.id}`);
                    onClose();
                  }}
                  className="flex-1 gap-2"
                >
                  <Users className="h-4 w-4" />
                  View Profile
                </Button>
                <Button
                  className="flex-1 gap-2"
                  onClick={handleConnect}
                  disabled={isConnecting || connectStatus === 'success' || !user?.email}
                >
                  {isConnecting ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Mail className="h-4 w-4" />
                  )}
                  {isConnecting
                    ? 'Sending...'
                    : connectStatus === 'success'
                    ? 'Request Sent'
                    : connectStatus === 'error'
                    ? 'Retry Connect' // Or just 'Connect'
                    : 'Connect'}
                </Button>
              </DialogFooter>
            </motion.div>
          </DialogContent>
        </Dialog>
      )}
    </AnimatePresence>
  );
};

export default UserPreviewOverlay;