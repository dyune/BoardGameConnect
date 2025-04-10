import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { updateGame, getGameById } from "../../service/game-api.js";

export default function ModifyGameDialog({ open, onOpenChange, onGameModified, gameId }) {
  const [isLoading, setIsLoading] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [isLoadingGame, setIsLoadingGame] = useState(false);

  const { register, handleSubmit, formState: { errors }, reset, setValue } = useForm({
    defaultValues: {
      name: "",
      minPlayers: "",
      maxPlayers: "",
      image: "",
      category: "",
    },
  });

  // Load game data when dialog opens
  useEffect(() => {
    if (open && gameId) {
      loadGameData();
    }
  }, [open, gameId]);

  const loadGameData = async () => {
    if (!gameId) return;
    
    setIsLoadingGame(true);
    try {
      const game = await getGameById(gameId);
      // Set form values
      setValue("name", game.name);
      setValue("minPlayers", game.minPlayers);
      setValue("maxPlayers", game.maxPlayers);
      setValue("image", game.image || "");
      setValue("category", game.category || "");
    } catch (error) {
      toast.error("Failed to load game data");
      console.error("Failed to load game data:", error);
    } finally {
      setIsLoadingGame(false);
    }
  };

  const onSubmit = handleSubmit(async (data) => {
    setIsLoading(true);
    setSubmitError("");

    try {
      // Ensure player counts are numbers
      const gameData = {
        ...data,
        minPlayers: parseInt(data.minPlayers, 10),
        maxPlayers: parseInt(data.maxPlayers, 10),
      };
      const result = await updateGame(gameId, gameData);
      toast.success(`Successfully updated game: ${result.name}`);
      onOpenChange(false); // Close dialog
      if (onGameModified) {
        onGameModified(result); // Notify parent component
      }
    } catch (error) {
      const errorMsg = `Failed to update game: ${error.message || 'Unknown error'}`;
      setSubmitError(errorMsg);
      toast.error(errorMsg);
    } finally {
      setIsLoading(false);
    }
  });

  // Custom reset function
  const handleCancel = () => {
    reset();
    setSubmitError("");
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={(isOpen) => {
      if (!isOpen) handleCancel();
      else onOpenChange(true);
    }}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">Modify Game</DialogTitle>
        </DialogHeader>

        {isLoadingGame ? (
          <div className="py-8 flex justify-center">
            <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-primary"></div>
          </div>
        ) : (
          <form onSubmit={onSubmit} className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="name">Game Name <span className="text-red-500">*</span></Label>
              <Input
                id="name"
                {...register("name", { required: "Game name is required" })}
                className={errors.name ? "border-red-500" : ""}
              />
              {errors.name && <p className="text-red-500 text-sm">{errors.name.message}</p>}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="minPlayers">Min Players <span className="text-red-500">*</span></Label>
                <Input
                  id="minPlayers"
                  type="number"
                  min="1"
                  {...register("minPlayers", {
                    required: "Min players is required",
                    valueAsNumber: true,
                    min: { value: 1, message: "Must be at least 1" }
                  })}
                  className={errors.minPlayers ? "border-red-500" : ""}
                />
                {errors.minPlayers && <p className="text-red-500 text-sm">{errors.minPlayers.message}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="maxPlayers">Max Players <span className="text-red-500">*</span></Label>
                <Input
                  id="maxPlayers"
                  type="number"
                  min="1"
                  {...register("maxPlayers", {
                    required: "Max players is required",
                    valueAsNumber: true,
                    validate: (value, { minPlayers }) => parseInt(value, 10) >= parseInt(minPlayers, 10) || "Max players must be >= min players"
                  })}
                  className={errors.maxPlayers ? "border-red-500" : ""}
                />
                {errors.maxPlayers && <p className="text-red-500 text-sm">{errors.maxPlayers.message}</p>}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="category">Category</Label>
              <Input id="category" {...register("category")} placeholder="e.g., Strategy, Party, Family" />
            </div>

            <div className="space-y-2">
              <Label htmlFor="image">Image URL</Label>
              <Input id="image" {...register("image")} placeholder="https://example.com/image.png" />
            </div>

            {submitError && (
              <p className="text-red-500 text-sm text-center">{submitError}</p>
            )}

            <DialogFooter className="pt-4">
              <Button variant="outline" type="button" onClick={handleCancel}>
                Cancel
              </Button>
              <Button type="submit" disabled={isLoading}>
                {isLoading ? "Updating..." : "Update Game"}
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
} 