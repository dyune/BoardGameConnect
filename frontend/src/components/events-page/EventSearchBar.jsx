import { useState, useEffect, useCallback, useRef } from "react";
import { Input } from "../../ui/input";
import { Search, CalendarX, RefreshCw } from "lucide-react";
import { searchEventsByTitle } from "../../service/event-api.js";
import { EventCard } from "./EventCard";
import { motion, AnimatePresence } from "framer-motion";
// Removed: import { getRegistrationsByEmail } from "../../service/registration-api.js";
import { useAuth } from "@/context/AuthContext"; // Import useAuth (still needed for user email if we decide to re-add refresh)

// Animation variants for search results
const resultsContainer = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1,
      delayChildren: 0.3 // Delay to allow previous content to fully exit
    }
  }
};

const resultItem = {
  hidden: { opacity: 0, y: 20 },
  show: { 
    opacity: 1, 
    y: 0,
    transition: { 
      type: "spring",
      stiffness: 300,
      damping: 24
    }
  }
};

export function EventSearchBar({ onSearchStateChange, userRegistrations }) { // Accept userRegistrations prop
  const [searchTerm, setSearchTerm] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [prevSearchTerm, setPrevSearchTerm] = useState("");
  // Removed: const [userRegistrations, setUserRegistrations] = useState([]);
  const { user } = useAuth(); // Keep user for potential future use (e.g., refresh logic)
  
  // Track if component is mounted to prevent state updates after unmount
  const isMountedRef = useRef(true);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  // Removed: fetchUserRegistrations function

  useEffect(() => {
    const delayDebounceFn = setTimeout(() => {
      if (searchTerm.trim() !== "") {
        // Only trigger search state change if we have a new search term
        if (searchTerm !== prevSearchTerm) {
          onSearchStateChange && onSearchStateChange(true);
          setPrevSearchTerm(searchTerm);
        }
        handleSearch();
      } else {
        setSearchResults([]);
        setHasSearched(false);
        onSearchStateChange && onSearchStateChange(false);
        setPrevSearchTerm("");
      }
    }, 300);

    return () => clearTimeout(delayDebounceFn);
  }, [searchTerm, onSearchStateChange, prevSearchTerm]);

  // Helper to adapt backend event DTO to what the child Event component expects
  const adaptEventData = useCallback((event) => {
    if (!event) return null;
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
  }, []);

  const handleSearch = useCallback(async () => {
    if (!isMountedRef.current) return;
    
    setIsLoading(true);
    try {
      // Fetch only search results
      const results = await searchEventsByTitle(searchTerm);
      
      if (isMountedRef.current) {
        const adaptedResults = results.map(event => adaptEventData(event));
        setSearchResults(adaptedResults);
        setHasSearched(true);
      }
    } catch (error) {
      console.error("Search error:", error);
      if (isMountedRef.current) {
        setSearchResults([]);
        setHasSearched(true);
      }
    } finally {
      if (isMountedRef.current) {
        setIsLoading(false);
      }
    }
  }, [searchTerm, adaptEventData]); // Removed fetchUserRegistrations dependency

  // Function to refresh data after registration action
  // Note: This currently only re-fetches search results.
  // If the parent's registration data needs refreshing, a callback prop from EventsPage would be needed.
  const handleRegistrationUpdate = useCallback(async () => {
    if (!isMountedRef.current) return;
    // Re-fetch search results only
    try {
      const results = await searchEventsByTitle(searchTerm);
      
      if (isMountedRef.current) {
        const adaptedResults = results.map(event => adaptEventData(event));
        setSearchResults(adaptedResults);
      }
    } catch (error) {
      console.error("Error refreshing search results:", error);
    }
  }, [searchTerm, adaptEventData]); // Removed fetchUserRegistrations dependency

  return (
    <div className="w-full">
      {/* Search Input */}
      <div className="relative max-w mb-6">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          type="search"
          placeholder="Search events..."
          className="w-full pl-10"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </div>

      {/* Search Results with AnimatePresence for smooth transitions */}
      <AnimatePresence mode="wait">
        {isLoading ? (
          <motion.div 
            className="py-12 text-center"
            key="loading"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <RefreshCw className="h-12 w-12 mx-auto mb-4 text-gray-400 animate-spin" />
            <p className="text-muted-foreground text-lg">Searching for events...</p>
          </motion.div>
        ) : searchTerm && hasSearched && searchResults.length === 0 ? (
          <motion.div 
            className="py-12 text-center"
            key="no-results"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            transition={{ type: "spring", stiffness: 300, damping: 25 }}
          >
            <div className="flex flex-col items-center justify-center">
              <div className="relative">
                <div className="absolute inset-0 bg-gray-100 rounded-full scale-125 opacity-30 animate-pulse"></div>
                <CalendarX className="h-16 w-16 mb-4 text-gray-400 relative z-10 animate-bounce" />
              </div>
              <h3 className="text-xl font-semibold mb-2">No events found</h3>
              <p className="text-muted-foreground mb-4 max-w-md">
                We couldn't find any events matching "{searchTerm}". Try a different search term or create your own event!
              </p>
            </div>
          </motion.div>
        ) : searchTerm && searchResults.length > 0 ? (
          <motion.div 
            className="space-y-4"
            key="results"
            variants={resultsContainer}
            initial="hidden"
            animate="show"
          >
            {searchResults.map((event, index) => {
              // Find registration for this specific event
              const registration = userRegistrations.find(reg => reg.eventId === event.id);
              const registrationId = registration ? registration.id : null;
              const isRegistered = !!registrationId;
              
              return (
                <motion.div key={event.id || index} variants={resultItem}>
                  <EventCard 
                    event={event}
                    onRegistrationUpdate={handleRegistrationUpdate}
                    isCurrentUserRegistered={isRegistered}
                    registrationId={registrationId}
                  />
                </motion.div>
              );
            })}
          </motion.div>
        ) : null}
      </AnimatePresence>
    </div>
  );
}