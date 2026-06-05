import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { setBookingFlight } from '../store/slices/bookingSlice';
import api from '../api';
import { Search as SearchIcon, Plane, Calendar, AlertCircle, ArrowUpDown, Brain, Globe, Zap, Clock, Layers } from 'lucide-react';

export default function Search() {
  const [origin, setOrigin] = useState('');
  const [destination, setDestination] = useState('');
  const [date, setDate] = useState('');
  const [flights, setFlights] = useState([]);
  const [realTimeFlights, setRealTimeFlights] = useState([]);
  const [loading, setLoading] = useState(false);
  const [realTimeLoading, setRealTimeLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [error, setError] = useState(null);
  const [sortBy, setSortBy] = useState('price-asc');
  const [predictions, setPredictions] = useState({});
  const [loadingPredictions, setLoadingPredictions] = useState({});
  const [showRealTime, setShowRealTime] = useState(true);
  const [activeTab, setActiveTab] = useState('all'); // 'all', 'aeronexus', 'realtime'

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!origin || !destination || !date) return;
    setLoading(true);
    setError(null);
    setSearched(true);
    setRealTimeFlights([]);

    // Search local flights
    try {
      const res = await api.get(`/flights/search?origin=${origin.toUpperCase()}&destination=${destination.toUpperCase()}&departureDate=${date}`);
      if (res.data.success) {
        setFlights(res.data.data);
      } else {
        setError(res.data.message);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Error occurred while searching flights');
    } finally {
      setLoading(false);
    }

    // Search real-time flights in parallel
    if (showRealTime) {
      fetchRealTimeFlights();
    }
  };

  const fetchRealTimeFlights = async () => {
    setRealTimeLoading(true);
    try {
      const res = await api.get(`/flights/realtime?origin=${origin.toUpperCase()}&destination=${destination.toUpperCase()}&date=${date}&adults=1`);
      if (res.data.success && res.data.data) {
        setRealTimeFlights(res.data.data);
      }
    } catch (err) {
      console.warn('Real-time pricing unavailable:', err.message);
    } finally {
      setRealTimeLoading(false);
    }
  };

  const getDelayPrediction = async (flightId) => {
    if (predictions[flightId] || loadingPredictions[flightId]) return;
    setLoadingPredictions(prev => ({ ...prev, [flightId]: true }));
    try {
      const res = await api.get(`/ai/predict-delay/${flightId}`);
      if (res.data.success) {
        setPredictions(prev => ({ ...prev, [flightId]: res.data.data }));
      }
    } catch (e) {
      console.error('Error fetching delay prediction', e);
    } finally {
      setLoadingPredictions(prev => ({ ...prev, [flightId]: false }));
    }
  };

  const sortedFlights = [...flights].sort((a, b) => {
    if (sortBy === 'price-asc') return a.currentPrice - b.currentPrice;
    if (sortBy === 'price-desc') return b.currentPrice - a.currentPrice;
    return a.id - b.id;
  });

  const sortedRealTimeFlights = [...realTimeFlights].sort((a, b) => {
    if (sortBy === 'price-asc') return (a.priceINR || 0) - (b.priceINR || 0);
    if (sortBy === 'price-desc') return (b.priceINR || 0) - (a.priceINR || 0);
    return 0;
  });

  const selectFlight = (flight) => {
    dispatch(setBookingFlight(flight));
    navigate('/seat-selection');
  };

  const totalResults = flights.length + realTimeFlights.length;
  const showAeronexus = activeTab === 'all' || activeTab === 'aeronexus';
  const showRealTimeTab = activeTab === 'all' || activeTab === 'realtime';

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      {/* Search Bar */}
      <div className="bg-white border border-slate-100 rounded-3xl p-6 md:p-8 shadow-md mb-8">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
            <SearchIcon className="h-5 w-5 text-brand-600" />
            <span>Find your next destination</span>
          </h2>
          {/* Real-Time Toggle */}
          <button
            type="button"
            onClick={() => setShowRealTime(!showRealTime)}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold transition-all border ${
              showRealTime
                ? 'bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100'
                : 'bg-slate-50 text-slate-400 border-slate-200 hover:bg-slate-100'
            }`}
          >
            <Globe className="h-3.5 w-3.5" />
            <span>Live Prices</span>
            <span className={`w-8 h-4 rounded-full relative transition-all ${showRealTime ? 'bg-emerald-500' : 'bg-slate-300'}`}>
              <span className={`absolute top-0.5 w-3 h-3 rounded-full bg-white transition-all shadow-sm ${showRealTime ? 'left-4' : 'left-0.5'}`} />
            </span>
          </button>
        </div>
        <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
          <div>
            <label className="block text-xs font-semibold text-slate-500 uppercase mb-2">From</label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-400 font-bold text-sm">DEP</span>
              <input
                type="text"
                required
                value={origin}
                onChange={(e) => setOrigin(e.target.value)}
                placeholder="e.g. JFK or New York"
                className="block w-full pl-12 pr-3 py-2.5 border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500 text-sm"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-500 uppercase mb-2">To</label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-400 font-bold text-sm">ARR</span>
              <input
                type="text"
                required
                value={destination}
                onChange={(e) => setDestination(e.target.value)}
                placeholder="e.g. LAX or California"
                className="block w-full pl-12 pr-3 py-2.5 border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500 text-sm"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-500 uppercase mb-2">Departure Date</label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-400"><Calendar className="h-4 w-4" /></span>
              <input
                type="date"
                required
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className="block w-full pl-10 pr-3 py-2 border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500 text-sm"
              />
            </div>
          </div>
          <button
            type="submit"
            disabled={loading}
            className="bg-brand-600 hover:bg-brand-700 disabled:bg-brand-400 text-white font-semibold py-2.5 px-6 rounded-xl transition-all shadow-md shadow-brand-100 flex justify-center items-center gap-2 text-sm"
          >
            {loading ? 'Searching...' : 'Search Flights'}
          </button>
        </form>
      </div>

      {error && (
        <div className="bg-red-50 border-l-4 border-red-500 p-4 rounded-xl flex items-start gap-2 text-red-700 text-sm mb-8">
          <AlertCircle className="h-5 w-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Search Results */}
      {searched && (
        <div>
          {/* Results Header */}
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
            <h3 className="text-lg font-bold text-slate-800">
              {totalResults} flights found from {origin.toUpperCase()} to {destination.toUpperCase()}
              {realTimeLoading && <span className="text-xs text-brand-500 ml-2 animate-pulse">⏳ Fetching live prices...</span>}
            </h3>
            <div className="flex items-center gap-3">
              {/* Tab filter */}
              {realTimeFlights.length > 0 && (
                <div className="flex bg-slate-100 rounded-xl p-0.5 gap-0.5">
                  <button
                    onClick={() => setActiveTab('all')}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                      activeTab === 'all' ? 'bg-white text-slate-800 shadow-sm' : 'text-slate-500 hover:text-slate-700'
                    }`}
                  >All</button>
                  <button
                    onClick={() => setActiveTab('aeronexus')}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1 ${
                      activeTab === 'aeronexus' ? 'bg-white text-brand-700 shadow-sm' : 'text-slate-500 hover:text-slate-700'
                    }`}
                  ><Zap className="h-3 w-3" /> AeroNexus</button>
                  <button
                    onClick={() => setActiveTab('realtime')}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1 ${
                      activeTab === 'realtime' ? 'bg-white text-emerald-700 shadow-sm' : 'text-slate-500 hover:text-slate-700'
                    }`}
                  ><Globe className="h-3 w-3" /> Live</button>
                </div>
              )}
              {totalResults > 0 && (
                <div className="flex items-center gap-2">
                  <span className="text-xs text-slate-500 flex items-center gap-1"><ArrowUpDown className="h-3.5 w-3.5" /> Sort by</span>
                  <select
                    value={sortBy}
                    onChange={(e) => setSortBy(e.target.value)}
                    className="bg-white border border-slate-200 rounded-xl text-xs px-2.5 py-1.5 focus:outline-none focus:ring-1 focus:ring-brand-500"
                  >
                    <option value="price-asc">Price: Low to High</option>
                    <option value="price-desc">Price: High to Low</option>
                  </select>
                </div>
              )}
            </div>
          </div>

          {loading ? (
            <div className="space-y-4">
              {[1, 2].map((n) => (
                <div key={n} className="bg-slate-50 rounded-2xl h-32 border border-slate-100 animate-pulse"></div>
              ))}
            </div>
          ) : (
            <div className="space-y-4">
              {/* AeroNexus Local Flights */}
              {showAeronexus && sortedFlights.length > 0 && (
                <>
                  {realTimeFlights.length > 0 && activeTab === 'all' && (
                    <div className="flex items-center gap-2 mb-2">
                      <Zap className="h-4 w-4 text-brand-600" />
                      <span className="text-sm font-bold text-slate-700">AeroNexus Flights</span>
                      <span className="text-xs text-slate-400">({flights.length} results)</span>
                    </div>
                  )}
                  {sortedFlights.map((flight) => {
                    const hasPrediction = !!predictions[flight.id];
                    const prediction = predictions[flight.id];
                    const loadingPred = loadingPredictions[flight.id];

                    return (
                      <div key={flight.id} className="bg-white border border-slate-100 rounded-2xl p-6 shadow-sm hover:shadow-md transition-shadow">
                        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
                          
                          {/* Left: Flight Details */}
                          <div className="flex-grow">
                            <div className="flex items-center gap-2 mb-3">
                              <span className="text-xs font-semibold text-brand-600 bg-brand-50 px-2.5 py-1 rounded-full uppercase tracking-wider">
                                {flight.flightNumber}
                              </span>
                              <span className="text-xs text-slate-400">
                                {flight.aircraftModel} ({flight.tailNumber})
                              </span>
                            </div>
                            <div className="flex items-center gap-6">
                              <div>
                                <span className="block text-xl font-bold text-slate-800">
                                  {new Date(flight.departureTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                </span>
                                <span className="text-xs text-slate-400 font-semibold">{flight.origin}</span>
                              </div>
                              <div className="flex flex-col items-center min-w-[80px]">
                                <Plane className="h-4 w-4 text-slate-300 rotate-90" />
                                <div className="w-full border-t border-dashed border-slate-200 my-1"></div>
                              </div>
                              <div>
                                <span className="block text-xl font-bold text-slate-800">
                                  {new Date(flight.arrivalTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                </span>
                                <span className="text-xs text-slate-400 font-semibold">{flight.destination}</span>
                              </div>
                            </div>
                          </div>

                          {/* Middle: AI Predict Delay */}
                          <div className="flex flex-col gap-2 shrink-0">
                            {hasPrediction ? (
                              <div className={`p-3 rounded-xl border flex items-start gap-2 text-xs max-w-[240px] ${
                                prediction.predictedDelayMinutes > 15 ? 'bg-amber-50 border-amber-200 text-amber-800' : 'bg-emerald-50 border-emerald-200 text-emerald-800'
                              }`}>
                                <Brain className="h-4 w-4 shrink-0 text-brand-600" />
                                <div>
                                  <span className="font-bold block">
                                    {prediction.predictedDelayMinutes > 0 ? `Predict ${prediction.predictedDelayMinutes}m delay` : 'Predict On-Time'}
                                  </span>
                                  <span className="block text-[10px] text-slate-500 mt-0.5">
                                    Delay Risk: {Math.round(prediction.delayProbability * 100)}%
                                  </span>
                                </div>
                              </div>
                            ) : (
                              <button
                                onClick={() => getDelayPrediction(flight.id)}
                                disabled={loadingPred}
                                className="text-xs text-brand-600 hover:text-brand-700 bg-brand-50 border border-brand-100 hover:border-brand-200 px-3 py-2 rounded-xl font-medium transition-colors flex items-center gap-1.5 disabled:opacity-50"
                              >
                                <Brain className="h-3.5 w-3.5" />
                                <span>{loadingPred ? 'Analyzing...' : 'Predict Delay'}</span>
                              </button>
                            )}
                          </div>

                          {/* Right: Pricing and Action */}
                          <div className="flex items-center gap-6 self-stretch md:self-auto border-t md:border-t-0 border-slate-50 pt-4 md:pt-0 justify-between">
                            <div className="text-right">
                              <span className="block text-2xl font-extrabold text-slate-800">₹{flight.currentPrice}</span>
                              <span className="text-xs text-slate-400 line-through">₹{flight.basePrice} base</span>
                            </div>
                            <button
                              onClick={() => selectFlight(flight)}
                              disabled={flight.availableSeats <= 0}
                              className="bg-brand-600 hover:bg-brand-700 disabled:bg-slate-200 disabled:text-slate-400 text-white font-semibold py-2.5 px-5 rounded-xl transition-all shadow-md shadow-brand-100/50 flex items-center gap-1 text-sm shrink-0"
                            >
                              {flight.availableSeats > 0 ? (
                                <>
                                  <span>Select Flight</span>
                                </>
                              ) : (
                                <span>Waitlist Join</span>
                              )}
                            </button>
                          </div>

                        </div>
                      </div>
                    );
                  })}
                </>
              )}

              {/* Real-Time Amadeus Flights */}
              {showRealTimeTab && sortedRealTimeFlights.length > 0 && (
                <>
                  {activeTab === 'all' && (
                    <div className="flex items-center gap-2 mt-6 mb-2">
                      <Globe className="h-4 w-4 text-emerald-600" />
                      <span className="text-sm font-bold text-slate-700">Live Market Prices</span>
                      <span className="text-xs text-slate-400">({realTimeFlights.length} results from global airlines)</span>
                      <span className="ml-auto text-[10px] font-semibold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-100">
                        Powered by Amadeus
                      </span>
                    </div>
                  )}
                  {sortedRealTimeFlights.map((rt, index) => (
                    <div key={`rt-${index}`} className="bg-gradient-to-r from-white to-emerald-50/30 border border-emerald-100 rounded-2xl p-6 shadow-sm hover:shadow-md transition-shadow relative overflow-hidden">
                      {/* Live badge */}
                      <div className="absolute top-3 right-3 flex items-center gap-1.5">
                        <span className="relative flex h-2 w-2">
                          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                          <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
                        </span>
                        <span className="text-[10px] font-bold text-emerald-600 uppercase tracking-wider">Live Price</span>
                      </div>

                      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
                        {/* Left: Flight Details */}
                        <div className="flex-grow">
                          <div className="flex items-center gap-2 mb-3">
                            <span className="text-xs font-semibold text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-full uppercase tracking-wider border border-emerald-100">
                              {rt.flightNumber}
                            </span>
                            <span className="text-xs text-slate-400">
                              {rt.aircraftType}
                            </span>
                            {rt.cabinClass && (
                              <span className="text-[10px] text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full font-semibold uppercase">
                                {rt.cabinClass}
                              </span>
                            )}
                          </div>
                          <div className="flex items-center gap-6">
                            <div>
                              <span className="block text-xl font-bold text-slate-800">
                                {rt.departureTime ? new Date(rt.departureTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '--:--'}
                              </span>
                              <span className="text-xs text-slate-400 font-semibold">{rt.origin}</span>
                            </div>
                            <div className="flex flex-col items-center min-w-[100px]">
                              <div className="flex items-center gap-1 text-[10px] text-slate-400 font-semibold">
                                <Clock className="h-3 w-3" />
                                <span>{rt.duration}</span>
                              </div>
                              <div className="w-full flex items-center gap-1 my-1">
                                <div className="flex-1 border-t border-dashed border-emerald-200"></div>
                                <Plane className="h-3.5 w-3.5 text-emerald-400 rotate-90" />
                                <div className="flex-1 border-t border-dashed border-emerald-200"></div>
                              </div>
                              {rt.numberOfStops > 0 && (
                                <span className="text-[10px] font-semibold text-amber-600 flex items-center gap-0.5">
                                  <Layers className="h-3 w-3" />
                                  {rt.numberOfStops} stop{rt.numberOfStops > 1 ? 's' : ''}
                                </span>
                              )}
                              {rt.numberOfStops === 0 && (
                                <span className="text-[10px] font-semibold text-emerald-600">Non-stop</span>
                              )}
                            </div>
                            <div>
                              <span className="block text-xl font-bold text-slate-800">
                                {rt.arrivalTime ? new Date(rt.arrivalTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '--:--'}
                              </span>
                              <span className="text-xs text-slate-400 font-semibold">{rt.destination}</span>
                            </div>
                          </div>
                        </div>

                        {/* Middle: Flight Info */}
                        <div className="flex flex-col gap-1.5 shrink-0">
                          {rt.seatsAvailable > 0 && rt.seatsAvailable <= 5 && (
                            <span className="text-[10px] font-bold text-red-600 bg-red-50 px-2 py-0.5 rounded-full border border-red-100">
                              Only {rt.seatsAvailable} seats left
                            </span>
                          )}
                        </div>

                        {/* Right: Price */}
                        <div className="flex items-center gap-6 self-stretch md:self-auto border-t md:border-t-0 border-slate-50 pt-4 md:pt-0 justify-between">
                          <div className="text-right">
                            <span className="block text-2xl font-extrabold text-slate-800">
                              ₹{rt.priceINR ? Number(rt.priceINR).toLocaleString('en-IN') : 'N/A'}
                            </span>
                            <span className="text-xs text-emerald-600 font-semibold">per person</span>
                          </div>
                          <div className="flex flex-col gap-1.5">
                            <span className="text-[10px] text-center text-slate-400 font-medium">View on airline site</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </>
              )}

              {/* Real-time loading skeleton */}
              {realTimeLoading && showRealTimeTab && (
                <div className="mt-4">
                  <div className="flex items-center gap-2 mb-3">
                    <Globe className="h-4 w-4 text-emerald-600 animate-spin" />
                    <span className="text-sm font-semibold text-emerald-700 animate-pulse">Fetching live prices from global airlines...</span>
                  </div>
                  {[1, 2, 3].map((n) => (
                    <div key={`skeleton-${n}`} className="bg-emerald-50/30 rounded-2xl h-28 border border-emerald-100 animate-pulse mb-3"></div>
                  ))}
                </div>
              )}

              {/* No results */}
              {sortedFlights.length === 0 && sortedRealTimeFlights.length === 0 && !realTimeLoading && (
                <div className="bg-slate-50 rounded-2xl p-12 text-center text-slate-500 border border-slate-200/50">
                  No flights schedule available for this route and date combination.
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
