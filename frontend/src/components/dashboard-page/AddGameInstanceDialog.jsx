import { useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { copyGame } from "@/service/game-api.js";
import { Loader2 } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";

export default function AddGameInstanceDialog({ open, onOpenChange, gameId, gameName, onInstanceAdded, initialMessage }) {
  const [isLoading, setIsLoading] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const { user } = useAuth();

  const { register, handleSubmit, formState: { errors }, reset, setValue, watch } = useForm({
    defaultValues: {
      name: "",
      condition: "Excellent",
      location: "Home",
    },
  });

  // Condition options
  const conditionOptions = [
    { value: "New", label: "New" },
    { value: "Excellent", label: "Excellent" },
    { value: "Good", label: "Good" },
    { value: "Fair", label: "Fair" },
    { value: "Poor", label: "Poor" }
  ];

  const onSubmit = handleSubmit(async (data) => {
    if (!gameId) {
      toast.error("Game ID is required");
      return;
    }

    setIsLoading(true);
    setSubmitError("");

    try {
      const instanceData = {
        name: data.name || null,
        condition: data.condition,
        location: data.location,
        available: true
      };
      
      const result = await copyGame(gameId, instanceData);
      
      toast.success("Game copy added successfully");
      reset(); // Reset form
      onOpenChange(false); // Close dialog
      
      if (onInstanceAdded) {
        onInstanceAdded(result);
      }
    } catch (error) {
      const errorMsg = error.message || "Failed to add game copy";
      setSubmitError(errorMsg);
      toast.error(errorMsg);
    } finally {
      setIsLoading(false);
    }
  });

  // Handle form field change
  const handleFieldChange = (field, value) => {
    setValue(field, value);
  };

  // Custom reset function
  const handleCancel = () => {
    reset();
    setSubmitError("");
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Add New Copy of {gameName}</DialogTitle>
          <DialogDescription>
            Enter the details for your new game copy to add it to your collection
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {initialMessage && (
            <div className="p-3 border border-blue-200 bg-blue-50 rounded-md mb-2">
              <p className="text-sm text-blue-800">{initialMessage}</p>
            </div>
          )}

          {!initialMessage && (
            <div className="p-3 border border-blue-200 bg-blue-50 rounded-md mb-2">
              <p className="text-sm text-blue-800">
                <strong>Game has been added!</strong> Now you need to create a copy of this game for your collection.
                This allows you to track specific details about your physical copy of the game.
              </p>
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor="name">Copy Name (Optional)</Label>
            <Input
              id="name"
              {...register("name")}
              placeholder="e.g., Deluxe Edition, Travel Version"
            />
            <p className="text-xs text-muted-foreground">
              A name to distinguish this copy from others (leave blank to use game name)
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="condition">Condition</Label>
            <Select value={conditionOptions.find(c => c.value === watch("condition"))?.value} onValueChange={(value) => handleFieldChange("condition", value)}>
              <SelectTrigger>
                <SelectValue placeholder="Select condition" />
              </SelectTrigger>
              <SelectContent>
                {conditionOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="location">Storage Location</Label>
            <Input
              id="location"
              {...register("location")}
              placeholder="e.g., Living Room Shelf, Game Cabinet"
            />
            <p className="text-xs text-muted-foreground">
              Where you keep this game (helps you remember where to find it)
            </p>
          </div>

          {submitError && (
            <p className="text-red-500 text-sm text-center">{submitError}</p>
          )}

          <DialogFooter className="pt-4">
            <Button variant="outline" type="button" onClick={handleCancel}>
              Cancel
            </Button>
            <Button 
              type="button" 
              onClick={onSubmit}
              disabled={isLoading}
            >
              {isLoading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Adding...
                </>
              ) : (
                "Add Copy"
              )}
            </Button>
          </DialogFooter>
        </div>
      </DialogContent>
    </Dialog>
  );
} 