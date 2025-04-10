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
import { updateEvent, updateEventWithGameChange, getEventById } from "../../service/event-api.js";
import { getGamesByOwner } from "../../service/game-api.js";
import { Loader2, Calendar, Users, MapPin, Search, X, Check, AlertCircle, Info } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { 
  Card,
  CardContent
} from "../../ui/card";

// Create a utility function to help with debugging
const logState = (message, data = {}) => {
  console.log(`[UpdateEventDialog] ${message}`, data);
};

export default function UpdateEventDialog({ open, onOpenChange, onEventUpdated, eventId }) {
  // Debug render count
  const renderCount = useRef(0);
  renderCount.current++;
  
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingEvent, setIsLoadingEvent] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [gameSearchResults, setGameSearchResults] = useState([]);
  const [selectedGameId, setSelectedGameId] = useState(null);
  const [userGames, setUserGames] = useState([]);
  const [isLoadingUserGames, setIsLoadingUserGames] = useState(false);
  const [gameLoadError, setGameLoadError] = useState("");
  const [isInputFocused, setIsInputFocused] = useState(false);
  const [eventData, setEventData] = useState(null);
  const { user } = useAuth();
  
  // Track if component is mounted - set to true by default
  const isMountedRef = useRef(true);
  const searchInputRef = useRef(null);
  const loadingTimeoutRef = useRef(null);
  const hasAttemptedLoadRef = useRef(false);
  const fetchPromiseRef = useRef(null);

  // Log component re-renders
  logState(`Render #${renderCount.current}, isLoadingUserGames=${isLoadingUserGames}, gamesCount=${userGames.length}`);

  const { register, handleSubmit, formState: { errors }, reset, setValue, watch } = useForm({
    defaultValues: {
      title: "",
      dateTime: "",
      location: "",
      description: "",
      maxParticipants: "",
      gameSearchTermInput: "",
    },
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

  // Load event data when dialog opens and eventId is provided
  useEffect(() => {
    if (!open || !eventId) return;

    const fetchEventData = async () => {
      try {
        setIsLoadingEvent(true);
        const event = await getEventById(eventId);
        
        if (!isMountedRef.current) return;
        
        setEventData(event);
        
        // Format date and time for the datetime-local input
        const dateObj = new Date(event.dateTime);
        
        // Adjust for timezone - convert from UTC to local time
        // The datetime-local input expects local time in YYYY-MM-DDTHH:MM format
        // without timezone info, so we need to adjust the dateObj first
        const year = dateObj.getFullYear();
        const month = String(dateObj.getMonth() + 1).padStart(2, '0');
        const day = String(dateObj.getDate()).padStart(2, '0');
        const hours = String(dateObj.getHours()).padStart(2, '0');
        const minutes = String(dateObj.getMinutes()).padStart(2, '0');
        
        // Create local datetime string in required format
        const formattedDateTime = `${year}-${month}-${day}T${hours}:${minutes}`;
        
        console.log("Original dateTime:", event.dateTime);
        console.log("Converted to local formatted datetime:", formattedDateTime);
        
        // Populate form with event data
        setValue("title", event.title || "");
        setValue("dateTime", formattedDateTime);
        setValue("location", event.location || "");
        setValue("description", event.description || "");
        setValue("maxParticipants", event.maxParticipants || "");
        
        // Set selected game
        if (event.featuredGame) {
          setSelectedGameId(event.featuredGame.id);
          setValue("gameSearchTermInput", event.featuredGame.name || "");
        }
      } catch (error) {
        console.error("Error fetching event data:", error);
        toast.error("Failed to load event data");
        if (isMountedRef.current) {
          setSubmitError("Failed to load event data. Please try again.");
        }
      } finally {
        if (isMountedRef.current) {
          setIsLoadingEvent(false);
        }
      }
    };
    
    fetchEventData();
  }, [open, eventId, setValue]);

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
        fetchPromiseRef.current = getGamesByOwner(userEmail);
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
          setGameLoadError("No games found. Please add a game to your collection first.");
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

  const onSubmit = handleSubmit(async (data) => {
    if (!selectedGameId) {
      setSubmitError("Please select one of your games from the search results.");
      return;
    }

    if (!eventId) {
      setSubmitError("Event ID is missing. Cannot update event.");
      return;
    }

    const isUserGame = userGames.some(game => game.id === selectedGameId);
    if (!isUserGame) {
      setSubmitError("You can only use games you own for events.");
      return;
    }

    const { gameSearchTermInput, ...formData } = data;
    setIsLoading(true);
    setSubmitError("");

    // Ensure proper formatting of the data
    const payload = {
      ...formData,
      featuredGameId: selectedGameId,
      maxParticipants: parseInt(formData.maxParticipants, 10) || 1
    };

    // Check if game selection changed
    const gameChanged = eventData?.featuredGame?.id !== selectedGameId;

    // Log the payload being sent
    console.log(`Submitting event ${gameChanged ? 'recreation' : 'update'} with payload:`, payload);
    console.log("Event ID:", eventId);
    
    // Display update attempt to user
    toast.info(gameChanged 
      ? "Updating event with new game (may take a moment)..." 
      : "Updating event...", 
      { duration: 2000 }
    );

    try {
      let result;
      
      if (gameChanged) {
        // Use special recreation approach when game changed
        result = await updateEventWithGameChange(eventId, payload);
        
        // Since we're creating a new event, the ID has changed
        if (result.eventId && result.eventId !== eventId) {
          console.log(`Event ID changed from ${eventId} to ${result.eventId}`);
        }
      } else {
        // Use standard update when game hasn't changed
        result = await updateEvent(eventId, payload);
      }
      
      if (isMountedRef.current) {
        toast.success(`Successfully updated event: ${result.title}`);
        if (onEventUpdated) {
          onEventUpdated(result);
        }
        handleCancel();
      }
    } catch (error) {
      console.error("Update event error:", error);
      
      if (isMountedRef.current) {
        let errorMsg = "Failed to update event. Please try again.";
        
        // Handle different types of errors
        if (error.message) {
          console.error("Error message:", error.message);
          
          if (error.message.includes("500")) {
            errorMsg = "Server error occurred. As a workaround, try creating a new event with these details and delete this one.";
            
            // Add a create option button
            toast.error(errorMsg, {
              duration: 5000,
              action: {
                label: "Create New",
                onClick: () => {
                  // Close this dialog and open create dialog with same data
                  handleCancel();
                  // Suggestion to create a new event instead
                  toast.info("To create a new event instead, use the 'Create Event' button with the same details", {
                    duration: 5000
                  });
                }
              }
            });
          } else {
            errorMsg = error.message;
            toast.error(errorMsg);
          }
        }
        
        setSubmitError(errorMsg);
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
                <p className="text-sm text-blue-700">You don't have any games. Please add a game to your collection first.</p>
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
          {userGames.length} games available for selection
        </p>
      </div>
    );
  };

  return (
    <Dialog open={open} onOpenChange={handleDialogChange}>
      <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-2xl font-bold text-center">Update Event</DialogTitle>
          <DialogDescription className="text-center text-gray-500">
            Update the details of your event.
          </DialogDescription>
        </DialogHeader>

        {isLoadingEvent ? (
          <div className="flex items-center justify-center py-10">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <span className="ml-2">Loading event details...</span>
          </div>
        ) : (
          <form onSubmit={onSubmit} className="space-y-6 py-4">
            {/* Event Basic Info Card */}
            <Card>
              <CardContent className="pt-6">
                <div className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="title" className="text-base font-medium">Event Title <span className="text-red-500">*</span></Label>
                    <Input
                      id="title"
                      {...register("title", { required: "Title is required" })}
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
                        {...register("dateTime", { required: "Date and time is required" })}
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
                          min: { value: 1, message: "Must be greater than 0" }
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
                      <div className="absolute inset-y-0 left-3 flex items-center pointer-events-none">
                        <Search className="h-5 w-5 text-gray-400" />
                      </div>
                      <Input
                        ref={searchInputRef}
                        id="gameSearchTermInput"
                        placeholder="Search your games..."
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
                                className="w-full text-left px-4 py-3 text-sm border-b border-gray-100 hover:bg-gray-50 transition-colors flex items-center justify-between"
                                onClick={() => handleGameSelect(game)}
                              >
                                <span>{game.name}</span>
                                <Check className="h-4 w-4 text-gray-400 opacity-0 group-hover:opacity-100" />
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
                  <div className="bg-gray-50 p-4 rounded-md border border-gray-200">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="font-medium">
                          {userGames.find(g => g.id === selectedGameId)?.name}
                        </p>
                        <p className="text-xs text-gray-500 mt-1">
                          Selected game for your event
                        </p>
                      </div>
                      <Button
                        type="button"
                        variant="outline"
                        size="icon"
                        onClick={clearSelectedGame}
                        className="h-8 w-8"
                        title="Change game"
                      >
                        <X className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Description Card */}
            <Card>
              <CardContent className="pt-6">
                <div className="space-y-2">
                  <Label htmlFor="description" className="text-base font-medium">Description</Label>
                  <Textarea
                    id="description"
                    {...register("description")}
                    className="min-h-[120px] resize-none"
                    placeholder="Describe your event... What makes it special? What should attendees know?"
                  />
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
                disabled={isLoading || isLoadingUserGames || (userGames.length === 0 && !gameLoadError)}
                className="flex-1"
              >
                {isLoading ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Updating...
                  </>
                ) : "Update Event"}
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
} 