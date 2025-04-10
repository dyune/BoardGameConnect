"use client"

import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { loginUser } from '../service/auth-api';
import { Loader2, GamepadIcon } from 'lucide-react';

// Import Shadcn UI components
import { Button } from "../components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "../components/ui/card";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Checkbox } from "../components/ui/checkbox";
import { Alert, AlertDescription } from "../components/ui/alert";

const LoginPage = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(() => {
    // Initialize from localStorage if available, default to false
    return localStorage.getItem('rememberMe') === 'true';
  });
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isAuthenticated, updateActivity } = useAuth();
  
  // Get the redirect URL from query parameters if available
  const searchParams = new URLSearchParams(location.search);
  const redirectTo = searchParams.get('redirect') || '/dashboard';
  
  // If already authenticated, redirect to dashboard
  useEffect(() => {
    if (isAuthenticated) {
      navigate(redirectTo, { replace: true });
    }
  }, [isAuthenticated, navigate, redirectTo]);
  
  // Handle checkbox change specifically (to ensure boolean conversion)
  const handleRememberMeChange = (checked) => {
    const boolValue = Boolean(checked);
    console.log('Setting rememberMe to:', boolValue);
    setRememberMe(boolValue);
    // Store in localStorage for persistence across page reloads
    localStorage.setItem('rememberMe', boolValue ? 'true' : 'false');
  };
  
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!email.trim() || !password.trim()) {
      setError('Email and password are required');
      return;
    }
    
    try {
      setIsLoading(true);
      setError('');
      
      console.log('Login: Using rememberMe value:', rememberMe);
      
      // Call the login API with explicit rememberMe value
      const userData = await loginUser(email, password, rememberMe);
      
      // Update activity timestamp
      updateActivity();
      
      // Update auth context with user data and remember me preference
      await login(userData, rememberMe);
      
      // Redirect to the target page
      navigate(redirectTo, { replace: true });
    } catch (err) {
      // Handle specific error cases
      if (err.status === 401) {
        setError('Invalid email or password');
      } else if (err.status === 0) {
        setError('Cannot connect to server. Please try again later.');
      } else {
        setError(err.message || 'An error occurred during login');
      }
    } finally {
      setIsLoading(false);
    }
  };
  
  return (
    <div className="container flex items-center justify-center min-h-screen py-12">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1">
          <div className="flex justify-center mb-4">
            <div className="flex items-center gap-2">
              <GamepadIcon className="h-6 w-6 text-primary" />
              <span className="text-xl font-bold">BoardGameConnect</span>
            </div>
          </div>
          <CardTitle className="text-2xl font-bold text-center">Welcome back</CardTitle>
          <CardDescription className="text-center">Enter your credentials to access your account</CardDescription>
        </CardHeader>
        
        <form onSubmit={handleSubmit}>
          <CardContent className="space-y-4">
            {error && (
              <Alert variant="destructive" className="text-sm">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input 
                id="email" 
                type="email" 
                placeholder="your.email@example.com" 
                required 
                value={email} 
                onChange={(e) => setEmail(e.target.value)}
                disabled={isLoading}
              />
            </div>
            
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="password">Password</Label>
                <Link to="/forgot-password" className="text-sm text-primary hover:underline">
                  Forgot password?
                </Link>
              </div>
              <Input 
                id="password" 
                type="password" 
                required 
                value={password} 
                onChange={(e) => setPassword(e.target.value)}
                disabled={isLoading}
              />
            </div>
            
            <div className="flex items-center space-x-2">
              <Checkbox 
                id="rememberMe" 
                checked={rememberMe}
                onCheckedChange={handleRememberMeChange}
                disabled={isLoading}
              />
              <Label htmlFor="rememberMe" className="text-sm font-medium leading-none cursor-pointer">
                Remember me
              </Label>
            </div>
          </CardContent>
          
          <CardFooter className="flex flex-col space-y-4 pt-6">
            <Button type="submit" className="w-full" disabled={isLoading}>
              {isLoading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Signing in...
                </>
              ) : (
                "Sign in"
              )}
            </Button>
            
            <div className="text-center text-sm">
              Don&apos;t have an account?{" "}
              <Link to="/register" className="text-primary hover:underline">
                Sign up
              </Link>
            </div>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
};

export default LoginPage;
