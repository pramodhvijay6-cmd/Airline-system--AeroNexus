import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { logoutSuccess } from '../store/slices/authSlice';
import api from '../api';
import { Plane, LogOut, User, LayoutDashboard } from 'lucide-react';

export default function Navbar() {
  const { isAuthenticated, user } = useSelector((state) => state.auth);
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      await api.post('/auth/logout');
    } catch (e) {
      console.error('Logout request failed', e);
    }
    dispatch(logoutSuccess());
    navigate('/login');
  };

  const isAdminOrStaff = user?.roles?.some(r => r === 'ROLE_ADMIN' || r === 'ROLE_STAFF');

  return (
    <nav className="bg-white border-b border-slate-100 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex">
            <Link to="/" className="flex-shrink-0 flex items-center gap-2 text-brand-600 font-bold text-xl">
              <Plane className="h-6 w-6 rotate-45" />
              <span>AeroNexus</span>
            </Link>
            <div className="hidden sm:ml-6 sm:flex sm:space-x-8">
              <Link to="/search" className="border-transparent text-slate-500 hover:border-brand-500 hover:text-brand-600 inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium">
                Search Flights
              </Link>
              {isAuthenticated && (
                <Link to="/bookings" className="border-transparent text-slate-500 hover:border-brand-500 hover:text-brand-600 inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium">
                  My Bookings
                </Link>
              )}
              {isAuthenticated && isAdminOrStaff && (
                <Link to="/admin" className="border-transparent text-slate-500 hover:border-brand-500 hover:text-brand-600 inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium gap-1">
                  <LayoutDashboard className="h-4 w-4" /> Admin
                </Link>
              )}
            </div>
          </div>
          <div className="flex items-center gap-4">
            {isAuthenticated ? (
              <>
                <Link to="/profile" className="flex items-center gap-1.5 text-sm text-slate-700 font-medium hover:text-brand-600">
                  <User className="h-4 w-4" />
                  <span>{user.username}</span>
                </Link>
                <button onClick={handleLogout} className="flex items-center gap-1 bg-slate-50 hover:bg-red-50 text-slate-600 hover:text-red-600 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors">
                  <LogOut className="h-4 w-4" />
                  <span>Logout</span>
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="text-slate-600 hover:text-brand-600 text-sm font-medium">
                  Sign In
                </Link>
                <Link to="/register" className="bg-brand-600 hover:bg-brand-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors shadow-sm shadow-brand-100">
                  Sign Up
                </Link>
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
