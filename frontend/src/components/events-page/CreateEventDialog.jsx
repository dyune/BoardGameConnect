import { useState, useEffect, useCallback, useRef } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { 
  Dialog, 
  DialogContent, 
  DialogHeader, 
  DialogTitle, 
  DialogFooter,
  DialogDescription
} from "../../ui/dialog";
import { Button } from "../../ui/button";
import { Input } from "../../ui/input";
import { Textarea } from "../../ui/textarea";
import { Label } from "../../ui/label";
import { createEvent } from "../../service/event-api.js";
import { getGamesAvailableForEvents } from "../../service/game-api.js";
import { Loader2, Calendar, Users, MapPin, Search, X, Check, AlertCircle, Info } from "lucide-react";
import { useAuth } from "../../context/AuthContext.jsx";
import { 
  Card,
  CardContent
} from "../../ui/card";
import {
  FormField,
  FormItem,
  FormLabel,
  FormControl
} from "../../ui/form";
import { ScrollArea } from "../../ui/scroll-area";
import { Badge } from "../../ui/badge";
import { cn } from "../../lib/utils";

// Create a utility function to help with debugging
const logState = (message, data = {}) => {
  if (process.env.NODE_ENV === 'development') {
    console.log(`[CreateEventDialog] ${message}`, data);
  }
};

export default function CreateEventDialog({ open, onOpenChange, onEventAdded }) {
  // Debug render count
  const renderCount = useRef(0);
  renderCount.current++;
  
  const [isLoading, setIsLoading] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [gameSearchResults, setGameSearchResults] = useState([]);
  const [selectedGameId, setSelectedGameId] = useState(null);
  const [userGames, setUserGames] = useState([]);
  const [isLoadingUserGames, setIsLoadingUserGames] = useState(false);
  const [gameLoadError, setGameLoadError] = useState("");
  const [isInputFocused, setIsInputFocused] = useState(false);
  const { user } = useAuth();
  
  // Track if component is mounted - set to true by default
  const isMountedRef = useRef(true);
  const searchInputRef = useRef(null);
  const loadingTimeoutRef = useRef(null);
  const hasAttemptedLoadRef = useRef(false);
  const fetchPromiseRef = useRef(null);

  // Log component re-renders
  logState(`Render #${renderCount.current}, isLoadingUserGames=${isLoadingUserGames}, gamesCount=${userGames.length}`);

  const { register, handleSubmit, formState: { errors, isValid, isDirty }, reset, setValue, watch, control } = useForm({
    defaultValues: {
      title: "",
      dateTime: "",
      location: "",
      description: "",
      maxParticipants: "",
      gameSearchTermInput: "",
    },
    mode: "onChange",
  });

  const userEmail = user?.email;
  
  // Cleanup on unmount
  useEffect(() => {
    logState("Component mounted");
    // Set mounted flag to true on mount
    isMountedRef.current = true;
    
    return () => {
      logState("Component unmounting");
      // Set mounted flag to false on unmount
      isMountedRef.current = false;
      if (loadingTimeoutRef.current) {
        clearTimeout(loadingTimeoutRef.current);
      }
    };
  }, []);

  // Load user's games when dialog opens
  useEffect(() => {
    // Don't run this effect if the component is unmounted
    if (!isMountedRef.current) {
      logState("Skipping fetch effect because component is unmounted");
      return;
    }
    
    // Clean old timeouts
    if (loadingTimeoutRef.current) {
      clearTimeout(loadingTimeoutRef.current);
      loadingTimeoutRef.current = null;
    }

    if (!open) {
      logState("Dialog closed, resetting states");
      setUserGames([]);
      setGameSearchResults([]);
      setGameLoadError("");
      setIsLoadingUserGames(false);
      hasAttemptedLoadRef.current = false;
      return;
    }

    if (!userEmail) {
      logState("No user email available");
      setGameLoadError("User email not available. Please log in again.");
      return;
    }

    if (hasAttemptedLoadRef.current) {
      logState("Already attempted loading games in this session");
      return;
    }

    logState("Starting to fetch games for email", { userEmail });
    setIsLoadingUserGames(true);
    setUserGames([]);
    setGameSearchResults([]);
    setGameLoadError("");
    hasAttemptedLoadRef.current = true;

    // Safety timeout to ensure loading state never gets stuck
    loadingTimeoutRef.current = setTimeout(() => {
      if (isMountedRef.current && isLoadingUserGames) {
        logState("Loading timeout triggered");
        setIsLoadingUserGames(false);
        setGameLoadError("Loading timed out. Please try again.");
        toast.error("Game loading timed out");
      }
    }, 15000);

    async function fetchUserGames() {
      try {
        logState("Fetching games for user", { userEmail });
        
        // Store the promise in a ref to prevent unmounting during fetch
        fetchPromiseRef.current = getGamesAvailableForEvents(userEmail);
        const games = await fetchPromiseRef.current;
        fetchPromiseRef.current = null;
        
        // Very critical check - ensure component is still mounted
        if (!isMountedRef.current) {
          logState("Component unmounted during fetch, skipping state updates");
          return;
        }

        logState("Successfully fetched games", { count: Array.isArray(games) ? games.length : 0, games });
        
        // Clear safety timeout since we got a response
        if (loadingTimeoutRef.current) {
          clearTimeout(loadingTimeoutRef.current);
          loadingTimeoutRef.current = null;
        }
        
        if (!Array.isArray(games)) {
          logState("Games response is not an array", { games });
          setGameLoadError("Invalid response from server. Please try again.");
          setUserGames([]);
          setGameSearchResults([]);
        } else if (games.length === 0) {
          logState("No games found for user");
          setGameLoadError("No games available for events. You need to own games or have approved borrowed games to create an event.");
          setUserGames([]);
          setGameSearchResults([]);
        } else {
          logState("Setting games in state", { count: games.length });
          // To avoid state update issues, update all related state in single synchronous block
          setUserGames(games);
          setGameSearchResults([...games]);
          setGameLoadError("");
          // IMPORTANT: Set loading to false immediately after setting games
          setIsLoadingUserGames(false);
        }
      } catch (error) {
        // Only process error if component is still mounted
        if (!isMountedRef.current) return;
        
        logState("Error fetching games", { error });
        setUserGames([]);
        setGameSearchResults([]);
        setGameLoadError("Failed to load your games. Please try again later.");
        setIsLoadingUserGames(false); // Immediately set loading to false
        toast.error("Failed to load your games");
      } finally {
        // This ensures loading state is always reset regardless of outcome
        if (isMountedRef.current) {
          logState("Setting loading state to false in finally block");
          setIsLoadingUserGames(false);
        }
        
        // Clean up fetch reference
        fetchPromiseRef.current = null;
      }
    }

    // Start the fetch process and ensure proper error handling
    fetchUserGames()
      .catch(err => {
        if (!isMountedRef.current) return;
        
        logState("Unhandled error in fetchUserGames", { error: err });
        setIsLoadingUserGames(false);
        setGameLoadError("An unexpected error occurred. Please try again.");
        if (loadingTimeoutRef.current) {
          clearTimeout(loadingTimeoutRef.current);
          loadingTimeoutRef.current = null;
        }
      });

  }, [open, userEmail]);

  // Handle dialog close properly - prevent unmounting during fetch
  const handleDialogChange = useCallback((isOpen) => {
    if (!isOpen) {
      // If there's an active fetch, wait for it to complete before closing
      if (fetchPromiseRef.current && isLoadingUserGames) {
        logState("Fetch in progress, delaying dialog close");
        // Set a short timeout to check again
        setTimeout(() => {
          if (isMountedRef.current) {
            handleCancel();
          }
        }, 100);
      } else {
        handleCancel();
      }
    } else {
      onOpenChange(true);
    }
  }, [isLoadingUserGames]);

  // Watch the game search term input to trigger filtering
  const watchedGameSearchTerm = watch("gameSearchTermInput");

  // Filter user's games based on search term
  useEffect(() => {
    if (userGames.length === 0 || selectedGameId) {
      return;
    }

    if (!watchedGameSearchTerm || watchedGameSearchTerm.trim() === '') {
      setGameSearchResults([...userGames]);
      return;
    }

    const searchTerm = watchedGameSearchTerm.toLowerCase().trim();
    const filteredGames = userGames.filter(game => 
      game.name.toLowerCase().includes(searchTerm)
    );
    setGameSearchResults(filteredGames);
  }, [watchedGameSearchTerm, selectedGameId, userGames]);

  const handleGameSelect = useCallback((game) => {
    logState("Game selected", { gameId: game.id, gameName: game.name });
    setSelectedGameId(game.id);
    setValue("gameSearchTermInput", game.name);
    setGameSearchResults([]);
    setSubmitError("");
    setIsInputFocused(false);
  }, [setValue]);

  const clearSelectedGame = useCallback(() => {
    logState("Clearing selected game");
    setSelectedGameId(null);
    setValue("gameSearchTermInput", "");
    if (searchInputRef.current) {
      searchInputRef.current.focus();
    }
  }, [setValue]);

  // Function to retry loading games
  const handleRetryLoadGames = useCallback(() => {
    if (isLoadingUserGames) {
      logState("Already loading games, ignoring retry request");
      return;
    }
    
    logState("Retrying to load games");
    setGameLoadError("");
    setIsLoadingUserGames(true);
    hasAttemptedLoadRef.current = false; // Reset the load attempt flag

    // Directly call the API without the state check 
    async function refetchGames() {
      try {
        logState("Re-fetching games directly");
        const emailToUse = userEmail ? `${userEmail}` : null;
        
        if (!emailToUse) {
          throw new Error("No user email available for retry");
        }
        
        const games = await getGamesAvailableForEvents(emailToUse);
        
        if (!isMountedRef.current) return;
        
        logState("Retry fetch complete", { 
          success: true, 
          count: Array.isArray(games) ? games.length : 0 
        });
        
        if (!Array.isArray(games)) {
          setGameLoadError("Invalid response from server. Please try again.");
          setUserGames([]);
          setGameSearchResults([]);
        } else if (games.length === 0) {
          setGameLoadError("No games available for events. You need to own games or have approved borrowed games to create an event.");
          setUserGames([]);
          setGameSearchResults([]);
        } else {
          setUserGames(games);
          setGameSearchResults([...games]);
          setGameLoadError("");
        }
      } catch (error) {
        if (!isMountedRef.current) return;
        
        logState("Error in retry fetch", { error });
        setGameLoadError("Failed to load your games. Please try again.");
        toast.error("Failed to reload your games");
      } finally {
        if (isMountedRef.current) {
          logState("Setting loading state to false after retry");
          setIsLoadingUserGames(false);
        }
      }
    }
    
    refetchGames();
  }, [userEmail, isLoadingUserGames]);

  const onSubmit = handleSubmit(async (data) => {
    if (!selectedGameId) {
      setSubmitError("Please select a game from the search results.");
      return;
    }

    // Get the selected game
    const selectedGame = userGames.find(game => game.id === selectedGameId);
    
    if (!selectedGame) {
      setSubmitError("Selected game not found. Please try again.");
      return;
    }
    
    // Log ALL properties of the selected game
    console.log("All properties of the selected game:", Object.keys(selectedGame));
    console.log("Full game object:", selectedGame);
    
    // If this is a borrowed game, validate that the event date is within the borrow period
    if (selectedGame?.isBorrowed) {
      const eventDate = new Date(data.dateTime);
      const borrowStartDate = new Date(selectedGame.borrowStartDate);
      const borrowEndDate = new Date(selectedGame.borrowEndDate);
      
      // Check if event date is within the borrow period
      if (eventDate < borrowStartDate || eventDate > borrowEndDate) {
        // Format dates with both date and time
        const formatDateTime = (date) => {
          return date.toLocaleString(undefined, {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
          });
        };
        
        setSubmitError(
          `This event must be scheduled between ${formatDateTime(borrowStartDate)} and ${formatDateTime(borrowEndDate)} since the game is borrowed.`
        );
        return;
      }
    }

    const { gameSearchTermInput, ...formData } = data;
    setIsLoading(true);
    setSubmitError("");

    // Log the full selected game object to debug
    console.log("Selected game for event:", selectedGame);

    // For borrowed games, ensure we're using the correct game ID
    // Some backends might expect the original game ID, not the instance ID
    let gameId = null;
    
    if (selectedGame.isBorrowed) {
      if (selectedGame.requestedGameId) {
        console.log("Using requestedGameId for borrowed game:", selectedGame.requestedGameId);
        gameId = selectedGame.requestedGameId;
      } else if (selectedGame.gameId) {
        console.log("Using gameId for borrowed game:", selectedGame.gameId);
        gameId = selectedGame.gameId;
      } else {
        console.log("Using id for borrowed game:", selectedGame.id);
        gameId = selectedGame.id;
      }
    } else {
      console.log("Using game.id for owned game:", selectedGame.id);
      gameId = selectedGame.id;
    }
    
    // Validate gameId is not null, undefined, or NaN
    if (!gameId || (typeof gameId === 'number' && isNaN(gameId))) {
      console.error("Invalid game ID:", gameId);
      setSubmitError("Invalid game ID. Please select a different game.");
      setIsLoading(false);
      return;
    }
    
    // Ensure gameId is a number if it's a numeric string
    if (typeof gameId === 'string' && !isNaN(gameId)) {
      gameId = parseInt(gameId, 10);
    }
    
    // Try to get the game instance ID
    let gameInstanceId = null;
    if (selectedGame.isBorrowed) {
      if (selectedGame.gameInstanceId) {
        gameInstanceId = selectedGame.gameInstanceId;
      } else if (selectedGame.instanceId) {
        gameInstanceId = selectedGame.instanceId;
      } else if (selectedGame.borrowRequestId) {
        gameInstanceId = selectedGame.borrowRequestId;
      }
      
      console.log("Using game instance ID:", gameInstanceId);
    }
    
    // Convert the local dateTime string to a proper ISO string (UTC)
    const localDateTime = new Date(formData.dateTime);
    const isoDateTimeString = localDateTime.toISOString();
    console.log(`[CreateEventDialog] Converted local dateTime '${formData.dateTime}' to ISO string '${isoDateTimeString}'`);

    // Build a payload with explicitly named fields, using the ISO string
    const payload = {
      ...formData,
      dateTime: isoDateTimeString, // Use the ISO string
      featuredGame: {
        id: gameId
      },
      // Only include gameInstanceId if it exists and this is a borrowed game
      ...(selectedGame.isBorrowed && gameInstanceId && {
        gameInstanceId: gameInstanceId,
        isBorrowed: true
      })
    };
    
    // Debug logging
    console.log("Event payload:", JSON.stringify(payload, null, 2));

    try {
      const result = await createEvent(payload);
      
      if (isMountedRef.current) {
        toast.success(`Successfully created event: ${result.title}`);
        if (onEventAdded) {
          onEventAdded();
        }
        handleCancel();
      }
    } catch (error) {
      console.error("Create event error:", error);
      
      if (isMountedRef.current) {
        // Extract the specific error message from the API response
        let errorMsg = "Failed to create event. Please try again.";
        
        if (error.message) {
          // Try to parse JSON error messages
          if (error.message.includes("Event description cannot be empty")) {
            errorMsg = "Event description cannot be empty.";
            setValue("description", "", { shouldValidate: true });
          } else if (error.message.includes("API error:")) {
            try {
              const jsonStr = error.message.replace("API error:", "").trim();
              const parsedError = JSON.parse(jsonStr);
              errorMsg = parsedError.error || errorMsg;
            } catch (e) {
              errorMsg = error.message;
            }
          } else {
            errorMsg = error.message;
          }
        }
        
        setSubmitError(errorMsg);
        toast.error(errorMsg);
      }
    } finally {
      if (isMountedRef.current) {
        setIsLoading(false);
      }
    }
  });

  const handleCancel = useCallback(() => {
    logState("Cancel button clicked");
    reset();
    setSelectedGameId(null);
    setGameSearchResults([]);
    setSubmitError("");
    setGameLoadError("");
    if (loadingTimeoutRef.current) {
      clearTimeout(loadingTimeoutRef.current);
      loadingTimeoutRef.current = null;
    }
    setIsLoadingUserGames(false);
    hasAttemptedLoadRef.current = false;
    onOpenChange(false);
  }, [reset, onOpenChange]);

  const handleInputFocus = useCallback(() => {
    setIsInputFocused(true);
    if (userGames.length > 0 && !selectedGameId) {
      setGameSearchResults([...userGames]);
    }
  }, [userGames, selectedGameId]);

  const handleInputBlur = useCallback(() => {
    setTimeout(() => {
      setIsInputFocused(false);
    }, 200);
  }, []);

  useEffect(() => {
    if (open) {
      logState("Dialog opened");
      setSelectedGameId(null);
      setGameSearchResults([]);
      setSubmitError("");
      setGameLoadError("");
    }
  }, [open]);

  // Add a new useEffect to trigger form validation when selectedGameId changes
  useEffect(() => {
    // When a game is selected or unselected, trigger form validation
    if (selectedGameId) {
      // Force form validation to update
      handleSubmit(() => {})();
    }
  }, [selectedGameId, handleSubmit]);

  const renderGameSelector = () => {
    // Show debug info in development environment
    const debugInfo = (
      <div className="text-xs text-gray-400 mb-2">
        {/* Loading: {isLoadingUserGames ? 'yes' : 'no'} |  */}
        {/* Games: {userGames.length} |  */}
        {/* Error: {gameLoadError ? 'yes' : 'no'} */}
      </div>
    );
    
    if (isLoadingUserGames) {
      return (
        <div className="my-4">
          {debugInfo}
          <div className="flex items-center justify-center py-4">
            <Loader2 className="h-6 w-6 animate-spin text-primary" />
            <span className="ml-2 text-sm">Loading your games...</span>
          </div>
        </div>
      );
    }

    if (gameLoadError) {
      return (
        <div className="my-4">
          {debugInfo}
          <div className="rounded-md bg-yellow-50 p-4 my-2">
            <div className="flex">
              <div className="flex-shrink-0">
                <AlertCircle className="h-5 w-5 text-yellow-400" />
              </div>
              <div className="ml-3 flex-1">
                <p className="text-sm text-yellow-700">{gameLoadError}</p>
                <div className="mt-2">
                  <Button 
                    type="button" 
                    variant="outline" 
                    size="sm" 
                    onClick={handleRetryLoadGames}
                    disabled={isLoadingUserGames}
                    className="text-xs"
                  >
                    {isLoadingUserGames ? (
                      <>
                        <Loader2 className="mr-1 h-3 w-3 animate-spin" />
                        Retrying...
                      </>
                    ) : "Try Again"}
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }

    if (userGames.length === 0) {
      return (
        <div className="my-4">
          {debugInfo}
          <div className="rounded-md bg-blue-50 p-4 my-2">
            <div className="flex">
              <div className="flex-shrink-0">
                <Info className="h-5 w-5 text-blue-400" />
              </div>
              <div className="ml-3">
                <p className="text-sm text-blue-700">You don't have any games available. You need to either own games or have active approved borrow requests to create an event.</p>
              </div>
            </div>
          </div>
        </div>
      );
    }

    return (
      <div className="my-2">
        {debugInfo}
        <p className="text-sm text-green-600">
          {userGames.length} games available for selection (owned and borrowed)
        </p>
      </div>
    );
  };

  return (
    <Dialog open={open} onOpenChange={handleDialogChange}>
      <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-2xl font-bold text-center">Create New Event</DialogTitle>
          <DialogDescription className="text-center text-gray-500">
            Fill out the form below to create a new event featuring a game from your collection or one you've borrowed.
            <span className="block mt-1 text-xs text-amber-600">
              Note: For borrowed games, events must be scheduled within your borrowing period.
            </span>
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={onSubmit} className="space-y-6 py-4">
          {/* Event Basic Info Card */}
          <Card>
            <CardContent className="pt-6">
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="title" className="text-base font-medium">Event Title <span className="text-red-500">*</span></Label>
                  <Input
                    id="title"
                    {...register("title", { 
                      required: "Title is required",
                      minLength: { value: 3, message: "Title must be at least 3 characters" } 
                    })}
                    className={`h-11 px-4 ${errors.title ? "border-red-500" : ""}`}
                    placeholder="Enter event title..."
                  />
                  {errors.title && <p className="text-red-500 text-sm mt-1">{errors.title.message}</p>}
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="dateTime" className="text-base font-medium flex items-center">
                      <Calendar className="h-4 w-4 mr-2" />
                      Date and Time <span className="text-red-500">*</span>
                    </Label>
                    <Input
                      id="dateTime"
                      type="datetime-local"
                      {...register("dateTime", { 
                        required: "Date and time is required",
                        validate: value => {
                          const date = new Date(value);
                          const now = new Date();
                          return date > now || "Event date must be in the future";
                        }
                      })}
                      className={`h-11 ${errors.dateTime ? "border-red-500" : ""}`}
                    />
                    {errors.dateTime && <p className="text-red-500 text-sm mt-1">{errors.dateTime.message}</p>}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="maxParticipants" className="text-base font-medium flex items-center">
                      <Users className="h-4 w-4 mr-2" />
                      Max Participants <span className="text-red-500">*</span>
                    </Label>
                    <Input
                      id="maxParticipants"
                      type="number"
                      min="1"
                      placeholder="Number of players"
                      {...register("maxParticipants", {
                        required: "Maximum participants is required",
                        valueAsNumber: true,
                        min: { value: 1, message: "Must be greater than 0" },
                        max: { value: 100, message: "Must be less than 100" }
                      })}
                      className={`h-11 ${errors.maxParticipants ? "border-red-500" : ""}`}
                    />
                    {errors.maxParticipants && <p className="text-red-500 text-sm mt-1">{errors.maxParticipants.message}</p>}
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="location" className="text-base font-medium flex items-center">
                    <MapPin className="h-4 w-4 mr-2" />
                    Location
                  </Label>
                  <Input 
                    id="location" 
                    {...register("location")} 
                    className="h-11"
                    placeholder="Where will the event take place?" 
                  />
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Game Selection Card */}
          <Card>
            <CardContent className="pt-6">
              <Label className="text-base font-medium mb-2 block">
                Select Your Game <span className="text-red-500">*</span>
                {isLoadingUserGames && <Loader2 className="ml-2 h-4 w-4 inline animate-spin" />}
              </Label>

              {!selectedGameId ? (
                <div className="space-y-3">
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
                      <Search className="h-5 w-5 text-gray-400" />
                    </div>
                    <Input
                      ref={searchInputRef}
                      id="gameSearchTermInput"
                      placeholder="Search your owned and borrowed games..."
                      {...register("gameSearchTermInput")}
                      onChange={(e) => {
                        setValue("gameSearchTermInput", e.target.value);
                      }}
                      onFocus={handleInputFocus}
                      onBlur={handleInputBlur}
                      autoComplete="off"
                      className={`pl-10 h-11 ${!selectedGameId && submitError.includes("select") ? "border-red-500" : ""}`}
                      disabled={isLoadingUserGames}
                    />
                  </div>

                  {renderGameSelector()}

                  {isInputFocused && userGames.length > 0 && !isLoadingUserGames && (
                    <div className="mt-1 border rounded-md border-gray-200 shadow-sm overflow-hidden">
                      {gameSearchResults.length > 0 ? (
                        <div className="max-h-48 overflow-y-auto">
                          {gameSearchResults.map((game) => (
                            <button
                              key={game.id}
                              type="button"
                              className="w-full px-4 py-2 text-left hover:bg-gray-100 focus:outline-none cursor-pointer"
                              onClick={() => {
                                // Store both the game ID and requestedGameId for borrowed games
                                setSelectedGameId(game.id);
                                setValue("gameSearchTermInput", game.name);
                                
                                // If this is a borrowed game, add a data attribute to track the original gameId
                                if (game.isBorrowed) {
                                  console.log("Selected a borrowed game:", game);
                                  // The requestedGameId is usually the main game ID we want to use
                                  if (game.requestedGameId) {
                                    console.log("This borrowed game has a requestedGameId:", game.requestedGameId);
                                  }
                                }
                                
                                // Also update search results for rendering correct count
                                setGameSearchResults([game]);
                                setIsInputFocused(false);
                              }}
                            >
                              <div className="flex items-center">
                                {game.image ? (
                                  <img 
                                    src={game.image} 
                                    alt={game.name} 
                                    className="w-10 h-10 mr-3 object-cover rounded" 
                                    onError={(e) => {
                                      e.target.onerror = null;
                                      e.target.src = "https://placehold.co/100x100?text=No+Image";
                                    }}
                                  />
                                ) : (
                                  <div className="w-10 h-10 mr-3 bg-gray-200 flex items-center justify-center rounded">
                                    <span className="text-xs text-gray-500">No img</span>
                                  </div>
                                )}
                                <div>
                                  <div className="font-medium">{game.name}</div>
                                  <div className="text-xs text-gray-500 flex items-center">
                                    <span className="mr-2">Players: {game.minPlayers}-{game.maxPlayers}</span>
                                    {game.isBorrowed && (
                                      <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">
                                        Borrowed
                                      </span>
                                    )}
                                  </div>
                                </div>
                              </div>
                            </button>
                          ))}
                        </div>
                      ) : (
                        <div className="p-4 text-sm text-gray-500 text-center">
                          No matching games found
                        </div>
                      )}
                    </div>
                  )}

                  {submitError && submitError.includes("select") && (
                    <p className="text-red-500 text-sm mt-1">{submitError}</p>
                  )}
                </div>
              ) : (
                <div className="space-y-2">
                  <p className="text-sm font-medium">Selected Game:</p>
                  <div className="flex items-center justify-between border rounded-lg p-3">
                    <div className="flex items-center space-x-3">
                      {selectedGameId && (
                        <div className="space-y-2">
                          {(() => {
                            const selectedGame = userGames.find(g => g.id === selectedGameId) || {};
                            return (
                              <div className="flex items-center justify-between border rounded-lg p-3">
                                <div className="flex items-center space-x-3">
                                  {selectedGame.image ? (
                                    <img 
                                      src={selectedGame.image} 
                                      alt={selectedGame.name} 
                                      className="w-12 h-12 object-cover rounded" 
                                      onError={(e) => {
                                        e.target.onerror = null;
                                        e.target.src = "https://placehold.co/100x100?text=No+Image";
                                      }}
                                    />
                                  ) : (
                                    <div className="w-12 h-12 bg-gray-200 flex items-center justify-center rounded">
                                      <span className="text-xs text-gray-500">No img</span>
                                    </div>
                                  )}
                                  <div>
                                    <div className="font-medium">{selectedGame.name}</div>
                                    <div className="text-xs text-gray-500 mt-1 flex items-center">
                                      {selectedGame.isBorrowed ? (
                                        <div className="flex flex-col">
                                          <Badge className="mb-1 self-start" variant="outline">Borrowed</Badge>
                                          <span className="text-xs text-amber-600">
                                            Available until: {new Date(selectedGame.borrowEndDate).toLocaleDateString()}
                                          </span>
                                        </div>
                                      ) : (
                                        <span>Players: {selectedGame.minPlayers}-{selectedGame.maxPlayers}</span>
                                      )}
                                    </div>
                                  </div>
                                </div>
                                <Button
                                  type="button"
                                  variant="ghost"
                                  size="sm"
                                  className="h-8 w-8 p-0"
                                  onClick={() => {
                                    setSelectedGameId(null);
                                    setValue("gameSearchTermInput", "");
                                  }}
                                >
                                  <X className="h-4 w-4" />
                                  <span className="sr-only">Clear selection</span>
                                </Button>
                              </div>
                            );
                          })()}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Description Card */}
          <Card>
            <CardContent className="pt-6">
              <div className="space-y-2">
                <Label htmlFor="description" className="text-base font-medium">Description <span className="text-red-500">*</span></Label>
                <Textarea
                  id="description"
                  {...register("description", { 
                    required: "Description is required",
                    minLength: { value: 10, message: "Description must be at least 10 characters" } 
                  })}
                  className={`min-h-[120px] resize-none ${errors.description ? "border-red-500" : ""}`}
                  placeholder="Describe your event... What makes it special? What should attendees know?"
                />
                {errors.description && <p className="text-red-500 text-sm mt-1">{errors.description.message}</p>}
              </div>
            </CardContent>
          </Card>

          {/* Error Message */}
          {submitError && !submitError.includes("select") && (
            <div className="rounded-md bg-red-50 p-4">
              <div className="flex">
                <div className="flex-shrink-0">
                  <AlertCircle className="h-5 w-5 text-red-400" />
                </div>
                <div className="ml-3">
                  <p className="text-sm text-red-700">{submitError}</p>
                </div>
              </div>
            </div>
          )}

          <DialogFooter className="pt-4 flex gap-3">
            <Button 
              variant="outline" 
              type="button" 
              onClick={handleCancel}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button 
              type="submit" 
              disabled={isLoading || isLoadingUserGames || (userGames.length === 0 && !gameLoadError) || !isDirty || !isValid || !selectedGameId}
              className="flex-1"
            >
              {isLoading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Creating...
                </>
              ) : "Create Event"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}