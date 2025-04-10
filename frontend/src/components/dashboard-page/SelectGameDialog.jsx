import { useState, useEffect } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList, CommandLoading } from "@/components/ui/command";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Check, ChevronsUpDown, Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { searchGames } from "@/service/game-api.js";
import { useAuth } from "@/context/AuthContext";
import AddGameInstanceDialog from "./AddGameInstanceDialog";

export default function SelectGameDialog({ open, onOpenChange, onGameInstanceAdded, maxVisibleGames = 4 }) {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [games, setGames] = useState([]);
  const [selectedGame, setSelectedGame] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [filteredGames, setFilteredGames] = useState([]);
  const [openCombobox, setOpenCombobox] = useState(false);
  const [showInstanceDialog, setShowInstanceDialog] = useState(false);
  const { user } = useAuth();

  // Fetch games when the dialog opens
  useEffect(() => {
    if (open) {
      fetchGames();
    }
  }, [open]);

  // Filter games based on search query
  useEffect(() => {
    if (!searchQuery.trim()) {
      setFilteredGames(games);
      return;
    }

    const lowercaseQuery = searchQuery.toLowerCase();
    const filtered = games.filter(
      game => game.name.toLowerCase().includes(lowercaseQuery)
    );
    setFilteredGames(filtered);
  }, [games, searchQuery]);

  // Fetch all games for the global library
  const fetchGames = async () => {
    setIsLoading(true);
    setError("");

    try {
      // Get all games from the global library
      const fetchedGames = await searchGames({ includeAllGames: true });
      
      // Log the results for debugging
      console.log("Fetched games for the global library:", fetchedGames);
      
      if (Array.isArray(fetchedGames) && fetchedGames.length > 0) {
        setGames(fetchedGames);
        setFilteredGames(fetchedGames);
      } else {
        setGames([]);
        setFilteredGames([]);
        setError("No games found in the global library.");
      }
    } catch (err) {
      console.error("Failed to fetch games:", err);
      setError("Could not load games from the library.");
      setGames([]);
      setFilteredGames([]);
    } finally {
      setIsLoading(false);
    }
  };

  // Handle selecting a game
  const handleSelectGame = (game) => {
    setSelectedGame(game);
    setOpenCombobox(false);
  };

  // Handle proceeding to add instance
  const handleAddInstance = () => {
    if (!selectedGame) {
      setError("Please select a game first");
      return;
    }
    
    setShowInstanceDialog(true);
  };

  // Handle instance added
  const handleInstanceAdded = (instance) => {
    if (onGameInstanceAdded) {
      onGameInstanceAdded(instance);
    }
    
    // Close all dialogs and reset state
    setSelectedGame(null);
    setShowInstanceDialog(false);
    onOpenChange(false);
  };

  // Custom reset function
  const handleCancel = () => {
    setSelectedGame(null);
    setError("");
    setOpenCombobox(false);
    onOpenChange(false);
  };

  // Calculate the height based on the number of visible games
  const getDropdownHeight = () => {
    const itemHeight = 50; // Approximate height of each game item in pixels
    return `${Math.min(filteredGames.length, maxVisibleGames) * itemHeight}px`;
  };

  return (
    <>
      <Dialog open={open} onOpenChange={(isOpen) => {
        if (!isOpen) handleCancel();
        else onOpenChange(true);
      }}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>Select Game</DialogTitle>
            <DialogDescription>
              Choose a game from the global library to add a copy to your collection
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="gameSelect">Select Game</Label>
              {games.length === 0 && !isLoading && (
                <div className="p-4 border border-amber-200 bg-amber-50 rounded-md mb-3">
                  <p className="text-sm text-amber-800">
                    No games are available in the library. You can still add a new game to create one.
                  </p>
                </div>
              )}
              <Popover open={openCombobox} onOpenChange={setOpenCombobox}>
                <PopoverTrigger asChild>
                  <Button
                    id="gameSelect"
                    variant="outline"
                    role="combobox"
                    aria-expanded={openCombobox}
                    className="w-full justify-between"
                    onClick={() => setOpenCombobox(true)}
                    disabled={games.length === 0}
                  >
                    {selectedGame ? selectedGame.name : "Choose a game..."}
                    {isLoading ? (
                      <Loader2 className="ml-2 h-4 w-4 animate-spin" />
                    ) : (
                      <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
                    )}
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-[400px] p-0 max-h-[400px] overflow-auto">
                  <Command shouldFilter={false}>
                    <CommandInput 
                      placeholder="Search games..." 
                      value={searchQuery}
                      onValueChange={setSearchQuery}
                      autoFocus
                    />
                    {isLoading && <CommandLoading />}
                    {!isLoading && filteredGames.length === 0 && (
                      <CommandEmpty>
                        {searchQuery.length > 0 ? "No games found." : "No games in the global library."}
                      </CommandEmpty>
                    )}
                    {filteredGames.length > 0 && (
                      <CommandGroup>
                        <CommandList 
                          className="overflow-y-auto" 
                          style={{ maxHeight: getDropdownHeight() }}
                        >
                          {filteredGames.map((game) => (
                            <div 
                              key={game.id}
                              className="px-2 py-1.5 text-sm rounded-sm cursor-pointer hover:bg-accent hover:text-accent-foreground flex items-start py-2"
                              onClick={() => handleSelectGame(game)}
                              tabIndex={0}
                              role="option"
                              aria-selected={selectedGame?.id === game.id}
                            >
                              {game.image && (
                                <img
                                  src={game.image}
                                  alt={game.name}
                                  className="h-10 w-10 object-cover rounded mr-2"
                                  onError={(e) => {
                                    e.target.style.display = 'none';
                                  }}
                                />
                              )}
                              <div className="flex-1 overflow-hidden">
                                <div className="flex items-center">
                                  <span className="font-medium">{game.name}</span>
                                  <Check
                                    className={cn(
                                      "ml-auto h-4 w-4",
                                      selectedGame?.id === game.id ? "opacity-100" : "opacity-0"
                                    )}
                                  />
                                </div>
                                <p className="text-muted-foreground text-sm truncate">
                                  Players: {game.minPlayers}-{game.maxPlayers}
                                </p>
                              </div>
                            </div>
                          ))}
                        </CommandList>
                      </CommandGroup>
                    )}
                  </Command>
                </PopoverContent>
              </Popover>
            </div>

            {error && (
              <p className="text-red-500 text-sm text-center">{error}</p>
            )}

            <DialogFooter className="pt-4">
              <Button variant="outline" type="button" onClick={handleCancel}>
                Cancel
              </Button>
              <Button 
                type="button" 
                onClick={handleAddInstance}
                disabled={!selectedGame}
              >
                Continue
              </Button>
            </DialogFooter>
          </div>
        </DialogContent>
      </Dialog>

      {/* Instance Dialog */}
      <AddGameInstanceDialog
        open={showInstanceDialog}
        onOpenChange={setShowInstanceDialog}
        gameId={selectedGame?.id}
        gameName={selectedGame?.name}
        onInstanceAdded={handleInstanceAdded}
      />
    </>
  );
} 