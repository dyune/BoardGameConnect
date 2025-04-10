import { useState } from "react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { GamepadIcon as GameController } from "lucide-react";
import { requestPasswordReset } from "@/service/auth-api";

export default function ForgotPasswordPage() {
  const [isLoading, setIsLoading] = useState(false);
  const [email, setEmail] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError("");
    setSuccess(false);

    try {
      await requestPasswordReset(email);
      setSuccess(true);
    } catch (error) {
      console.error("Error requesting password reset:", error);
      setError("Failed to process your request. Please try again later.");
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
              <GameController className="h-6 w-6 text-primary" />
              <span className="text-xl font-bold">BoardGameConnect</span>
            </div>
          </div>
          <CardTitle className="text-2xl font-bold text-center">Reset your password</CardTitle>
          <CardDescription className="text-center">
            Enter your email and we'll send you instructions to reset your password. If you don't receive an email, please check your spam folder.
          </CardDescription>
        </CardHeader>
        {!success ? (
          <form onSubmit={handleSubmit}>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input 
                  id="email" 
                  type="email" 
                  placeholder="your.email@example.com" 
                  required 
                  value={email} 
                  onChange={(e) => setEmail(e.target.value)} 
                />
              </div>
            </CardContent>
            <CardFooter className="flex flex-col space-y-4 mt-6">
              {error && <p className="text-red-500 text-sm text-center">{error}</p>}
              <Button type="submit" className="w-full" disabled={isLoading}>
                {isLoading ? "Processing..." : "Reset Password"}
              </Button>
              <div className="text-center text-sm">
                Remember your password?{" "}
                <Link to="/login" className="text-primary hover:underline">
                  Sign in
                </Link>
              </div>
            </CardFooter>
          </form>
        ) : (
          <CardContent className="space-y-4 py-6">
            <div className="bg-green-50 border border-green-200 text-green-800 rounded-md p-4 text-center">
              <p>If an account exists with that email, we've sent password reset instructions.</p>
              <p className="mt-2">Please check your email.</p>
            </div>
            <div className="flex justify-center mt-4">
              <Link to="/login">
                <Button variant="outline">Return to Login</Button>
              </Link>
            </div>
          </CardContent>
        )}
      </Card>
    </div>
  );
} 