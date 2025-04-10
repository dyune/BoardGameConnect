import { useState, useEffect, useRef, useCallback } from "react";
import { useSearchParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent } from "@/components/ui/card";
import { EventCard } from "../components/events-page/EventCard"; // Assuming EventCard is reusable
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
 import { Badge } from "@/components/ui/badge";
 import { getUserInfoByEmail } from "../service/user-api.js";
 import { getUserGameInstances, getInstancesByOwnerEmail } from "../service/game-api.js"; // Import getInstancesByOwnerEmail
 import { getEventsByHostEmail } from "../service/event-api.js";
 import { getRegistrationsByEmail } from "../service/registration-api.js"; // Import the registration API
import { Loader2 } from "lucide-react";
import { useAuth } from "@/context/AuthContext"; // Import useAuth

// Define GameInstanceCard component for displaying game instances
const GameInstanceCard = ({ instance }) => {
  // Get the game information from the instance
  const game = {
    id: instance.gameId,
    title: instance.gameName || "Unnamed Game", // The main game title
    copyName: instance.name, // The specific copy/instance name
    image: instance.image || instance.gameImage,
    condition: instance.condition || "Not specified",
    location: instance.location || "Not specified",
    available: instance.available || false,
    acquiredDate: instance.acquiredDate ? new Date(instance.acquiredDate).toLocaleDateString() : "Unknown"
  };

  return (
    <Card className="overflow-hidden">
      <div className="h-40 overflow-hidden bg-muted flex items-center justify-center">
        {game.image ? (
           <img src={game.image} alt={game.title} className="w-full h-full object-cover" />
        ) : (
           <span className="text-sm text-muted-foreground">No Image</span>
        )}
      </div>
      <CardContent className="p-4">
        {/* Game title displayed prominently */}
        <h3 className="font-semibold text-lg truncate">{game.title}</h3>
        
        {/* Copy name displayed smaller if it exists and is different from title */}
        {game.copyName && game.copyName !== game.title && (
          <p className="text-sm text-muted-foreground mt-1 truncate">
            "{game.copyName}"
          </p>
        )}
        
        <div className="mt-2 space-y-1 text-sm">
          <div className="flex justify-between">
            <span className="text-muted-foreground">Condition:</span>
            <span>{game.condition}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Location:</span>
            <span>{game.location}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Status:</span>
            <Badge variant={game.available ? "success" : "secondary"}>
              {game.available ? "Available" : "Unavailable"}
            </Badge>
          </div>
          {game.acquiredDate && (
            <div className="flex justify-between">
              <span className="text-muted-foreground">Acquired:</span>
              <span>{game.acquiredDate}</span>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
};

export default function UserProfilePage() {
  const [searchParams] = useSearchParams();
  const profileEmail = searchParams.get('email');
  const { user: currentUser } = useAuth(); // Get current user from AuthContext
  const [activeTab, setActiveTab] = useState(null);

  // State for fetched data
  const [userInfo, setUserInfo] = useState(null); // Includes name, events, isGameOwner
  const [gameInstances, setGameInstances] = useState([]); // Changed from ownedGamesList to gameInstances
  const [hostedEventsList, setHostedEventsList] = useState([]);
  const [registeredEventsList, setRegisteredEventsList] = useState([]); // New state for registered events
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Ref to track the last fetched email to prevent redundant API calls
  const lastFetchedEmailRef = useRef(null);

  // Check if this is the current user's own profile
  const isOwnProfile = currentUser && 
    ((!profileEmail && currentUser.email) || (profileEmail === currentUser.email));

  // Memoize fetchData to prevent recreation on each render
  const fetchData = useCallback(async (emailToFetch) => {
    // Skip if we're trying to fetch the same email again
    if (lastFetchedEmailRef.current === emailToFetch && userInfo) {
      return;
    }
    
    // Update the ref to the current email being fetched
    lastFetchedEmailRef.current = emailToFetch;
    
    if (!emailToFetch) {
      setError("No user email specified in URL and you are not logged in.");
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);
    setUserInfo(null);
    setGameInstances([]); // Clear game instances
    setHostedEventsList([]);
    setRegisteredEventsList([]); // Clear registered events list

    try {
      // Fetch basic account info (name, type)
      const accountData = await getUserInfoByEmail(emailToFetch);
      console.log("User info data:", accountData);
      
       setUserInfo(accountData);
 
       // If the profile user is a game owner, fetch their game instances
       if (accountData && accountData.gameOwner) { // Remove isOwnProfile check
         try {
           // Fetch instances for the profile user's email instead of current user
           const instancesData = await getInstancesByOwnerEmail(emailToFetch);
           console.log(`Retrieved ${instancesData?.length || 0} game instances for ${emailToFetch}:`, instancesData);
           setGameInstances(instancesData || []);
         } catch (instancesError) {
           console.error("Failed to fetch game instances:", instancesError);
           // Don't set error state here - just log it as we want to continue even if games can't be fetched
        }
      }
      
      // Fetch events that the user is hosting
      try {
        console.log(`Fetching hosted events for ${emailToFetch}`);
        const hostedEvents = await getEventsByHostEmail(emailToFetch);
        console.log(`Retrieved ${hostedEvents?.length || 0} hosted events`);
        setHostedEventsList(hostedEvents || []);
      } catch (eventsError) {
        console.error("Failed to fetch hosted events:", eventsError);
        // Don't set error, just log it to avoid blocking the UI
      }
      
      // Fetch events that the user is registered for (similarly to hosted events)
      try {
        console.log(`Fetching registered events for ${emailToFetch}`);
        const registrations = await getRegistrationsByEmail(emailToFetch);
        console.log(`Retrieved ${registrations?.length || 0} registrations:`, registrations);
        
        // Extract events from registrations
        const regEvents = [];
        if (Array.isArray(registrations)) {
          for (const reg of registrations) {
            if (reg.event) {
              regEvents.push({
                ...reg.event,
                registrationId: reg.id // Store registration ID for potential unregister action
              });
            }
          }
        }
        
        console.log(`Processed ${regEvents.length} registered events`);
        setRegisteredEventsList(regEvents);
      } catch (regError) {
        console.error("Failed to fetch registered events:", regError);
        // Don't set error, just log it to avoid blocking the UI
      }
    } catch (accountError) {
      console.error("Failed to fetch user info:", accountError);
      setError(accountError.message || "Could not load user profile.");
    } finally {
      setIsLoading(false);
    }
  }, [isOwnProfile]);

  useEffect(() => {
    // Use profile email from URL params, or fall back to current user's email
    const emailToFetch = profileEmail || currentUser?.email;
    
    // Only fetch if we have an email to fetch
    if (emailToFetch) {
      fetchData(emailToFetch);
    }
  }, [profileEmail, fetchData]); // Remove currentUser?.email from dependencies

  // Set the initial active tab once userInfo is loaded
  useEffect(() => {
    if (userInfo && !activeTab) {
      // If the user is hosting events, start on that tab, otherwise go to the default tab
      if (hostedEventsList.length > 0) {
        setActiveTab("hosting");
      } else if (registeredEventsList.length > 0) {
        setActiveTab("registered");
      } else {
        const userType = userInfo.gameOwner ? "owner" : "player";
        setActiveTab(userType === 'owner' ? "games" : "registered");
      }
    }
  }, [userInfo, activeTab, hostedEventsList.length, registeredEventsList.length]);

  // Loading state
  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[calc(100vh-100px)]">
        <Loader2 className="h-16 w-16 animate-spin text-muted-foreground" />
      </div>
    );
  }

  // Error state
  if (error) {
     return (
       <div className="text-center py-10 text-destructive">
         <p>Error loading profile: {error}</p>
       </div>
     );
  }

   // No user found state (userInfo is null after loading without error)
   if (!userInfo) {
      return (
        <div className="text-center py-10 text-muted-foreground">
          User profile not found for email: {profileEmail || 'N/A'}
        </div>
      );
   }

  // Handle tab change
  const handleTabChange = (value) => {
    setActiveTab(value);
  };

  return (
    <div className="bg-background text-foreground p-6">
      {/* Profile Header - Use fetched userInfo */}
      <div className="flex flex-col md:flex-row gap-6 mb-8">
        <div className="flex-shrink-0">
          <Avatar className="h-32 w-32">
            <AvatarImage src="/placeholder.svg?height=128&width=128" alt={userInfo.name || 'User'}/>
            <AvatarFallback className="text-3xl">{userInfo.name ? userInfo.name.substring(0, 2).toUpperCase() : 'U'}</AvatarFallback>
          </Avatar>
        </div>

        <div className="flex-grow">
          <div className="flex justify-between items-start">
            <div>
              <h1 className="text-4xl font-bold">{userInfo.name}</h1>
              {userInfo.gameOwner && (
                <Badge variant="secondary" className="mt-2">Game Owner</Badge>
              )}
              <p className="text-sm text-muted-foreground mt-1">{profileEmail || currentUser?.email}</p>
            </div>
            {/* TODO: Implement Edit Profile functionality only if viewing own profile */}
          </div>
        </div>
      </div>

      {/* Tabs for different sections */}
       <Tabs value={activeTab} onValueChange={handleTabChange} className="w-full">
         <TabsList className="mb-6">
           {userInfo.gameOwner && ( // Remove isOwnProfile check
              <TabsTrigger value="games">Game Collection</TabsTrigger>
           )}
           <TabsTrigger value="hosting">Hosting</TabsTrigger>
          <TabsTrigger value="registered">Registered Events</TabsTrigger>
         </TabsList>
 
         {/* Games Tab - Render if profile user is owner */}
         {userInfo.gameOwner && ( // Remove isOwnProfile check
           <TabsContent value="games">
             <div>
               <div className="flex items-center justify-between mb-4">
                <h2 className="text-2xl font-semibold">Game Collection</h2>
              </div>
              {gameInstances && gameInstances.length > 0 ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                  {gameInstances.map((instance) => (
                    <GameInstanceCard key={instance.id} instance={instance} />
                  ))}
                </div>
              ) : (
                <p className="text-muted-foreground">No game instances found in your collection.</p>
              )}
            </div>
          </TabsContent>
        )}

        {/* Hosting Tab - Events user is hosting */}
        <TabsContent value="hosting">
          <div>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-2xl font-semibold">Events Hosting</h2>
            </div>
            {hostedEventsList && hostedEventsList.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {hostedEventsList.map((event) => {
                  const adaptedEvent = {
                    id: event.id || event.eventId,
                    eventId: event.id || event.eventId,
                    title: event.title,
                    name: event.title,
                    dateTime: event.dateTime,
                    location: event.location,
                    host: event.host ? {
                      name: event.host.name,
                      email: event.host.email
                    } : { name: userInfo.name },
                    hostName: event.host ? event.host.name : userInfo.name,
                    hostEmail: event.host ? event.host.email : profileEmail || currentUser?.email,
                    featuredGame: event.featuredGame ? { name: event.featuredGame.name } : { name: 'Unknown Game' },
                    game: event.featuredGame ? event.featuredGame.name : 'Unknown Game',
                    featuredGameImage: event.featuredGame?.image || "https://placehold.co/400x300/e9e9e9/1d1d1d?text=No+Image",
                    maxParticipants: event.maxParticipants,
                    currentNumberParticipants: event.currentNumberParticipants,
                    participants: {
                      current: event.currentNumberParticipants,
                      capacity: event.maxParticipants
                    },
                    description: event.description,
                  };
                  
                  return <EventCard 
                    key={adaptedEvent.id} 
                    event={adaptedEvent}
                    isUserEventHost={isOwnProfile}
                    hideRegisterButtons={true}
                    onRegistrationUpdate={() => {
                      // Ensure we stay on the hosting tab
                      setActiveTab("hosting");
                      // Refresh the data
                      fetchData(profileEmail || currentUser?.email);
                    }}
                  />;
                })}
              </div>
            ) : (
              <p className="text-muted-foreground">Not hosting any events yet.</p>
            )}
          </div>
        </TabsContent>

        {/* Registered Tab - Use directly fetched registered events list instead of userInfo.events */}
        <TabsContent value="registered">
          <div>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-2xl font-semibold">Events Attending</h2>
            </div>
            
            {console.log('Registered events list:', registeredEventsList)}
            
            {registeredEventsList && registeredEventsList.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {registeredEventsList.map((event, index) => {
                   console.log(`Processing registered event ${index}:`, event);
                   
                   try {
                     const adaptedEvent = {
                       id: event.id || event.eventId,
                       eventId: event.id || event.eventId, // Add explicit eventId property as backup
                       title: event.title,
                       name: event.title, // add name as backup
                       dateTime: event.dateTime,
                       location: event.location || 'No location specified',
                       // Properly map host information
                       host: event.host ? { 
                         name: event.host.name,
                         email: event.host.email
                       } : { name: 'Unknown Host' },
                       hostName: event.host ? event.host.name : 'Unknown Host',
                       hostEmail: event.host ? event.host.email : null,
                       // Properly map game information
                       featuredGame: event.featuredGame ? { name: event.featuredGame.name } : { name: 'Unknown Game' },
                       game: event.featuredGame ? event.featuredGame.name : 'Unknown Game',
                       featuredGameImage: event.featuredGame?.image || "https://placehold.co/400x300/e9e9e9/1d1d1d?text=No+Image",
                       // Participants info - multiple formats to ensure compatibility
                       maxParticipants: event.maxParticipants || 0,
                       currentNumberParticipants: event.currentNumberParticipants || 0,
                       participants: {
                         current: event.currentNumberParticipants || 0,
                         capacity: event.maxParticipants || 0
                       },
                       description: event.description || 'No description available',
                     };
                     
                     console.log(`Successfully created adapted event ${index}:`, adaptedEvent);
                     
                     return <EventCard 
                       key={`reg-${event.registrationId || index}`}
                       event={adaptedEvent}
                       isCurrentUserRegistered={isOwnProfile}
                       registrationId={isOwnProfile ? event.registrationId : null}
                       hideRegisterButtons={true}
                       onRegistrationUpdate={() => {
                         // Ensure we stay on the registered events tab
                         setActiveTab("registered"); 
                         // Refresh the data
                         fetchData(profileEmail || currentUser?.email);
                       }}
                     />;
                   } catch (error) {
                     console.error(`Error processing registered event ${index}:`, error);
                     return null;
                   }
                })}
              </div>
            ) : (
              <p className="text-muted-foreground">
                {isLoading 
                  ? "Loading registered events..." 
                  : "Not registered for any events."}
              </p>
            )}
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}
