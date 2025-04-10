import { Link, useLocation, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button.jsx";
import { useEffect, useState, useCallback } from "react";
import { useAuth } from "@/context/AuthContext";
import apiClient from "@/service/apiClient"; // Import apiClient
import { BellIcon, User, Moon, Sun } from "lucide-react"; // Added Moon and Sun icons
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"; // Added DropdownMenu imports
import {
  Menubar,
  MenubarContent,
  MenubarItem,
  MenubarMenu,
  MenubarTrigger,
} from "@/components/ui/menubar"; // Kept Menubar imports
import { getGameById } from "@/service/game-api.js"; // Import function to fetch game details
import white_logo from '../../assets/logo/white-logo.svg'; // Import logo image

// Main menu component shown on all pages
export default function MenuBar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, isAuthenticated, logout, loading } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isDarkMode, setIsDarkMode] = useState(false);

  // Initialize theme from localStorage on component mount
  useEffect(() => {
    const savedTheme = localStorage.getItem('theme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    
    if (savedTheme === 'dark' || (!savedTheme && prefersDark)) {
      setIsDarkMode(true);
      document.documentElement.classList.add('dark');
    } else {
      setIsDarkMode(false);
      document.documentElement.classList.remove('dark');
    }
  }, []);

  // Toggle dark mode
  const toggleDarkMode = () => {
    setIsDarkMode(prevMode => {
      const newMode = !prevMode;
      if (newMode) {
        document.documentElement.classList.add('dark');
        localStorage.setItem('theme', 'dark');
      } else {
        document.documentElement.classList.remove('dark');
        localStorage.setItem('theme', 'light');
      }
      return newMode;
    });
  };

  // Helper function to get read notification IDs from localStorage
  const getReadNotifications = useCallback(() => {
    const readNotificationsStr = localStorage.getItem('readNotifications');
    return readNotificationsStr ? JSON.parse(readNotificationsStr) : [];
  }, []);

  // Helper function to save read notification IDs to localStorage
  const saveReadNotifications = useCallback((notificationIds) => {
    localStorage.setItem('readNotifications', JSON.stringify(notificationIds));
  }, []);

  // Fetch notifications when user is authenticated
  useEffect(() => {
    if (isAuthenticated && user) {
      fetchNotifications();

      // Set up polling for new notifications every 30 seconds
      const interval = setInterval(fetchNotifications, 30000);
      return () => clearInterval(interval);
    }
  }, [isAuthenticated, user]);

  // Function to fetch borrow request notifications
  const fetchNotifications = async () => {
    try {
      if (!user?.id) return;

      // For game owners: fetch their games' requests using apiClient
      const ownerRequests = await apiClient('/borrowrequests', {
        method: 'GET',
        requiresAuth: true,
        skipPrefix: false // Assuming endpoint is /api/borrowrequests
      });
      
      // Debug the response to see what data we're getting
      console.log('Borrow requests (owner):', ownerRequests);
      
      // For game owners: we'll only show PENDING requests that need their attention
      // Since owners don't need to be notified about actions they took themselves
      const pendingRequests = ownerRequests.filter(req => {
        // Only include PENDING requests that need owner's attention
        return req.status === 'PENDING';
      });

      // If user is a requester, get their requests too using apiClient
      const requesterRequests = await apiClient(`/borrowrequests/requester/${user.id}`, {
        method: 'GET',
        requiresAuth: true,
        skipPrefix: false // Assuming endpoint is /api/borrowrequests/requester/{id}
      });
      
      // Debug the response to see what data we're getting
      console.log('Borrow requests (requester):', requesterRequests);

      // For requesters: we'll only show recently APPROVED or DECLINED requests
      const recentStatusChanges = requesterRequests.filter(req => {
        // Only include APPROVED or DECLINED statuses
        if (req.status !== 'APPROVED' && req.status !== 'DECLINED') return false;
        
        // Only include recent status changes (last 7 days)
        const updateDate = new Date(req.updateDate || req.requestDate);
        const sevenDaysAgo = new Date();
        sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
        return updateDate > sevenDaysAgo;
      });

      // Create a set to track all game IDs we need to fetch
      const gameIds = new Set();
      
      // Track game IDs from pending owner requests
      pendingRequests.forEach(req => {
        // Use requestedGameId which is the field in the DTO
        if (req.requestedGameId) gameIds.add(req.requestedGameId);
      });
      
      // Track game IDs from requester requests with status changes
      recentStatusChanges.forEach(req => {
        // Use requestedGameId which is the field in the DTO
        if (req.requestedGameId) gameIds.add(req.requestedGameId);
      });
      
      // Fetch game details for all IDs
      const gameDetailsMap = {};
      await Promise.all([...gameIds].map(async (gameId) => {
        try {
          const gameDetails = await getGameById(gameId);
          gameDetailsMap[gameId] = gameDetails;
        } catch (err) {
          console.error(`Failed to fetch game details for ID ${gameId}:`, err);
        }
      }));

      // Get already read notification IDs from localStorage
      const readNotificationIds = getReadNotifications();

      // Combine notifications with game names and mark previously read notifications
      const allNotifications = [
        ...pendingRequests.map(req => {
          // For owners: Show notifications about pending requests that need action
          let gameName = null;
          
          // Try multiple possible sources for game name
          if (req.gameName) {
            gameName = req.gameName;
          } 
          else if (req.game && req.game.name) {
            gameName = req.game.name;
          }
          else if (req.requestedGameId && gameDetailsMap[req.requestedGameId]?.name) {
            gameName = gameDetailsMap[req.requestedGameId].name;
          }
          else if (req.requestedGame && req.requestedGame.name) {
            gameName = req.requestedGame.name;
          }
          else {
            gameName = "Unknown Game";
          }
          
          const notificationId = `owner-${req.id}`;
          return {
            id: notificationId,
            message: `New request for game ${gameName} needs your attention`,
            status: req.status,
            date: new Date(req.requestDate),
            read: readNotificationIds.includes(notificationId) // Check if already read
          };
        }),
        ...recentStatusChanges.map(req => {
          // For requesters: Show notifications about status changes to their requests
          let gameName = null;
          
          // Try multiple possible sources for game name
          if (req.gameName) {
            gameName = req.gameName;
          } 
          else if (req.game && req.game.name) {
            gameName = req.game.name;
          }
          else if (req.requestedGameId && gameDetailsMap[req.requestedGameId]?.name) {
            gameName = gameDetailsMap[req.requestedGameId].name;
          }
          else if (req.requestedGame && req.requestedGame.name) {
            gameName = req.requestedGame.name;
          }
          else {
            gameName = "Unknown Game";
          }
          
          const notificationId = `requester-${req.id}`;
          return {
            id: notificationId,
            message: `Your request for game ${gameName} was ${req.status.toLowerCase()}`,
            status: req.status,
            date: new Date(req.updateDate || req.requestDate),
            read: readNotificationIds.includes(notificationId) // Check if already read
          };
        })
      ];

      // Sort by date (newest first)
      allNotifications.sort((a, b) => b.date - a.date);
      
      setNotifications(allNotifications);
      setUnreadCount(allNotifications.filter(n => !n.read).length);
    } catch (error) {
      console.error("Error fetching notifications:", error);
    }
  };

  // Mark all notifications as read
  const markAllAsRead = () => {
    // Update notification state
    const updatedNotifications = notifications.map(n => ({...n, read: true}));
    setNotifications(updatedNotifications);
    setUnreadCount(0);
    
    // Save all notification IDs as read to localStorage
    const notificationIds = notifications.map(n => n.id);
    saveReadNotifications([...getReadNotifications(), ...notificationIds]);
  };

  // Mark a single notification as read
  const markAsRead = (notificationId) => {
    // Update notification state
    const updatedNotifications = notifications.map(n => 
      n.id === notificationId ? {...n, read: true} : n
    );
    setNotifications(updatedNotifications);
    setUnreadCount(updatedNotifications.filter(n => !n.read).length);
    
    // Save notification ID as read to localStorage
    const readNotificationIds = getReadNotifications();
    if (!readNotificationIds.includes(notificationId)) {
      saveReadNotifications([...readNotificationIds, notificationId]);
    }
  };

  const handleLogout = async () => {
    await logout();
    navigate("/");
  };

  // Render the actual menu bar, including navigation and user actions
  return (
    <header className="bg-white dark:bg-sidebar border-b shadow-sm dark:border-sidebar-border">
      <div className="flex items-center justify-between py-4 px-6 md:px-10 max-w-screen-xl mx-auto">
        
          <Link to="/" className="flex items-center gap-2">
            <img
              src={isDarkMode 
                ? white_logo
                : "https://www.svgrepo.com/show/83116/board-games-set.svg"}
              alt="Board Games Icon"
              className="w-10 h-10"
            />
            <span className="text-xl font-bold dark:text-sidebar-foreground">BoardGameConnect</span>
          </Link>

          {/* Right-side navigation options */}
        <div className="flex items-center gap-3">
          {/* Links shown only when user is logged in */}
          {isAuthenticated && (
            <div className="flex items-center gap-2">
              <Link to="/dashboard">
                <Button
                  variant="ghost"
                  className="text-sm font-semibold dark:text-sidebar-foreground dark:hover:bg-sidebar-accent"
                  title="Go to your main dashboard"
                >
                  Dashboard
                </Button>
              </Link>

              <Link to="/events">
                <Button
                  variant="ghost"
                  className="text-sm font-semibold dark:text-sidebar-foreground dark:hover:bg-sidebar-accent"
                  title="View and manage events"
                >
                  Events
                </Button>
              </Link>

              <Link to="/games">
                <Button
                  variant="ghost"
                  className="text-sm font-semibold dark:text-sidebar-foreground dark:hover:bg-sidebar-accent"
                  title="Find and explore games"
                >
                  Game Search
                </Button>
              </Link>

              <Link to="/user-search">
                <Button
                  variant="ghost"
                  className="text-sm font-semibold dark:text-sidebar-foreground dark:hover:bg-sidebar-accent"
                  title="Browse and view other users"
                >
                  Users
                </Button>
              </Link>
            </div>
          )}

          {/* Show login/signup buttons when not logged in */}
          {!isAuthenticated && !loading && (
            <>
              <Link to="/login">
                <Button variant="outline" className="text-sm px-4 dark:border-sidebar-border dark:text-sidebar-foreground">
                  Login
                </Button>
              </Link>
              <Link to="/register">
                <Button className="text-sm px-4 dark:bg-sidebar-primary dark:text-sidebar-primary-foreground">Sign Up</Button>
              </Link>
            </>
          )}
            {/* Hide dark mode toggle on landing page */}
            {location.pathname !== "/" && (
            <Button 
              variant="ghost" 
              className="w-10 h-10 p-0 rounded-full"
              onClick={toggleDarkMode}
              aria-label={isDarkMode ? "Switch to light mode" : "Switch to dark mode"}
            >
              {isDarkMode ? (
              <Sun className="h-5 w-5 text-sidebar-foreground" />
              ) : (
              <Moon className="h-5 w-5" />
              )}
            </Button>
            )}
          {/* Dark Mode Toggle */}

          {/* Notifications */}
          {isAuthenticated && (
            <Menubar className="border-none shadow-none bg-transparent">
              <MenubarMenu>
                <MenubarTrigger className="focus:bg-gray-100 hover:bg-gray-100 dark:hover:bg-sidebar-accent dark:focus:bg-sidebar-accent rounded-full p-2 relative">
                  <BellIcon className="w-5 h-5 text-gray-700 dark:text-sidebar-foreground" />
                  {unreadCount > 0 && (
                    <span className="absolute -top-1 -right-1 text-xs bg-red-500 text-white rounded-full w-5 h-5 flex items-center justify-center text-[10px] font-bold">
                      {unreadCount > 9 ? '9+' : unreadCount}
                    </span>
                  )}
                </MenubarTrigger>
                <MenubarContent className="w-80 max-h-[400px] overflow-auto dark:bg-sidebar dark:border-sidebar-border" align="end">
                  <div className="py-2 px-3 bg-gray-50 dark:bg-sidebar-accent border-b dark:border-sidebar-border flex justify-between items-center">
                    <h3 className="font-medium text-sm dark:text-sidebar-foreground">Notifications</h3>
                    {notifications.length > 0 && (
                      <Button 
                        variant="ghost" 
                        className="h-7 text-xs dark:text-sidebar-foreground dark:hover:bg-sidebar"
                        onClick={markAllAsRead}
                      >
                        Mark all read
                      </Button>
                    )}
                  </div>
                  
                  {notifications.length === 0 ? (
                    <div className="py-6 text-center text-gray-500 dark:text-sidebar-foreground/70">
                      <p className="text-sm">No notifications</p>
                    </div>
                  ) : (
                    <>
                      {notifications.map((notification) => (
                        <MenubarItem 
                          key={notification.id} 
                          className={`px-3 py-2 cursor-default ${
                            notification.read 
                              ? 'bg-white dark:bg-sidebar' 
                              : 'bg-blue-50 dark:bg-sidebar-accent/50'
                          }`}
                          onClick={() => markAsRead(notification.id)}
                        >
                          <div className="flex items-start gap-2">
                            <div className={`w-2 h-2 mt-1.5 rounded-full flex-shrink-0 ${
                              notification.status === 'APPROVED' ? 'bg-green-500' : 
                              notification.status === 'DECLINED' ? 'bg-red-500' : 'bg-blue-500'
                            }`}/>
                            <div className="flex-1">
                              <p className="text-sm dark:text-sidebar-foreground">{notification.message}</p>
                              <p className="text-xs text-gray-500 dark:text-sidebar-foreground/70 mt-1">
                                {notification.date.toLocaleDateString()} at {notification.date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                              </p>
                            </div>
                          </div>
                        </MenubarItem>
                      ))}
                    </>
                  )}
                </MenubarContent>
              </MenubarMenu>
            </Menubar>
          )}

          {/* Logout Button (only if logged in) */}
          {isAuthenticated && (
            <div className="pl-3 border-l border-gray-200 dark:border-sidebar-border">
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" className="relative h-8 w-8 rounded-full">
                    {/* Placeholder for User Avatar - using User icon for now */}
                    <User className="h-5 w-5 dark:text-sidebar-foreground" />
                    <span className="sr-only">Toggle user menu</span>
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent className="w-56 dark:bg-sidebar dark:border-sidebar-border" align="end" forceMount>
                  <DropdownMenuLabel className="font-normal">
                    <div className="flex flex-col space-y-1">
                      <p className="text-sm font-medium leading-none dark:text-sidebar-foreground">{user?.username || "User"}</p>
                      <p className="text-xs leading-none text-muted-foreground dark:text-sidebar-foreground/70">
                        {user?.email || "No email"}
                      </p>
                    </div>
                  </DropdownMenuLabel>
                  <DropdownMenuSeparator className="dark:border-sidebar-border" />
                  <DropdownMenuItem asChild>
                    <Link to="/profile" className="cursor-pointer dark:text-sidebar-foreground dark:focus:bg-sidebar-accent">
                      My Profile
                    </Link>
                  </DropdownMenuItem>
                  {/* Add other items like Settings, etc. here if needed */}
                  <DropdownMenuSeparator className="dark:border-sidebar-border" />
                  <DropdownMenuItem onClick={handleLogout} className="cursor-pointer text-red-600 focus:text-red-600 focus:bg-red-50 dark:text-red-400 dark:focus:text-red-400 dark:focus:bg-red-900/20">
                    Log out
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}