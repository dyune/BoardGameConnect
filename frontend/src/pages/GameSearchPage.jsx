import { useState, useEffect, useCallback } from "react";
import { Search, Filter, X, ArrowLeft, Loader2 } from "lucide-react";
import { Dialog, DialogTrigger } from "../components/ui/dialog";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { useSearchParams, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

// Import game components
import { GameCard } from "../components/game-search-page/GameCard";
import AuthRestrictedGameCard from "../components/game-search-page/AuthRestrictedGameCard";
import { GameDetailsDialog } from "../components/game-search-page/GameDetailsDialog";
import { RequestGameDialog } from "../components/game-search-page/RequestGameDialog";
import { searchGames, getGameInstances } from "../service/game-api";

export default function GameSearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [searchTerm, setSearchTerm] = useState(searchParams.get("q") || "");
  const fromUserId = searchParams.get("fromUser");
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [filters, setFilters] = useState({
    category: "",
    minPlayers: "",
    maxPlayers: "",
    minRating: ""
  });
  const [isRequestModalOpen, setIsRequestModalOpen] = useState(false);
  const [selectedGame, setSelectedGame] = useState(null);
  const [selectedInstance, setSelectedInstance] = useState(null);
  
  const [games, setGames] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  
  // Select the appropriate GameCard component based on authentication status
  const GameCardComponent = isAuthenticated ? GameCard : AuthRestrictedGameCard;

  // Function to apply filters
  const applyFilters = () => {
    setIsFilterOpen(false);
  };

  // Function to clear all filters
  const clearFilters = () => {
    setFilters({
      category: "",
      minPlayers: "",
      maxPlayers: "",
      minRating: ""
    });
    setIsFilterOpen(false);
  };
  
  // Check if any filters are applied
  const hasActiveFilters = () => {
    return Object.values(filters).some(value => value !== "");
  };
  
  // Function to get instance counts for each game
  const fetchGameInstanceCounts = useCallback(async (gamesList) => {
    if (!gamesList || gamesList.length === 0) return [];
    
    try {
      // Create a map to deduplicate games by ID
      const uniqueGamesMap = new Map();
      
      // First pass: create unique game entries
      gamesList.forEach(game => {
        if (!uniqueGamesMap.has(game.id)) {
          uniqueGamesMap.set(game.id, {
            ...game,
            instanceCount: 0,
            availableInstanceCount: 0,
            instances: []
          });
        }
      });
      
      // Second pass: fetch instances for each unique game
      await Promise.all(
        Array.from(uniqueGamesMap.values()).map(async (game) => {
          try {
            const instances = await getGameInstances(game.id);
            
            // Update the game with instance information
            const gameEntry = uniqueGamesMap.get(game.id);
            if (gameEntry) {
              gameEntry.instances = instances;
              gameEntry.instanceCount = instances.length;
              gameEntry.availableInstanceCount = instances.filter(i => i.available).length;
            }
          } catch (error) {
            console.error(`Error fetching instances for game ${game.id}:`, error);
          }
        })
      );
      
      // Convert the map back to an array
      return Array.from(uniqueGamesMap.values());
    } catch (error) {
      console.error("Error fetching game instance counts:", error);
      return gamesList;
    }
  }, []);
  
  const handleRequestGame = (game, instance) => {
    setSelectedGame(game);
    setSelectedInstance(instance);
    setIsRequestModalOpen(true);
  };
  
  const handleSubmitRequest = (requestData) => {
    console.log("Submitting request:", requestData);
    setSelectedInstance(null);
    setIsRequestModalOpen(false);
  };

  // Effect to update search from URL parameters
  useEffect(() => {
    const queryParam = searchParams.get("q");
    if (queryParam) {
      setSearchTerm(queryParam);
    }
  }, [searchParams]);
  
  // Effect to update URL when search term changes
  useEffect(() => {
    if (searchTerm) {
      searchParams.set("q", searchTerm);
      setSearchParams(searchParams);
    } else if (searchParams.has("q")) {
      searchParams.delete("q");
      setSearchParams(searchParams, { replace: true });
    }
  }, [searchTerm, searchParams, setSearchParams]);

  // Effect to fetch games when search term or filters change
  useEffect(() => {
    const fetchGames = async () => {
      setIsLoading(true);
      setError(null);
      const criteria = {
        name: searchTerm || undefined,
        category: filters.category || undefined,
        minPlayers: filters.minPlayers || undefined,
        maxPlayers: filters.maxPlayers || undefined,
        minRating: filters.minRating || undefined,
        // Remove owner filter to show all games from all users
        includeAllGames: true // Add this flag to include all games in the global library
      };
      // Remove empty/undefined criteria before sending
      Object.keys(criteria).forEach(key => (criteria[key] === undefined || criteria[key] === '') && delete criteria[key]);

      try {
        // Fetch games from API
        const fetchedGames = await searchGames(criteria);
        console.log("Fetched games:", fetchedGames);
        
        // Deduplicate and add instance data
        const gamesWithCounts = await fetchGameInstanceCounts(fetchedGames);
        console.log("Games with instance counts:", gamesWithCounts);
        
        setGames(gamesWithCounts);
      } catch (err) {
        console.error("Error fetching games:", err);
        setError(err.message || "Failed to fetch games. Please try again later.");
        setGames([]);
      } finally {
        setIsLoading(false);
      }
    };

    // Debounce the fetch call
    const debounceTimer = setTimeout(() => {
      fetchGames();
    }, 300);

    return () => clearTimeout(debounceTimer);
  }, [searchTerm, filters, fetchGameInstanceCounts]);

  // Function to navigate back to the user search page
  const handleBackToUsers = () => {
    if (fromUserId) {
      navigate(`/user-search?previewUser=${fromUserId}`);
    } else {
      navigate('/user-search');
    }
  };

  return (
    <div className="flex flex-col min-h-screen">
      <div className="flex-1 flex flex-col p-4 sm:p-6 lg:p-8 overflow-y-auto">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold">Game Library</h1>
          {fromUserId && (
            <Button 
              variant="outline" 
              className="flex items-center gap-2"
              onClick={handleBackToUsers}
            >
              <ArrowLeft size={16} />
              Back to Users
            </Button>
          )}
        </div>
        
        <div className="flex flex-col md:flex-row gap-4 mb-6">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search for games..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10"
            />
          </div>
          
          <div className="flex gap-2">
            <Button 
              variant={hasActiveFilters() ? "default" : "outline"} 
              className="flex items-center gap-2"
              onClick={() => setIsFilterOpen(!isFilterOpen)}
            >
              <Filter size={16} />
              Filter
              {hasActiveFilters() && <span className="bg-primary-foreground text-primary w-5 h-5 rounded-full text-xs flex items-center justify-center">{Object.values(filters).filter(v => v !== "").length}</span>}
            </Button>
          </div>
        </div>
        
        {isFilterOpen && (
          <div className="mb-6 p-4 border rounded-lg">
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-medium">Filter Games</h3>
              <Button variant="ghost" size="sm" onClick={clearFilters}>
                Clear All
              </Button>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium mb-1">Category</label>
                <Input
                  placeholder="Enter category (e.g., Strategy)"
                  value={filters.category}
                  onChange={(e) => setFilters({...filters, category: e.target.value})}
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Min Players</label>
                <Input 
                  type="number" 
                  min="1"
                  placeholder="Any"
                  value={filters.minPlayers}
                  onChange={(e) => setFilters({...filters, minPlayers: e.target.value})}
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-1">Max Players</label>
                <Input 
                  type="number" 
                  min="1"
                  placeholder="Any"
                  value={filters.maxPlayers}
                  onChange={(e) => setFilters({...filters, maxPlayers: e.target.value})}
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Min Rating</label>
                <Input 
                  type="number" 
                  min="0"
                  max="5"
                  step="0.5"
                  placeholder="Any"
                  value={filters.minRating}
                  onChange={(e) => setFilters({...filters, minRating: e.target.value})}
                />
              </div>
            </div>
            
            <div className="flex justify-end mt-4">
              <Button variant="default" onClick={applyFilters}>
                Apply Filters
              </Button>
            </div>
          </div>
        )}
        
        {isLoading && (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <span className="ml-2 text-lg">Loading games...</span>
          </div>
        )}
        
        {error && !isLoading && (
          <div className="bg-destructive/10 p-4 rounded-lg text-destructive mb-6">
            <p className="font-medium">{error}</p>
            <p>Please try again or adjust your search criteria.</p>
          </div>
        )}
        
        {!isLoading && !error && (
          <>
            {games.length === 0 ? (
              <div className="text-center py-12">
                <p className="text-lg text-muted-foreground">No games found matching your criteria.</p>
                <p className="text-muted-foreground">Try adjusting your search or filters.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                {games.map((game) => (
                  <Dialog key={game.id}>
                    <DialogTrigger asChild>
                      <div className="cursor-pointer">
                        <GameCardComponent 
                          game={game} 
                          showInstanceCount={true}
                        />
                      </div>
                    </DialogTrigger>
                    {isAuthenticated && (
                      <GameDetailsDialog 
                        game={{...game, instances: game.instances || []}} 
                        onRequestGame={handleRequestGame} 
                      />
                    )}
                  </Dialog>
                ))}
              </div>
            )}
          </>
        )}
        
        <RequestGameDialog 
          open={isRequestModalOpen} 
          onOpenChange={setIsRequestModalOpen}
          game={selectedGame}
          gameInstance={selectedInstance}
          onSubmit={handleSubmitRequest}
        />
      </div>
    </div>
  );
}
