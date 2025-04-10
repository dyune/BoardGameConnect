import { useState, useEffect, useCallback } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Loader2, Edit, Plus, Check, X, Trash2 } from "lucide-react";
import { getGameInstances, updateGameInstance, createGameInstance, deleteGameInstance } from "@/service/game-api.js";
import { useAuth } from "@/context/AuthContext";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { useForm } from "react-hook-form";
import { Switch } from "@/components/ui/switch";

export default function GameInstanceManager({ gameId, gameName, refreshGames }) {
  const [instances, setInstances] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [editingInstance, setEditingInstance] = useState(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);
  const [instanceToDelete, setInstanceToDelete] = useState(null);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [hasNoInstances, setHasNoInstances] = useState(false);
  const { user } = useAuth();

  const editForm = useForm({
    defaultValues: {
      name: "",
      condition: "",
      location: "",
      available: true,
    }
  });

  const addForm = useForm({
    defaultValues: {
      name: "",
      condition: "Excellent",
      location: "Home",
    }
  });

  // Fetch instances
  const fetchInstances = useCallback(async () => {
    if (!gameId) return;
    
    setIsLoading(true);
    setError(null);
    
    try {
      const data = await getGameInstances(gameId);
      // Filter instances to only show those owned by the current user
      const userInstances = data.filter(instance => 
        instance.owner && instance.owner.id === user?.id
      );
      setInstances(userInstances);
      
      // If no instances remain, set state and trigger refresh to remove game from dashboard
      if (userInstances.length === 0) {
        setHasNoInstances(true);
        if (refreshGames) {
          refreshGames();
        }
      }
    } catch (err) {
      console.error(`Failed to fetch instances for game ${gameId}:`, err);
      setError("Failed to load game copies");
      toast.error("Could not load game copies");
    } finally {
      setIsLoading(false);
    }
  }, [gameId, user?.id, refreshGames]);

  useEffect(() => {
    fetchInstances();
  }, [fetchInstances]);

  // Handle opening the edit dialog
  const handleEditClick = (instance) => {
    setEditingInstance(instance);
    editForm.reset({
      name: instance.name || "",
      condition: instance.condition || "",
      location: instance.location || "",
      available: instance.available
    });
    setIsEditModalOpen(true);
  };

  // Handle updating an instance
  const handleUpdateInstance = async (data) => {
    if (!editingInstance) return;
    
    setIsLoading(true);
    try {
      const updateData = {
        name: data.name || null,
        condition: data.condition,
        location: data.location,
        available: data.available,
        gameId: gameId
      };
      
      await updateGameInstance(editingInstance.id, updateData);
      
      toast.success("Game copy updated successfully");
      await fetchInstances();
      setIsEditModalOpen(false);
    } catch (err) {
      console.error("Failed to update game instance:", err);
      toast.error("Failed to update game copy");
    } finally {
      setIsLoading(false);
    }
  };

  // Handle adding a new instance
  const handleAddInstance = async (data) => {
    setIsLoading(true);
    try {
      const instanceData = {
        name: data.name || null,
        condition: data.condition,
        location: data.location,
        available: true,
        gameId: gameId,
        ownerId: user?.id
      };
      
      await createGameInstance(gameId, instanceData);
      
      toast.success("New game copy added successfully");
      await fetchInstances();
      setIsAddModalOpen(false);
      if (refreshGames) refreshGames();
    } catch (err) {
      console.error("Failed to add game instance:", err);
      toast.error("Failed to add game copy");
    } finally {
      setIsLoading(false);
    }
  };

  // Handle deleting an instance
  const handleDeleteInstance = async () => {
    if (!instanceToDelete) return;
    
    setIsDeleting(true);
    setDeleteError(null);

    try {
      await deleteGameInstance(gameId, instanceToDelete.id);
      toast.success("Game copy deleted successfully");
      
      // Check if this was the last instance
      if (instances.length === 1) {
        setHasNoInstances(true);
        setIsDeleteDialogOpen(false);
        setInstanceToDelete(null);
        // Force refresh parent
        if (refreshGames) {
          refreshGames();
        }
      } else {
        await fetchInstances();
        setIsDeleteDialogOpen(false);
        setInstanceToDelete(null);
      }
    } catch (err) {
      console.error("Failed to delete game instance:", err);
      setDeleteError(err.message || "Failed to delete game copy. Please try again.");
      toast.error("Failed to delete game copy");
    } finally {
      setIsDeleting(false);
    }
  };

  // Handle opening delete confirmation dialog
  const handleDeleteClick = (instance) => {
    setInstanceToDelete(instance);
    setIsDeleteDialogOpen(true);
  };

  // Condition options
  const conditionOptions = [
    { value: "New", label: "New" },
    { value: "Excellent", label: "Excellent" },
    { value: "Good", label: "Good" },
    { value: "Fair", label: "Fair" },
    { value: "Poor", label: "Poor" }
  ];

  // If no instances remain, don't render anything
  if (hasNoInstances) {
    return null;
  }

  return (
    <div className="space-y-4">
      {isLoading && !isDeleting && (
        <div className="flex justify-center items-center py-6">
          <Loader2 className="h-6 w-6 animate-spin text-primary" />
        </div>
      )}
      
      {error && (
        <div className="text-center text-destructive py-4">{error}</div>
      )}
      
      {!isLoading && !error && (
        <>
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-medium">Your Copies of {gameName}</h3>
            <Button 
              variant="outline" 
              size="sm" 
              onClick={() => {
                addForm.reset({
                  name: "",
                  condition: "Excellent",
                  location: "Home"
                });
                setIsAddModalOpen(true);
              }}
              className="flex items-center gap-1"
            >
              <Plus className="h-4 w-4" />
              Add Copy
            </Button>
          </div>

          <div className="grid gap-4">
            {instances.map((instance) => (
              <Card key={`instance-${instance.id}`}>
                <CardContent className="p-4">
                  <div className="flex justify-between items-start">
                    <div>
                      <h4 className="font-medium">{instance.name || "Unnamed Copy"}</h4>
                      <p className="text-sm text-muted-foreground">
                        Condition: {instance.condition}
                      </p>
                      <p className="text-sm text-muted-foreground">
                        Location: {instance.location}
                      </p>
                      <p className="text-sm text-muted-foreground">
                        Status: {instance.available ? "Available" : "Not Available"}
                      </p>
                    </div>
                    <div className="flex gap-2">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleEditClick(instance)}
                      >
                        <Edit className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleDeleteClick(instance)}
                        className="text-destructive hover:text-destructive"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </>
      )}

      {/* Edit Instance Dialog */}
      <Dialog open={isEditModalOpen} onOpenChange={setIsEditModalOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>Edit Game Copy</DialogTitle>
            <DialogDescription>
              Edit the details of your copy of {gameName}.
            </DialogDescription>
          </DialogHeader>
          <Form {...editForm}>
            <form onSubmit={editForm.handleSubmit(handleUpdateInstance)} className="space-y-4">
              <FormField
                control={editForm.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Copy Name (Optional)</FormLabel>
                    <FormControl>
                      <Input placeholder="Enter a name for this copy" {...field} />
                    </FormControl>
                    <FormDescription>
                      Give this copy a unique name to help you identify it
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={editForm.control}
                name="condition"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Condition</FormLabel>
                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select condition" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {conditionOptions.map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={editForm.control}
                name="location"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Location</FormLabel>
                    <FormControl>
                      <Input placeholder="Enter location" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={editForm.control}
                name="available"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
                    <div className="space-y-0.5">
                      <FormLabel className="text-base">Available</FormLabel>
                      <FormDescription>
                        Whether this copy is available for borrowing
                      </FormDescription>
                    </div>
                    <FormControl>
                      <Switch
                        checked={field.value}
                        onCheckedChange={field.onChange}
                      />
                    </FormControl>
                  </FormItem>
                )}
              />
              <DialogFooter>
                <Button type="submit" disabled={isLoading}>
                  {isLoading ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Saving...
                    </>
                  ) : (
                    <>
                      <Check className="mr-2 h-4 w-4" />
                      Save Changes
                    </>
                  )}
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      {/* Add Instance Dialog */}
      <Dialog open={isAddModalOpen} onOpenChange={setIsAddModalOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>Add New Game Copy</DialogTitle>
            <DialogDescription>
              Add a new copy of {gameName} to your collection.
            </DialogDescription>
          </DialogHeader>
          <Form {...addForm}>
            <form onSubmit={addForm.handleSubmit(handleAddInstance)} className="space-y-4">
              <FormField
                control={addForm.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Copy Name (Optional)</FormLabel>
                    <FormControl>
                      <Input placeholder="Enter a name for this copy" {...field} />
                    </FormControl>
                    <FormDescription>
                      Give this copy a unique name to help you identify it
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={addForm.control}
                name="condition"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Condition</FormLabel>
                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select condition" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {conditionOptions.map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={addForm.control}
                name="location"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Location</FormLabel>
                    <FormControl>
                      <Input placeholder="Enter location" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <DialogFooter>
                <Button type="submit" disabled={isLoading}>
                  {isLoading ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Adding...
                    </>
                  ) : (
                    <>
                      <Plus className="mr-2 h-4 w-4" />
                      Add Copy
                    </>
                  )}
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      {/* Delete Instance Confirmation Dialog */}
      <AlertDialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Game Copy</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete this copy of {gameName}? This action cannot be undone.
              {deleteError && <p className="text-red-500 text-sm mt-2">{deleteError}</p>}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isDeleting}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteInstance}
              disabled={isDeleting}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {isDeleting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Deleting...
                </>
              ) : (
                <>
                  <Trash2 className="mr-2 h-4 w-4" />
                  Delete
                </>
              )}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
} 