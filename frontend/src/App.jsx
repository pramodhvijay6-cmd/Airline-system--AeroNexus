import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Provider, useSelector } from 'react-redux';
import { store } from './store';
import Navbar from './components/Navbar';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import Search from './pages/Search';
import SeatSelection from './pages/SeatSelection';
import Checkout from './pages/Checkout';
import MyBookings from './pages/MyBookings';
import Profile from './pages/Profile';
import AdminDashboard from './pages/AdminDashboard';

// Route guards
function ProtectedRoute({ children }) {
  const { isAuthenticated } = useSelector((state) => state.auth);
  return isAuthenticated ? children : <Navigate to="/login" replace />;
}

function AdminRoute({ children }) {
  const { isAuthenticated, user } = useSelector((state) => state.auth);
  const isAdminOrStaff = user?.roles?.some((r) => r === 'ROLE_ADMIN' || r === 'ROLE_STAFF');
  return isAuthenticated && isAdminOrStaff ? children : <Navigate to="/" replace />;
}

function AppContent() {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col font-sans">
      <Navbar />
      <main className="flex-grow">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/search" element={<Search />} />
          
          {/* Protected Customer Routes */}
          <Route 
            path="/seat-selection" 
            element={
              <ProtectedRoute>
                <SeatSelection />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/checkout" 
            element={
              <ProtectedRoute>
                <Checkout />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/bookings" 
            element={
              <ProtectedRoute>
                <MyBookings />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/profile" 
            element={
              <ProtectedRoute>
                <Profile />
              </ProtectedRoute>
            } 
          />

          {/* Admin and Staff console */}
          <Route 
            path="/admin" 
            element={
              <AdminRoute>
                <AdminDashboard />
              </AdminRoute>
            } 
          />
          
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <footer className="bg-white border-t border-slate-100 py-6 text-center text-xs text-slate-400">
        &copy; {new Date().getFullYear()} AeroNexus Airlines Reservation System. All rights reserved.
      </footer>
    </div>
  );
}

export default function App() {
  return (
    <Provider store={store}>
      <BrowserRouter>
        <AppContent />
      </BrowserRouter>
    </Provider>
  );
}
