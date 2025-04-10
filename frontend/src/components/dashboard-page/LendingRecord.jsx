import { useState, useEffect } from 'react';
import { Card, CardContent } from "@/components/ui/card.jsx";
import { Badge } from "@/components/ui/badge.jsx"; // Import Badge
import { Button } from "@/components/ui/button.jsx";
import { toast } from 'sonner'; // Import toast for notifications
import apiClient from '@/service/apiClient.js';
import { useAuth } from "@/context/AuthContext";

export default function LendingRecord({ id, name, requester, startDate, endDate, status, refreshRecords, imageSrc }) {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [isReturned, setIsReturned] = useState(false);
  const [localStatus, setLocalStatus] = useState(status);
  const { user } = useAuth();

  // Update isReturned state when status changes
  useEffect(() => {
    setIsReturned(status === 'Returned' || status === 'CLOSED');
    setLocalStatus(status);
  }, [status]);

  // Calculate if the lending is overdue based on the current date and end date
  const isOverdue = new Date() > new Date(endDate) && !isReturned;
  
  // Determine the current status to display
  const displayStatus = isReturned 
    ? 'Returned' 
    : (isOverdue ? 'Overdue' : (localStatus || 'Active'));

  const handleMarkReturned = async () => {
    if (!id) {
      console.error("Lending record ID is missing!");
      setError("Cannot process request: ID missing.");
      return;
    }
    
    setIsLoading(true);
    setError(null);
    
    try {
      console.log(`Attempting to mark record ${id} as returned...`);
      
      // Determine which endpoint to use based on user role
      const endpoint = user?.gameOwner 
        ? `/api/lending-records/${id}/confirm-return`
        : `/api/lending-records/${id}/mark-returned`;
      
      const requestBody = user?.gameOwner 
        ? {
            isDamaged: false,
            damageNotes: "",
            damageSeverity: 0
          }
        : {};
      
      const response = await apiClient(endpoint, {
        method: "POST",
        body: requestBody,
        skipPrefix: false,
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        }
      });
      
      console.log("Mark as returned response:", response);
      
      // Update the local state immediately
      setIsReturned(true);
      setLocalStatus('Returned');
      
      // Show success message to user
      toast.success("Game marked as returned successfully");
      
      // Refresh the list in the parent component
      if (refreshRecords) {
        refreshRecords();
      }
    } catch (err) {
      console.error("Failed to mark as returned:", err);
      
      // Create a more user-friendly error message
      let errorMessage = "Failed to mark as returned.";
      
      if (err.message && err.message.includes("404")) {
        errorMessage = "Record not found. It may have been deleted or already processed.";
      } else if (err.message && err.message.includes("403")) {
        errorMessage = "You don't have permission to mark this record as returned.";
      } else if (err.message) {
        // Include the actual error message for debugging
        errorMessage = `Failed to mark as returned: ${err.message}`;
      }
      
      setError(errorMessage);
      toast.error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  return <Card>
    <CardContent className="p-4">
      <div className="flex flex-col md:flex-row gap-4">
        <div className="md:w-1/4">
          <img
            src={imageSrc || "https://placehold.co/200x200/e9e9e9/1d1d1d?text=No+Image"}
            alt={`Cover art for ${name}`}
            className="w-full h-full object-cover rounded-lg aspect-square"
            onError={(e) => {
              e.target.src = "https://placehold.co/200x200/e9e9e9/1d1d1d?text=No+Image";
            }}
          />
        </div>
        <div className="flex-1">
          <div className="flex justify-between">
            <h3 className="text-xl font-semibold">"{name}" has been lent</h3>
            <Badge
              variant={
                displayStatus === 'Returned' ? 'positive' :
                displayStatus === 'Overdue' ? 'destructive' :
                'outline' // Default for Active or other statuses
              }
              className="text-xs"
            >
              {displayStatus}
            </Badge>
          </div>
          {error && <p className="text-red-500 text-sm mt-2">{error}</p>}
          <div className="grid gap-1 mt-2">
            <div className="text-sm">
              <span className="font-medium">Borrower:</span> {requester}
            </div>
            <div className="text-sm">
              <span className="font-medium">Lent on:</span> {startDate ? new Date(startDate).toLocaleDateString() : 'Unknown'}
            </div>
            <div className="text-sm">
              <span className="font-medium">Return by:</span> {endDate ? new Date(endDate).toLocaleDateString() : 'Unknown'}
            </div>
          </div>
          {!isReturned && ( // Only show button if not already returned
            <div className="flex mt-4">
              <Button variant="positive" size="sm" onClick={handleMarkReturned} disabled={isLoading}>
                {isLoading ? 'Processing...' : 'Mark as returned'}
              </Button>
            </div>
          )}
        </div>
      </div>
    </CardContent>
  </Card>
}