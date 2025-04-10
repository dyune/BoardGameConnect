import { useState, useEffect, useCallback } from "react";
import { TabsContent } from "@/components/ui/tabs.jsx";
import { Button } from "@/components/ui/button.jsx";
// Imports kept from HEAD
import { EventCard } from "../events-page/EventCard.jsx"; // Use EventCard for consistency
import Event from "./Event.jsx"; // Import the Event component
import CreateEventDialog from "../events-page/CreateEventDialog.jsx"; // Import dialog
import { getEventsByHostEmail } from "../../service/event-api.js";
import { getRegistrationsByEmail } from "../../service/registration-api.js"; // Import attended events fetcher
import { UnauthorizedError } from "@/service/apiClient"; // Import UnauthorizedError
import { useAuth } from "@/context/AuthContext"; // Import useAuth
import { Loader2 } from "lucide-react"; // Import loader
import { formatDateTimeForDisplay } from '@/lib/dateUtils.js'; // Import the new utility

export default function DashboardEvents({ userType }) {
  const [hostedEvents, setHostedEvents] = useState([]);
  const [attendedRegistrations, setAttendedRegistrations] = useState([]); // Store full registration objects
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const { user, isAuthenticated, authReady } = useAuth(); // Get auth context with authReady
  const [apiCallAttempted, setApiCallAttempted] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0); // Simpler refresh key

  // Function to fetch both hosted and attended events - memoized to prevent infinite loops
  const fetchDashboardEvents = useCallback(async () => {
    // Don't try to fetch if we're not authenticated or auth isn't ready
    if (!user?.email || !isAuthenticated || !authReady) {
      if (!isLoading) return; // Don't update state if not loading
      setIsLoading(false);
      return;
    }

    // Don't refetch if we already tried and no auth state has changed
    if (apiCallAttempted && !isLoading) return;
    
    setIsLoading(true);
    setError(null);
    setApiCallAttempted(true);
    const userEmail = user?.email;

    if (!userEmail) {
      setError("User email not found. Please log in again.");
      setIsLoading(false);
      return;
    }

    try {
      // Fetch hosted events only if the user is an owner
      let hosted = [];
      if (userType === "owner") {
        try {
          console.log(`[DashboardEvents] Fetching events hosted by ${userEmail}`);
          hosted = await getEventsByHostEmail(userEmail);
          console.log(`[DashboardEvents] Retrieved ${hosted.length} hosted events`);
          
          // Ensure hosted is an array
          hosted = Array.isArray(hosted) ? hosted : [];
        } catch (hostedError) {
          console.error(`[DashboardEvents] Error fetching hosted events:`, hostedError);
          hosted = []; // Ensure hosted is an empty array on error
        }
      }
      // Update hosted events state
      setHostedEvents(hosted);

      // Fetch registrations (attended events)
      let registrations = [];
      try {
        console.log(`[DashboardEvents] Fetching registrations for ${userEmail}`);
        const response = await getRegistrationsByEmail(userEmail);
        // Ensure registrations is an array
        registrations = Array.isArray(response) ? response : [];
        console.log(`[DashboardEvents] Retrieved ${registrations.length} registrations`);
      } catch (regError) {
        console.error(`[DashboardEvents] Error fetching registrations:`, regError);
        registrations = []; // Ensure registrations is an empty array on error
      }

      // Store the full registration objects, filtering out any potentially invalid ones
      setAttendedRegistrations(registrations.filter(reg => reg && reg.event) || []);
    } catch (err) {
      // This will catch any other errors that might occur
      if (err instanceof UnauthorizedError) {
        console.error("[DashboardEvents] Unauthorized error:", err);
        setError("Authentication error. Please try logging in again.");
      } else {
        console.error("[DashboardEvents] Failed to fetch dashboard events data:", err);
        setError(err.message || "Could not load events.");
      }
      // Ensure empty arrays on error
      setHostedEvents([]);
      setAttendedRegistrations([]);
    } finally {
      setIsLoading(false);
    }
  }, [userType, user, isAuthenticated, authReady, isLoading, apiCallAttempted]);

  // Reset API call attempted when auth state changes
  useEffect(() => {
    if (authReady && isAuthenticated && user?.email) {
      setApiCallAttempted(false);
    }
  }, [authReady, isAuthenticated, user]);

  // Fetch events on component mount and when userType or user changes
  useEffect(() => {
    // Add a small delay to ensure auth state is fully updated
    const timer = setTimeout(() => {
      if (authReady && isAuthenticated && user?.email) {
        fetchDashboardEvents();
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [userType, user, fetchDashboardEvents, authReady, isAuthenticated, refreshKey]);

  // Simple refresh function that won't cause infinite loops
  const handleRefresh = useCallback(() => {
    console.log("[DashboardEvents] Manual refresh triggered");
    setApiCallAttempted(false); // Reset API call attempt flag
    setRefreshKey(prev => prev + 1); // Increment refresh key to trigger useEffect
  }, []);

  // Function to handle event creation success (passed to dialog)
  const handleEventAdded = useCallback(() => {
    handleRefresh(); // Use our new refresh function
  }, [handleRefresh]);

  /**
   * Format event data for consistent use in EventCard
   */
  const adaptEventData = (event, registrationId = null) => {
    if (!event) return null;
    
    const {
      id,
      title,
      dateTime, 
      location, 
      currentNumberParticipants, 
      maxParticipants,
      description,
      host
    } = event;

    // Get host information
    const hostName = host?.name || event.hostName || "Unknown Host";
    const hostEmail = host?.email || event.hostEmail || null;
    const hostId = host?.id || event.hostId || null;

    // Extract featured game image with proper fallbacks
    // The backend Game model uses 'image' field, but might be transformed differently in DTOs
    const featuredGameImage = 
      event.featuredGame?.image || // From EventResponse.GameDto.image
      event.featuredGame?.gameImage || // Alternative DTO structure
      event.gameImage || // Flattened structure
      null;

    return {
      id: id || event.eventId, // Ensure there's an ID
      title: title || event.title || event.name || "Untitled Event",
      dateTime: dateTime || event.dateTime,
      location: location || event.location,
      // Support different ways events might store game data
      game: event.featuredGame?.name || event.game || event.gameName || "Unknown Game",
      featuredGameImage: featuredGameImage,
      // Set host data with fallbacks
      host: host || { name: hostName, email: hostEmail, id: hostId },
      hostName: hostName,
      hostEmail: hostEmail,
      hostId: hostId,
      // Normalize participant counting
      participants: {
        current: currentNumberParticipants,
        capacity: maxParticipants,
      },
      description: description || event.description || '',
      registrationId: registrationId, // Add registrationId if provided (from origin logic)
    };
  };

  // formatDateAndTime function is no longer needed here

  return (
    <>
      <TabsContent value="events" className="space-y-6">
        <div className="flex justify-between items-center">
          <h2 className="text-2xl font-bold">My Events</h2>
          {/* Only show Create Event button if user is owner (checked via prop) */}
          {userType === "owner" && (
             <Button onClick={() => setIsCreateDialogOpen(true)}>Create Event</Button>
          )}
        </div>

        {isLoading ? (
          <div className="flex justify-center items-center py-10">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        ) : error ? (
          <div className="text-center py-10 text-destructive">
            <p>Error loading events: {error}</p>
          </div>
        ) : (
          <div className="space-y-8">
            {/* Events Hosting Section (only if owner - checked via prop and auth context) */}
            {userType === "owner" && (
              <div>
                <h3 className="text-xl font-semibold mb-4">Hosting</h3>
                 {hostedEvents.length > 0 ? (
                   <div className="space-y-4">
                     {hostedEvents.map((event, index) => {
                        const adapted = adaptEventData(event); // Adapt hosted event
                        if (!adapted) return null;
                        
                        // Use the utility function here before passing to Event component
                        const { date, time } = formatDateTimeForDisplay(adapted.dateTime);
                        
                        // Use the Event component for hosted events
                        return (
                          <Event
                            key={`hosted-${adapted.id || index}`}
                            id={adapted.id}
                            name={adapted.title}
                            date={date}
                            time={time}
                            location={adapted.location}
                            game={adapted.game}
                            participants={adapted.participants}
                            onCancelRegistration={handleRefresh} // Pass our simple refresh function
                            onRegistrationUpdate={handleRefresh} // For consistency, use the same function
                            gameImage={adapted.featuredGameImage}
                          />
                        );
                     })}
                   </div>
                ) : (
                  <p className="text-muted-foreground">You are not hosting any events.</p>
                )}
              </div>
            )}

            {/* Events Attending Section - Using HEAD's logic */}
            <div>
              <h3 className="text-xl font-semibold mb-4">Attending</h3>
                 {attendedRegistrations.length > 0 ? (
                   <div className="space-y-4">
                     {attendedRegistrations.map((registration, index) => {
                       const event = registration.event; // Get the event object from registration
                       const registrationId = registration.id; // Get the registration ID
                       // Adapt the event data, passing the registrationId
                       const adaptedEvent = adaptEventData(event, registrationId);
                       if (!adaptedEvent) return null; // Skip if event data is invalid

                       // Render EventCard for attended events
                       return (
                         <EventCard
                           key={`attended-${adaptedEvent.id || index}`}
                           event={adaptedEvent}
                           onRegistrationUpdate={handleRefresh} // Use our simple refresh function
                           isCurrentUserRegistered={true} // Always true for this list
                           registrationId={registrationId} // Pass the specific registration ID
                         />
                       );
                     })}
                   </div>
              ) : (
                <p className="text-muted-foreground">You are not attending any events.</p>
              )}
            </div>
          </div>
        )}
      </TabsContent>

      {/* Render the Create Event Dialog */}
      <CreateEventDialog
        open={isCreateDialogOpen}
        onOpenChange={setIsCreateDialogOpen}
        onEventAdded={handleEventAdded} // Pass the refresh function
      />
    </>
  );
}