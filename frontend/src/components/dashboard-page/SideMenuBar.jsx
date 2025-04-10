"use client"

import { useState, useContext, useEffect } from "react"
import { Button } from "@/components/ui/button";
import { useAuth } from "@/context/AuthContext.jsx";
import { upgradeAccountToGameOwner, updateUsernamePassword } from '@/service/dashboard-api.js';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input.jsx"
import { Label } from "@/components/ui/label.jsx"
import { Settings, User, KeyRound, Crown } from "lucide-react"
import { motion } from "framer-motion"

export default function SideMenuBar({ userType }) {
  const { user, isAuthenticated } = useAuth();
  const [open, setOpen] = useState(false);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isUpgrading, setIsUpgrading] = useState(false);
  const [upgradeError, setUpgradeError] = useState(null);
  const [upgradeSuccess, setUpgradeSuccess] = useState(false);
  const [isSavingSettings, setIsSavingSettings] = useState(false);
  const [settingsError, setSettingsError] = useState(null);
  const [isGameOwner, setIsGameOwner] = useState(false);
  
  async function handleSettingsSubmit(e) {
    e.preventDefault();
    setSettingsError(null);

    if (newPassword && newPassword !== confirmPassword) {
      setSettingsError("Passwords don't match");
      return;
    }

    setIsSavingSettings(true);

    const updateData = {};
    updateData.email = localStorage.getItem("userEmail");
    if (username.trim()) {
      updateData.username = username.trim();
    }
    updateData.password = password;
    if (newPassword) {
      updateData.newPassword = newPassword;
    }

    if (Object.keys(updateData).length === 0) {
      setIsSavingSettings(false);
      setOpen(false);
      return;
    }

    try {
      await updateUsernamePassword(updateData);
      setOpen(false);
      setUsername("");
      setPassword("");
      setConfirmPassword("");
    } catch (err) {
      console.error("Failed to update account settings:", err);
      setSettingsError(err.message || "Failed to update settings. Please try again.");
    } finally {
      setIsSavingSettings(false);
    }
  }

  const handleUpgrade = async () => {
    if (!isAuthenticated || !user?.email) {
      setUpgradeError("You must be logged in to perform this action.");
      return;
    }

    setIsUpgrading(true);
    setUpgradeError(null);
    setUpgradeSuccess(false);

    try {
      // If user is already a game owner, just update the state
      if (user?.gameOwner || user?.role === 'GAME_OWNER') {
        setUpgradeSuccess(true);
        setIsGameOwner(true);
        localStorage.setItem('userRole', 'GAME_OWNER');
        return;
      }

      console.log("Attempting to upgrade account for:", user.email);
      await upgradeAccountToGameOwner(user.email);
      setUpgradeSuccess(true);
    } catch (err) {
      console.error("Failed to upgrade account:", err);
      setUpgradeError(err.message || "Failed to upgrade account. Please try again.");
    } finally {
      setIsUpgrading(false);
    }
  };

  return (
    <>
      {isAuthenticated && !(user?.gameOwner || user?.role === 'GAME_OWNER') && (
        <>
          <Button 
            variant="ghost" 
            className="w-full justify-start gap-2 relative overflow-hidden" 
            onClick={handleUpgrade} 
            disabled={isUpgrading || upgradeSuccess}
          >
            {!upgradeSuccess ? (
              <>
                <User className="h-4 w-4" />
                {isUpgrading ? "Upgrading..." : "Become a Game Owner"}
              </>
            ) : (
              <motion.div 
                className="flex items-center gap-2 w-full" 
                initial={{ scale: 0.5, opacity: 0 }}
                animate={{ 
                  scale: 1, 
                  opacity: 1,
                  transition: { duration: 0.5 }
                }}
              >
                <motion.div
                  initial={{ y: -20, opacity: 0 }}
                  animate={{ 
                    y: 0, 
                    opacity: 1,
                    transition: { delay: 0.3, duration: 0.5 }
                  }}
                >
                  <Crown className="h-5 w-5 text-yellow-500" />
                </motion.div>
                <motion.span
                  initial={{ x: -20, opacity: 0 }}
                  animate={{ 
                    x: 0, 
                    opacity: 1,
                    transition: { delay: 0.5, duration: 0.5 }
                  }}
                  className="text-green-500 font-semibold"
                >
                  Upgrade Successful!
                </motion.span>
              </motion.div>
            )}
            
            {upgradeSuccess && (
              <motion.div 
                className="absolute inset-0 bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 opacity-20"
                initial={{ opacity: 0 }}
                animate={{ 
                  opacity: [0, 0.2, 0.1],
                  transition: { 
                    duration: 1.5,
                    repeat: 1,
                    repeatType: "reverse"
                  }
                }}
              />
            )}
          </Button>
          
          {upgradeError && (
            <motion.p 
              className="text-red-500 text-xs px-4 py-1"
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3 }}
            >
              {upgradeError}
            </motion.p>
          )}
          
          {upgradeSuccess && (
            <motion.p 
              className="text-green-500 text-xs px-4 py-1"
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3 }}
            >
              Please log out and log back in to see your new privileges!
            </motion.p>
          )}
        </>
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogTrigger asChild>
          <Button variant="ghost" className="w-full justify-start gap-2">
            <Settings className="h-4 w-4" />
            Account Settings
          </Button>
        </DialogTrigger>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>Account Settings</DialogTitle>
            <DialogDescription>
              Update your account information. Leave fields blank to keep current values. Click save when you're done.
              {settingsError && <p className="text-red-500 text-sm mt-2">{settingsError}</p>}
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleSettingsSubmit}>
            <div className="grid gap-4 py-4">
              <div className="grid gap-2">
                <Label htmlFor="username">
                  <User className="h-4 w-4 inline mr-2"/>
                  Username
                </Label>
                <Input
                  id="username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="Enter new username"
                />
              </div>

              <div className="grid gap-2">
                <Label htmlFor="current-password">
                  <KeyRound className="h-4 w-4 inline mr-2"/>
                  Current Password
                </Label>
                <Input
                  id="current-password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter current password"
                />
              </div>

              <div className="grid gap-2">
                <Label htmlFor="new-password">
                  <KeyRound className="h-4 w-4 inline mr-2"/>
                  New Password
                </Label>
                <Input
                  id="new-password"
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Enter new password"
                />
                <p className="text-xs text-muted-foreground">Leave blank if you don't want to change your password</p>
              </div>

              {newPassword && (
                <div className="grid gap-2">
                  <Label htmlFor="confirmPassword">Confirm Password</Label>
                  <Input
                    id="confirmPassword"
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Confirm new password"
                  />
                </div>
              )}
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setOpen(false)} disabled={isSavingSettings}>
                Cancel
              </Button>
              <Button type="submit" disabled={isSavingSettings}>
                {isSavingSettings ? "Saving..." : "Save changes"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </>
  )
}