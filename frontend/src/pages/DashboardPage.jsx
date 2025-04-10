import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card.jsx";
import {Avatar, AvatarFallback, AvatarImage} from "@/components/ui/avatar.jsx";
import {Tabs, TabsList, TabsTrigger} from "@/components/ui/tabs.jsx";
import DashboardBorrowRequests from "@/components/dashboard-page/DashboardBorrowRequests.jsx";
import DashboardEvents from "@/components/dashboard-page/DashboardEvents.jsx";
import DashboardGameLibrary from "@/components/dashboard-page/DashboardGameLibrary.jsx";
import DashboardLendingRecord from "@/components/dashboard-page/DashboardLendingRecord.jsx";
import SideMenuBar from "@/components/dashboard-page/SideMenuBar.jsx";
import { Route, Routes } from "react-router-dom";
import { useState, useEffect } from "react"; // Added useEffect for logging
import { useAuth } from "@/context/AuthContext"; // Import useAuth
import { Loader2 } from "lucide-react"; // Import loader

export default function DashboardPage() {
  // Get user, loading, and authReady states from AuthContext
  const { user, loading: authLoading, authReady } = useAuth();
  
  // Add debug logging for the user object
  useEffect(() => {
    // Convert to boolean and log
    const isGameOwner = !!user?.gameOwner;
  }, [user, authReady]);
  
  // Derive userType directly from the user object when available
  // Make sure to convert gameOwner property to boolean with !!
  const userType = user ? (!!user.gameOwner ? "owner" : "player") : null;

  // Error state can be simplified or removed if ProtectedRoute handles redirects
  const [error, setError] = useState(null); // Keep for potential non-auth errors? Or remove.

  // Loading state - also check if auth is ready
  if (authLoading || !authReady) {
    return (
      <div className="flex justify-center items-center min-h-[calc(100vh-100px)]"> {/* Adjust height as needed */}
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="h-16 w-16 animate-spin text-muted-foreground" />
          <p className="text-sm text-muted-foreground">
            {!authReady ? "Preparing your dashboard..." : "Loading..."}
          </p>
        </div>
      </div>
    );
  }

  // Error state: If loading is finished but user is still null,
  // ProtectedRoute should have redirected.
  // We can show a generic message or nothing here as a fallback.
  if (!authLoading && !user) {
     return (
       <div className="flex justify-center items-center min-h-[calc(100vh-100px)] text-destructive">
         <p>Error: User not authenticated or failed to load.</p>
         {/* Or simply return null, relying on ProtectedRoute redirect */}
       </div>
     );
  }
  
  // If we reach here, authLoading is false, authReady is true, and user exists.

  // Render dashboard once data is loaded
  return (
    <div className="flex flex-col">
      <main className="flex py-5 mx-10 justify-between space-x-10">
        <aside className="min-w-[300px]">
          <div className="flex flex-row gap-4">
            <Card className="w-full flex flex-col gap-4">
              <CardHeader className="w-[1/4] flex flex-row items-center gap-4">
                <Avatar className="h-12 w-12">
                  {/* TODO: Add actual user avatar if available (user.avatarUrl?) */}
                  <AvatarImage src={user?.avatarUrl || "/placeholder.svg?height=48&width=48"} alt={user?.name || 'User'}/>
                  {/* Fallback uses initials from user's name */}
                  <AvatarFallback>{user?.name ? user.name.substring(0, 2).toUpperCase() : 'U'}</AvatarFallback>
                </Avatar>
                <div>
                  <CardTitle>{user?.name || 'User'}</CardTitle> {/* Use name from context */}
                  <CardDescription>{userType === "owner" ? "Game Owner" : "Player"}</CardDescription>
                </div>
              </CardHeader>
              <CardContent>
                <nav className="flex flex-col gap-2 pt-4">
                  <SideMenuBar userType={userType}/>
                </nav>
              </CardContent>
            </Card>
          </div>
        </aside>
        <div className="flex-1 w-full min-w-[420px]">
          <Tabs className="bg-background">
            <TabsList className="w-full h-10 mb-2">
              {/* Conditionally render Games tab trigger if user is owner? Or handle inside component */}
              {userType === 'owner' && <TabsTrigger value="games">Game Library</TabsTrigger>}
              <TabsTrigger value="events">Events</TabsTrigger>
              <TabsTrigger value="requests">Borrow Requests</TabsTrigger>
              {userType === 'owner' && <TabsTrigger value="borrowing">Lending History</TabsTrigger>}
            </TabsList>
            {/* Pass fetched userType to child components */}
            {userType === 'owner' && <DashboardGameLibrary userType={userType} />}
            <DashboardEvents userType={userType} />
            <DashboardBorrowRequests userType={userType} />
            {userType === 'owner' && <DashboardLendingRecord userType={userType} />}
          </Tabs>
        </div>
      </main>
    </div>
  )
}
