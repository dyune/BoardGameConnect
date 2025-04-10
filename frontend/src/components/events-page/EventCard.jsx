import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import confetti from "canvas-confetti";
import { registerForEvent } from "@/service/event-api.js"; // Keep registerForEvent here
import { unregisterFromEvent } from "@/service/registration-api.js"; // Import unregister from the correct file
import { motion, AnimatePresence } from "framer-motion";
import { useAuth } from "@/context/AuthContext"; // Import Auth context
import {
  Card,
  CardContent
} from "../../ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Calendar, AlertCircle, Users, MapPin, Gamepad2, Info } from "lucide-react";
import { cn } from '@/lib/utils'; // Corrected path assuming utils is in lib
import { toast } from "sonner";
import { formatDateTimeForDisplay } from '@/lib/dateUtils.js'; // Import the new utility

// Accept onRegistrationUpdate, isCurrentUserRegistered, registrationId, and hideRegisterButtons props
export function EventCard({ event, onRegistrationUpdate, isCurrentUserRegistered, registrationId, hideRegisterButtons = false }) {
  // Initial setup and state
  const [isRegistered, setIsRegistered] = useState(isCurrentUserRegistered || false);
  const [isAnimating, setIsAnimating] = useState(false);
  const [error, setError] = useState(null);
  const [showDescription, setShowDescription] = useState(false);
  const [isCancelConfirmOpen, setIsCancelConfirmOpen] = useState(false);
  const { user } = useAuth(); // Get current user from auth context

  // Check if current user is the host of this event
  const isUserEventHost = user && 
    ((event.host && event.host.email && event.host.email === user.email) || 
     (event.hostEmail && event.hostEmail === user.email) ||
     (event.hostId && user.id && event.hostId === user.id) ||
     (event.hostName && user.name && event.hostName === user.name));

  // Update isRegistered state if the prop changes (e.g., after parent refresh)
  useEffect(() => {
    // If a registrationId is provided, the user is definitely registered
    const isDefinitelyRegistered = !!registrationId || isCurrentUserRegistered;
    setIsRegistered(isDefinitelyRegistered);
    
    // Debug registration state
    console.log("[EventCard] Registration state updated:", {
      eventId: event.id,
      eventTitle: event.title,
      isCurrentUserRegistered,
      registrationId,
      isDefinitelyRegistered,
      isUserEventHost // Log if user is host
    });
  }, [isCurrentUserRegistered, registrationId, event.id, event.title, isUserEventHost]);

  const handleRegisterClick = async (e) => {
    setError(null);

    // If already registered, show cancel confirmation
    if (isRegistered) {
      setIsCancelConfirmOpen(true);
      return;
    }

    // Register logic
    try {
      setIsAnimating(true);
      const { clientX: x, clientY: y } = e;

      // Get event ID from all possible properties, with fallbacks
      const eventId = event.id || event.eventId;
      console.log(`[EventCard] Attempting to register for event:`, { 
        eventId, 
        event: {
          id: event.id,
          eventId: event.eventId,
          title: event.title
        }
      });
      
      if (!eventId) {
        console.error("[EventCard] Missing event ID:", event);
        throw new Error("Cannot register: Event ID is missing");
      }

      await registerForEvent(eventId); // Pass event ID

      // If successful:
      setIsRegistered(true); // Update button state locally first
      setIsAnimating(false);
      toast.success(`Successfully registered for ${event.title || event.name}!`);
      if (onRegistrationUpdate) { // Call the refresh function from parent
        onRegistrationUpdate(); // This will cause props to update
      }

      // Trigger confetti
      confetti({
        particleCount: 100,
        spread: 70,
        origin: { x: x / window.innerWidth, y: y / window.innerHeight },
      });
    } catch (error) {
      setIsAnimating(false);
      
      // Check if this is the "already registered" error
      if (error.message && (
          error.message.includes("already exists") || 
          error.message.includes("already registered")
      )) {
        // User is already registered for this event
        setError("You are already registered for this event");
        setIsRegistered(true); // Update the UI state to show registered
        toast.warning("You are already registered for this event");
        
        // Refresh to get the registration data
        if (onRegistrationUpdate) {
          setTimeout(() => {
            onRegistrationUpdate();
          }, 1000);
        }
      } else if (error.message && error.message.includes("full capacity")) {
        // Event is at full capacity
        setError("⚠️ Event is at full capacity!");
        toast.error("Event is at full capacity!");
      } else if (error.message && error.message.includes("cannot register for your own event")) {
        // User trying to register for their own event
        setError("You cannot register for your own event.");
        toast.error("You cannot register for your own event.");
      } else {
        // Generic error - extract the 'detail' message if it exists
        let errorMsg = error.message || "Something went wrong. Please try again.";
        
        // Try to extract the detail message from JSON error response
        if (errorMsg.includes("detail")) {
          try {
            const jsonStart = errorMsg.indexOf("{");
            if (jsonStart !== -1) {
              const jsonPart = errorMsg.substring(jsonStart);
              const errorObj = JSON.parse(jsonPart);
              if (errorObj.detail) {
                errorMsg = errorObj.detail;
              }
            }
          } catch (e) {
            // If JSON parsing fails, keep the original message
            console.log("Error parsing error message JSON:", e);
          }
        }
        
        setError(errorMsg);
        toast.error(errorMsg);
      }
    }
  };
  
  const handleConfirmCancelRegistration = async () => {
     setError(null);
     try {
       // Log detailed registration ID info for debugging
       console.log("[EventCard] Registration ID when unregistering:", {
         regId: registrationId, 
         type: typeof registrationId,
         eventId: event.id,
         eventTitle: event.title
       });
       
       // Check if we actually have a registration ID to delete
       if (!registrationId) {
         throw new Error("Cannot unregister: Registration ID not found.");
       }
       setIsAnimating(true);
       // Call the actual unregister function with the correct registration ID
       await unregisterFromEvent(registrationId);

       // If successful:
       setIsRegistered(false); // Update button state locally first
       setIsAnimating(false);
       setIsCancelConfirmOpen(false);
       toast.info(`Unregistered from ${event.title || event.name}.`);
       if (onRegistrationUpdate) { // Call the refresh function from parent
         // Call onRegistrationUpdate immediately without delay
         onRegistrationUpdate();
       }
     } catch (error) {
       setIsAnimating(false);
       const errorMsg = error.message || "Something went wrong. Please try again.";
       setError(errorMsg);
       toast.error(errorMsg);
       setIsCancelConfirmOpen(false);
     }
  };

  const toggleDescription = () => {
    setShowDescription(!showDescription);
  };

  // Use the utility function for formatting
  const { date: formattedDate, time: formattedTime } = formatDateTimeForDisplay(event.dateTime);

  return (
    <motion.div
      className="rounded-lg overflow-hidden bg-card text-card-foreground shadow flex flex-col h-full border border-border"
      whileHover={{ y: -5, boxShadow: "0 10px 25px rgba(0,0,0,0.1)" }}
      transition={{ type: "spring", stiffness: 300, damping: 20 }}
    >
      {/* Event Image */}
      <div className="w-full h-48 bg-muted relative flex-shrink-0">
        {event.featuredGameImage && event.featuredGameImage !== "https://placehold.co/400x300/e9e9e9/1d1d1d?text=No+Image" ? (
          <img
            src={event.featuredGameImage}
            alt={event.featuredGame?.name || "Featured Game"}
            className="w-full h-full object-cover object-center"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center bg-muted">
             <span className="text-muted-foreground">No Image</span>
          </div>
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-black/70 to-transparent"></div>
        <div className="absolute bottom-0 left-0 p-4 w-full">
          <h3 className="font-bold text-xl text-white drop-shadow-lg shadow-black line-clamp-2">
            {event.title || event.name || 'Untitled Event'}
          </h3>
          <div className="flex items-center text-white mt-1 drop-shadow-md">
            <span className="font-medium text-xs">{formattedDate}</span>
          </div>
        </div>
      </div>

      {/* Event Details */}
      <div className="p-4 flex flex-col flex-grow">
        {/* Host Info */}
        <div className="flex items-center text-foreground mb-2 text-sm">
          <Users className="w-4 h-4 mr-2 flex-shrink-0" />
          <span className="font-medium truncate">
            Hosted by: {event.hostName || "Unknown Host"}
          </span>
        </div>

        {/* Game Info */}
        <div className="flex items-center text-foreground mb-2 text-sm">
          <Gamepad2 className="w-4 h-4 mr-2 flex-shrink-0" />
          <span className="truncate">
            Featured Game: {event.game || "Unknown Game"}
          </span>
        </div>

        {/* Location Info */}
        <div className="flex items-center text-foreground mb-2 text-sm">
          <MapPin className="w-4 h-4 mr-2 flex-shrink-0" />
          <span className="truncate">
            Location: {event.location || "Not specified"}
          </span>
        </div>

        {/* Date and Time */}
        <div className="flex items-center text-foreground mb-2 text-sm">
          <Calendar className="w-4 h-4 mr-2 flex-shrink-0" />
          <span>{formattedDate} - {formattedTime}</span>
        </div>

        {/* Participants - Display directly from prop */}
        <div className="flex items-center text-foreground mb-4 text-sm">
          <Users className="w-4 h-4 mr-2 flex-shrink-0" />
          <span>
            {/* Try multiple fallback options for participant data */}
            {event.participants && typeof event.participants.current === 'number' 
              ? `${event.participants.current}/${event.participants.capacity} participants`
              : typeof event.currentNumberParticipants === 'number' && typeof event.maxParticipants === 'number'
                ? `${event.currentNumberParticipants}/${event.maxParticipants} participants` 
                : "Participants: N/A"}
          </span>
        </div>

        {/* Description Expandable Section */}
        <AnimatePresence>
          {showDescription && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: "auto", opacity: 1, marginBottom: 8 }}
              exit={{ height: 0, opacity: 0, marginBottom: 0 }}
              transition={{ duration: 0.3 }}
              className="overflow-hidden"
            >
              <Card className="bg-muted/50 border-border/50">
                <CardContent className="pt-3 pb-3 px-4">
                  <h4 className="font-semibold mb-1 text-sm flex items-center text-foreground">
                    <Info className="w-4 h-4 mr-1.5"/>Description
                  </h4>
                  <p className="text-muted-foreground text-sm">
                    {event.description || "No description available."}
                  </p>
                </CardContent>
              </Card>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Spacer to push buttons down */}
        <div className="flex-grow"></div>

        {/* Error Message */}
        {error && (
          <motion.div
            className="text-destructive text-sm mb-2 text-center"
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
          >
            {error}
          </motion.div>
        )}

        {/* Action Buttons */}
        <div className="space-y-2 mt-2">
            <Button
              variant="outline"
              className="w-full text-sm"
              onClick={toggleDescription}
            >
              {showDescription ? "Hide Details" : "Show Details"}
            </Button>
            {/* Only show register button if user is not the host AND hideRegisterButtons is false */}
            {!isUserEventHost && !hideRegisterButtons ? (
              <Button
                className={`w-full text-primary-foreground transition-all duration-300 text-sm ${
                  isRegistered
                    ? "bg-destructive hover:bg-destructive/90"
                    : "bg-primary hover:bg-primary/90"
                } ${isAnimating ? "scale-95" : "scale-100"}`}
                onClick={handleRegisterClick}
                disabled={isAnimating}
              >
                {isRegistered ? "Unregister" : "Register"}
              </Button>
            ) : isUserEventHost ? (
              <Button
                className="w-full text-muted-foreground bg-muted hover:bg-muted cursor-not-allowed"
                disabled={true}
              >
                You are hosting this event
              </Button>
            ) : null}
        </div>

         {/* Unregister Confirmation Dialog */}
         <Dialog open={isCancelConfirmOpen} onOpenChange={setIsCancelConfirmOpen}>
            <DialogContent className="sm:max-w-[425px]">
              <DialogHeader>
                <DialogTitle className="flex items-center gap-2">
                  <AlertCircle className="h-5 w-5 text-destructive" />
                  Cancel Registration
                </DialogTitle>
                <DialogDescription>
                  Are you sure you want to cancel your registration for "{event.title || event.name}"? 
                </DialogDescription>
              </DialogHeader>
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsCancelConfirmOpen(false)}>
                  Keep Registration
                </Button>
                <Button variant="destructive" onClick={handleConfirmCancelRegistration} disabled={isAnimating}>
                  {isAnimating ? "Cancelling..." : "Cancel Registration"}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>

      </div>
    </motion.div>
  );
}