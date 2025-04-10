import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { createGame, getGamesByOwner } from "../../service/game-api.js";
import { searchBoardGames, getBoardGameById, convertBgaGameToAppFormat } from "../../service/boardgame-api.js";
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList, CommandLoading } from "@/components/ui/command";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Check, ChevronsUpDown, Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { Textarea } from "@/components/ui/textarea";
import AddGameInstanceDialog from "./AddGameInstanceDialog";
import { useAuth } from "@/context/AuthContext";

export default function AddGameDialog({ open, onOpenChange, onGameAdded }) {
  const { user } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [boardGameSuggestions, setBoardGameSuggestions] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [isSearching, setIsSearching] = useState(false);
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);
  const [openCombobox, setOpenCombobox] = useState(false);
  const [selectedGame, setSelectedGame] = useState(null);
  const [createdGameId, setCreatedGameId] = useState(null);
  const [createdGameName, setCreatedGameName] = useState("");
  const [showInstanceDialog, setShowInstanceDialog] = useState(false);
  const [userGames, setUserGames] = useState([]);
  const [isCheckingDuplicates, setIsCheckingDuplicates] = useState(false);

  const { register, handleSubmit, formState: { errors }, reset, setValue, watch } = useForm({
    defaultValues: {
      name: "",
      minPlayers: "",
      maxPlayers: "",
      image: "",
      category: "",
      description: "",
    },
  });

  // Watch the name field for changes
  const nameValue = watch("name");

  // Effect to handle search as user types in the name field
  useEffect(() => {
    const delaySearch = setTimeout(async () => {
      if (nameValue && nameValue.trim().length >= 2) {
        await searchBoardGamesFromAPI(nameValue);
      }
    }, 500);

    return () => clearTimeout(delaySearch);
  }, [nameValue]);

  // Fetch user's games when dialog opens
  useEffect(() => {
    if (open && user?.email) {
      const fetchUserGames = async () => {
        try {
          const games = await getGamesByOwner(user.email, false);
          setUserGames(games || []);
        } catch (error) {
          console.error("Error fetching user games:", error);
        }
      };
      fetchUserGames();
    }
  }, [open, user?.email]);

  // Search board games from the API
  const searchBoardGamesFromAPI = async (query) => {
    if (!query || query.trim().length < 2) {
      setBoardGameSuggestions([]);
      return;
    }
    
    setIsSearching(true);
    try {
      const games = await searchBoardGames(query);
      setBoardGameSuggestions(games || []);
    } catch (error) {
      console.error("Error searching board games:", error);
      toast.error("Failed to fetch game suggestions");
    } finally {
      setIsSearching(false);
    }
  };

  // Handle selecting a game from suggestions
  const handleSelectGame = async (game) => {
    // First set basic info from search result
    setSelectedGame(game);
    setOpenCombobox(false);
    
    // Set basic form values from the search result
    setValue("name", game.name);
    if (game.year_published) {
      setValue("yearPublished", game.year_published);
    }
    
    // If we need more details, fetch them
    if (!game.description || game.min_players === null) {
      setIsLoadingDetails(true);
      try {
        const detailedGame = await getBoardGameById(game.id);
        if (detailedGame) {
          // Update the selected game with detailed info
          setSelectedGame(detailedGame);
          // Convert and update form values
          const formattedGame = convertBgaGameToAppFormat(detailedGame);
          setValue("name", formattedGame.name);
          setValue("minPlayers", formattedGame.minPlayers);
          setValue("maxPlayers", formattedGame.maxPlayers);
          setValue("image", formattedGame.image);
          setValue("category", formattedGame.category);
          setValue("description", formattedGame.description);
        }
      } catch (error) {
        console.error("Error fetching game details:", error);
        toast.error("Failed to load detailed game information");
      } finally {
        setIsLoadingDetails(false);
      }
    } else {
      // We already have enough info from the search result
      const formattedGame = convertBgaGameToAppFormat(game);
      setValue("minPlayers", formattedGame.minPlayers);
      setValue("maxPlayers", formattedGame.maxPlayers);
      setValue("image", formattedGame.image);
      setValue("category", formattedGame.category);
      setValue("description", formattedGame.description);
    }
  };

  const onSubmit = handleSubmit(async (data) => {
    setIsLoading(true);
    setSubmitError("");

    if (!user?.email) {
      setSubmitError("You must be logged in to add a game. Please refresh the page or log in again.");
      setIsLoading(false);
      return;
    }

    // Check if a game with this name already exists in the user's collection
    const gameName = data.name.trim();
    const gameExists = userGames.some(game => 
      game.name.toLowerCase() === gameName.toLowerCase()
    );

    if (gameExists) {
      setSubmitError("A game with this name already exists in your collection. Please use a different name.");
      toast.error("Game already exists in your collection");
      setIsLoading(false);
      return;
    }

    try {
      // Ensure player counts are numbers
      const gameData = {
        ...data,
        minPlayers: parseInt(data.minPlayers, 10),
        maxPlayers: parseInt(data.maxPlayers, 10),
        // Include the BoardGameGeek ID if available
        bggId: selectedGame?.id || null,
        // Include ownerId which is required by the backend
        ownerId: user.email,
        // Always set to false since we'll create the instance separately
        createInstance: false
      };
      
      // Validate the player counts
      if (isNaN(gameData.minPlayers) || gameData.minPlayers < 1) {
        throw new Error("Minimum players must be at least 1");
      }
      
      if (isNaN(gameData.maxPlayers) || gameData.maxPlayers < gameData.minPlayers) {
        throw new Error("Maximum players must be at least equal to minimum players");
      }
      
      const result = await createGame(gameData);
      toast.success(`Successfully added game: ${result.name}`);
      
      // Store the created game info for the instance creation
      setCreatedGameId(result.id);
      setCreatedGameName(result.name);
      
      // Close this dialog since the game has been created
      reset();
      setSelectedGame(null);
      onOpenChange(false);
      
      if (onGameAdded) {
        onGameAdded(result); // Notify parent component of game creation
      }
    } catch (error) {
      let errorMsg = error.message || "Failed to add game. Game name must be unique.";
      
      // Check for specific error types
      if (errorMsg.includes("Owner not found")) {
        errorMsg = "User account is not found or not authorized to add games. Please refresh and try again.";
      } else if (errorMsg.includes("Authentication required")) {
        errorMsg = "Your session may have expired. Please log in again.";
      } else if (error.status === 500 || errorMsg.includes("unexpected error")) {
        // Handle 500 errors which are likely due to duplicate game names
        errorMsg = "A game with this name already exists in your collection. Please use a different name.";
      }
      
      setSubmitError(errorMsg);
      toast.error(errorMsg);
    } finally {
      setIsLoading(false);
    }
  });

  // Handle instance added
  const handleInstanceAdded = (instance) => {
    // Reset everything and close all dialogs
    reset();
    setSelectedGame(null);
    setShowInstanceDialog(false);
    onOpenChange(false);
    
    // Notify the user that both game and instance were added
    toast.success("Game and copy added to your collection!");
  };

  // Custom reset function
  const handleCancel = () => {
    reset();
    setSubmitError("");
    setSelectedGame(null);
    setOpenCombobox(false);
    onOpenChange(false);
  };

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="sm:max-w-[500px] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Add New Game</DialogTitle>
            <DialogDescription>
              Enter the details for your new game
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={onSubmit} className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="name">Game Name</Label>
              <Input
                id="name"
                value={nameValue}
                onChange={(e) => setValue("name", e.target.value)}
                placeholder="Enter game name"
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="minPlayers">Minimum Players</Label>
              <Input
                id="minPlayers"
                type="number"
                value={watch("minPlayers")}
                onChange={(e) => setValue("minPlayers", e.target.value)}
                min="1"
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="maxPlayers">Maximum Players</Label>
              <Input
                id="maxPlayers"
                type="number"
                value={watch("maxPlayers")}
                onChange={(e) => setValue("maxPlayers", e.target.value)}
                min={watch("minPlayers")}
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="image">Image URL (Optional)</Label>
              <Input
                id="image"
                value={watch("image")}
                onChange={(e) => setValue("image", e.target.value)}
                placeholder="Enter image URL"
              />
            </div>

            {watch("image") && (
              <div className="mt-2 w-full flex justify-center">
                <img 
                  src={watch("image")} 
                  alt="Game preview" 
                  className="h-32 object-contain rounded border"
                  onError={(e) => {
                    e.target.src = "https://placehold.co/400x400?text=No+Image";
                  }}
                />
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="category">Category</Label>
              <Input id="category" {...register("category")} placeholder="e.g., Strategy, Party, Family" />
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Textarea 
                id="description" 
                {...register("description")} 
                placeholder="A brief description of the game..."
                className="min-h-[100px]"
              />
            </div>

            {submitError && (
              <p className="text-red-500 text-sm text-center">{submitError}</p>
            )}

            <DialogFooter className="pt-4">
              <Button variant="outline" type="button" onClick={handleCancel}>
                Cancel
              </Button>
              <Button 
                type="submit" 
                disabled={isLoading}
              >
                {isLoading ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Adding...
                  </>
                ) : (
                  "Add Game"
                )}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Instance Dialog */}
      <AddGameInstanceDialog
        open={false}
        onOpenChange={(isOpen) => {
          if (!isOpen) {
            setShowInstanceDialog(false);
          }
        }}
        gameId={createdGameId}
        gameName={createdGameName}
        onInstanceAdded={handleInstanceAdded}
        initialMessage="The system automatically creates a default copy. You can customize its details here or do it later."
      />
    </>
  );
}
