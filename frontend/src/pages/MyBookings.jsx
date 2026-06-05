import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { Ticket, Plane, Calendar, XCircle, AlertCircle, ShoppingBag, Plus, RefreshCw } from 'lucide-react';

export default function MyBookings() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  const [showBaggageModal, setShowBaggageModal] = useState(false);
  const [selectedPassenger, setSelectedPassenger] = useState(null);
  const [bagTag, setBagTag] = useState('');
  const [weight, setWeight] = useState('');
  const [passengerBaggage, setPassengerBaggage] = useState([]);
  const [loadingBaggage, setLoadingBaggage] = useState(false);

  const navigate = useNavigate();

  const fetchBookings = () => {
    setLoading(true);
    api.get('/bookings/my')
      .then((res) => {
        if (res.data.success) {
          setBookings(res.data.data);
        }
      })
      .catch((err) => setError(err.response?.data?.message || 'Failed to load bookings'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchBookings();
  }, []);

  const handleCancelBooking = async (bookingId) => {
    if (!window.confirm('Are you sure you want to cancel this booking?')) return;
    try {
      const res = await api.post(`/bookings/${bookingId}/cancel`);
      if (res.data.success) {
        fetchBookings();
      }
    } catch (err) {
      alert(err.response?.data?.message || 'Cancellation failed');
    }
  };

  const openBaggageManager = (passenger) => {
    setSelectedPassenger(passenger);
    setShowBaggageModal(true);
    loadPassengerBaggage(passenger.id);
  };

  const loadPassengerBaggage = (passengerId) => {
    setLoadingBaggage(true);
    api.get(`/baggage/passenger/${passengerId}`)
      .then((res) => {
        if (res.data.success) {
          setPassengerBaggage(res.data.data);
        }
      })
      .catch((err) => console.error(err))
      .finally(() => setLoadingBaggage(false));
  };

  const handleBaggageCheckIn = async (e) => {
    e.preventDefault();
    if (!bagTag || !weight) return;
    try {
      const res = await api.post(`/baggage/check-in?passengerId=${selectedPassenger.id}&bagTag=${bagTag.toUpperCase()}&weight=${weight}`);
      if (res.data.success) {
        setBagTag('');
        setWeight('');
        loadPassengerBaggage(selectedPassenger.id);
      }
    } catch (err) {
      alert(err.response?.data?.message || 'Baggage check-in failed');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <div className="flex justify-between items-center mb-8">
        <h2 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
          <Ticket className="h-6 w-6 text-brand-600" />
          <span>My Reservations</span>
        </h2>
        <button onClick={fetchBookings} className="p-2 text-slate-500 hover:text-brand-600 rounded-xl hover:bg-slate-50 transition-colors">
          <RefreshCw className="h-5 w-5" />
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border-l-4 border-red-500 p-4 rounded-xl flex items-center gap-2 text-red-700 text-sm mb-8">
          <AlertCircle className="h-5 w-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="space-y-4">
          {[1, 2].map((n) => (
            <div key={n} className="bg-slate-50 border border-slate-100 rounded-2xl h-48 animate-pulse"></div>
          ))}
        </div>
      ) : bookings.length > 0 ? (
        <div className="space-y-6">
          {bookings.map((booking) => (
            <div key={booking.id} className="bg-white border border-slate-100 rounded-3xl p-6 shadow-sm relative overflow-hidden">
              <div className="flex flex-wrap justify-between items-center gap-4 border-b border-slate-50 pb-4 mb-6">
                <div>
                  <span className="text-xs text-slate-400 block font-semibold uppercase tracking-wider">PNR Reference</span>
                  <span className="text-xl font-black text-brand-600 tracking-widest">{booking.bookingReference}</span>
                </div>
                <div className="flex items-center gap-4">
                  <div>
                    <span className="text-xs text-slate-400 block font-semibold text-right">Total Price</span>
                    <span className="text-lg font-extrabold text-slate-800">₹{booking.totalPrice.toFixed(2)}</span>
                  </div>
                  <div>
                    <span className={`inline-flex px-3 py-1 rounded-full text-xs font-semibold uppercase ${
                      booking.status === 'CONFIRMED'
                        ? 'bg-emerald-50 text-emerald-700'
                        : booking.status === 'PENDING'
                        ? 'bg-amber-50 text-amber-700'
                        : 'bg-slate-100 text-slate-600'
                    }`}>
                      {booking.status}
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex flex-col md:flex-row justify-between gap-6 mb-6">
                <div className="flex-grow">
                  <span className="text-xs font-semibold text-brand-600 bg-brand-50 px-2.5 py-0.5 rounded-full">{booking.flight.flightNumber}</span>
                  <div className="flex items-center gap-6 mt-3">
                    <div>
                      <span className="block text-2xl font-bold text-slate-800">{booking.flight.origin}</span>
                      <span className="text-xs text-slate-400 font-semibold">Departure</span>
                    </div>
                    <div className="flex flex-col items-center min-w-[80px]">
                      <Plane className="h-4 w-4 text-slate-300 rotate-90" />
                      <div className="w-full border-t border-dashed border-slate-200 my-1"></div>
                    </div>
                    <div>
                      <span className="block text-2xl font-bold text-slate-800">{booking.flight.destination}</span>
                      <span className="text-xs text-slate-400 font-semibold">Arrival</span>
                    </div>
                  </div>
                </div>
                <div className="shrink-0 flex flex-col justify-end text-sm text-slate-500">
                  <span className="flex items-center gap-1.5"><Calendar className="h-4 w-4" /> {new Date(booking.flight.departureTime).toLocaleString()}</span>
                </div>
              </div>

              <div className="bg-slate-50 rounded-2xl p-4 border border-slate-200/50 mb-6">
                <span className="text-xs font-bold text-slate-400 uppercase tracking-widest block mb-3">Passenger Manifest</span>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {booking.passengers.map((p) => (
                    <div key={p.id} className="bg-white border border-slate-100 rounded-xl p-3.5 flex justify-between items-center shadow-sm">
                      <div>
                        <span className="block font-semibold text-slate-800 text-sm">{p.firstName} {p.lastName}</span>
                        {p.ticketNumber ? (
                          <span className="text-[10px] text-slate-400 font-bold block mt-0.5 uppercase">Tkt: {p.ticketNumber}</span>
                        ) : (
                          <span className="text-[10px] text-amber-500 font-bold block mt-0.5 uppercase">Ticket Pending</span>
                        )}
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-black text-brand-600 bg-brand-50 px-2 py-0.5 rounded-md">Seat {p.seatNumber}</span>
                        {booking.status === 'CONFIRMED' && (
                          <button
                            onClick={() => openBaggageManager(p)}
                            className="bg-slate-100 hover:bg-brand-50 text-slate-500 hover:text-brand-600 p-1.5 rounded-lg border border-slate-200 hover:border-brand-200 transition-colors"
                            title="Manage Baggage"
                          >
                            <ShoppingBag className="h-4 w-4" />
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {booking.status !== 'CANCELLED' && (
                <div className="flex justify-end border-t border-slate-50 pt-4">
                  <button
                    onClick={() => handleCancelBooking(booking.id)}
                    className="flex items-center gap-1 text-xs text-red-600 hover:text-red-700 font-semibold border border-red-200 hover:border-red-300 bg-red-50/50 px-3.5 py-2 rounded-xl transition-all"
                  >
                    <XCircle className="h-4 w-4" />
                    <span>Cancel Flight</span>
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      ) : (
        <div className="bg-white border border-slate-100 rounded-3xl p-12 text-center text-slate-500 shadow-sm max-w-xl mx-auto">
          <Ticket className="h-12 w-12 text-slate-300 mx-auto mb-4" />
          <h3 className="text-lg font-bold text-slate-800 mb-1">No bookings found</h3>
          <p className="text-sm text-slate-400 mb-6">You haven't scheduled any flight reservations yet.</p>
          <button onClick={() => navigate('/search')} className="bg-brand-600 text-white font-semibold px-6 py-2.5 rounded-xl text-sm transition-all shadow-md">
            Book Flight Now
          </button>
        </div>
      )}

      {showBaggageModal && selectedPassenger && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl border border-slate-100 shadow-xl max-w-lg w-full p-6 relative overflow-hidden">
            <button
              onClick={() => setShowBaggageModal(false)}
              className="absolute top-4 right-4 text-slate-400 hover:text-slate-600 text-lg font-bold"
            >
              ✕
            </button>

            <h3 className="text-lg font-bold text-slate-800 mb-2">Baggage Management</h3>
            <p className="text-xs text-slate-500 mb-6 border-b border-slate-50 pb-4">
              Passenger: <span className="font-semibold text-slate-700">{selectedPassenger.firstName} {selectedPassenger.lastName}</span> (Seat {selectedPassenger.seatNumber})
            </p>

            <form onSubmit={handleBaggageCheckIn} className="bg-slate-50 rounded-2xl p-4 border border-slate-200/50 mb-6 grid grid-cols-1 sm:grid-cols-3 gap-3 items-end">
              <div className="sm:col-span-2">
                <label className="block text-[10px] font-bold text-slate-500 uppercase mb-1">Bag Tag Barcode</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. BAG-889922"
                  value={bagTag}
                  onChange={(e) => setBagTag(e.target.value)}
                  className="block w-full px-3 py-1.5 border border-slate-200 rounded-xl text-xs bg-white uppercase text-slate-800"
                />
              </div>
              <div>
                <label className="block text-[10px] font-bold text-slate-500 uppercase mb-1">Weight (KG)</label>
                <input
                  type="number"
                  required
                  step="0.1"
                  placeholder="23.0"
                  value={weight}
                  onChange={(e) => setWeight(e.target.value)}
                  className="block w-full px-3 py-1.5 border border-slate-200 rounded-xl text-xs bg-white text-slate-800"
                />
              </div>
              <div className="sm:col-span-3">
                <button
                  type="submit"
                  className="w-full bg-brand-600 hover:bg-brand-700 text-white font-semibold py-2 rounded-xl text-xs flex justify-center items-center gap-1"
                >
                  <Plus className="h-4.5 w-4.5" /> Check-In Bag
                </button>
              </div>
            </form>

            <h4 className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-3">Checked Bags</h4>
            {loadingBaggage ? (
              <p className="text-xs text-slate-400">Loading bags...</p>
            ) : passengerBaggage.length > 0 ? (
              <div className="space-y-2 max-h-48 overflow-y-auto">
                {passengerBaggage.map((bag) => (
                  <div key={bag.id} className="bg-white border border-slate-100 rounded-xl p-3 flex justify-between items-center text-xs">
                    <div>
                      <span className="font-bold text-slate-800 uppercase block">{bag.bagTag}</span>
                      <span className="text-[10px] text-slate-400 font-semibold">{bag.weightKg} kg</span>
                    </div>
                    <div>
                      <span className={`inline-flex px-2 py-0.5 rounded-md text-[10px] font-bold uppercase ${
                        bag.status === 'ARRIVED'
                          ? 'bg-emerald-50 text-emerald-600'
                          : bag.status === 'LOST'
                          ? 'bg-red-50 text-red-600'
                          : 'bg-slate-100 text-slate-500'
                      }`}>
                        {bag.status}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-slate-400 text-center py-4 bg-slate-50/50 rounded-xl">No luggage checked in yet.</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
