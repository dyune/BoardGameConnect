import React, { useState } from 'react';
import { Filter, Search, X, Mail, User } from 'lucide-react';
import { Input } from "@/components/ui/input.jsx";
import { Button } from "@/components/ui/button.jsx";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu.jsx";
import { motion, AnimatePresence } from "framer-motion";
import { cn } from "@/components/lib/utils";

const UserSearchBar = ({ 
  searchQuery, 
  setSearchQuery, 
  onSearchSubmit, 
  filterGameOwnersOnly, 
  setFilterGameOwnersOnly,
  searchType = 'all'
}) => {
  const [isFocused, setIsFocused] = useState(false);
  
  // Handler for Enter key press
  const handleKeyDown = (event) => {
    if (event.key === 'Enter') {
      onSearchSubmit();
    }
  };

  // Handle clearing the search input
  const handleClearSearch = () => {
    setSearchQuery('');
    // Optionally trigger a search with empty query
    onSearchSubmit();
  };

  // Determine placeholder text based on search type
  const getPlaceholderText = () => {
    switch(searchType) {
      case 'email':
        return 'Search by email address...';
      case 'name':
        return 'Search by user name...';
      default:
        return 'Search users by name or email...';
    }
  };

  // Determine search icon based on search type
  const SearchIcon = () => {
    switch(searchType) {
      case 'email':
        return <Mail className="h-4 w-4 text-muted-foreground" />;
      case 'name':
        return <User className="h-4 w-4 text-muted-foreground" />;
      default:
        return <Search className="h-4 w-4 text-muted-foreground" />;
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: -10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className={cn(
        "flex w-full items-center gap-3 p-4 rounded-lg",
        "bg-card/80 backdrop-blur-sm",
        "border border-border/50",
        "transition-all duration-200",
        isFocused ? "shadow-md border-primary/30" : "shadow-sm"
      )}
    >
      <div className="relative flex-grow flex items-center">
        <AnimatePresence mode="wait">
          <motion.div 
            key={searchType}
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.8 }}
            transition={{ duration: 0.2 }}
            className="absolute left-3"
          >
            <SearchIcon />
          </motion.div>
        </AnimatePresence>
        <Input
          type="text"
          placeholder={getPlaceholderText()}
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => setIsFocused(true)}
          onBlur={() => setIsFocused(false)}
          className={cn(
            "pl-10 pr-8 transition-all",
            "bg-transparent",
            "h-10",
            isFocused && "ring-1 ring-primary/20"
          )}
        />
        <AnimatePresence>
          {searchQuery && (
            <motion.button
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.8 }}
              transition={{ duration: 0.2 }}
              className="absolute right-3 rounded-full p-1 hover:bg-muted focus:outline-none focus:ring-1 focus:ring-primary"
              onClick={handleClearSearch}
              aria-label="Clear search"
            >
              <X className="h-3 w-3 text-muted-foreground" />
            </motion.button>
          )}
        </AnimatePresence>
      </div>
      
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button 
            variant="outline" 
            size="icon" 
            className={cn(
              "transition-all duration-200",
              filterGameOwnersOnly && "bg-primary/10 text-primary border-primary/20"
            )}
          >
            <Filter className="h-4 w-4" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-56">
          <DropdownMenuLabel>Filter Users</DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuCheckboxItem
            checked={filterGameOwnersOnly}
            onCheckedChange={setFilterGameOwnersOnly}
          >
            Game Owner
          </DropdownMenuCheckboxItem>
          {/* Additional filters can be added here */}
        </DropdownMenuContent>
      </DropdownMenu>
      
      <Button 
        onClick={onSearchSubmit}
        className="transition-all duration-200 hover:shadow-md"
      >
        Search
      </Button>
    </motion.div>
  );
};

export default UserSearchBar;