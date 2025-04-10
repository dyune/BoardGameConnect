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
import { AlertCircle, Loader2 } from 'lucide-react';
import { useForm, useWatch } from "react-hook-form";
import { getBorrowRequestById } from '@/service/borrow_request-api.js';
import { checkGameAvailability, getGameById } from '@/service/game-api.js';
import { toast } from 'sonner';
import { useAuth } from '@/context/AuthContext';
import { updateUserBorrowRequest } from '@/service/dashboard-api.js';

export default function ModifyBorrowRequestDialog({ open, onOpenChange, requestId, gameId, onSuccess }) {
  const { user } = useAuth();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isCheckingAvailability, setIsCheckingAvailability] = useState(false);
  const [isAvailable, setIsAvailable] = useState(null);
  const [requestData, setRequestData] = useState(null);
  const [gameDetails, setGameDetails] = useState(null);

  const form = useForm({
    defaultValues: {
      date: '',
      time: '',
      duration: '1',
    }
  });

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
  
  // Calculate duration in hours between two dates
  const calculateDuration = (startDate, endDate) => {
    if (!startDate || !endDate) return '1';
    const start = new Date(startDate);
    const end = new Date(endDate);
    const durationHours = (end - start) / (1000 * 60 * 60);
    return durationHours.toString();
  };

  // Get today's date in YYYY-MM-DD format
  const today = new Date().toISOString().split('T')[0];
  
  // Watch date, time, and duration fields for changes to check availability
  const date = useWatch({ control: form.control, name: "date" });
  const time = useWatch({ control: form.control, name: "time" });
  const duration = useWatch({ control: form.control, name: "duration" });

  // Fetch the borrow request and game details
  useEffect(() => {
    const fetchData = async () => {
      try {
        setIsLoading(true);
        setError(null);
        
        // Fetch the borrow request
        const request = await getBorrowRequestById(requestId);
        setRequestData(request);
        
        // Fetch game details
        const game = await getGameById(gameId);
        setGameDetails(game);
        
        // Set form values from the request
        form.reset({
          date: formatDateForInput(request.startDate),
          time: formatTimeForInput(request.startDate),
          duration: calculateDuration(request.startDate, request.endDate),
        });
        
        // Check availability
        const startDateTime = new Date(request.startDate);
        const endDateTime = new Date(request.endDate);
        const available = await checkGameAvailability(gameId, startDateTime, endDateTime);
        setIsAvailable(available);
      } catch (err) {
        console.error("Error fetching request data:", err);
        setError("Failed to load borrow request details. Please try again.");
        toast.error("Failed to load borrow request details.");
      } finally {
        setIsLoading(false);
      }
    };
    
    if (requestId && gameId) {
      fetchData();
    }
  }, [requestId, gameId, form]);
  
  // Check availability when date or time changes
  useEffect(() => {
    const checkAvailability = async () => {
      // Only check if we have all required data
      if (!gameId || !date || !time || !duration) {
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
        const available = await checkGameAvailability(gameId, startDateTime, endDateTime);
        setIsAvailable(available);
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
  }, [gameId, date, time, duration]);

  const handleSubmit = async (data) => {
    if (!user?.id) {
       toast.error("You must be logged in to modify a request.");
       return;
    }
    if (!gameId) {
        toast.error("Game information is missing.");
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
      const available = await checkGameAvailability(gameId, startDateTime, endDateTime);
      if (!available) {
        throw new Error("This game is not available for the selected time period. Please select a different date or time.");
      }

      // Update the request with new data - only send necessary fields to update
      const updatedRequestData = {
        startDate: startDateTime.toISOString(),
        endDate: endDateTime.toISOString(),
        // Include requesterId and gameId for backend validation
        requesterId: user.id,
        requestedGameId: gameId
      };

      // Use the new update function specifically for user modifications
      await updateUserBorrowRequest(requestId, updatedRequestData);

      toast.success(`Borrow request has been successfully updated!`);

      onOpenChange(false);
      if (onSuccess) {
        onSuccess();
      }
    } catch (error) {
      console.error("Error updating borrow request:", error);
      
      // Check for access denied or permission errors
      if (error.status === 403 || error.message?.includes('permission') || error.message?.includes('denied')) {
        setError("Access denied: You don't have permission to modify this request.");
        toast.error("Access denied: You don't have permission to modify this request.");
      } else if (error.status === 401 || error.message?.includes('auth')) {
        setError("Authentication error: Please log in again.");
        toast.error("Authentication error: Please log in again.");
      } else {
        setError(error.message || "Failed to update borrow request. Please try again.");
        toast.error(error.message || "Failed to update borrow request. Please try again.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="sm:max-w-md">
          <div className="flex justify-center items-center py-10">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
          </div>
        </DialogContent>
      </Dialog>
    );
  }

  if (error) {
    return (
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Error</DialogTitle>
          </DialogHeader>
          <div className="text-center py-6 text-red-500">{error}</div>
          <DialogFooter>
            <Button variant="outline" onClick={() => onOpenChange(false)}>Close</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    );
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Modify Borrow Request</DialogTitle>
          <DialogDescription>
            Update the details of your borrow request for "{gameDetails?.name}".
          </DialogDescription>
        </DialogHeader>

        {gameDetails && (
          <div className="bg-muted/50 p-3 rounded-md mb-4">
            <p className="font-medium text-sm">Game: {gameDetails.name}</p>
            <p className="text-sm">Category: {gameDetails.category || 'Unknown'}</p>
            <p className="text-sm">Players: {gameDetails.minPlayers}-{gameDetails.maxPlayers}</p>
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
            
            <div className="grid grid-cols-2 gap-4">
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
            </div>
            
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
                {isSubmitting ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Updating...
                  </>
                ) : "Update Request"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
} 