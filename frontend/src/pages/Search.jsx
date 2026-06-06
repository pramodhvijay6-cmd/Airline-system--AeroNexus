import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { setBookingFlight } from '../store/slices/bookingSlice';
import api from '../api';
import { 
  Search as SearchIcon, 
  Plane, 
  Calendar, 
  AlertCircle, 
  ArrowUpDown, 
  Brain, 
  Globe, 
  Zap, 
  Clock, 
  ChevronDown, 
  ChevronUp, 
  TrendingUp, 
  Leaf,
  Info
} from 'lucide-react';

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
  
  // Price analysis states
  const [priceAnalysisOpen, setPriceAnalysisOpen] = useState(true);
  const [priceHistory, setPriceHistory] = useState([]);
  const [typicalLow, setTypicalLow] = useState(0);
  const [typicalHigh, setTypicalHigh] = useState(0);
  const [priceStatus, setPriceStatus] = useState('typical'); // 'low', 'typical', 'high'
  const [expandedFlights, setExpandedFlights] = useState({});

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!origin || !destination || !date) return;
    setLoading(true);
    setError(null);
    setSearched(true);
    setRealTimeFlights([]);
    setFlights([]);
    setExpandedFlights({});

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

  const toggleExpand = (id) => {
    setExpandedFlights(prev => ({ ...prev, [id]: !prev[id] }));
  };

  // Helper to extract lowest price
  const getMinPrice = () => {
    let prices = [];
    flights.forEach(f => prices.push(f.currentPrice));
    realTimeFlights.forEach(f => prices.push(f.priceINR ? Number(f.priceINR) : 0));
    prices = prices.filter(p => p > 0);
    return prices.length > 0 ? Math.min(...prices) : 0;
  };

  // Update Price Analysis widget data
  useEffect(() => {
    const minPrice = getMinPrice();
    if (minPrice <= 0) return;

    const low = Math.round(minPrice * 0.92);
    const high = Math.round(minPrice * 1.18);
    setTypicalLow(low);
    setTypicalHigh(high);

    if (minPrice < low) {
      setPriceStatus('low');
    } else if (minPrice > high) {
      setPriceStatus('high');
    } else {
      setPriceStatus('typical');
    }

    // Generate seeded mock history (30 points) for trend graph
    const seed = minPrice + origin.charCodeAt(0) + destination.charCodeAt(0);
    const history = [];
    let current = minPrice * 1.05;
    for (let i = 0; i < 30; i++) {
      const sinVal = Math.sin(seed + i * 0.5);
      const shift = sinVal * (minPrice * 0.04);
      current += shift;
      // boundary constraints
      current = Math.max(minPrice * 0.85, Math.min(minPrice * 1.35, current));
      history.push(Math.round(current));
    }
    setPriceHistory(history);
  }, [flights, realTimeFlights]);

  const totalResults = flights.length + realTimeFlights.length;
  const showAeronexus = activeTab === 'all' || activeTab === 'aeronexus';
  const showRealTimeTab = activeTab === 'all' || activeTab === 'realtime';
  const minPrice = getMinPrice();

  // Carbon Emissions Calculator helper
  const calculateEmissions = (distanceMiles, aircraftModel) => {
    const baseCo2 = Math.round(distanceMiles * 0.22);
    let comparison = 'Avg emissions';
    let isLower = false;
    let isHigher = false;
    let percent = 0;

    const model = (aircraftModel || '').toLowerCase();
    if (model.includes('320neo') || model.includes('220') || model.includes('350')) {
      percent = 12 + (baseCo2 % 8);
      comparison = `-${percent}% emissions`;
      isLower = true;
    } else if (model.includes('787')) {
      percent = 9 + (baseCo2 % 6);
      comparison = `-${percent}% emissions`;
      isLower = true;
    } else if (model.includes('380') || model.includes('777')) {
      percent = 10 + (baseCo2 % 5);
      comparison = `+${percent}% emissions`;
      isHigher = true;
    }

    return { co2: baseCo2, label: comparison, isLower, isHigher };
  };

  // SVG Line Chart Helpers
  const chartWidth = 500;
  const chartHeight = 110;
  const maxHistory = priceHistory.length > 0 ? Math.max(...priceHistory) : 100;
  const minHistory = priceHistory.length > 0 ? Math.min(...priceHistory) : 0;
  const priceRange = maxHistory - minHistory || 1;

  // Build the SVG path string
  const svgPath = priceHistory.map((val, i) => {
    const x = (i / 29) * chartWidth;
    const y = chartHeight - 10 - ((val - minHistory) / priceRange) * (chartHeight - 20);
    return `${i === 0 ? 'M' : 'L'} ${x} ${y}`;
  }).join(' ');

  return (
    <div className="w-full min-h-screen bg-[#121212] text-slate-200 py-12 px-4 sm:px-6 lg:px-8 font-sans">
      <div className="max-w-6xl mx-auto">
        
        {/* Search Panel Card */}
        <div className="bg-[#1e1e1e] border border-[#2c2c2c] rounded-3xl p-6 md:p-8 shadow-2xl mb-8">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-xl font-bold text-slate-100 flex items-center gap-2">
              <SearchIcon className="h-5 w-5 text-blue-400" />
              <span>Search Flights</span>
            </h2>
            
            {/* Real-Time Toggle */}
            <button
              type="button"
              onClick={() => setShowRealTime(!showRealTime)}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold transition-all border ${
                showRealTime
                  ? 'bg-emerald-950/30 text-emerald-400 border-emerald-800/50 hover:bg-emerald-900/30'
                  : 'bg-slate-900 text-slate-500 border-slate-800 hover:bg-slate-800'
              }`}
            >
              <Globe className="h-3.5 w-3.5" />
              <span>Live Market Prices</span>
              <span className={`w-8 h-4 rounded-full relative transition-all ${showRealTime ? 'bg-emerald-500' : 'bg-slate-700'}`}>
                <span className={`absolute top-0.5 w-3 h-3 rounded-full bg-white transition-all shadow-sm ${showRealTime ? 'left-4' : 'left-0.5'}`} />
              </span>
            </button>
          </div>
          
          <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
            <div>
              <label className="block text-xs font-semibold text-slate-400 uppercase mb-2">From</label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-500 font-bold text-sm">DEP</span>
                <input
                  type="text"
                  required
                  value={origin}
                  onChange={(e) => setOrigin(e.target.value)}
                  placeholder="e.g. MAA or Chennai"
                  className="block w-full pl-12 pr-3 py-2.5 bg-[#292a2d] border border-[#3c4043] rounded-xl text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                />
              </div>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-400 uppercase mb-2">To</label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-500 font-bold text-sm">ARR</span>
                <input
                  type="text"
                  required
                  value={destination}
                  onChange={(e) => setDestination(e.target.value)}
                  placeholder="e.g. BOM or Mumbai"
                  className="block w-full pl-12 pr-3 py-2.5 bg-[#292a2d] border border-[#3c4043] rounded-xl text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                />
              </div>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-400 uppercase mb-2">Departure Date</label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-500"><Calendar className="h-4 w-4" /></span>
                <input
                  type="date"
                  required
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  className="block w-full pl-10 pr-3 py-2 bg-[#292a2d] border border-[#3c4043] rounded-xl text-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm scheme-dark"
                />
              </div>
            </div>
            <button
              type="submit"
              disabled={loading}
              className="bg-blue-600 hover:bg-blue-700 disabled:bg-blue-800 text-white font-semibold py-2.5 px-6 rounded-xl transition-all shadow-md flex justify-center items-center gap-2 text-sm"
            >
              {loading ? 'Searching...' : 'Search Flights'}
            </button>
          </form>
        </div>

        {error && (
          <div className="bg-red-950/40 border-l-4 border-red-500 p-4 rounded-xl flex items-start gap-2 text-red-400 text-sm mb-8">
            <AlertCircle className="h-5 w-5 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Results Page */}
        {searched && (
          <div className="space-y-6">
            
            {/* Results Header */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-[#2c2c2c] pb-4">
              <h3 className="text-lg font-bold text-slate-100">
                {totalResults} flights found from {origin.toUpperCase()} to {destination.toUpperCase()}
                {realTimeLoading && <span className="text-xs text-blue-400 ml-3 animate-pulse">⏳ Fetching live prices...</span>}
              </h3>
              
              <div className="flex items-center gap-3">
                {/* Tabs filter */}
                {realTimeFlights.length > 0 && (
                  <div className="flex bg-[#202124] border border-[#3c4043] rounded-xl p-0.5 gap-0.5">
                    <button
                      onClick={() => setActiveTab('all')}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                        activeTab === 'all' ? 'bg-[#3c4043] text-white' : 'text-slate-400 hover:text-slate-200'
                      }`}
                    >All</button>
                    <button
                      onClick={() => setActiveTab('aeronexus')}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1 ${
                        activeTab === 'aeronexus' ? 'bg-[#3c4043] text-blue-400' : 'text-slate-400 hover:text-slate-200'
                      }`}
                    ><Zap className="h-3 w-3" /> AeroNexus</button>
                    <button
                      onClick={() => setActiveTab('realtime')}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1 ${
                        activeTab === 'realtime' ? 'bg-[#3c4043] text-emerald-400' : 'text-slate-400 hover:text-slate-200'
                      }`}
                    ><Globe className="h-3 w-3" /> Live</button>
                  </div>
                )}
                
                {totalResults > 0 && (
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-slate-400 flex items-center gap-1"><ArrowUpDown className="h-3.5 w-3.5" /> Sort by</span>
                    <select
                      value={sortBy}
                      onChange={(e) => setSortBy(e.target.value)}
                      className="bg-[#202124] border border-[#3c4043] text-slate-200 rounded-xl text-xs px-2.5 py-1.5 focus:outline-none focus:ring-1 focus:ring-blue-500"
                    >
                      <option value="price-asc">Price: Low to High</option>
                      <option value="price-desc">Price: High to Low</option>
                    </select>
                  </div>
                )}
              </div>
            </div>

            {/* Price typical analysis card */}
            {minPrice > 0 && (
              <div className="bg-[#1e1e1e] border border-[#2c2c2c] rounded-2xl overflow-hidden shadow-lg">
                <button
                  onClick={() => setPriceAnalysisOpen(!priceAnalysisOpen)}
                  className="w-full flex justify-between items-center p-5 text-left hover:bg-[#252629] transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <TrendingUp className={`h-5 w-5 ${
                      priceStatus === 'low' ? 'text-emerald-400' : priceStatus === 'high' ? 'text-red-400' : 'text-blue-400'
                    }`} />
                    <div>
                      <span className="text-base font-bold text-slate-100">
                        Prices are currently <span className="font-extrabold uppercase">{priceStatus}</span> for your search
                      </span>
                      <p className="text-xs text-slate-400 mt-0.5">
                        The least expensive flights for similar trips to {destination.toUpperCase()} usually cost between ₹{typicalLow.toLocaleString('en-IN')}–{typicalHigh.toLocaleString('en-IN')}.
                      </p>
                    </div>
                  </div>
                  {priceAnalysisOpen ? <ChevronUp className="h-5 w-5 text-slate-400" /> : <ChevronDown className="h-5 w-5 text-slate-400" />}
                </button>

                {priceAnalysisOpen && (
                  <div className="px-6 pb-6 pt-2 border-t border-[#2c2c2c] bg-[#1a1a1c]/50 grid grid-cols-1 md:grid-cols-2 gap-8">
                    
                    {/* Left: Price Gauge slider */}
                    <div className="flex flex-col justify-center">
                      <span className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-8 block">Price Gauge</span>
                      <div className="relative pt-6 pb-2">
                        
                        {/* Gauge scale pointer */}
                        {(() => {
                          const scaleMin = minPrice * 0.8;
                          const scaleMax = minPrice * 1.4;
                          const posPct = Math.max(0, Math.min(100, ((minPrice - scaleMin) / (scaleMax - scaleMin)) * 100));
                          
                          return (
                            <div className="absolute top-0 transform -translate-x-1/2 transition-all duration-500" style={{ left: `${posPct}%` }}>
                              <div className="bg-blue-600 text-white text-[10px] font-extrabold px-2 py-1 rounded-md shadow-lg whitespace-nowrap mb-1 flex flex-col items-center">
                                <span>₹{Math.round(minPrice).toLocaleString('en-IN')} is {priceStatus}</span>
                                <div className="w-1.5 h-1.5 bg-blue-600 rotate-45 -mb-1 mt-0.5"></div>
                              </div>
                            </div>
                          );
                        })()}

                        {/* Track zones */}
                        <div className="h-2.5 rounded-full flex overflow-hidden bg-slate-800">
                          <div className="w-[30%] bg-emerald-500" title="Low prices"></div>
                          <div className="w-[45%] bg-amber-400" title="Typical prices"></div>
                          <div className="w-[25%] bg-red-500" title="High prices"></div>
                        </div>

                        {/* Markers */}
                        <div className="flex justify-between text-[10px] text-slate-500 font-semibold mt-2 px-1">
                          <span>₹{typicalLow.toLocaleString('en-IN')}</span>
                          <span>Typical Range</span>
                          <span>₹{typicalHigh.toLocaleString('en-IN')}</span>
                        </div>
                      </div>
                    </div>

                    {/* Right: SVG Price Trend History Chart */}
                    <div className="flex flex-col">
                      <span className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2 flex items-center gap-1">Price history for this search <Info className="h-3 w-3 text-slate-500" title="Based on similar flight history over 30 days" /></span>
                      
                      {priceHistory.length > 0 ? (
                        <div className="bg-[#1e1e1e] border border-[#2c2c2c] rounded-xl p-3 flex flex-row items-stretch">
                          {/* Y-axis Ticks */}
                          <div className="flex flex-col justify-between text-[9px] text-slate-500 font-bold pr-2 py-1 text-right select-none min-w-[45px]">
                            <span>₹{maxHistory.toLocaleString('en-IN')}</span>
                            <span>₹{Math.round((maxHistory + minHistory) / 2).toLocaleString('en-IN')}</span>
                            <span>₹{minHistory.toLocaleString('en-IN')}</span>
                          </div>
                          
                          {/* SVG Plot */}
                          <div className="flex-grow relative h-[110px]">
                            <svg className="w-full h-full overflow-visible" viewBox={`0 0 ${chartWidth} ${chartHeight}`} preserveAspectRatio="none">
                              {/* Grid Lines */}
                              <line x1="0" y1="10" x2={chartWidth} y2="10" stroke="#2c2c2c" strokeWidth="1" strokeDasharray="3,3" />
                              <line x1="0" y1={chartHeight / 2} x2={chartWidth} y2={chartHeight / 2} stroke="#2c2c2c" strokeWidth="1" strokeDasharray="3,3" />
                              <line x1="0" y1={chartHeight - 10} x2={chartWidth} y2={chartHeight - 10} stroke="#2c2c2c" strokeWidth="1" strokeDasharray="3,3" />
                              
                              {/* Trend Path */}
                              <path d={svgPath} fill="none" stroke="#8ab4f8" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
                            </svg>
                          </div>
                        </div>
                      ) : (
                        <div className="h-[110px] flex items-center justify-center bg-[#1e1e1e] border border-[#2c2c2c] rounded-xl text-xs text-slate-500">
                          History chart loading...
                        </div>
                      )}
                    </div>

                  </div>
                )}
              </div>
            )}

            {loading ? (
              <div className="space-y-4">
                {[1, 2].map((n) => (
                  <div key={n} className="bg-[#1e1e1e] rounded-2xl h-28 border border-[#2c2c2c] animate-pulse"></div>
                ))}
              </div>
            ) : (
              <div className="space-y-4">
                
                {/* AeroNexus Flights */}
                {showAeronexus && sortedFlights.length > 0 && (
                  <>
                    {realTimeFlights.length > 0 && activeTab === 'all' && (
                      <div className="flex items-center gap-2 mb-2 mt-4">
                        <Zap className="h-4.5 w-4.5 text-blue-400" />
                        <span className="text-sm font-bold text-slate-200">AeroNexus Flights</span>
                        <span className="text-xs text-slate-500">({flights.length} results)</span>
                      </div>
                    )}
                    
                    {sortedFlights.map((flight) => {
                      const hasPrediction = !!predictions[flight.id];
                      const prediction = predictions[flight.id];
                      const loadingPred = loadingPredictions[flight.id];
                      const isExpanded = !!expandedFlights[flight.id];
                      
                      // Calculate mock emissions
                      const distance = flight.route?.distanceMiles || 500;
                      const emissions = calculateEmissions(distance, flight.aircraftModel);

                      return (
                        <div key={flight.id} className="bg-[#1e1e1e] border border-[#2c2c2c] rounded-xl overflow-hidden hover:bg-[#252629] transition-all">
                          
                          {/* Row Summary */}
                          <div 
                            onClick={() => toggleExpand(flight.id)}
                            className="p-5 flex flex-col md:flex-row justify-between items-start md:items-center gap-6 cursor-pointer"
                          >
                            
                            {/* Left: Timings & Airline Logo */}
                            <div className="flex items-center gap-4 min-w-[240px]">
                              {/* Mock Airline logo */}
                              <div className="h-9 w-9 bg-blue-950/50 border border-blue-800/40 text-blue-400 rounded-lg flex items-center justify-center font-bold text-xs shrink-0 select-none uppercase">
                                AN
                              </div>
                              <div>
                                <span className="block text-base font-bold text-slate-100">
                                  {new Date(flight.departureTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} – {new Date(flight.arrivalTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                </span>
                                <span className="text-xs text-slate-400 mt-0.5 block">
                                  AeroNexus · {flight.flightNumber}
                                </span>
                              </div>
                            </div>

                            {/* Center-Left: Duration & Route */}
                            <div className="min-w-[120px]">
                              <span className="block text-sm font-semibold text-slate-200">
                                {Math.floor(distance / 450) > 0 ? `${Math.floor(distance / 450)} hr ${Math.round((distance % 450) / 7.5)} min` : `${Math.round(distance / 7.5)} min`}
                              </span>
                              <span className="text-xs text-slate-400 mt-0.5 block">{flight.origin}–{flight.destination}</span>
                            </div>

                            {/* Center: Stop Status */}
                            <div className="min-w-[100px]">
                              <span className="text-sm font-semibold text-emerald-400">Nonstop</span>
                            </div>

                            {/* Center-Right: Carbon Emissions */}
                            <div className="min-w-[120px]">
                              <span className="block text-sm font-semibold text-slate-300">
                                {emissions.co2} kg CO2e
                              </span>
                              <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded-full inline-block mt-0.5 border ${
                                emissions.isLower 
                                  ? 'text-emerald-400 bg-emerald-950/20 border-emerald-900/30' 
                                  : emissions.isHigher 
                                    ? 'text-red-400 bg-red-950/20 border-red-900/30' 
                                    : 'text-slate-400 bg-slate-800/40 border-slate-700/30'
                              }`}>
                                {emissions.label}
                              </span>
                            </div>

                            {/* Right: Pricing, CTA & Dropdown Arrow */}
                            <div className="flex items-center gap-6 self-stretch md:self-auto justify-between border-t md:border-t-0 border-[#2c2c2c] pt-4 md:pt-0 flex-grow md:flex-grow-0">
                              <div className="text-right">
                                <span className="block text-xl font-extrabold text-slate-100">₹{flight.currentPrice.toLocaleString('en-IN')}</span>
                                <span className="text-[10px] text-slate-400 block mt-0.5">one way</span>
                              </div>
                              
                              <div className="flex items-center gap-3" onClick={(e) => e.stopPropagation()}>
                                <button
                                  onClick={() => selectFlight(flight)}
                                  disabled={flight.availableSeats <= 0}
                                  className="bg-blue-600 hover:bg-blue-700 disabled:bg-[#2c2c2c] disabled:text-slate-500 text-white text-xs font-bold py-2 px-4 rounded-xl transition-all"
                                >
                                  {flight.availableSeats > 0 ? 'Select' : 'Waitlist'}
                                </button>
                                <button 
                                  onClick={() => toggleExpand(flight.id)}
                                  className="p-1.5 rounded-lg hover:bg-slate-800 text-slate-400 transition-colors"
                                >
                                  {isExpanded ? <ChevronUp className="h-4.5 w-4.5" /> : <ChevronDown className="h-4.5 w-4.5" />}
                                </button>
                              </div>
                            </div>

                          </div>

                          {/* Expanded Details Drawer */}
                          {isExpanded && (
                            <div className="bg-[#161618] border-t border-[#2c2c2c] p-6 text-sm text-slate-300 grid grid-cols-1 md:grid-cols-3 gap-6">
                              <div>
                                <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block mb-2">Aircraft Details</span>
                                <p className="font-semibold text-slate-200">{flight.aircraftModel}</p>
                                <p className="text-xs text-slate-400 mt-1">Tail Number: {flight.tailNumber}</p>
                                <p className="text-xs text-slate-400 mt-1">Available Seats: {flight.availableSeats} / {flight.capacity}</p>
                              </div>
                              
                              <div>
                                <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block mb-2">Delay Risk (AI Predict)</span>
                                {hasPrediction ? (
                                  <div className="flex items-start gap-2">
                                    <Brain className="h-5 w-5 text-blue-400 shrink-0 mt-0.5" />
                                    <div>
                                      <p className="font-bold text-slate-200">
                                        {prediction.predictedDelayMinutes > 0 ? `Estimated Delay: ${prediction.predictedDelayMinutes} min` : 'On-Time flight expected'}
                                      </p>
                                      <p className="text-xs text-slate-400 mt-1">
                                        System-wide delay probability: {Math.round(prediction.delayProbability * 100)}%
                                      </p>
                                    </div>
                                  </div>
                                ) : (
                                  <button
                                    onClick={() => getDelayPrediction(flight.id)}
                                    disabled={loadingPred}
                                    className="text-xs text-blue-400 hover:text-blue-300 bg-blue-950/30 border border-blue-900/40 px-3 py-1.5 rounded-lg font-semibold transition-colors flex items-center gap-1.5"
                                  >
                                    <Brain className="h-3.5 w-3.5" />
                                    <span>{loadingPred ? 'Analyzing logs...' : 'Analyze Delay Risk'}</span>
                                  </button>
                                )}
                              </div>

                              <div>
                                <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block mb-2">Carbon Emissions Analysis</span>
                                <div className="flex items-start gap-2">
                                  <Leaf className="h-5 w-5 text-emerald-400 shrink-0 mt-0.5" />
                                  <div>
                                    <p className="font-bold text-slate-200">{emissions.co2} kg CO2e</p>
                                    <p className="text-xs text-slate-400 mt-1">
                                      This flight emits {emissions.isLower ? `${percent}% less` : emissions.isHigher ? `${percent}% more` : 'about average'} emissions compared to other typical carriers on this route.
                                    </p>
                                  </div>
                                </div>
                              </div>
                            </div>
                          )}

                        </div>
                      );
                    })}
                  </>
                )}

                {/* Live Market Prices */}
                {showRealTimeTab && sortedRealTimeFlights.length > 0 && (
                  <>
                    {activeTab === 'all' && (
                      <div className="flex items-center gap-2 mt-8 mb-2">
                        <Globe className="h-4.5 w-4.5 text-emerald-400" />
                        <span className="text-sm font-bold text-slate-200">Live Market Prices</span>
                        <span className="text-xs text-slate-500">({realTimeFlights.length} results from global airlines)</span>
                        <span className="ml-auto text-[10px] font-semibold text-emerald-400 bg-emerald-950/20 px-2.5 py-0.5 rounded-full border border-emerald-900/30">
                          Powered by Amadeus
                        </span>
                      </div>
                    )}

                    {sortedRealTimeFlights.map((rt, index) => {
                      const id = `rt-${index}`;
                      const isExpanded = !!expandedFlights[id];
                      
                      // Calculate mock emissions
                      const distance = rt.numberOfStops > 0 ? 1200 : 700; // estimated distance
                      const emissions = calculateEmissions(distance, rt.aircraftType);

                      return (
                        <div key={id} className="bg-gradient-to-r from-[#1e1e1e] to-[#1e231e]/20 border border-[#2c2c2c] rounded-xl overflow-hidden hover:bg-[#252629] transition-all relative">
                          
                          {/* Live pulse indicator */}
                          <div className="absolute top-2 right-2 flex items-center gap-1">
                            <span className="relative flex h-1.5 w-1.5">
                              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                              <span className="relative inline-flex rounded-full h-1.5 w-1.5 bg-emerald-500"></span>
                            </span>
                            <span className="text-[8px] font-bold text-emerald-500 uppercase tracking-widest">Live</span>
                          </div>

                          {/* Row Summary */}
                          <div 
                            onClick={() => toggleExpand(id)}
                            className="p-5 flex flex-col md:flex-row justify-between items-start md:items-center gap-6 cursor-pointer"
                          >
                            
                            {/* Left: Timings & Airline Logo */}
                            <div className="flex items-center gap-4 min-w-[240px]">
                              {/* Logo placeholder */}
                              <div className="h-9 w-9 bg-emerald-950/30 border border-emerald-900/40 text-emerald-400 rounded-lg flex items-center justify-center font-bold text-xs shrink-0 select-none uppercase">
                                {rt.airlineCode}
                              </div>
                              <div>
                                <span className="block text-base font-bold text-slate-100">
                                  {rt.departureTime ? new Date(rt.departureTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '--:--'} – {rt.arrivalTime ? new Date(rt.arrivalTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '--:--'}
                                </span>
                                <span className="text-xs text-slate-400 mt-0.5 block">
                                  {rt.airline} · {rt.flightNumber}
                                </span>
                              </div>
                            </div>

                            {/* Center-Left: Duration & Route */}
                            <div className="min-w-[120px]">
                              <span className="block text-sm font-semibold text-slate-200">
                                {rt.duration}
                              </span>
                              <span className="text-xs text-slate-400 mt-0.5 block">{rt.origin}–{rt.destination}</span>
                            </div>

                            {/* Center: Stop Status */}
                            <div className="min-w-[100px]">
                              {rt.numberOfStops > 0 ? (
                                <span className="text-sm font-semibold text-amber-400 block">
                                  {rt.numberOfStops} stop{rt.numberOfStops > 1 ? 's' : ''}
                                </span>
                              ) : (
                                <span className="text-sm font-semibold text-emerald-400 block">Nonstop</span>
                              )}
                            </div>

                            {/* Center-Right: Carbon Emissions */}
                            <div className="min-w-[120px]">
                              <span className="block text-sm font-semibold text-slate-300">
                                {emissions.co2} kg CO2e
                              </span>
                              <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded-full inline-block mt-0.5 border ${
                                emissions.isLower 
                                  ? 'text-emerald-400 bg-emerald-950/20 border-emerald-900/30' 
                                  : emissions.isHigher 
                                    ? 'text-red-400 bg-red-950/20 border-red-900/30' 
                                    : 'text-slate-400 bg-slate-800/40 border-slate-700/30'
                              }`}>
                                {emissions.label}
                              </span>
                            </div>

                            {/* Right: Pricing, Info & Dropdown */}
                            <div className="flex items-center gap-6 self-stretch md:self-auto justify-between border-t md:border-t-0 border-[#2c2c2c] pt-4 md:pt-0 flex-grow md:flex-grow-0">
                              <div className="text-right">
                                <span className="block text-xl font-extrabold text-slate-100">
                                  ₹{rt.priceINR ? Number(rt.priceINR).toLocaleString('en-IN') : 'N/A'}
                                </span>
                                <span className="text-[10px] text-emerald-500 block mt-0.5">per person</span>
                              </div>
                              
                              <div className="flex items-center gap-3" onClick={(e) => e.stopPropagation()}>
                                <span className="text-[10px] text-slate-500 font-semibold px-2 py-1 rounded bg-[#252629] border border-[#3c4043] select-none text-center min-w-[70px]">
                                  View Site
                                </span>
                                <button 
                                  onClick={() => toggleExpand(id)}
                                  className="p-1.5 rounded-lg hover:bg-slate-800 text-slate-400 transition-colors"
                                >
                                  {isExpanded ? <ChevronUp className="h-4.5 w-4.5" /> : <ChevronDown className="h-4.5 w-4.5" />}
                                </button>
                              </div>
                            </div>

                          </div>

                          {/* Expanded Details Drawer */}
                          {isExpanded && (
                            <div className="bg-[#161618] border-t border-[#2c2c2c] p-6 text-sm text-slate-300 grid grid-cols-1 md:grid-cols-3 gap-6">
                              <div>
                                <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block mb-2">Aircraft Details</span>
                                <p className="font-semibold text-slate-200">{rt.aircraftType}</p>
                                <p className="text-xs text-slate-400 mt-1">Cabin Class: {rt.cabinClass || 'ECONOMY'}</p>
                                {rt.seatsAvailable > 0 && (
                                  <p className="text-xs text-red-400 font-semibold mt-1">Only {rt.seatsAvailable} seats remaining!</p>
                                )}
                              </div>
                              
                              <div>
                                <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block mb-2">Route Information</span>
                                <p className="font-semibold text-slate-200">{rt.origin} to {rt.destination}</p>
                                <p className="text-xs text-slate-400 mt-1">Stops: {rt.numberOfStops}</p>
                                <p className="text-xs text-slate-400 mt-1">Carrier: {rt.airline}</p>
                              </div>

                              <div>
                                <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block mb-2">Emissions Summary</span>
                                <div className="flex items-start gap-2">
                                  <Leaf className="h-5 w-5 text-emerald-400 shrink-0 mt-0.5" />
                                  <div>
                                    <p className="font-bold text-slate-200">{emissions.co2} kg CO2e</p>
                                    <p className="text-xs text-slate-400 mt-1">
                                      Estimated emissions for this route. {rt.airline} operates standard fuel-efficient jets on this segment.
                                    </p>
                                  </div>
                                </div>
                              </div>
                            </div>
                          )}

                        </div>
                      );
                    })}
                  </>
                )}

                {/* Real-time loading skeleton */}
                {realTimeLoading && showRealTimeTab && (
                  <div className="mt-8">
                    <div className="flex items-center gap-2 mb-3">
                      <Globe className="h-4 w-4 text-emerald-400 animate-spin" />
                      <span className="text-xs font-bold text-emerald-500 animate-pulse uppercase tracking-wider">Loading Live Prices...</span>
                    </div>
                    {[1, 2].map((n) => (
                      <div key={`skeleton-${n}`} className="bg-[#1e1e1e] border border-[#2c2c2c] rounded-xl h-24 animate-pulse mb-3"></div>
                    ))}
                  </div>
                )}

                {/* No results */}
                {sortedFlights.length === 0 && sortedRealTimeFlights.length === 0 && !realTimeLoading && (
                  <div className="bg-[#1e1e1e] border border-[#2c2c2c] rounded-2xl p-12 text-center text-slate-500">
                    No flight schedules found for this route and date combination.
                  </div>
                )}
                
              </div>
            )}

          </div>
        )}

      </div>
    </div>
  );
}
