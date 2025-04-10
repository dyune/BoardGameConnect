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
import { unregisterFromEvent } from "../../service/registration-api"; // Update import to use registration-api instead

export default function EventRegistered({
  name,
  date,
  time,
  location,
  game,
  participants: { current, capacity },
  onCancelRegistration, // Keep this if used elsewhere
  onRegistrationUpdate, // For refreshing parent component
  registrationId, // This prop is now properly passed from DashboardEvents
  gameImage,
}) {
  const [open, setOpen] = useState(false)

  const handleCancelRegistration = async () => {
    try {
      // Log the registration ID being used for debugging
      console.log("EventRegistered: Attempting to unregister with ID:", {
        registrationId,
        typeOf: typeof registrationId,
        valueCheck: registrationId ? "Has value" : "No value",
      });
      
      // Validate registration ID before proceeding
      if (!registrationId) {
        console.error("EventRegistered: Registration ID is missing");
        alert("Cannot unregister: Registration ID not found.");
        return;
      }
      
      // Call the API function with the registrationId
      const successMessage = await unregisterFromEvent(registrationId);
      console.log("EventRegistered: Unregistration successful:", successMessage);
      
      // Call the refresh function passed from the parent dashboard page
      if (typeof onRegistrationUpdate === "function") {
        onRegistrationUpdate();
      }
      
      // Call the provided callback function (if it exists for other purposes)
      if (typeof onCancelRegistration === "function") {
        onCancelRegistration();
      }
    } catch (error) {
      console.error("EventRegistered: Error during unregistration:", error);
      alert(`Failed to unregister: ${error.message}`); // Notify the user
    }

    // Close the dialog
    setOpen(false)
  }

  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex flex-col md:flex-row gap-4">
          <div className="md:w-1/4">
            <div className="aspect-square bg-muted rounded-lg flex items-center justify-center">
              <img
                src={gameImage}
                alt={`${game} image`}
                className="h-full w-full object-cover rounded-lg"
              />
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
                    Cancel Registration
                  </Button>
                </DialogTrigger>
                <DialogContent className="sm:max-w-[425px]">
                  <DialogHeader>
                    <DialogTitle className="flex items-center gap-2">
                      <AlertCircle className="h-5 w-5 text-destructive" />
                      Cancel Registration
                    </DialogTitle>
                    <DialogDescription>
                      Are you sure you want to cancel your registration? 
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
                    </div>
                  </div>
                  <DialogFooter>
                    <Button variant="outline" onClick={() => setOpen(false)}>
                      Keep Registration
                    </Button>
                    <Button variant="destructive" onClick={handleCancelRegistration}>
                      Cancel Registration
                    </Button>
                  </DialogFooter>
                </DialogContent>
              </Dialog>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}