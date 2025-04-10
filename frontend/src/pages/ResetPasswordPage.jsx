import { useState, useEffect } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { GamepadIcon as GameController } from "lucide-react";
import { resetPassword } from "@/service/auth-api";

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [token, setToken] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    // Get token from URL parameters
    const tokenFromUrl = searchParams.get("token");
    if (tokenFromUrl) {
      setToken(tokenFromUrl);
    }
  }, [searchParams]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError("");

    // Validate passwords match
    if (newPassword !== confirmPassword) {
      setError("Passwords do not match");
      setIsLoading(false);
      return;
    }

    // Validate password length
    if (newPassword.length < 8) {
      setError("Password must be at least 8 characters long");
      setIsLoading(false);
      return;
    }

    try {
      await resetPassword(token, newPassword);
      setSuccess(true);
      
      // Redirect to login after 3 seconds
      setTimeout(() => {
        navigate("/login");
      }, 3000);
    } catch (error) {
      console.error("Error resetting password:", error);
      setError("Failed to reset password. The link may be invalid or expired.");
    } finally {
      setIsLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="container flex items-center justify-center min-h-screen py-12">
        <Card className="w-full max-w-md">
          <CardContent className="space-y-4 py-6">
            <div className="bg-red-50 border border-red-200 text-red-800 rounded-md p-4 text-center">
              <p>Invalid or missing reset token.</p>
              <p className="mt-2">Please request a new password reset link.</p>
            </div>
            <div className="flex justify-center mt-4">
              <Link to="/forgot-password">
                <Button>Request New Reset Link</Button>
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    );
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
          <CardTitle className="text-2xl font-bold text-center">Set New Password</CardTitle>
          <CardDescription className="text-center">
            Enter your new password below
          </CardDescription>
        </CardHeader>
        {!success ? (
          <form onSubmit={handleSubmit}>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="newPassword">New Password</Label>
                <Input
                  id="newPassword"
                  type="password"
                  required
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                />
                <p className="text-xs text-muted-foreground">
                  Password must be at least 8 characters long
                </p>
              </div>
              <div className="space-y-2">
                <Label htmlFor="confirmPassword">Confirm Password</Label>
                <Input
                  id="confirmPassword"
                  type="password"
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                />
              </div>
            </CardContent>
            <CardFooter className="flex flex-col space-y-4 mt-6">
              {error && <p className="text-red-500 text-sm text-center">{error}</p>}
              <Button type="submit" className="w-full" disabled={isLoading}>
                {isLoading ? "Processing..." : "Reset Password"}
              </Button>
            </CardFooter>
          </form>
        ) : (
          <CardContent className="space-y-4 py-6">
            <div className="bg-green-50 border border-green-200 text-green-800 rounded-md p-4 text-center">
              <p>Your password has been successfully reset!</p>
              <p className="mt-2">Redirecting to login page...</p>
            </div>
          </CardContent>
        )}
      </Card>
    </div>
  );
} 