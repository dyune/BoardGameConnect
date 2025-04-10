import { useEffect, useState, useCallback } from "react";
import BorrowRequest from "@/components/dashboard-page/BorrowRequest.jsx";
import { TabsContent } from "@/components/ui/tabs.jsx";
// Imports kept/adapted from HEAD for Auth/Loading/Error handling
import { UnauthorizedError } from "@/service/apiClient";
import { useAuth } from "@/context/AuthContext";
import { Loader2 } from "lucide-react";
// Imports added from origin/dev-Yessine-D3 for logic/UI
import { getBorrowRequestsByOwner } from "@/service/borrow_request-api.js"; // For received requests
import { getGameById } from "@/service/game-api.js"; // For enriching data
import { toast } from "sonner"; // For notifications
import { getBorrowRequestsByRequester } from "@/service/borrow_request-api.js"; // For sent requests
import { getOutgoingBorrowRequests } from "@/service/dashboard-api.js"; // Alternative API for sent requests

export default function DashboardBorrowRequests({ userType }) {
  // State from HEAD (more robust)
  const [receivedRequests, setReceivedRequests] = useState([]);
  const [sentRequests, setSentRequests] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [fetchAttempted, setFetchAttempted] = useState(false);
  const { user, isAuthenticated, authReady } = useAuth(); // Auth context from HEAD

  // Fetch function for both types of requests
  const fetchBorrowRequests = useCallback(async () => {
    // Checks from HEAD
    if (!user?.id || !isAuthenticated || !authReady) {
      if (!isLoading) return;
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setFetchAttempted(true); // Mark that fetch was attempted
      setError(null); // Clear previous errors

      // Fetch sent requests for all users
      const outgoingRequests = await getOutgoingBorrowRequests(user.id);
      
      // Enrich sent requests with game details
      const enrichedSentRequests = await Promise.all(
        outgoingRequests.map(async (req) => {
          try {
            const game = await getGameById(req.requestedGameId);
            console.log(`Enriched sent request ${req.id} with game ${req.requestedGameId}:`, {
              game,
              gameInstanceId: req.gameInstanceId
            });
            return { 
              ...req, 
              requestedGameName: game.name, 
              gameImage: game.image || game.imageUrl || null, // Try both image field names
              requesterName: user.name, // Use current user's name for sent requests
              gameInstanceId: req.gameInstanceId // Pass gameInstanceId if available
            };
          } catch (error) {
            console.error(`Error fetching game details for request ${req.id}, game ${req.requestedGameId}:`, error);
            return { 
              ...req, 
              requestedGameName: "(Unknown Game)", 
              gameImage: null,
              requesterName: user.name,
              gameInstanceId: req.gameInstanceId // Pass gameInstanceId if available
            };
          }
        })
      );
      
      setSentRequests(enrichedSentRequests);

      // Only fetch received requests if user is a game owner
      if (userType === 'owner') {
        // Fetch logic for received requests
        const ownerRequests = await getBorrowRequestsByOwner(user.id);

        // Enriching logic for received requests
        const enrichedReceivedRequests = await Promise.all(
          ownerRequests.map(async (req) => {
            try {
              const game = await getGameById(req.requestedGameId);
              console.log(`Enriched received request ${req.id} with game ${req.requestedGameId}:`, {
                game,
                gameInstanceId: req.gameInstanceId
              });
              return { 
                ...req, 
                requestedGameName: game.name, 
                gameImage: game.image || game.imageUrl || null, // Try both image field names
                requesterName: req.requesterName,
                gameInstanceId: req.gameInstanceId // Pass gameInstanceId if available
              };
            } catch (error) {
              console.error(`Error fetching game details for request ${req.id}, game ${req.requestedGameId}:`, error);
              return { 
                ...req, 
                requestedGameName: "(Unknown Game)", 
                gameImage: null, 
                requesterName: req.requesterName,
                gameInstanceId: req.gameInstanceId // Pass gameInstanceId if available
              };
            }
          })
        );

        setReceivedRequests(enrichedReceivedRequests);
      } else {
        // Clear received requests if user is not a game owner
        setReceivedRequests([]);
      }
    } catch (err) {
      console.error("Error fetching borrow requests:", err);
      const errorMsg = err instanceof UnauthorizedError
        ? "Authentication error. Please try logging in again."
        : "Failed to load borrow requests: " + (err.message || "Please try again later.");

      setError(errorMsg);
      toast.error(errorMsg);
      setSentRequests([]);
      setReceivedRequests([]);
    } finally {
      setIsLoading(false);
    }
  }, [user, isAuthenticated, authReady, userType]);

  // useEffect hooks from HEAD to manage fetching based on auth state
  useEffect(() => {
    // Reset fetch attempted flag when auth state becomes ready and authenticated
    if (authReady && isAuthenticated && user?.id) {
      setFetchAttempted(false);
    }
  }, [authReady, isAuthenticated, user]);

  useEffect(() => {
    // Fetch only when auth is ready, user is authenticated, and fetch hasn't been attempted yet for this auth state
    if (authReady && isAuthenticated && user?.id && !fetchAttempted) {
       const timer = setTimeout(() => {
         fetchBorrowRequests();
       }, 300); // Short delay might still be useful
       return () => clearTimeout(timer);
    } else if (authReady && (!isAuthenticated || !user?.id)) {
       // If auth is ready but user is not logged in, ensure loading is false and clear data
       setIsLoading(false);
       setError(null);
       setSentRequests([]);
       setReceivedRequests([]);
       setFetchAttempted(false); // Reset fetch attempt flag
    }
  }, [fetchBorrowRequests, authReady, isAuthenticated, user, fetchAttempted]);

  // JSX structure updated to display both received and sent requests
  return (
    <TabsContent value="requests" className="space-y-6">
      {isLoading ? (
        <div className="flex justify-center items-center py-10">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
        </div>
      ) : error ? (
        <div className="text-center py-10 text-red-500">{error}</div>
      ) : (
        <>
          {/* Sent Requests Section - shown to all users */}
          <div>
            <h2 className="text-2xl font-bold">Sent Borrow Requests</h2>
            <div className="space-y-4 mt-4">
              {sentRequests.length === 0 ? (
                <div className="text-center py-10 text-muted-foreground">No sent borrow requests found.</div>
              ) : (
                sentRequests.map(request =>
                  <BorrowRequest
                    key={request.id}
                    id={request.id}
                    name={request.requestedGameName || "(Unknown Game)"}
                    requester={request.requesterName || user.name || "(Unknown Requester)"}
                    date={new Date(request.startDate || request.requestDate).toLocaleString()}
                    endDate={new Date(request.endDate).toLocaleString()}
                    status={request.status}
                    imageSrc={request.gameImage}
                    refreshRequests={fetchBorrowRequests}
                    gameId={request.requestedGameId}
                    requestedGameId={request.requestedGameId}
                    gameInstanceId={request.gameInstanceId}
                  />
                )
              )}
            </div>
          </div>

          {/* Received Requests Section - only shown to game owners */}
          {userType === 'owner' && (
            <div className="mt-8">
              <h2 className="text-2xl font-bold">Received Borrow Requests</h2>
              <div className="space-y-4 mt-4">
                {receivedRequests.length === 0 ? (
                  <div className="text-center py-10 text-muted-foreground">No received borrow requests found.</div>
                ) : (
                  receivedRequests.map(request =>
                    <BorrowRequest
                      key={request.id}
                      id={request.id}
                      name={request.requestedGameName || "(Unknown Game)"}
                      requester={request.requesterName || request.requesterId || "(Unknown Requester)"}
                      date={new Date(request.startDate || request.requestDate).toLocaleString()}
                      endDate={new Date(request.endDate).toLocaleString()}
                      status={request.status}
                      imageSrc={request.gameImage}
                      refreshRequests={fetchBorrowRequests}
                      gameId={request.requestedGameId}
                      requestedGameId={request.requestedGameId}
                      gameInstanceId={request.gameInstanceId}
                    />
                  )
                )}
              </div>
            </div>
          )}
        </>
      )}
    </TabsContent>
  );
}
