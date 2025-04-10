import { useState } from 'react'
import './App.css'
import {BrowserRouter as Router, Route, Routes} from "react-router-dom";
import LandingPage from "./pages/LandingPage.jsx";
import EventsPage from "./pages/EventsPage.jsx";
import GameSearchPage from "./pages/GameSearchPage.jsx";
import LoginPage from "./pages/LoginPage.jsx";
import RegistrationPage from "./pages/RegistrationPage.jsx";
import ForgotPasswordPage from "./pages/ForgotPasswordPage.jsx";
import ResetPasswordPage from "./pages/ResetPasswordPage.jsx";
import UserProfilePage from "./pages/UserProfilePage.jsx";
import UserSearchPage from "./pages/UserSearchPage.jsx";
import DashboardPage from "./pages/DashboardPage.jsx";
import MenuBar from "./components/menubar/MenuBar.jsx";
import ProtectedRoute from "./components/common/ProtectedRoute.jsx"; // Import ProtectedRoute
import PublicRoute from "./components/common/PublicRoute.jsx"; // Import PublicRoute for public pages with auth elements
import SessionExpirationHandler from "./components/common/SessionExpirationHandler.jsx";
import { AuthProvider } from "./context/AuthContext.jsx";
import { Toaster } from 'sonner'; // Added from origin/dev-Yessine-D3

function App() {
  return (
    <AuthProvider>
      <div className="flex flex-col min-h-screen mx-auto">
        <MenuBar />
        <SessionExpirationHandler />
        <Routes>
          {/* Public Routes - Accessible to everyone */}
          <Route path="/"               element={<LandingPage />} />
          <Route path="/login"          element={<LoginPage />}/>
          <Route path="/register"       element={<RegistrationPage />}/>
          <Route path="/forgot-password" element={<ForgotPasswordPage />}/>
          <Route path="/reset-password" element={<ResetPasswordPage />}/>

          {/* Public Routes with Auth Restrictions - Can be viewed by anyone, but with restricted interaction */}
          <Route path="/games"          element={<PublicRoute><GameSearchPage /></PublicRoute>}/>

          {/* Protected Routes - Require authentication */}
          <Route path="/events"         element={<ProtectedRoute><EventsPage /></ProtectedRoute>} />
          <Route path="/profile"        element={<ProtectedRoute><UserProfilePage /></ProtectedRoute>}/>
          <Route path="/user-search"    element={<ProtectedRoute><UserSearchPage /></ProtectedRoute>}/>
          <Route path="/dashboard"    element={<ProtectedRoute><DashboardPage /></ProtectedRoute>}/>
        </Routes>
        <Toaster position="top-right" richColors expand={true} /> {/* Added from origin/dev-Yessine-D3 */}
      </div>
    </AuthProvider>
  )
}

export default App
