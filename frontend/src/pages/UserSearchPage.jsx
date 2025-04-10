import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import UserSearchBar from '../components/user-search-page/UserSearchBar.jsx';
import UserList from '../components/user-search-page/UserList.jsx';
import { getUserInfoByEmail, searchUsers, searchUsersByName, searchUsersByEmail } from '@/service/user-api.js';
import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button.jsx';
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs.jsx";
import { motion } from 'framer-motion';
import { toast } from 'sonner';
import { Search } from 'lucide-react';

function UserSearchPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [searchQuery, setSearchQuery] = useState(searchParams.get('q') || '');
  const [searchResults, setSearchResults] = useState([]);
  const [isLoadingSearch, setIsLoadingSearch] = useState(false);
  const [searchError, setSearchError] = useState(null);
  const [filterGameOwnersOnly, setFilterGameOwnersOnly] = useState(
    searchParams.get('gameOwner') === 'true'
  );
  const [searchType, setSearchType] = useState(searchParams.get('type') || 'all');

  // Function to handle search by email and name
  const handleSearch = async (query) => {
    if (!query || query.trim() === '') {
      setSearchResults([]);
      setSearchError(null);
      setIsLoadingSearch(false);
      return;
    }
    
    setIsLoadingSearch(true);
    setSearchError(null);
    setSearchResults([]);

    try {
      let results = [];
      
      // Search based on the selected type and query
      switch (searchType) {
        case 'email':
          // Check if it's an exact email search or a partial search
          if (query.includes('@') && !query.includes('*') && !query.includes('%')) {
            // Exact email search
            try {
              const emailResult = await getUserInfoByEmail(query);
              if (emailResult) {
                results.push({
                  id: emailResult.id,
                  username: emailResult.name,
                  email: emailResult.email,
                  isGameOwner: emailResult.gameOwner,
                  gamesPlayed: emailResult.gamesPlayed || [],
                  gamesOwned: emailResult.gamesOwned || [],
                  avatarUrl: "/placeholder.svg?height=48&width=48"
                });
              }
            } catch (error) {
              console.log('Exact email search failed:', error);
            }
          } else {
            // Partial email search
            const emailResults = await searchUsersByEmail(query);
            if (emailResults && emailResults.length > 0) {
              results = emailResults.map(user => ({
                id: user.id,
                username: user.name,
                email: user.email,
                isGameOwner: user.gameOwner,
                gamesPlayed: user.gamesPlayed || [],
                gamesOwned: user.gamesOwned || [],
                avatarUrl: "/placeholder.svg?height=48&width=48"
              }));
            }
          }
          break;
          
        case 'name':
          // Search by name
          const nameResults = await searchUsersByName(query);
          if (nameResults && nameResults.length > 0) {
            results = nameResults.map(user => ({
              id: user.id,
              username: user.name,
              email: user.email,
              isGameOwner: user.gameOwner,
              gamesPlayed: user.gamesPlayed || [],
              gamesOwned: user.gamesOwned || [],
              avatarUrl: "/placeholder.svg?height=48&width=48"
            }));
          }
          break;
          
        case 'all':
        default:
          // Search by both name and email
          const combinedResults = await searchUsers({ 
            term: query,
            gameOwnerOnly: filterGameOwnersOnly
          });
          
          if (combinedResults && combinedResults.length > 0) {
            results = combinedResults.map(user => ({
              id: user.id,
              username: user.name,
              email: user.email,
              isGameOwner: user.gameOwner,
              gamesPlayed: user.gamesPlayed || [],
              gamesOwned: user.gamesOwned || [],
              avatarUrl: "/placeholder.svg?height=48&width=48"
            }));
          }
          break;
      }
      
      // Filter by game owner if requested
      if (filterGameOwnersOnly) {
        results = results.filter(user => user.isGameOwner);
      }
      
      setSearchResults(results);
      setSearchError(null);
    } catch (error) {
      console.error("User search failed:", error);
      setSearchResults([]);
      setSearchError('Search failed. Please try again.');
    } finally {
      setIsLoadingSearch(false);
    }
  };

  // Load initial search if query param exists
  useEffect(() => {
    const initialQuery = searchParams.get('q');
    const initialType = searchParams.get('type');
    
    if (initialType) {
      setSearchType(initialType);
    }
    
    if (initialQuery) {
      setSearchQuery(initialQuery);
    }
    
    setFilterGameOwnersOnly(searchParams.get('gameOwner') === 'true');
  }, []);

  // Effect for debounced search
  useEffect(() => {
    const debounceTimer = setTimeout(() => {
      if (searchQuery.trim() !== '') {
        handleSearch(searchQuery);
      } else {
        setSearchResults([]);
        setSearchError(null);
      }
    }, 500);

    return () => clearTimeout(debounceTimer);
  }, [searchQuery, searchType, filterGameOwnersOnly]);

  // Update URL when search parameters change
  useEffect(() => {
    const params = new URLSearchParams(searchParams);
    if (searchQuery) {
      params.set('q', searchQuery);
    } else {
      params.delete('q');
    }
    
    if (searchType !== 'all') {
      params.set('type', searchType);
    } else {
      params.delete('type');
    }
    
    if (filterGameOwnersOnly) {
      params.set('gameOwner', 'true');
    } else {
      params.delete('gameOwner');
    }
    
    setSearchParams(params, { replace: true });
  }, [searchQuery, searchType, filterGameOwnersOnly]);

  const handleUserCardClick = (user) => {
    if (user && user.email) {
      navigate(`/profile?email=${encodeURIComponent(user.email)}`);
    } else {
      console.error("Cannot navigate to profile: user email missing.", user);
      toast.error("Could not open user profile.");
    }
  };

  const handleSearchSubmit = () => {
    if (searchQuery.trim() !== '') {
      handleSearch(searchQuery);
    }
  };

  const handleSearchTypeChange = (value) => {
    setSearchType(value);
  };

  return (
    <motion.div 
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.5 }}
      className="container p-8 space-y-8"
    >
      {/* Header */}
      <div className="text-center mb-6">
        <h1 className="text-3xl font-bold tracking-tight mb-2">Find Users</h1>
        <p className="text-muted-foreground">Search for other users by name or email</p>
      </div>
      
      {/* Search Type Tabs */}
      <Tabs 
        defaultValue={searchType} 
        className="w-full md:w-3/4 lg:w-2/3 mx-auto"
        onValueChange={handleSearchTypeChange}
      >
        <TabsList className="grid grid-cols-3 mb-4">
          <TabsTrigger value="all">All</TabsTrigger>
          <TabsTrigger value="name">By Name</TabsTrigger>
          <TabsTrigger value="email">By Email</TabsTrigger>
        </TabsList>
      </Tabs>

      {/* Search Bar Area */}
      <div className="w-full md:w-3/4 lg:w-2/3 mx-auto">
        <UserSearchBar
          searchQuery={searchQuery}
          setSearchQuery={setSearchQuery}
          onSearchSubmit={handleSearchSubmit}
          filterGameOwnersOnly={filterGameOwnersOnly}
          setFilterGameOwnersOnly={setFilterGameOwnersOnly}
          searchType={searchType}
        />
        <p className="text-sm text-muted-foreground mt-2 text-center">
          {searchType === 'email' 
            ? 'Search by user email address' 
            : searchType === 'name' 
              ? 'Search by user name' 
              : 'Search by user name or email address'}
        </p>
      </div>

      {/* Search Results Area */}
      {(searchQuery.trim() !== '' || searchResults.length > 0) && (
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.1 }}
          className="mt-12"
        >
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-2xl font-semibold tracking-tight">Search Results</h2>
            {!isLoadingSearch && searchResults.length > 0 && (
              <span className="text-sm text-muted-foreground">
                Found {searchResults.length} user{searchResults.length !== 1 ? 's' : ''}
              </span>
            )}
          </div>
          <UserList
            users={searchResults}
            isLoading={isLoadingSearch}
            error={searchError}
            emptyMessage={searchQuery ? `No users found matching "${searchQuery}"` : "Enter a search term to find users"}
            onUserClick={handleUserCardClick}
          />
        </motion.div>
      )}

      {/* Empty State */}
      {searchQuery.trim() === '' && searchResults.length === 0 && !isLoadingSearch && (
        <motion.div 
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.5, delay: 0.2 }}
          className="flex flex-col items-center justify-center py-16 text-center"
        >
          <div className="rounded-full bg-primary/10 p-4 mb-4">
            <Search className="h-8 w-8 text-primary" />
          </div>
          <h3 className="text-xl font-medium mb-2">Search for Users</h3>
          <p className="text-muted-foreground max-w-md">
            Enter a name or email address to find users in the system.
            Click on a user card to view their profile.
          </p>
        </motion.div>
      )}
    </motion.div>
  );
}

export default UserSearchPage;
