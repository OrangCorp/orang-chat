import { Routes, Route, Navigate } from 'react-router-dom';
import PrivateRoute from './PrivateRoute';

// Pages
import Home from '../pages/Home';
import Login from '../pages/Login';
import Signup from '../pages/Signup';
import Chat from '../pages/Chat';
import Profile from '../pages/Profile';
import NotFound from '../pages/NotFound';
import EmailVerification from '../pages/EmailVerification';

// Layouts
import MainLayout from '../layouts/MainLayout';
import AuthLayout from '../layouts/AuthLayout';

const AppRoutes = () => {
  return (
    <Routes>
      {/* Public Routes */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/verify-email" element={<EmailVerification />} />
      </Route>

      {/* Protected Routes - Wrap the ENTIRE layout */}
      <Route element={
        <PrivateRoute>
          <MainLayout />
        </PrivateRoute>
      }>
        <Route path="/" element={<Home />} />
        <Route path="/chat/:chatId?" element={<Chat />} />
        <Route path="/profile/me" element={<Profile />} />
        <Route path="/profile/:userId" element={<Profile />} />
      </Route>

      {/* 404 Route */}
      <Route path="/404" element={<NotFound />} />
      <Route path="*" element={<Navigate to="/404" replace />} />
    </Routes>
  );
};

export default AppRoutes;