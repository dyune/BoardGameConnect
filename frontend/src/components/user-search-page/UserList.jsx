import React, { memo } from 'react';
import UserProfileCard from './UserProfileCard';
import { Skeleton } from "@/components/ui/skeleton";
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate, useSearchParams } from "react-router-dom";
import { AlertCircle, UserSearch, Users } from 'lucide-react';

// Staggered animation variants
const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1
    }
  }
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { type: "spring", stiffness: 300, damping: 24 }
  }
};

// Memoized UserProfileCard component for better performance
const MemoizedUserProfileCard = memo(UserProfileCard);

const UserList = ({ users = [], isLoading, error, emptyMessage = "No users found.", onUserClick }) => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const searchQuery = searchParams.get("q");

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {[...Array(6)].map((_, index) => (
          <motion.div 
            key={index} 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.3, delay: index * 0.05 }}
            className="flex flex-col space-y-3"
          >
            <div className="border border-border/50 rounded-xl p-4 bg-card/50">
              <div className="flex items-center gap-4">
                <Skeleton className="h-16 w-16 rounded-full" />
                <div className="space-y-2 flex-1">
                  <Skeleton className="h-4 w-[70%]" />
                  <Skeleton className="h-3 w-[90%]" />
                  <Skeleton className="h-3 w-[40%]" />
                </div>
              </div>
              <div className="mt-4">
                <Skeleton className="h-3 w-[60%] mb-3" />
                <div className="flex gap-2">
                  <Skeleton className="h-6 w-20 rounded-full" />
                  <Skeleton className="h-6 w-20 rounded-full" />
                  <Skeleton className="h-6 w-20 rounded-full" />
                </div>
              </div>
            </div>
          </motion.div>
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="p-8 text-center rounded-lg bg-red-50 border border-red-200 text-red-600 flex flex-col items-center"
      >
        <AlertCircle className="h-8 w-8 mb-2" />
        <p className="font-medium">{error}</p>
      </motion.div>
    );
  }

  if (users.length === 0) {
    return (
      <motion.div 
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="p-8 text-center rounded-lg bg-muted/30 text-muted-foreground flex flex-col items-center py-16"
      >
        <UserSearch className="h-12 w-12 mb-4 text-muted-foreground/70" />
        <p className="text-lg font-medium mb-2">No Results Found</p>
        <p className="max-w-md text-sm">{emptyMessage}</p>
      </motion.div>
    );
  }

  return (
    <AnimatePresence mode="wait">
      <motion.div 
        key={`user-list-${users.length}`}
        className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
        variants={containerVariants}
        initial="hidden"
        animate="visible"
        exit={{ opacity: 0 }}
      >
        {users.map((user) => (
          <motion.div 
            key={user.id || user.email || user.username} 
            variants={itemVariants} 
            layout
            className="h-full"
          >
            <MemoizedUserProfileCard
              user={user}
              onClick={() => onUserClick(user)}
            />
          </motion.div>
        ))}
      </motion.div>
    </AnimatePresence>
  );
};

export default UserList;
