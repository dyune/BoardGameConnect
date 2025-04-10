import { useState, useEffect } from 'react';
import { Button } from "@/components/ui/button.jsx";
import { Card, CardContent } from "@/components/ui/card.jsx";
import { Badge } from "@/components/ui/badge.jsx"; // Import Badge
import { actOnBorrowRequest, deleteBorrowRequest } from '@/service/dashboard-api.js'; // Add deleteBorrowRequest
import { useAuth } from "@/context/AuthContext"; // Import useAuth to check user type
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog.jsx";
import { getGameById, getGameInstanceById, getGameInstances } from '@/service/game-api.js';
import { getLendingRecordByRequestId } from '@/service/dashboard-api.js';
import { getBorrowRequestWithInstanceDetails } from '@/service/borrow_request-api.js';
import ReviewForm from '../game-search-page/ReviewForm.jsx';
import { useNavigate } from 'react-router-dom'; // Import useNavigate for navigation
import ModifyBorrowRequestDialog from './ModifyBorrowRequestDialog.jsx'; // Import the new dialog component

import { toast } from 'sonner';

export default function BorrowRequest({ id, name, requester, date, endDate, status, refreshRequests, imageSrc, gameId, requestedGameId, gameInstanceId }) {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const { user } = useAuth(); // Get user from auth context
  const [showDetails, setShowDetails] = useState(false);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [gameDetails, setGameDetails] = useState(null);
  const [lendingRecord, setLendingRecord] = useState(null);
  const [showReviewForm, setShowReviewForm] = useState(false);
  const [showModifyDialog, setShowModifyDialog] = useState(false); // State for modify dialog
  const [instanceLoading, setInstanceLoading] = useState(false); // New state for instance loading
  const [instanceOwner, setInstanceOwner] = useState(null); // State to store instance owner info

  const navigate = useNavigate(); // Initialize useNavigate

  const [isReturned, setIsReturned] = useState(false);

  
  // Check if user is a game owner
  const isGameOwner = user?.gameOwner === true;

  // Determine if this is a received request (not a sent request)
  // For received requests, the current user is the game owner, not the requester
  const isReceivedRequest = isGameOwner && requester !== user?.name;
  
  // Determine if the game has been returned and can be reviewed by the requester
  const canReview = status === 'APPROVED' && 
                    !isGameOwner &&
                    lendingRecord?.status === 'CLOSED'; // Only allow reviews for returned games

  // Load borrow request details when component mounts
  useEffect(() => {
    const loadBorrowRequestDetails = async () => {
      if (!id) return;
      
      try {
        setInstanceLoading(true);
        console.log("Loading borrow request details:", id);
        
        // Try to get detailed borrow request info
        const requestDetails = await getBorrowRequestWithInstanceDetails(id);
        console.log("Got borrow request details:", requestDetails);
        
        if (requestDetails.instanceOwner) {
          console.log("Found instance owner in borrow request:", requestDetails.instanceOwner);
          setInstanceOwner(requestDetails.instanceOwner);
        } else if (requestDetails.gameInstance?.owner) {
          console.log("Found instance owner in game instance:", requestDetails.gameInstance.owner);
          setInstanceOwner(requestDetails.gameInstance.owner);
        }
      } catch (err) {
        console.error("Error loading borrow request details:", err);
      } finally {
        setInstanceLoading(false);
      }
    };
    
    loadBorrowRequestDetails();
  }, [id]);

  // Load game details when component mounts to get owner information
  useEffect(() => {
    const loadGameDetails = async () => {
      try {
        setInstanceLoading(true);
        console.log("Loading game details for request:", { 
          requestId: id, 
          gameId: requestedGameId || gameId,
          gameInstanceId: gameInstanceId
        });
        
        // First try to load the instance directly if we have an ID
        if (gameInstanceId) {
          try {
            console.log("Attempting to fetch instance directly:", gameInstanceId);
            const instanceData = await getGameInstanceById(gameInstanceId);
            console.log("Successfully fetched instance:", instanceData);
            
            // Now fetch the game this instance belongs to
            const targetGameId = requestedGameId || gameId || instanceData.gameId;
            if (targetGameId) {
              const gameResponse = await getGameById(targetGameId);
              console.log("Fetched game details:", gameResponse);
              
              // Add the instance data to the game response
              if (!gameResponse.instances) {
                gameResponse.instances = [];
              }
              
              // Check if the instance is already in the list
              const existingIndex = gameResponse.instances.findIndex(
                inst => inst.id === parseInt(gameInstanceId)
              );
              
              if (existingIndex === -1) {
                gameResponse.instances.push(instanceData);
              } else {
                gameResponse.instances[existingIndex] = instanceData;
              }
              
              // Add instance owner to game details
              gameResponse.instanceOwner = instanceData.owner;
              gameResponse.gameInstance = instanceData;
              
              setGameDetails(gameResponse);
            }
          } catch (instanceErr) {
            console.error("Failed to fetch instance directly:", instanceErr);
            
            // Fall back to fetching the game and its instances
            await loadGameFromId();
          }
        } else {
          // No instance ID, just load the game
          await loadGameFromId();
        }
      } catch (err) {
        console.error("Failed to load initial game details:", err);
      } finally {
        setInstanceLoading(false);
      }
    };
    
    // Helper function to load game by ID
    const loadGameFromId = async () => {
      const targetGameId = requestedGameId || gameId;
      if (targetGameId) {
        const gameResponse = await getGameById(targetGameId);
        console.log("Fetched game details:", gameResponse);
        
        // If game doesn't have instances loaded, try to fetch them
        if (!gameResponse.instances || gameResponse.instances.length === 0) {
          try {
            console.log("Game has no instances, fetching them separately:", targetGameId);
            const instances = await getGameInstances(targetGameId);
            console.log("Fetched instances for game:", instances);
            gameResponse.instances = instances;
          } catch (instErr) {
            console.error("Failed to fetch instances for game:", instErr);
          }
        }
        
        // Try to find the game instance and its owner
        if (gameResponse.instances && gameResponse.instances.length > 0) {
          console.log("Found instances:", gameResponse.instances);
          
          // Try to find the specific instance by ID
          let matchedInstance = null;
          
          if (gameInstanceId) {
            console.log("Looking for instance with ID:", gameInstanceId);
            matchedInstance = gameResponse.instances.find(inst => inst.id === parseInt(gameInstanceId));
            if (matchedInstance) {
              console.log("Found matching instance by ID:", matchedInstance);
            } else {
              console.log("No matching instance found with ID:", gameInstanceId);
              // If not found in game response, we've already tried fetching directly above
            }
          }
          
          if (!matchedInstance) {
            console.log("Using first available instance as fallback");
            matchedInstance = gameResponse.instances[0];
          }
          
          if (matchedInstance && matchedInstance.owner) {
            console.log("Instance owner found:", matchedInstance.owner);
            // Add instance owner to game details
            gameResponse.instanceOwner = matchedInstance.owner;
            gameResponse.gameInstance = matchedInstance;
          } else {
            console.log("No owner found for instance:", matchedInstance);
          }
        } else {
          console.log("No instances found for game:", gameResponse);
        }
        
        setGameDetails(gameResponse);
      }
    };
    
    loadGameDetails();
  }, [requestedGameId, gameId, gameInstanceId, id]);

  // Determine if this is a sent request by the current user
  const isSentRequest = !isReceivedRequest && requester === user?.name;

  // Update isReturned state when lendingRecord changes
  useEffect(() => {
    if (lendingRecord) {
      setIsReturned(lendingRecord.status === 'CLOSED' || lendingRecord.status === 'Returned');
    }
  }, [lendingRecord]);

  const handleAction = async (status) => {
    if (!id) {
      console.error("Borrow request ID is missing!");
      setError("Cannot process request: ID missing.");
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      // Create a request object with only the status
      // The backend expects just the status for updating
      const requestBody = {
        status: status.toUpperCase()
      };
      
      console.log("[API Request] Updating borrow request:", {
        requestId: id,
        requestBody: requestBody,
        userId: localStorage.getItem('userId'),
        user: user
      });
      
      const response = await actOnBorrowRequest(id, requestBody);
      
      console.log("[API Response] Borrow request update response:", response);
      
      if (refreshRequests) {
        console.log("[UI] Refreshing borrow requests list");
        refreshRequests(); // Refresh the list in the parent component
      }
    } catch (err) {
      console.error("[API Error] Failed to update borrow request:", {
        error: err,
        errorMessage: err.message,
        errorStack: err.stack,
        userId: localStorage.getItem('userId'),
        user: user
      });
      setError(`Failed to ${status.toLowerCase()} request. Please try again.`);
    } finally {
      setIsLoading(false);
    }
  };

  // Handle image error by falling back to placeholder
  const handleImageError = (e) => {
    e.target.src = "https://placehold.co/200x200/e9e9e9/1d1d1d?text=No+Image";
  };

  const handleViewDetails = async () => {
    setDetailsLoading(true);
    setError(null);
    try {
      // If we have an instance ID, fetch it directly first
      if (gameInstanceId) {
        try {
          console.log("View Details - Fetching instance directly:", gameInstanceId);
          const instanceData = await getGameInstanceById(gameInstanceId);
          console.log("View Details - Successfully fetched instance:", instanceData);
          
          // Now fetch the game this instance belongs to
          const targetGameId = requestedGameId || gameId || instanceData.gameId;
          if (targetGameId) {
            const gameResponse = await getGameById(targetGameId);
            console.log("View Details - Fetched game details:", gameResponse);
            
            // Add the instance to the game response
            if (!gameResponse.instances) {
              gameResponse.instances = [];
            }
            
            // Check if the instance is already in the list
            const existingIndex = gameResponse.instances.findIndex(
              inst => inst.id === parseInt(gameInstanceId)
            );
            
            if (existingIndex === -1) {
              gameResponse.instances.push(instanceData);
            } else {
              gameResponse.instances[existingIndex] = instanceData;
            }
            
            // Add instance owner to game details
            gameResponse.instanceOwner = instanceData.owner;
            gameResponse.gameInstance = instanceData;
            
            setGameDetails(gameResponse);
          }
        } catch (instanceErr) {
          console.error("View Details - Failed to fetch instance directly:", instanceErr);
          
          // Fall back to just fetching the game
          await loadGameDetailsForViewDetails();
        }
      } else {
        // No instance ID, just load the game
        await loadGameDetailsForViewDetails();
      }

      // Only check for lending record if the request is approved
      // This avoids 404 errors for pending requests
      if (status === 'APPROVED' || status === 'ACTIVE') {
        try {
          const lendingResponse = await getLendingRecordByRequestId(id);
          console.log("Lending record response:", lendingResponse);
          setLendingRecord(lendingResponse);
          setIsReturned(lendingResponse.status === 'CLOSED' || lendingResponse.status === 'Returned');
        } catch (lendingErr) {
          console.log("No lending record found or not yet processed", lendingErr);
          setLendingRecord(null);
          setIsReturned(false);
        }
      } else {
        // For pending or declined requests, we know there's no lending record yet
        setLendingRecord(null);
        setIsReturned(false);
      }
      
      setShowDetails(true);
    } catch (err) {
      console.error("Failed to load game details:", err);
      setError("Failed to load game details.");
      toast.error("Failed to load game details. Please try again.");
    } finally {
      setDetailsLoading(false);
    }
  };

  // Helper function to load game details for the view details dialog
  const loadGameDetailsForViewDetails = async () => {
    const targetGameId = requestedGameId || gameId;
    if (targetGameId) {
      const gameResponse = await getGameById(targetGameId);
      console.log("View Details - Fetched game details:", gameResponse);
      
      // Try to find the game instance and its owner
      if (gameResponse.instances && gameResponse.instances.length > 0) {
        console.log("View Details - Found instances:", gameResponse.instances);
        
        // Try to find the specific instance by ID
        let matchedInstance = null;
        
        if (gameInstanceId) {
          console.log("View Details - Looking for instance with ID:", gameInstanceId);
          matchedInstance = gameResponse.instances.find(inst => inst.id === parseInt(gameInstanceId));
          if (matchedInstance) {
            console.log("View Details - Found matching instance by ID:", matchedInstance);
          }
        }
        
        if (!matchedInstance) {
          console.log("View Details - Using first available instance as fallback");
          matchedInstance = gameResponse.instances[0];
        }
        
        if (matchedInstance && matchedInstance.owner) {
          console.log("View Details - Instance owner found:", matchedInstance.owner);
          // Update gameDetails with the instance owner info
          setGameDetails(prev => ({
            ...prev,
            instanceOwner: matchedInstance.owner,
            gameInstance: matchedInstance
          }));
        } else {
          console.log("View Details - No owner found for instance:", matchedInstance);
        }
      }
      
      setGameDetails(gameResponse);
    }
  };

  const handleSubmitReview = (review) => {
    console.log("Review submitted:", review);
    setShowReviewForm(false);
    toast.success("Review submitted successfully!");
  };
  
  // Handler for navigating to game search page
  const handleGoToGame = () => {
    if (gameDetails?.name) {
      navigate(`/games?q=${encodeURIComponent(gameDetails.name)}`);
    }
  };

  // Helper function to get the display name for the game instance owner
  const getGameInstanceOwnerName = () => {
    console.log("Getting instance owner name from gameDetails:", {
      instanceOwner: gameDetails?.instanceOwner,
      gameInstance: gameDetails?.gameInstance,
      instances: gameDetails?.instances,
      gameInstanceId: gameInstanceId,
      directInstanceOwner: instanceOwner
    });
    
    // If we have a direct instance owner from the request, use that first
    if (instanceOwner) {
      if (instanceOwner.name) {
        console.log("Using direct instanceOwner.name:", instanceOwner.name);
        return instanceOwner.name;
      } else if (instanceOwner.email) {
        console.log("Using direct instanceOwner.email:", instanceOwner.email);
        return instanceOwner.email;
      }
    }
    
    // If game details are still loading, show a loading indicator
    if (!gameDetails) {
      return "Loading...";
    }
    
    // Prioritize the instance owner over the game creator
    if (gameDetails?.instanceOwner?.name) {
      console.log("Using instanceOwner.name:", gameDetails.instanceOwner.name);
      return gameDetails.instanceOwner.name;
    } 
    else if (gameDetails?.instanceOwner?.email) {
      console.log("Using instanceOwner.email:", gameDetails.instanceOwner.email);
      return gameDetails.instanceOwner.email;
    }
    else if (gameDetails?.gameInstance?.owner?.name) {
      console.log("Using gameInstance.owner.name:", gameDetails.gameInstance.owner.name);
      return gameDetails.gameInstance.owner.name;
    }
    else if (gameDetails?.gameInstance?.owner?.email) {
      console.log("Using gameInstance.owner.email:", gameDetails.gameInstance.owner.email);
      return gameDetails.gameInstance.owner.email;
    }
    else if (gameDetails?.instances && gameInstanceId) {
      // Try to find instance by ID
      const instance = gameDetails.instances.find(inst => inst.id === parseInt(gameInstanceId));
      if (instance?.owner?.name) {
        console.log("Found instance by ID with owner name:", instance.owner.name);
        return instance.owner.name;
      }
      else if (instance?.owner?.email) {
        console.log("Found instance by ID with owner email:", instance.owner.email);
        return instance.owner.email;
      }
    }
    else if (gameDetails?.instances && gameDetails.instances.length > 0) {
      const firstInstance = gameDetails.instances[0];
      if (firstInstance?.owner?.name) {
        console.log("Using first instance owner name:", firstInstance.owner.name);
        return firstInstance.owner.name;
      }
      else if (firstInstance?.owner?.email) {
        console.log("Using first instance owner email:", firstInstance.owner.email);
        return firstInstance.owner.email;
      }
    }
    
    // Fallback to the game creator if we can't find the instance owner
    if (gameDetails?.owner?.name) {
      console.log("Falling back to game creator name:", gameDetails.owner.name);
      return gameDetails.owner.name;
    }
    else if (gameDetails?.owner?.email) {
      console.log("Falling back to game creator email:", gameDetails.owner.email);
      return gameDetails.owner.email;
    }
    
    console.log("No owner information found, using default");
    return "Unknown Owner";
  };

  // Handle canceling a borrow request
  const handleCancelRequest = async () => {
    if (!id) {
      console.error("Borrow request ID is missing!");
      setError("Cannot process request: ID missing.");
      return;
    }
    
    if (!confirm("Are you sure you want to cancel this borrow request?")) {
      return;
    }
    
    setIsLoading(true);
    setError(null);
    try {
      console.log("[API Request] Cancelling borrow request:", id);
      
      await deleteBorrowRequest(id);
      
      toast.success("Borrow request cancelled successfully");
      
      if (refreshRequests) {
        console.log("[UI] Refreshing borrow requests list");
        refreshRequests(); // Refresh the list in the parent component
      }
    } catch (err) {
      console.error("[API Error] Failed to cancel borrow request:", {
        error: err,
        errorMessage: err.message,
        errorStack: err.stack,
        userId: localStorage.getItem('userId'),
        user: user
      });
      setError("Failed to cancel request. Please try again.");
      toast.error("Failed to cancel request. Please try again.");
    } finally {
      setIsLoading(false);
    }
  };

  // Toggle the modify dialog
  const handleToggleModifyDialog = () => {
    setShowModifyDialog(!showModifyDialog);
  };

  // Handle modify success
  const handleModifySuccess = () => {
    setShowModifyDialog(false);
    if (refreshRequests) {
      refreshRequests();
    }
  };

  return (
    <>
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-col md:flex-row gap-4">
            <div className="md:w-1/4">
              <img
                src={imageSrc || "https://placehold.co/200x200/e9e9e9/1d1d1d?text=No+Image"}
                alt={`Cover art for ${name}`}
                className="w-full h-full object-cover rounded-lg aspect-square"
                onError={handleImageError}
              />
            </div>
            <div className="flex-1">
              <div className="flex justify-between">
                <h3 className="text-xl font-semibold">Request for "{name}"</h3>
                <Badge
                  variant={
                    status === 'APPROVED' ? 'positive' :
                    status === 'DECLINED' ? 'destructive' :
                    'outline' // Default for Pending or other statuses
                  }
                  className="text-xs"
                >
                  {status || 'Pending'} {/* Display status, default to Pending */}
                </Badge>
              </div>
              {error && <p className="text-red-500 text-sm mt-2">{error}</p>}
              <div className="grid gap-1 mt-2">
                <div className="text-sm">
                  <span className="font-medium">{isReceivedRequest ? "From:" : "From:"}</span> {requester}
                </div>
                <div className="text-sm">
                  <span className="font-medium">{isReceivedRequest ? "To:" : "To:"}</span> {
                    instanceLoading ? 
                      <span className="text-muted-foreground italic">Loading instance owner...</span> : 
                      getGameInstanceOwnerName()
                  }
                </div>
                <div className="text-sm">
                  <span className="font-medium">Requested on:</span> {date}
                </div>
                <div className="text-sm">
                  <span className="font-medium">End Date:</span> {endDate}
                </div>
              </div>
              
              <div className="flex gap-2 mt-4">
                {/* Only show action buttons for game owners and if it's a received request and status is Pending */}
                {isGameOwner && isReceivedRequest && status === 'PENDING' && (
                  <>
                    <Button 
                      variant="outline" 
                      disabled={isLoading} 
                      onClick={() => handleAction('DECLINED')}
                    >
                      {isLoading ? 'Processing...' : 'Decline'}
                    </Button>
                    <Button 
                      variant="positive" 
                      disabled={isLoading} 
                      onClick={() => handleAction('APPROVED')}
                    >
                      {isLoading ? 'Processing...' : 'Approve'}
                    </Button>
                  </>
                )}
                
                {/* Show Modify and Cancel buttons for the user's own pending requests */}
                {isSentRequest && status === 'PENDING' && (
                  <>
                    <Button 
                      variant="default" 
                      disabled={isLoading} 
                      onClick={handleToggleModifyDialog}
                    >
                      {isLoading ? 'Processing...' : 'Modify Request'}
                    </Button>
                    <Button 
                      variant="destructive" 
                      disabled={isLoading} 
                      onClick={handleCancelRequest}
                    >
                      {isLoading ? 'Processing...' : 'Cancel Request'}
                    </Button>
                  </>
                )}
                
                {/* View Details button for anyone */}
                <Button
                  variant="outline"
                  disabled={detailsLoading}
                  onClick={handleViewDetails}
                >
                  {detailsLoading ? 'Loading...' : 'View Details'}
                </Button>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Details Dialog */}
      <Dialog open={showDetails} onOpenChange={setShowDetails}>
        <DialogContent className="max-w-[600px] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Borrow Request Details</DialogTitle>
            <DialogDescription>
              View details about your borrow request and its current status.
            </DialogDescription>
          </DialogHeader>
          
          <div className="space-y-4">
            {gameDetails && (
              <div className="space-y-2">
                <h3 className="font-medium text-lg">{gameDetails.name}</h3>
                <div className="flex gap-4">
                  <img
                    src={gameDetails.image || imageSrc || "https://placehold.co/200x200/e9e9e9/1d1d1d?text=No+Image"}
                    alt={gameDetails.name}
                    className="w-24 h-24 object-cover rounded"
                    onError={handleImageError}
                  />
                  <div className="space-y-1 text-sm">
                    <p><span className="font-medium">Category:</span> {gameDetails.category || 'Unknown'}</p>
                    <p><span className="font-medium">Players:</span> {gameDetails.minPlayers}-{gameDetails.maxPlayers}</p>
                    <p><span className="font-medium">Request Status:</span> {status}</p>
                    <p>
                      <span className="font-medium">Return Status:</span>{" "}
                      {isReturned ? (
                        <Badge variant="positive" className="ml-2">Returned</Badge>
                      ) : lendingRecord ? (
                        <Badge variant="outline" className="ml-2">Not Returned</Badge>
                      ) : (
                        <Badge variant="outline" className="ml-2">Not Yet Lent</Badge>
                      )}
                    </p>
                  </div>
                </div>
                {gameDetails.description && (
                  <p className="text-sm text-gray-600">{gameDetails.description}</p>
                )}
                
                {/* Add a Go to Game button */}
                <div className="flex justify-end mt-2">
                  <Button 
                    onClick={handleGoToGame}
                    variant="outline"
                    size="sm"
                    className="flex items-center gap-1"
                  >
                    Go to Game
                  </Button>
                </div>
              </div>
            )}

            {/* Review section - only show for borrowers if game is returned */}
            {canReview && !showReviewForm && (
              <div className="pt-4 border-t">
                <p className="text-sm text-muted-foreground mb-2 italic">
                  You can only review games that you have borrowed and returned.
                </p>
                <Button 
                  onClick={() => setShowReviewForm(true)}
                  variant="outline"
                  className="w-full"
                >
                  Review This Game
                </Button>
              </div>
            )}

            {/* Review form */}
            {showReviewForm && (
              <div className="pt-4 border-t">
                <ReviewForm 
                  gameId={requestedGameId || gameId}
                  onReviewSubmitted={handleSubmitReview}
                  onCancel={() => setShowReviewForm(false)}
                />
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>

      {/* Modify Borrow Request Dialog */}
      {showModifyDialog && (
        <ModifyBorrowRequestDialog
          open={showModifyDialog}
          onOpenChange={setShowModifyDialog}
          requestId={id}
          gameId={requestedGameId || gameId}
          onSuccess={handleModifySuccess}
        />
      )}
    </>
  );
}