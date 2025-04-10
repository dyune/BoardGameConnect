import React, { useState, useEffect } from "react";
import { Button } from "../ui/button";
import { Textarea } from "../ui/textarea";
import { Star, Loader2, AlertCircle } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import { submitReview, updateReview, deleteReview } from "../../service/game-api";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "../ui/alert-dialog";

/**
 * Component for submitting or editing a game review
 * 
 * @param {Object} props - Component props
 * @param {number} props.gameId - ID of the game being reviewed
 * @param {Object} [props.existingReview] - Existing review if editing
 * @param {Function} props.onReviewSubmitted - Callback when review is submitted
 * @param {Function} props.onCancel - Callback when review form is cancelled
 */
const ReviewForm = ({ gameId, existingReview, onReviewSubmitted, onCancel }) => {
  const { user } = useAuth();
  const [rating, setRating] = useState(existingReview ? existingReview.rating : 0);
  const [comment, setComment] = useState(existingReview ? existingReview.comment : "");
  const [hoveredRating, setHoveredRating] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  const isEdit = !!existingReview;

  useEffect(() => {
    if (existingReview) {
      setRating(existingReview.rating);
      setComment(existingReview.comment || "");
    }
  }, [existingReview]);

  const handleStarClick = (value) => {
    setRating(value === rating ? 0 : value);
  };

  const handleStarHover = (value) => {
    setHoveredRating(value);
  };

  const handleCommentChange = (e) => {
    setComment(e.target.value);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (rating === 0) {
      setError("Please select a rating");
      return;
    }

    setIsSubmitting(true);

    try {
      const reviewData = {
        rating,
        comment,
        gameId,
        reviewerId: user?.email,
      };

      let response;
      if (isEdit) {
        response = await updateReview(existingReview.id, reviewData);
      } else {
        response = await submitReview(reviewData);
      }

      onReviewSubmitted(response);
    } catch (err) {
      console.error("Error submitting review:", err);
      setError(err.message || "Failed to submit review. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async () => {
    setIsDeleting(true);
    try {
      await deleteReview(existingReview.id);
      onReviewSubmitted(null, true); // true indicates deleted
      setShowDeleteDialog(false);
    } catch (err) {
      console.error("Error deleting review:", err);
      setError(err.message || "Failed to delete review");
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="space-y-4 border rounded-lg p-4">
      <h3 className="text-lg font-medium">{isEdit ? "Edit Your Review" : "Leave a Review"}</h3>
      
      {error && (
        <div className="bg-destructive/10 text-destructive p-2 rounded-md flex items-center gap-2 text-sm">
          <AlertCircle className="h-4 w-4" />
          {error}
        </div>
      )}
      
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <p className="text-sm font-medium mb-2">Your Rating</p>
          <div className="flex gap-1">
            {[1, 2, 3, 4, 5].map((star) => (
              <button
                key={star}
                type="button"
                onClick={() => handleStarClick(star)}
                onMouseEnter={() => handleStarHover(star)}
                onMouseLeave={() => handleStarHover(0)}
                className="focus:outline-none"
              >
                <Star
                  className={`h-8 w-8 cursor-pointer ${
                    star <= (hoveredRating || rating)
                      ? "fill-yellow-400 text-yellow-400"
                      : "text-gray-300"
                  }`}
                />
              </button>
            ))}
          </div>
        </div>
        
        <div>
          <label htmlFor="comment" className="text-sm font-medium">
            Your Review (Optional)
          </label>
          <Textarea
            id="comment"
            placeholder="Share your thoughts about this game..."
            value={comment}
            onChange={handleCommentChange}
            rows={4}
            className="mt-1"
          />
        </div>
        
        <div className="flex justify-between">
          <Button 
            type="button" 
            variant="outline" 
            onClick={onCancel}
            disabled={isSubmitting || isDeleting}
          >
            Cancel
          </Button>
          
          <div className="flex gap-2">
            {isEdit && (
              <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
                <AlertDialogTrigger asChild>
                  <Button 
                    type="button" 
                    variant="destructive" 
                    disabled={isSubmitting || isDeleting}
                  >
                    {isDeleting ? (
                      <Loader2 className="h-4 w-4 animate-spin mr-1" />
                    ) : (
                      "Delete"
                    )}
                  </Button>
                </AlertDialogTrigger>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>Delete Review?</AlertDialogTitle>
                    <AlertDialogDescription>
                      Are you sure you want to delete your review? This action cannot be undone.
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>Cancel</AlertDialogCancel>
                    <AlertDialogAction onClick={handleDelete}>
                      {isDeleting ? (
                        <Loader2 className="h-4 w-4 animate-spin mr-1" />
                      ) : (
                        "Delete"
                      )}
                    </AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            )}
            
            <Button 
              type="submit" 
              disabled={isSubmitting || isDeleting}
            >
              {isSubmitting && <Loader2 className="h-4 w-4 animate-spin mr-1" />}
              {isEdit ? "Update Review" : "Submit Review"}
            </Button>
          </div>
        </div>
      </form>
    </div>
  );
};

export default ReviewForm; 