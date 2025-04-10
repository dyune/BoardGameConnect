import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import { useNavigate } from 'react-router-dom';
import { createGame } from '../../service/game-api';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage
} from '../ui/form';
import { Input } from '../ui/input';
import { Button } from '../ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { Loader2 } from 'lucide-react';

const AddGameForm = () => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();
  
  const form = useForm({
    defaultValues: {
      name: '',
      category: '',
      minPlayers: 1,
      maxPlayers: 4,
      image: '',
      condition: 'Excellent',
      location: 'Home'
    }
  });
  
  const onSubmit = async (data) => {
    setIsSubmitting(true);
    
    try {
      // Call the API to create the game
      const response = await createGame(data);
      
      // Show success message
      toast.success(`Game ${response.name} was added successfully!`);
      
      // Reset form
      form.reset();
      
      // Navigate to game details page
      navigate(`/games/${response.id}`);
    } catch (error) {
      // Handle errors
      console.error('Error creating game:', error);
      toast.error(`Failed to add game: ${error.message || 'Unknown error'}`);
    } finally {
      setIsSubmitting(false);
    }
  };
  
  return (
    <div className="max-w-md mx-auto p-6 bg-card rounded-lg shadow-sm">
      <h2 className="text-2xl font-bold mb-6">Add a Game</h2>
      <p className="text-muted-foreground mb-6">
        Fill out this form to add a new game to your collection or create a new copy of an existing game.
        If a game with the same name already exists, this will create a new instance of that game.
      </p>
      
      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
          <FormField
            control={form.control}
            name="name"
            rules={{ required: 'Game name is required' }}
            render={({ field }) => (
              <FormItem>
                <FormLabel>Game Name</FormLabel>
                <FormControl>
                  <Input placeholder="Enter game name" {...field} />
                </FormControl>
                <FormDescription>
                  Enter the exact name of the game. If this name already exists, a new copy will be created.
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          
          <FormField
            control={form.control}
            name="category"
            rules={{ required: 'Category is required' }}
            render={({ field }) => (
              <FormItem>
                <FormLabel>Category</FormLabel>
                <Select 
                  onValueChange={field.onChange} 
                  defaultValue={field.value}
                >
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Select a category" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value="Strategy">Strategy</SelectItem>
                    <SelectItem value="Party">Party</SelectItem>
                    <SelectItem value="Family">Family</SelectItem>
                    <SelectItem value="Card">Card</SelectItem>
                    <SelectItem value="Cooperative">Cooperative</SelectItem>
                    <SelectItem value="Deck Building">Deck Building</SelectItem>
                    <SelectItem value="Role Playing">Role Playing</SelectItem>
                    <SelectItem value="Wargame">Wargame</SelectItem>
                    <SelectItem value="Other">Other</SelectItem>
                  </SelectContent>
                </Select>
                <FormDescription>
                  Select the category that best describes this game.
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          
          <div className="grid grid-cols-2 gap-4">
            <FormField
              control={form.control}
              name="minPlayers"
              rules={{ 
                required: 'Min players is required',
                min: { value: 1, message: 'Min players must be at least 1' }
              }}
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Min Players</FormLabel>
                  <FormControl>
                    <Input type="number" min="1" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            
            <FormField
              control={form.control}
              name="maxPlayers"
              rules={{ 
                required: 'Max players is required',
                min: { value: 1, message: 'Max players must be at least 1' },
                validate: (value, formValues) => 
                  parseInt(value) >= parseInt(formValues.minPlayers) || 
                  'Max players must be greater than or equal to Min players'
              }}
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Max Players</FormLabel>
                  <FormControl>
                    <Input type="number" min="1" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
          
          <FormField
            control={form.control}
            name="image"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Image URL (Optional)</FormLabel>
                <FormControl>
                  <Input placeholder="URL to game image" {...field} />
                </FormControl>
                <FormDescription>
                  Add a link to an image of the game box or components.
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          
          {/* Instance-specific fields */}
          <h3 className="text-lg font-medium pt-4">Game Copy Details</h3>
          
          <FormField
            control={form.control}
            name="condition"
            rules={{ required: 'Condition is required' }}
            render={({ field }) => (
              <FormItem>
                <FormLabel>Condition</FormLabel>
                <Select 
                  onValueChange={field.onChange} 
                  defaultValue={field.value}
                >
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Select condition" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value="New">New (unopened)</SelectItem>
                    <SelectItem value="Excellent">Excellent (like new)</SelectItem>
                    <SelectItem value="Good">Good (minor wear)</SelectItem>
                    <SelectItem value="Fair">Fair (noticeable wear)</SelectItem>
                    <SelectItem value="Poor">Poor (significant damage)</SelectItem>
                  </SelectContent>
                </Select>
                <FormDescription>
                  Describe the physical condition of this copy of the game.
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          
          <FormField
            control={form.control}
            name="location"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Location (Optional)</FormLabel>
                <FormControl>
                  <Input placeholder="Where is this game stored?" {...field} />
                </FormControl>
                <FormDescription>
                  Specify where this copy of the game is stored or located.
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          
          <Button type="submit" className="w-full" disabled={isSubmitting}>
            {isSubmitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Adding Game...
              </>
            ) : (
              'Add Game'
            )}
          </Button>
        </form>
      </Form>
    </div>
  );
};

export default AddGameForm; 