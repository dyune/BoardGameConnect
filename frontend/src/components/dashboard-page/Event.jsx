"use client"

import { useState } from "react"
import { Card, CardContent } from "@/components/ui/card"
import { Calendar, AlertCircle } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { deleteEvent } from "../../service/event-api" // Import deleteEvent function
import UpdateEventDialog from "../events-page/UpdateEventDialog.jsx" // Import UpdateEventDialog

export default function Event({
  id, // Make sure we get the event ID
  name,
  date,
  time,
  location,
  game,
  participants: { current, capacity },
  onCancelRegistration: onCancelEvent, // Keep this if used elsewhere
  onRegistrationUpdate, // Add the refresh prop
  gameImage,
}) {
  const [open, setOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false) // Track deletion state
  const [error, setError] = useState(null) // Track error state
  const [isUpdateDialogOpen, setIsUpdateDialogOpen] = useState(false) // For update dialog

  const handleCancelEvent = async () => {
    if (!id) {
      setError("Event ID is missing. Cannot delete event.");
      return;
    }

    setIsDeleting(true);
    setError(null);

    try {
      // Call the API function to delete the event
      console.log("Deleting event with ID:", id);
      const successMessage = await deleteEvent(id);
      console.log("Event deleted successfully:", successMessage);
      
      // Call the provided callback function (if it exists for other purposes)
      if (typeof onCancelEvent === "function") {
        onCancelEvent();
      }
      
      // Call the refresh function passed from the parent dashboard page
      if (typeof onRegistrationUpdate === "function") {
        onRegistrationUpdate();
      }

      // Close the dialog
      setOpen(false);
    } catch (error) {
      console.error("Failed to delete event:", error);
      setError(error.message || "Failed to delete event. Please try again.");
    } finally {
      setIsDeleting(false);
    }
  }

  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex flex-col md:flex-row gap-4">
          <div className="md:w-1/4">
            <div className="aspect-square bg-muted rounded-lg flex items-center justify-center overflow-hidden">
              {gameImage ? (
                <img
                  src={gameImage}
                  alt={`${game} image`}
                  className="h-full w-full object-cover rounded-lg"
                  onError={(e) => {
                    e.target.onerror = null;
                    e.target.src = "https://placehold.co/400x400?text=Game";
                  }}
                />
              ) : (
                <div className="h-full w-full flex items-center justify-center bg-gray-100">
                  <span className="text-sm text-gray-500">No image available</span>
                </div>
              )}
            </div>
          </div>
          <div className="flex-1">
            <h3 className="text-xl font-semibold">{name}</h3>
            <div className="grid gap-1 mt-2">
              <div className="text-sm">
                <span className="font-medium">Date:</span> {date}
              </div>
              <div className="text-sm">
                <span className="font-medium">Time:</span> {time}
              </div>
              <div className="text-sm">
                <span className="font-medium">Location:</span> {location}
              </div>
              <div className="text-sm">
                <span className="font-medium">Game:</span> {game}
              </div>
              <div className="text-sm">
                <span className="font-medium">Participants:</span> {current}/{capacity}
              </div>
            </div>
            <div className="flex gap-2 mt-4">
              <Dialog open={open} onOpenChange={setOpen}>
                <DialogTrigger asChild>
                  <Button variant="destructive" size="sm">
                    Cancel Event
                  </Button>
                </DialogTrigger>
                <DialogContent className="sm:max-w-[425px]">
                  <DialogHeader>
                    <DialogTitle className="flex items-center gap-2">
                      <AlertCircle className="h-5 w-5 text-destructive" />
                      Cancel Event
                    </DialogTitle>
                    <DialogDescription>
                      Are you sure you want to cancel your event? 
                      This will notify all registered participants and remove the event from the system.
                    </DialogDescription>
                  </DialogHeader>
                  <div className="py-4">
                    <div className="rounded-lg bg-muted p-4 text-sm">
                      <p>
                        <span className="font-medium">Event:</span> {name}
                      </p>
                      <p>
                        <span className="font-medium">Date:</span> {date}
                      </p>
                      <p>
                        <span className="font-medium">Time:</span> {time}
                      </p>
                      <p>
                        <span className="font-medium">Location:</span> {location}
                      </p>
                      <p>
                        <span className="font-medium">Participants:</span> {current}/{capacity}
                      </p>
                    </div>
                    
                    {/* Error message display */}
                    {error && (
                      <div className="mt-4 p-2 bg-red-50 text-red-600 rounded-md text-sm">
                        {error}
                      </div>
                    )}
                  </div>
                  <DialogFooter>
                    <Button variant="outline" onClick={() => setOpen(false)} disabled={isDeleting}>
                      Keep Event
                    </Button>
                    <Button 
                      variant="destructive" 
                      onClick={handleCancelEvent} 
                      disabled={isDeleting}
                    >
                      {isDeleting ? "Cancelling..." : "Cancel Event"}
                    </Button>
                  </DialogFooter>
                </DialogContent>
              </Dialog>

              {/* Modify Event Button */}
              <Button 
                variant="outline" 
                size="sm" 
                onClick={() => setIsUpdateDialogOpen(true)}
              >
                Modify Event
              </Button>
            </div>
          </div>
        </div>
      </CardContent>

      {/* UpdateEventDialog Component */}
      <UpdateEventDialog
        open={isUpdateDialogOpen}
        onOpenChange={setIsUpdateDialogOpen}
        onEventUpdated={onRegistrationUpdate}
        eventId={id}
      />
    </Card>
  )
}