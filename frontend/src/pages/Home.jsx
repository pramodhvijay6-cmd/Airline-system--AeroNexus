import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import api from '../api';
import { Plane, Star, Calendar, ArrowRight } from 'lucide-react';

export default function Home() {
  const navigate = useNavigate();
  const { isAuthenticated } = useSelector((state) => state.auth);
  const [recommendations, setRecommendations] = useState([]);
  const [loadingRecs, setLoadingRecs] = useState(false);

  useEffect(() => {
    if (isAuthenticated) {
      setLoadingRecs(true);
      api.get('/ai/recommendations')
        .then((res) => {
          if (res.data.success) {
            setRecommendations(res.data.data);
          }
        })
        .catch((err) => console.error('Error loading recommendations', err))
        .finally(() => setLoadingRecs(false));
    }
  }, [isAuthenticated]);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      {/* Hero Section */}
      <div className="relative rounded-3xl overflow-hidden bg-gradient-to-r from-brand-700 to-indigo-900 text-white p-8 md:p-16 mb-12 shadow-xl shadow-brand-100/50">
        <div className="absolute top-0 right-0 w-1/2 h-full opacity-10 pointer-events-none">
          <Plane className="w-full h-full rotate-45 transform translate-x-1/4 -translate-y-1/4" />
        </div>
        <div className="max-w-2xl relative z-10">
          <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight mb-4">
            Search, Book, and Fly Seamlessly
          </h1>
          <p className="text-lg text-brand-100 mb-8 leading-relaxed">
            Experience premium airline booking with real-time seat availability, intelligent delay predictions, and dynamic loyalty points rewards.
          </p>
          <button 
            onClick={() => navigate('/search')}
            className="bg-white hover:bg-brand-50 text-brand-700 px-6 py-3 rounded-xl font-semibold shadow-md transition-all hover:scale-105 flex items-center gap-2"
          >
            <span>Book a Flight</span>
            <ArrowRight className="h-5 w-5" />
          </button>
        </div>
      </div>

      {/* AI Recommendations */}
      {isAuthenticated && (
        <div className="mb-12">
          <div className="flex items-center gap-2 mb-6">
            <Star className="h-6 w-6 text-amber-500 fill-amber-500" />
            <h2 className="text-2xl font-bold text-slate-800">Tailored Recommendations</h2>
          </div>

          {loadingRecs ? (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 animate-pulse">
              {[1, 2, 3].map((n) => (
                <div key={n} className="bg-slate-100 rounded-2xl h-48 border border-slate-200"></div>
              ))}
            </div>
          ) : recommendations.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {recommendations.slice(0, 3).map((flight) => (
                <div key={flight.id} className="bg-white border border-slate-100 rounded-2xl p-6 shadow-sm hover:shadow-md transition-shadow relative overflow-hidden flex flex-col justify-between">
                  <div className="flex justify-between items-start mb-4">
                    <div>
                      <span className="text-xs font-semibold text-brand-600 bg-brand-50 px-2.5 py-1 rounded-full uppercase tracking-wider">
                        {flight.flightNumber}
                      </span>
                    </div>
                    <div className="text-right">
                      <span className="text-xl font-bold text-slate-800">₹{flight.currentPrice}</span>
                      <span className="block text-xs text-slate-400">dynamic price</span>
                    </div>
                  </div>

                  <div className="flex justify-between items-center gap-4 py-3 border-y border-slate-50 mb-4">
                    <div>
                      <span className="block text-2xl font-bold text-slate-800">{flight.origin}</span>
                      <span className="text-xs text-slate-400">Origin</span>
                    </div>
                    <div className="flex flex-col items-center flex-grow">
                      <Plane className="h-4 w-4 text-slate-300 rotate-90" />
                      <div className="w-full border-t border-dashed border-slate-200 my-1"></div>
                    </div>
                    <div className="text-right">
                      <span className="block text-2xl font-bold text-slate-800">{flight.destination}</span>
                      <span className="text-xs text-slate-400">Destination</span>
                    </div>
                  </div>

                  <div className="flex items-center justify-between">
                    <span className="text-xs text-slate-500 flex items-center gap-1">
                      <Calendar className="h-3.5 w-3.5" />
                      {new Date(flight.departureTime).toLocaleDateString()}
                    </span>
                    <button 
                      onClick={() => {
                        navigate('/search');
                      }}
                      className="text-xs font-semibold text-brand-600 hover:text-brand-700 flex items-center gap-0.5"
                    >
                      <span>Search flights</span>
                      <ArrowRight className="h-3 w-3" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="bg-slate-50 rounded-2xl p-8 border border-slate-200/50 text-center text-slate-500">
              No flight recommendation metrics available yet. Book flights to see recommendations.
            </div>
          )}
        </div>
      )}

      {/* Info Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div className="bg-slate-50 border border-slate-100 rounded-3xl p-8 hover:bg-slate-100/50 transition-colors">
          <div className="bg-brand-100 text-brand-700 w-12 h-12 rounded-2xl flex items-center justify-center mb-6">
            <Plane className="h-6 w-6" />
          </div>
          <h3 className="text-xl font-bold text-slate-800 mb-2">Real-time Seat Locks</h3>
          <p className="text-slate-600">
            Never lose your selected seat. We lock your seat selection in our cache for 10 minutes while you complete checkout.
          </p>
        </div>
        <div className="bg-slate-50 border border-slate-100 rounded-3xl p-8 hover:bg-slate-100/50 transition-colors">
          <div className="bg-emerald-100 text-emerald-700 w-12 h-12 rounded-2xl flex items-center justify-center mb-6">
            <Star className="h-6 w-6" />
          </div>
          <h3 className="text-xl font-bold text-slate-800 mb-2">Dynamic Pricing</h3>
          <p className="text-slate-600">
            Our intelligent algorithms optimize prices in real-time based on seat demand, route variables, and occupancy.
          </p>
        </div>
        <div className="bg-slate-50 border border-slate-100 rounded-3xl p-8 hover:bg-slate-100/50 transition-colors">
          <div className="bg-indigo-100 text-indigo-700 w-12 h-12 rounded-2xl flex items-center justify-center mb-6">
            <Calendar className="h-6 w-6" />
          </div>
          <h3 className="text-xl font-bold text-slate-800 mb-2">Loyalty Points Program</h3>
          <p className="text-slate-600">
            Earn points with every flight. Progress from Bronze up to Platinum for customized multipliers and exclusive savings.
          </p>
        </div>
      </div>
    </div>
  );
}
