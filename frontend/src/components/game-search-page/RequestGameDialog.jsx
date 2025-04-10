import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '../ui/dialog';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '../ui/form';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Alert, AlertDescription } from '../ui/alert';
import { AlertCircle } from 'lucide-react';
import { useForm, useWatch } from "react-hook-form";
import { createBorrowRequest } from '../../service/borrow_request-api.js';
import { checkGameAvailability } from '../../service/game-api.js';
import { toast } from 'sonner';
import { useAuth } from '@/context/AuthContext';

export const RequestGameDialog = ({ open, onOpenChange, onSubmit, game, gameInstance }) => {
  const { user } = useAuth();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isCheckingAvailability, setIsCheckingAvailability] = useState(false);
  const [isAvailable, setIsAvailable] = useState(null);

  // Format dates to YYYY-MM-DD for the date input
  const formatDateForInput = (date) => {
    if (!date) return '';
    return new Date(date).toISOString().split('T')[0];
  };
  
  // Format time to HH:MM for the time input
  const formatTimeForInput = (date) => {
    if (!date) return '';
    const d = new Date(date);
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  };
  
  // Get initial date/time from pre-selected dates if available
  const initialDate = gameInstance?.requestStartDate ? formatDateForInput(gameInstance.requestStartDate) : '';
  const initialTime = gameInstance?.requestStartDate ? formatTimeForInput(gameInstance.requestStartDate) : '';
  
  // Calculate initial duration in hours if both start and end dates are provided
  const calculateInitialDuration = () => {
    if (gameInstance?.requestStartDate && gameInstance?.requestEndDate) {
      const start = new Date(gameInstance.requestStartDate);
      const end = new Date(gameInstance.requestEndDate);
      const durationHours = (end - start) / (1000 * 60 * 60);
      return durationHours.toString();
    }
    return '1';
  };

  const form = useForm({
    defaultValues: {
      date: initialDate,
      time: initialTime,
      duration: calculateInitialDuration(),
    }
  });

  // Get today's date in YYYY-MM-DD format
  const today = new Date().toISOString().split('T')[0];
  
  // Watch date, time, and duration fields for changes to check availability
  const date = useWatch({ control: form.control, name: "date" });
  const time = useWatch({ control: form.control, name: "time" });
  const duration = useWatch({ control: form.control, name: "duration" });
  
  // Check availability when date or time changes
  useEffect(() => {
    const checkAvailability = async () => {
      // Only check if we have all required data
      if (!game?.id || !date || !time || !duration) {
        setIsAvailable(null);
        return;
      }
      
      try {
        setIsCheckingAvailability(true);
        
        const startDateTime = new Date(`${date}T${time}`);
        const endDateTime = new Date(startDateTime);
        endDateTime.setHours(endDateTime.getHours() + parseFloat(duration));
        
        if (isNaN(startDateTime.getTime()) || isNaN(endDateTime.getTime())) {
          setIsAvailable(null);
          return;
        }
        
        // Check availability for the selected dates
        const available = await checkGameAvailability(game.id, startDateTime, endDateTime);
        setIsAvailable(available);
        
        // If the selected gameInstance is marked as unavailable, show a warning toast
        if (!gameInstance?.available) {
          toast.warning("This game copy is marked as generally unavailable. Your request may be rejected.");
        }
      } catch (error) {
        console.error("Error checking game availability:", error);
        setIsAvailable(null);
      } finally {
        setIsCheckingAvailability(false);
      }
    };
    
    // Debounce the availability check
    const timeoutId = setTimeout(checkAvailability, 500);
    return () => clearTimeout(timeoutId);
  }, [game?.id, date, time, duration]);

  const handleSubmit = async (data) => {
    if (!user?.id) {
       toast.error("You must be logged in to request a game.");
       return;
    }
    if (!game?.id) {
        toast.error("Game information is missing.");
        return;
    }
    if (!gameInstance?.id) {
        toast.error("Please select a specific game copy to borrow.");
        return;
    }

    try {
      setIsSubmitting(true);

      const startDateTime = new Date(`${data.date}T${data.time}`);
      const endDateTime = new Date(startDateTime);
      endDateTime.setHours(endDateTime.getHours() + parseFloat(data.duration));

      if (isNaN(startDateTime.getTime()) || isNaN(endDateTime.getTime())) {
        throw new Error("Invalid date or time input.");
      }
      if (endDateTime <= startDateTime) {
        throw new Error("End date/time must be after the start date/time.");
      }
      
      // Check availability one more time before submitting
      const available = await checkGameAvailability(game.id, startDateTime, endDateTime);
      if (!available) {
        throw new Error("This game is not available for the selected time period. Please select a different date or time.");
      }

      const requestData = {
        requesterId: user.id,
        requestedGameId: game.id,
        gameInstanceId: gameInstance.id,
        startDate: startDateTime.toISOString(),
        endDate: endDateTime.toISOString(),
      };

      const response = await createBorrowRequest(requestData);
      console.log("Borrow request created:", response);

      onSubmit?.({
        game,
        gameInstance,
        ...data,
        requestId: response.id,
        status: response.status
      });

      toast.success(
        <div>
          <p>Request to borrow {game?.name} was successfully sent! 🎉</p>
          <p className="text-xs mt-1">After returning the game, you'll be able to write a review!</p>
        </div>
      );

      onOpenChange(false);
      form.reset();
    } catch (error) {
      console.error("Borrow request error:", error);
      let message = error.message || "Failed to submit borrow request.";
      if (message.toLowerCase().includes("own game instance")) {
        message = "You cannot borrow a game instance that you own.";
      } else if (message.toLowerCase().includes("not found")) {
        message = "The game or user could not be found.";
      } else if (message.toLowerCase().includes("400")) {
        message = "The request could not be processed. Please check the details and try again.";
      }
      toast.error(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Request to Borrow {game?.name}</DialogTitle>
          <DialogDescription>
            Fill out the form below to request to borrow this game.
          </DialogDescription>
        </DialogHeader>

        {gameInstance && (
          <div className="bg-muted/50 p-3 rounded-md mb-4">
            <p className="font-medium text-sm">Selected Copy:</p>
            <p className="text-sm">Owner: {gameInstance.owner?.name || 'Unknown'}</p>
            {gameInstance.condition && (
              <p className="text-sm">Condition: {gameInstance.condition}</p>
            )}
          </div>
        )}
        
        {isAvailable === false && (
          <Alert variant="destructive" className="mb-4">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>
              This game is not available for the selected time period.
              Please choose a different date or time.
            </AlertDescription>
          </Alert>
        )}
        
        {isAvailable === true && (
          <Alert variant="success" className="mb-4 bg-green-50 text-green-800 border-green-200">
            <AlertCircle className="h-4 w-4 text-green-600" />
            <AlertDescription className="text-green-700">
              Great! This game is available for the selected time period.
            </AlertDescription>
          </Alert>
        )}

        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="date"
                rules={{
                  required: 'Date is required',
                   validate: (value) => new Date(value) >= new Date(today) || 'Date must be today or in the future'
                }}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Date</FormLabel>
                    <FormControl>
                      <Input type="date" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              
              <FormField
                control={form.control}
                name="time"
                rules={{ required: 'Time is required' }}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Start Time</FormLabel>
                    <FormControl>
                      <Input type="time" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
            
            <FormField
              control={form.control}
              name="duration"
              rules={{ 
                required: 'Duration is required',
                min: { value: 0.5, message: 'Minimum duration is 30 minutes' },
                max: { value: 72, message: 'Maximum duration is 72 hours' }
              }}
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Duration (hours)</FormLabel>
                  <FormControl>
                    <Input 
                      type="number" 
                      step="0.5" 
                      min="0.5" 
                      max="72" 
                      {...field} 
                      onChange={e => field.onChange(e.target.value)}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            
            <DialogFooter className="gap-2 sm:gap-0">
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={isSubmitting || isAvailable === false}
              >
                {isSubmitting ? 'Requesting...' : 'Send Request'}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
};
