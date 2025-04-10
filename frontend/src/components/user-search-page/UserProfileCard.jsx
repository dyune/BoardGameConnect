import React from 'react';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card.jsx';
import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from '@/components/ui/avatar.jsx';
import { Badge } from '@/components/ui/badge.jsx';
import Tag from '../common/Tag.jsx';
import GameOwnerTag from '../common/GameOwnerTag.jsx';
import PlayerTag from '../common/PlayerTag.jsx';
import { motion } from 'framer-motion';
import { cn } from '@/components/lib/utils';
import { formatJoinDate } from '../lib/dateUtils.js';
import { Calendar, Mail, User, Gamepad, ArrowUpRight } from 'lucide-react';

const UserProfileCard = ({ user, onClick }) => {
  // Provide default values or placeholders if user data might be missing
  const username = user?.username || 'N/A';
  const email = user?.email || 'No email available';
  const isGameOwner = user?.isGameOwner || false;
  
  // Merge gamesPlayed and gamesBorrowed into a single array (as per backend update)
  const gamesPlayed = user?.gamesPlayed || [];
  const gamesOwned = user?.gamesOwned || [];

  // Only show up to 3 games for each category
  const displayedGamesPlayed = gamesPlayed.slice(0, 3);
  const hasMoreGamesPlayed = gamesPlayed.length > 3;
  
  const displayedGamesOwned = gamesOwned.slice(0, 3);
  const hasMoreGamesOwned = gamesOwned.length > 3;

  // Format join date if available
  const formattedJoinDate = user?.joinDate ? formatJoinDate(user.joinDate) : null;

  // Get initials for avatar fallback
  const getInitials = () => {
    if (!username || username === 'N/A') return 'U';
    const words = username.split(' ');
    if (words.length === 1) return words[0][0].toUpperCase();
    return (words[0][0] + words[words.length - 1][0]).toUpperCase();
  };
  
  // Function to truncate email with ellipsis
  const formatEmail = (email) => {
    if (!email) return '';
    
    // If email is short enough, return it as is
    if (email.length <= 24) return email;
    
    // Split the email at @ symbol
    const parts = email.split('@');
    
    if (parts.length !== 2) return email; // Not a valid email format
    
    const [username, domain] = parts;
    
    // If the username part is long, truncate it
    if (username.length > 15) {
      return `${username.substring(0, 12)}...@${domain}`;
    }
    
    // If the domain part is long, truncate it
    if (domain.length > 15) {
      return `${username}@${domain.substring(0, 12)}...`;
    }
    
    // Shouldn't reach here, but just in case
    return `${email.substring(0, 21)}...`;
  };
  
  // Check if the user has any games (played or owned)
  const hasAnyGames = gamesPlayed.length > 0 || gamesOwned.length > 0;

  return (
    <motion.div
      whileHover={{ scale: 1.02 }}
      whileTap={{ scale: 0.98 }}
      transition={{ type: "spring", stiffness: 400, damping: 17 }}
      className="h-full"
    >
      <Card 
        className={cn(
          "h-full overflow-hidden cursor-pointer group",
          "border border-border/50 hover:border-primary/30",
          "transition-all duration-300",
          "bg-card/50 hover:bg-card",
          "hover:shadow-md hover:shadow-primary/10"
        )}
        onClick={onClick}
      > 
        <CardHeader className="flex flex-row items-center gap-4 pb-2 relative">
          <Avatar className="h-16 w-16 ring-2 ring-primary/10 group-hover:ring-primary/30 transition-all duration-200">
            <AvatarImage src={user?.avatarUrl} alt={username} />
            <AvatarFallback className="bg-primary/10 text-primary font-medium">
              {getInitials()}
            </AvatarFallback>
          </Avatar>
          <div className="space-y-1 flex-1 min-w-0">
            <CardTitle className="text-lg leading-tight group-hover:text-primary transition-colors duration-200 truncate">
              {username}
            </CardTitle>
            <div className="flex items-center text-sm text-muted-foreground" title={email}>
              <Mail className="h-3 w-3 mr-1 flex-shrink-0" />
              <span className="truncate">{formatEmail(email)}</span>
            </div>
            <div className="flex flex-wrap gap-1 items-center mt-1">
              {isGameOwner ? 
                <GameOwnerTag className="mt-1" /> : 
                <PlayerTag className="mt-1" />
              }
              {formattedJoinDate && (
                <div className="flex items-center text-xs text-muted-foreground mt-1">
                  <Calendar className="h-3 w-3 mr-1" />
                  <span>Joined {formattedJoinDate}</span>
                </div>
              )}
            </div>
          </div>
          <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
            <ArrowUpRight className="h-4 w-4 text-primary" />
          </div>
        </CardHeader>
        <CardContent className="pt-0">
          {hasAnyGames ? (
            <div className="space-y-3">
              {/* Games Played Section */}
              {gamesPlayed.length > 0 && (
                <div className="mt-2">
                  <div className="flex items-center gap-1 mb-2">
                    <Gamepad className="h-3 w-3 text-muted-foreground" />
                    <CardDescription className="text-xs">Games Played</CardDescription>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {displayedGamesPlayed.map((game, index) => (
                      <Tag
                        key={`played-${index}`}
                        text={game}
                        variant="primary"
                        interactive
                        searchable
                        fromUserId={user.id}
                        onClick={(e) => e.stopPropagation()}
                      />
                    ))}
                    {hasMoreGamesPlayed && (
                      <Badge variant="outline" className="text-xs">
                        +{gamesPlayed.length - 3} more
                      </Badge>
                    )}
                  </div>
                </div>
              )}
              
              {/* Games Owned Section - Only for Game Owners */}
              {isGameOwner && gamesOwned.length > 0 && (
                <div className="mt-2">
                  <div className="flex items-center gap-1 mb-2">
                    <span className="h-3 w-3 text-muted-foreground">🎲</span>
                    <CardDescription className="text-xs">Games Owned</CardDescription>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {displayedGamesOwned.map((game, index) => (
                      <Tag
                        key={`owned-${index}`}
                        text={game}
                        variant="owner"
                        interactive
                        searchable
                        fromUserId={user.id}
                        onClick={(e) => e.stopPropagation()}
                      />
                    ))}
                    {hasMoreGamesOwned && (
                      <Badge variant="outline" className="text-xs">
                        +{gamesOwned.length - 3} more
                      </Badge>
                    )}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="mt-4 text-center py-2">
              <CardDescription className="text-xs text-muted-foreground">No game activity yet</CardDescription>
            </div>
          )}
        </CardContent>
      </Card>
    </motion.div>
  );
};

export default UserProfileCard;