import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { updateSelectedSeats, setPassengersInfo } from '../store/slices/bookingSlice';
import api from '../api';
import { AlertCircle, Armchair, ChevronRight } from 'lucide-react';

export default function SeatSelection() {
  const { currentFlight } = useSelector((state) => state.booking);
  const [occupiedSeats, setOccupiedSeats] = useState([]);
  const [selected, setSelected] = useState([]);
  const [numPassengers, setNumPassengers] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  useEffect(() => {
    if (!currentFlight) {
      navigate('/search');
      return;
    }

    setLoading(true);
    api.get(`/bookings/flight/${currentFlight.id}/occupied-seats`)
      .then((res) => {
        if (res.data.success) {
          setOccupiedSeats(res.data.data);
        }
      })
      .catch((err) => console.error('Error loading occupied seats', err))
      .finally(() => setLoading(false));
  }, [currentFlight, navigate]);

  const handleSeatClick = (seatId) => {
    if (occupiedSeats.includes(seatId)) return;

    if (selected.includes(seatId)) {
      setSelected(selected.filter((s) => s !== seatId));
    } else {
      if (selected.length < numPassengers) {
        setSelected([...selected, seatId]);
      } else {
        setSelected([...selected.slice(1), seatId]);
      }
    }
  };

  const proceedToCheckout = () => {
    if (selected.length !== numPassengers) {
      setError(`Please select exactly ${numPassengers} seat(s) before proceeding.`);
      return;
    }
    dispatch(updateSelectedSeats(selected));

    const placeholders = selected.map((seat) => ({
      firstName: '',
      lastName: '',
      email: '',
      passportNumber: '',
      seatNumber: seat,
    }));
    dispatch(setPassengersInfo(placeholders));
    navigate('/checkout');
  };

  if (!currentFlight) return null;

  const rows = Array.from({ length: 20 }, (_, i) => i + 1);
  const cols = ['A', 'B', 'C', 'D', 'E', 'F'];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Left 2 Cols: Seat Selection Map */}
        <div className="lg:col-span-2 bg-white border border-slate-100 rounded-3xl p-6 shadow-sm">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
              <Armchair className="h-5 w-5 text-brand-600" />
              <span>Select Your Seats</span>
            </h2>
            <div className="flex items-center gap-4">
              <label className="text-sm font-semibold text-slate-600">Passengers:</label>
              <select
                value={numPassengers}
                onChange={(e) => {
                  setNumPassengers(parseInt(e.target.value));
                  setSelected([]);
                }}
                className="bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5 text-sm"
              >
                {[1, 2, 3, 4, 5].map((n) => (
                  <option key={n} value={n}>{n}</option>
                ))}
              </select>
            </div>
          </div>

          {error && (
            <div className="mb-4 bg-red-50 border-l-4 border-red-500 p-4 rounded-xl flex items-center gap-2 text-red-700 text-sm">
              <AlertCircle className="h-5 w-5 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Seat Map Layout */}
          <div className="flex flex-col items-center border-t border-slate-100 pt-8 overflow-x-auto">
            <div className="w-48 h-16 bg-slate-50 rounded-t-full border border-slate-200 flex items-center justify-center mb-12 shadow-inner">
              <span className="text-xs font-bold text-slate-400 tracking-wider">AIRCRAFT FRONT</span>
            </div>

            <div className="space-y-3 min-w-[340px]">
              {rows.map((row) => (
                <div key={row} className="flex items-center justify-center gap-3">
                  <span className="w-6 text-right text-xs font-bold text-slate-400 mr-2">{row}</span>
                  
                  {cols.slice(0, 3).map((col) => {
                    const seatId = `${row}${col}`;
                    const isOccupied = occupiedSeats.includes(seatId);
                    const isSelected = selected.includes(seatId);

                    return (
                      <button
                        key={col}
                        onClick={() => handleSeatClick(seatId)}
                        disabled={isOccupied}
                        className={`h-9 w-9 rounded-xl flex items-center justify-center transition-all ${
                          isOccupied
                            ? 'bg-slate-100 text-slate-300 border border-slate-200 cursor-not-allowed'
                            : isSelected
                            ? 'bg-brand-600 text-white border border-brand-700 shadow-md shadow-brand-100'
                            : 'bg-white hover:bg-brand-50 text-slate-600 border border-slate-200 hover:border-brand-300'
                        }`}
                      >
                        <span className="text-[10px] font-bold">{col}</span>
                      </button>
                    );
                  })}

                  <div className="w-8 text-center text-[10px] font-bold text-slate-300 uppercase tracking-widest mx-1 select-none">
                    AISLE
                  </div>

                  {cols.slice(3, 6).map((col) => {
                    const seatId = `${row}${col}`;
                    const isOccupied = occupiedSeats.includes(seatId);
                    const isSelected = selected.includes(seatId);

                    return (
                      <button
                        key={col}
                        onClick={() => handleSeatClick(seatId)}
                        disabled={isOccupied}
                        className={`h-9 w-9 rounded-xl flex items-center justify-center transition-all ${
                          isOccupied
                            ? 'bg-slate-100 text-slate-300 border border-slate-200 cursor-not-allowed'
                            : isSelected
                            ? 'bg-brand-600 text-white border border-brand-700 shadow-md shadow-brand-100'
                            : 'bg-white hover:bg-brand-50 text-slate-600 border border-slate-200 hover:border-brand-300'
                        }`}
                      >
                        <span className="text-[10px] font-bold">{col}</span>
                      </button>
                    );
                  })}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right 1 Col: Summary card */}
        <div className="bg-white border border-slate-100 rounded-3xl p-6 shadow-sm h-fit">
          <h3 className="text-lg font-bold text-slate-800 mb-6">Flight Summary</h3>
          
          <div className="space-y-4 mb-6 border-b border-slate-50 pb-6 text-sm">
            <div className="flex justify-between">
              <span className="text-slate-400">Flight</span>
              <span className="font-semibold text-slate-800">{currentFlight.flightNumber}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Route</span>
              <span className="font-semibold text-slate-800">{currentFlight.origin} → {currentFlight.destination}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Departure</span>
              <span className="font-semibold text-slate-800">{new Date(currentFlight.departureTime).toLocaleDateString()}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Selected Seats</span>
              <span className="font-bold text-brand-600">
                {selected.length > 0 ? selected.join(', ') : 'none'}
              </span>
            </div>
          </div>

          <div className="flex justify-between items-center mb-6">
            <span className="text-slate-500 font-medium">Estimated Total</span>
            <span className="text-2xl font-extrabold text-slate-800">
              ₹{(currentFlight.currentPrice * numPassengers).toFixed(2)}
            </span>
          </div>

          <button
            onClick={proceedToCheckout}
            className="w-full bg-brand-600 hover:bg-brand-700 text-white font-semibold py-3 rounded-xl transition-all shadow-md shadow-brand-100 flex items-center justify-center gap-1.5"
          >
            <span>Proceed to Checkout</span>
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>

      </div>
    </div>
  );
}
