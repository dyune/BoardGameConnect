import { useState, useEffect, useCallback } from "react"; // Import useState, useEffect, useCallback
import { useAuth } from "@/context/AuthContext"; // Import useAuth
import { Button } from "@/components/ui/button.jsx";
import Game from "./Game.jsx";
import { TabsContent } from "@/components/ui/tabs.jsx";
import AddGameDialog from "./AddGameDialog.jsx"; // Import the dialog
import SelectGameDialog from "./SelectGameDialog.jsx"; // Import the select game dialog
import { Loader2, Plus, Copy } from "lucide-react"; // Import Loader and other icons
import { getUserGameInstances, getGameById } from "../../service/game-api.js"; // Import the service functions
import { UnauthorizedError } from "@/service/apiClient"; // Import UnauthorizedError

export default function DashboardGameLibrary({ userType }) {
  const { user, logout } = useAuth(); // Get user and logout from context
  const [isAddGameDialogOpen, setIsAddGameDialogOpen] = useState(false);
  const [isSelectGameDialogOpen, setIsSelectGameDialogOpen] = useState(false);
  const [games, setGames] = useState([]); // State for fetched games
  const [isLoading, setIsLoading] = useState(true); // Keep loading state for fetch operation
  const [error, setError] = useState(null);

  // Log user data for debugging
  useEffect(() => {
  }, [user, userType]);

  // Function to fetch games
  // Use useCallback to memoize fetchGames, prevent re-creation if user object reference changes unnecessarily
  const fetchGames = useCallback(async () => {
    if (!user?.id) { // Check if user and user.id exist
      setError("User ID not found. Cannot fetch games.");
      setIsLoading(false);
      setGames([]);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      console.log("[DashboardGameLibrary] Cookie state during fetch:", {
        isAuthenticated: document.cookie.includes('isAuthenticated=true'),
        hasAccessToken: document.cookie.includes('accessToken='),
        allCookies: document.cookie
      });

      // Get all game instances owned by the current user
      const userInstances = await getUserGameInstances();
      
      if (!userInstances.length) {
        setGames([]);
        setIsLoading(false);
        return;
      }
      
      // Group instances by game to create a collection
      const gameMap = new Map();
      
      // Process each instance and group by game ID
      for (const instance of userInstances) {
        const gameId = instance.gameId;
        
        if (!gameMap.has(gameId)) {
          try {
            // Fetch the full game details if not already in the map
            const game = await getGameById(gameId);
            gameMap.set(gameId, {
              ...game,
              instances: [instance],
              hasUserInstances: true
            });
          } catch (error) {
            console.error(`Error fetching game ${gameId}:`, error);
          }
        } else {
          // Add this instance to the existing game's instances
          const game = gameMap.get(gameId);
          game.instances.push(instance);
        }
      }
      
      // Convert the map values to an array
      const userGames = Array.from(gameMap.values());
      setGames(userGames);
      
    } catch (err) {
      console.error("[DashboardGameLibrary] Error details:", err);

      if (err instanceof UnauthorizedError) {
        console.warn("[DashboardGameLibrary] Unauthorized access fetching games. Logging out.", err);
        logout(); // Call logout function on 401
      } else {
        console.error("[DashboardGameLibrary] Failed to fetch games:", err);
        setError(err.message || "Could not load your games.");
        setGames([]); // Clear games on error
      }
    } finally {
      setIsLoading(false);
    }
  }, [user?.id, logout]); // Update dependencies

  // Fetch games when the component mounts or when the user object changes (specifically the email)
  useEffect(() => {
    // Only fetch if the user is identified as an owner and their email is available
    if (userType === "owner" && user?.id) {
      fetchGames();
    } else if (userType !== "owner") {
      // If not an owner, explicitly set loading to false and games to empty
      setIsLoading(false);
      setGames([]);
    }
  }, [userType, user?.id, fetchGames]);

  // Function to handle adding a game (refreshes the list)
  const handleGameAdded = useCallback((newGame) => {
    // Re-fetch the list to include the new game
    fetchGames();
  }, [fetchGames]);

  // Function to handle adding a game instance (refreshes the list)
  const handleGameInstanceAdded = useCallback((instance) => {
    // Re-fetch the list to include the new instance
    fetchGames();
  }, [fetchGames]);

  return (
    <>
      <TabsContent value="games" className="space-y-6">
        <div className="flex justify-between items-center">
          <h2 className="text-2xl font-bold">My Games</h2>
          {/* Only show buttons if userType is owner (checked via prop and auth context) */}
          {userType === "owner" && user?.gameOwner && (
            <div className="flex gap-2">
              <Button 
                variant="outline" 
                onClick={() => setIsSelectGameDialogOpen(true)}
                className="flex items-center"
              >
                <Copy className="mr-2 h-4 w-4" />
                Add Game Copy
              </Button>
              <Button 
                onClick={() => setIsAddGameDialogOpen(true)}
                className="flex items-center"
              >
                <Plus className="mr-2 h-4 w-4" />
                Add New Game
              </Button>
            </div>
          )}
        </div>

        {/* Conditional Rendering based on loading/error/data */}
        {isLoading ? (
          <div className="flex justify-center items-center py-10">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        ) : error ? (
          <div className="text-center py-10 text-destructive">
            <p>Error loading games: {error}</p>
          </div>
        ) : games.length === 0 && userType === "owner" ? (
           <div className="text-center py-10 text-muted-foreground">
             <p>You haven't added any games to your collection yet.</p>
             <p className="mt-2">Click "Add New Game" to create a new game or "Copy Existing Game" to add a copy from the global library.</p>
           </div>
        ) : games.length === 0 && userType !== "owner" ? (
            <div className="text-center py-10 text-muted-foreground">
              Game library is only available for Game Owners.
              {user && (
                <div className="mt-4">
                  <p className="text-sm text-destructive mb-2">
                    Note: You are registered as: {user.gameOwner ? 'Game Owner' : 'Player'}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    If this seems incorrect, please contact support.
                  </p>
                </div>
              )}
            </div>
        ) : (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {games.map(game => (
              <Game
                key={game.id}
                game={game}
                refreshGames={fetchGames}
              />
            ))}
          </div>
        )}
      </TabsContent>

      {/* Render the Add Game Dialog */}
      <AddGameDialog
        open={isAddGameDialogOpen}
        onOpenChange={setIsAddGameDialogOpen}
        onGameAdded={handleGameAdded}
      />

      {/* Render the Select Game Dialog */}
      <SelectGameDialog
        open={isSelectGameDialogOpen}
        onOpenChange={setIsSelectGameDialogOpen}
        onGameInstanceAdded={handleGameInstanceAdded}
        maxVisibleGames={4}
      />
    </>
  );
}
