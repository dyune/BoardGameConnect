"use client"

import { useState, useEffect } from "react" // Add useEffect
import { Link, useNavigate } from "react-router-dom"
import { useAuth } from "@/context/AuthContext"; // Import useAuth
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { GamepadIcon as GameController } from "lucide-react"

export default function RegistrationPage() {
  const navigate = useNavigate();
  const { login, user } = useAuth(); // Get login function and user state
  const [isLoading, setIsLoading] = useState(false);
  const [accountType, setAccountType] = useState("player");
  const [firstName, setFirstName] = useState(""); // State for form inputs
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(""); // State for errors

  // Redirect if already logged in
  useEffect(() => {
    if (user) {
      navigate("/dashboard");
    }
  }, [user, navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError(""); // Clear previous errors

    try {
      // Step 1: Register the user
      // TODO: Replace with environment variable for API URL
      const registrationResponse = await fetch("http://localhost:8080/api/account", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          username: `${firstName} ${lastName}`, // Use state variables
          email,
          password,
          gameOwner: accountType === "owner", // Ensure this properly evaluates based on accountType
        }),
        credentials: 'include', // <<< Send cookies if needed by backend for registration step? Maybe not.
      });

      if (registrationResponse.ok) {
        // Step 2: Log the user in - Backend should automatically set HttpOnly cookie
        // TODO: Replace with environment variable for API URL
        const loginResponse = await fetch("http://localhost:8080/auth/login", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ email, password }), // Use state variables
          credentials: 'include', // <<< Send cookies (important for subsequent requests)
        });

        if (loginResponse.ok) {
          // Backend sets cookie, response body contains user summary
          const userData = await loginResponse.json();

          login(userData); // Update global auth state

          // No need for localStorage/sessionStorage or delays.
          // The useEffect hook above will handle redirection once `user` state updates.
          // navigate("/dashboard"); // Let useEffect handle redirect
        } else {
          const loginErrorText = await loginResponse.text();
          console.error("Login failed after registration:", loginResponse.status, loginErrorText);
          setError("Registration successful, but automatic login failed. Please try logging in manually.");
        }
      } else {
        const errorData = await registrationResponse.text(); // Or response.json() if backend sends structured errors
        console.error("Registration failed:", registrationResponse.status, errorData);
        setError(`Registration failed: ${errorData || registrationResponse.status}`);
      }
    } catch (error) {
      console.error("Error during registration or login:", error);
      setError("Failed to connect to the server. Please try again later.");
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="container flex items-center justify-center min-h-screen py-12">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1">
          <div className="flex justify-center mb-4">
            <div className="flex items-center gap-2">
              <GameController className="h-6 w-6 text-primary" />
              <span className="text-xl font-bold">BoardGameConnect</span>
            </div>
          </div>
          <CardTitle className="text-2xl font-bold text-center">Create an account</CardTitle>
          <CardDescription className="text-center">Join our community of board game enthusiasts</CardDescription>
        </CardHeader>
        <form onSubmit={handleSubmit}>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="first-name">First name</Label>
                <Input id="first-name" required value={firstName} onChange={(e) => setFirstName(e.target.value)} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="last-name">Last name</Label>
                <Input id="last-name" required value={lastName} onChange={(e) => setLastName(e.target.value)} />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" placeholder="our.email@example.com" required value={email} onChange={(e) => setEmail(e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <Input id="password" type="password" required value={password} onChange={(e) => setPassword(e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label>Account type</Label>
              <RadioGroup
                value={accountType}
                onValueChange={setAccountType}
                className="flex flex-col space-y-1"
              >
                <div className="flex items-center space-x-2">
                  <RadioGroupItem value="player" id="player" />
                  <Label htmlFor="player" className="font-normal cursor-pointer">
                    Player (join events and borrow games)
                  </Label>
                </div>
                <div className="flex items-center space-x-2">
                  <RadioGroupItem value="owner" id="owner" />
                  <Label htmlFor="owner" className="font-normal cursor-pointer">
                    Game Owner (share your collection and organize events)
                  </Label>
                </div>
              </RadioGroup>
            </div>
          </CardContent>
          <CardFooter className="flex flex-col space-y-4 mt-4">
            {error && <p className="text-red-500 text-sm text-center">{error}</p>} {/* Display error message */}
            <Button type="submit" className="w-full" disabled={isLoading}>
              {isLoading ? "Creating account..." : "Create account"}
            </Button>
            <div className="text-center text-sm">
              Already have an account?{" "}
              <Link to="/login" className="text-primary hover:underline">
                Sign in
              </Link>
            </div>
          </CardFooter>
        </form>
      </Card>
    </div>
  )
}
