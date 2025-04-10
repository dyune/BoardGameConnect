import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge"; // Keep Badge import in case needed later
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Package, Edit } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion"; // Import Framer Motion
import GameInstanceManager from "./GameInstanceManager.jsx"; // Import the new component
import ModifyGameDialog from "./ModifyGameDialog.jsx"; // Import ModifyGameDialog

// Combined props: pass the whole game object and the refresh callback
export default function Game({ game, refreshGames }) {
  // State for game instances dialog
  const [showInstances, setShowInstances] = useState(false);
  // State for modify game dialog
  const [showModifyGame, setShowModifyGame] = useState(false);

  // Use game.id from the game prop
  const gameId = game?.id;
  const gameName = game?.name || "this game"; // Use game name or placeholder
  
  // Removed the default image as we'll handle it with conditional rendering

  // JSX structure using Framer Motion from origin/dev-Yessine-D3
  return (
    <>
      <AnimatePresence>
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -20, transition: { duration: 0.2 } }}
          key={`game-card-${gameId}`}
        >
          <Card 
            className="min-w-[260px] cursor-pointer hover:shadow-md transition-shadow duration-200"
            onClick={() => setShowInstances(true)}
          >
            <CardContent className="p-0">
              <div className="aspect-[4/3] relative">
                {game?.image ? (
                  <img
                    src={game?.image}
                    alt={gameName}
                    className="object-cover w-full h-full rounded-t-lg"
                    onError={(e) => {
                      e.target.onerror = null;
                      e.target.src = "https://placehold.co/400x300?text=Game";
                    }}
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center bg-gray-100 rounded-t-lg">
                    <span className="text-sm text-gray-500">No image available</span>
                  </div>
                )}
              </div>
              <div className="p-4">
                <h3 className="font-semibold text-lg">{gameName}</h3>
                <div className="flex justify-between items-center mt-4 gap-2">
                  <Button variant="outline" size="sm" className="flex gap-1" onClick={(e) => {
                    e.stopPropagation();
                    setShowInstances(true);
                  }}>
                    <Package className="h-4 w-4" />
                    Manage Copies
                  </Button>
                  <Button variant="outline" size="sm" className="flex gap-1" onClick={(e) => {
                    e.stopPropagation();
                    setShowModifyGame(true);
                  }}>
                    <Edit className="h-4 w-4" />
                    Modify Game
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </motion.div>
      </AnimatePresence>
      
      {/* Game Instances Dialog */}
      <Dialog 
        open={showInstances} 
        onOpenChange={(openState) => {
          if (!openState && document.activeElement instanceof HTMLElement) {
            document.activeElement.blur();
          }
          setShowInstances(openState);
        }}
      >
        <DialogContent className="sm:max-w-[800px] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Manage Game Copies - {gameName}</DialogTitle>
            <DialogDescription>
              View and manage all your copies of this game, including their condition and availability.
            </DialogDescription>
          </DialogHeader>
          
          <GameInstanceManager 
            gameId={gameId} 
            gameName={gameName} 
            refreshGames={refreshGames} 
          />
          
          <DialogFooter className="mt-4">
            <Button variant="outline" onClick={() => setShowInstances(false)}>
              Close
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Modify Game Dialog */}
      <ModifyGameDialog
        open={showModifyGame}
        onOpenChange={setShowModifyGame}
        onGameModified={refreshGames}
        gameId={gameId}
      />
    </>
  );
}
