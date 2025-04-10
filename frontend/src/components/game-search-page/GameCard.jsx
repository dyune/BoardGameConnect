import React from 'react';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '../ui/card';
import { motion } from 'framer-motion';
import { cn } from '@/components/lib/utils';
import { Users, Star } from 'lucide-react';
import Tag from '../common/Tag.jsx';

export const GameCard = ({ game, showInstanceCount = false }) => {
  // Use data from API, not placeholder values
  const name = game?.name || 'Unknown Game';
  const category = game?.category || 'Uncategorized';
  // Use image from API
  const imageUrl = game?.image;
  const minPlayers = game?.minPlayers || 1;
  const maxPlayers = game?.maxPlayers || 4;
  
  // Calculate average rating if reviews are available
  const rating = game?.averageRating || 0;
  
  return (
    <motion.div
      whileHover={{ scale: 1.02 }}
      whileTap={{ scale: 0.98 }}
      transition={{ type: "spring", stiffness: 400, damping: 17 }}
      className="h-full"
    >
      <Card 
        className={cn(
          "h-full overflow-hidden cursor-pointer",
          "border border-border/50 hover:border-border",
          "transition-colors duration-200",
          "bg-card/50 hover:bg-card"
        )}
      > 
        <div className="relative h-48 overflow-hidden">
          <img 
            src={imageUrl || 'https://placehold.co/400x300/e9e9e9/1d1d1d?text=No+Image'} 
            alt={name} 
            className="w-full h-full object-cover"
          />
          <div className="absolute top-3 left-3">
            <Tag text={category} variant="secondary" />
          </div>
          {showInstanceCount && game.instanceCount !== undefined && (
            <div className="absolute top-3 right-3 bg-black/60 text-white text-xs px-2 py-1 rounded-full">
              {game.instanceCount} {game.instanceCount === 1 ? 'instance' : 'instances'} available
            </div>
          )}
        </div>
        
        <CardHeader className="pb-2">
          <CardTitle className="text-xl">{name}</CardTitle>
        </CardHeader>
        
        <CardContent>
          <div className="flex flex-wrap gap-3 text-sm text-muted-foreground mb-4">
            <div className="flex items-center gap-1">
              <Users className="h-4 w-4" />
              <span>{minPlayers === maxPlayers ? minPlayers : `${minPlayers}-${maxPlayers}`} players</span>
            </div>
            
            {rating > 0 && (
              <div className="flex items-center gap-1">
                <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                <span>{rating.toFixed(1)}</span>
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </motion.div>
  );
}; 