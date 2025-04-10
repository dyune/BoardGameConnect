import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { EventSearchBar } from "../components/events-page/EventSearchBar";
import { EventCard } from "../components/events-page/EventCard";
import { DateFilterComponent } from "../components/events-page/DateFilterComponent";
import CreateEventDialog from "../components/events-page/CreateEventDialog";
import { AnimatePresence, motion } from "framer-motion";
import { getAllEvents } from "../service/event-api";
import { getRegistrationsByEmail } from "../service/registration-api.js"; // Import registration fetcher
import { Loader2 } from "lucide-react";
import { useAuth } from "@/context/AuthContext"; // Import auth context


// Create card stagger animation variants (remains the same)
const container = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1
    }
  },
  exit: {
    opacity: 0,
    transition: {
      staggerChildren: 0.05,
      staggerDirection: -1,
      when: "afterChildren"
    }
  }
};

const item = {
  hidden: { opacity: 0, scale: 0.8, y: 20 },
  show: {
    opacity: 1,
    scale: 1,
    y: 0,
    transition: {
      type: "spring",
      stiffness: 300,
      damping: 24
    }
  },
  exit: {
    opacity: 0,
    scale: 0.8,
    y: -20,
    transition: {
      duration: 0.3,
      ease: "easeInOut"
    }
  }
};

export default function EventsPage() {
  const { user, isAuthenticated } = useAuth(); // Get user from auth context
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [allEvents, setAllEvents] = useState([]); // Store all fetched events
  const [filteredEvents, setFilteredEvents] = useState([]); // Store currently filtered events
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [userRegistrations, setUserRegistrations] = useState([]); // Store full registration objects
  const [isSearchActive, setIsSearchActive] = useState(false);
  const [displayedEvents, setDisplayedEvents] = useState([]);
  const [isSearchTransitioning, setIsSearchTransitioning] = useState(false);

  // Function to fetch events and registrations
  const fetchEvents = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      // Check if we have a user and user.email from auth context
      const userEmail = user?.email;
      
      // Fetch all events unconditionally - handle the possibility of an empty array
      let eventData = [];
      try {
        eventData = await getAllEvents();
        console.log(`[EventsPage] Retrieved ${eventData.length} events`);
      } catch (eventError) {
        console.error("Error fetching events:", eventError);
        setError("Failed to load events. Please try again later.");
        eventData = []; // Ensure we have an empty array
      }
      
      // Fetch registrations only if user is authenticated and has an email
      let registrationData = [];
      if (isAuthenticated && userEmail) {
        try {
          registrationData = await getRegistrationsByEmail(userEmail);
          console.log(`[EventsPage] Retrieved ${registrationData.length} registrations`);
        } catch (regError) {
          console.error("Failed to fetch registrations:", regError);
          // Don't fail the whole operation, just log the error and continue with empty registrations
          registrationData = [];
        }
      } else {
        if (!isAuthenticated) console.log("User is not authenticated");
        if (!userEmail) console.log("User email is not available");
      }

      // Ensure all state updates use arrays even if the API returns null/undefined
      setAllEvents(Array.isArray(eventData) ? eventData : []);
      setFilteredEvents(Array.isArray(eventData) ? eventData : []);
      setDisplayedEvents(Array.isArray(eventData) ? eventData : []);

      // Store the full registration data
      setUserRegistrations(Array.isArray(registrationData) ? registrationData : []);
    } catch (err) {
      console.error("Failed to fetch events or registrations:", err);
      
      // Set appropriate error message
      if (!isAuthenticated) {
        setError("Please log in to view and register for events");
      } else if (!user?.email) {
        setError("User email not found. Please log in again");
      } else {
        setError(err.message || "Could not load page data.");
      }
      
      // Ensure empty arrays
      setAllEvents([]);
      setFilteredEvents([]);
      setDisplayedEvents([]);
      setUserRegistrations([]);
    } finally {
      setIsLoading(false);
    }
  };

  // Fetch all events on component mount and when auth state changes
  useEffect(() => {
    fetchEvents();
  }, [isAuthenticated, user]); // Re-fetch when auth state changes

  // Update displayed events with animation delay (remains similar)
  useEffect(() => {
    if (!isSearchActive) {
      setDisplayedEvents(filteredEvents);
    }
  }, [filteredEvents, isSearchActive]);

  // Filter events by date range (now filters 'allEvents')
  const handleDateFilter = (filterValue) => {
    if (filterValue === "all") {
      setFilteredEvents(allEvents); // Reset to all fetched events
      return;
    }

    const now = new Date();
    const filtered = allEvents.filter(event => {
      const eventDate = event.dateTime ? new Date(event.dateTime) : null;
      if (!eventDate) return false;

      if (filterValue === "this-week") {
        const weekFromNow = new Date();
        weekFromNow.setDate(now.getDate() + 7);
        return eventDate >= now && eventDate <= weekFromNow;
      } else if (filterValue === "this-month") {
        const endOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0);
        return eventDate >= now && eventDate <= endOfMonth;
      } else if (filterValue === "next-month") {
        const startOfNextMonth = new Date(now.getFullYear(), now.getMonth() + 1, 1);
        const endOfNextMonth = new Date(now.getFullYear(), now.getMonth() + 2, 0);
        return eventDate >= startOfNextMonth && eventDate <= endOfNextMonth;
      }
      return false;
    });

    setFilteredEvents(filtered);
  };

  // Handle search activity state with transition
  const handleSearchStateChange = (isActive) => {
    if (isActive && !isSearchActive) {
      setIsSearchTransitioning(true);
      setTimeout(() => {
        setIsSearchActive(true);
        setIsSearchTransitioning(false);
      }, 400);
    } else if (!isActive) {
      setIsSearchActive(false);
    }
  };

   // Helper to adapt backend event DTO to what the child Event component expects
   const adaptEventData = (event) => {
    if (!event) return null;
    // Log the structure of host and featuredGame before accessing name
    return {
      id: event.eventId,
      title: event.title,
      dateTime: event.dateTime, // Pass raw date/time; formatting done in EventCard
      location: event.location || 'N/A',
      hostName: event.host?.name || 'Unknown Host', // Use hostName prop
      game: event.featuredGame?.name || 'Unknown Game', // Use game prop
      currentNumberParticipants: event.currentNumberParticipants,
      maxParticipants: event.maxParticipants,
      featuredGameImage: event.featuredGame?.image || "https://placehold.co/400x300/e9e9e9/1d1d1d?text=No+Image",
      participants: {
        current: event.currentNumberParticipants ?? 0,
        capacity: event.maxParticipants ?? 0,
      },
      description: event.description || '',
    };
 };


  return (
    <div className="bg-background text-foreground p-6">
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="text-4xl font-bold">Upcoming Events</h1>
          <h2 className="text-gray-600 mt-2">
            Join board game events in your area.
          </h2>
        </div>
        <Button
          className="bg-black hover:bg-gray-800 text-white"
          onClick={() => setCreateDialogOpen(true)}
        >
          Create Event
        </Button>
      </div>

      <div className="flex gap-4 mb-8">
        <div className="flex-grow">
        <EventSearchBar 
          onSearchStateChange={handleSearchStateChange} 
          adaptEventData={adaptEventData}
          userRegistrations={userRegistrations} // Pass registrations down
          />
        </div>
        <div className="w-48">
          <DateFilterComponent onFilterChange={handleDateFilter} />
        </div>
      </div>

      {/* Event Card Grid - Conditional Rendering based on loading/error/data */}
      {isLoading ? (
        <div className="flex justify-center items-center py-10">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : error ? (
        <div className="text-center py-10 text-destructive">
          <p>Error loading events: {error}</p>
        </div>
      ) : (
        <AnimatePresence mode="wait">
          {!isSearchActive && !isSearchTransitioning && displayedEvents.length > 0 ? (
            <motion.div
              className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
              key="grid"
              variants={container}
              initial="hidden"
              animate="show"
              exit="exit"
            >
              {displayedEvents.map((event, index) => {
                // Adapt event data for EventCard component
                const adaptedEvent = adaptEventData(event);
                if (!adaptedEvent) return null;

                // Find the registration ID for this specific event
                const registration = userRegistrations.find(reg => reg.eventId === adaptedEvent.id);
                const registrationId = registration ? registration.id : null;
                const isRegistered = !!registrationId;
                // console.log(`Event ID: ${adaptedEvent.id}, Registration ID: ${registrationId}, Is Registered: ${isRegistered}`); // Optional log

                return (
                  <motion.div
                    key={adaptedEvent.id} // Use unique eventId from backend
                    variants={item}
                    custom={index}
                    layout
                  >
                    {/* Pass adaptedEvent, refresh function, registration status, and registration ID */}
                    <EventCard
                       event={adaptedEvent}
                       onRegistrationUpdate={fetchEvents}
                       isCurrentUserRegistered={isRegistered} // Pass registration status
                       registrationId={registrationId} // Pass the specific ID for unregistering
                    />
                  </motion.div>
                );
              })}
            </motion.div>
          ) : !isSearchActive && !isSearchTransitioning && displayedEvents.length === 0 ? (
             <motion.div key="no-events" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="text-center py-10 text-muted-foreground">
               No events found. {/* Simplified message */}
             </motion.div>
          ) : null}
        </AnimatePresence>
      )}

      <CreateEventDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
        onEventAdded={fetchEvents} // Pass the fetch function as a prop
      />
    </div>
  );
}
