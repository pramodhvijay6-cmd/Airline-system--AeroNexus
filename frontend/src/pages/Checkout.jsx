import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { clearBooking } from '../store/slices/bookingSlice';
import api from '../api';
import { AlertCircle, CreditCard, Tag, Sparkles, CheckCircle2, Ticket } from 'lucide-react';

export default function Checkout() {
  const { currentFlight, selectedSeats, passengers } = useSelector((state) => state.booking);
  const [passengerDetails, setPassengerDetails] = useState(
    passengers.map((p) => ({ ...p }))
  );
  
  const [couponCode, setCouponCode] = useState('');
  const [appliedCoupon, setAppliedCoupon] = useState(null);
  const [couponError, setCouponError] = useState(null);

  const [paymentMethod, setPaymentMethod] = useState('credit_card');
  const [cardDetails, setCardDetails] = useState({ number: '', expiry: '', cvv: '' });
  const [upiId, setUpiId] = useState('');
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [confirmedBooking, setConfirmedBooking] = useState(null);

  const navigate = useNavigate();
  const dispatch = useDispatch();

  if (!currentFlight || selectedSeats.length === 0) {
    return (
      <div className="max-w-md mx-auto text-center py-12">
        <p className="text-slate-500 mb-4">No active booking session found.</p>
        <button onClick={() => navigate('/search')} className="bg-brand-600 text-white px-6 py-2 rounded-xl text-sm font-semibold">
          Search Flights
        </button>
      </div>
    );
  }

  const subtotal = currentFlight.currentPrice * selectedSeats.length;

  const handlePassengerChange = (index, field, val) => {
    const updated = [...passengerDetails];
    updated[index][field] = val;
    setPassengerDetails(updated);
  };

  const handleApplyCoupon = async () => {
    if (!couponCode) return;
    setCouponError(null);
    try {
      const res = await api.get(`/coupons/validate?code=${couponCode}&amount=${subtotal}`);
      if (res.data.success) {
        setAppliedCoupon(res.data.data);
      } else {
        setCouponError(res.data.message);
      }
    } catch (err) {
      setCouponError(err.response?.data?.message || 'Invalid coupon code');
    }
  };

  const calculateDiscount = () => {
    if (!appliedCoupon) return 0;
    if (appliedCoupon.discountType === 'PERCENTAGE') {
      const val = subtotal * (appliedCoupon.discountValue / 100);
      return appliedCoupon.maxDiscount ? Math.min(val, appliedCoupon.maxDiscount) : val;
    }
    return appliedCoupon.discountValue;
  };

  const discount = calculateDiscount();
  const totalPrice = Math.max(0, subtotal - discount);

  const handleCheckout = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const bookingPayload = {
        flightId: currentFlight.id,
        couponCode: appliedCoupon?.code || null,
        passengers: passengerDetails,
      };

      const bookingRes = await api.post('/bookings', bookingPayload);
      if (!bookingRes.data.success) {
        throw new Error(bookingRes.data.message || 'Failed to initiate booking.');
      }

      const bookingData = bookingRes.data.data;

      const paymentPayload = {
        bookingId: bookingData.id,
        paymentMethod: paymentMethod === 'credit_card' ? 'CREDIT_CARD' : 'UPI',
        details: paymentMethod === 'credit_card' ? cardDetails.number : upiId,
      };

      const paymentRes = await api.post('/payments/process', paymentPayload);
      if (paymentRes.data.success) {
        setConfirmedBooking({
          ...bookingData,
          pnr: bookingData.bookingReference,
          ticketDetails: paymentRes.data.data
        });
        dispatch(clearBooking());
      } else {
        throw new Error(paymentRes.data.message || 'Payment processing failed.');
      }
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Checkout failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  if (confirmedBooking) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-16 text-center">
        <div className="bg-white border border-slate-100 rounded-3xl p-8 md:p-12 shadow-md relative overflow-hidden">
          <div className="absolute top-0 inset-x-0 h-2 bg-emerald-500"></div>
          <div className="flex justify-center text-emerald-500 mb-6">
            <CheckCircle2 className="h-16 w-16" />
          </div>
          <h2 className="text-3xl font-extrabold text-slate-800 mb-2">Booking Confirmed!</h2>
          <p className="text-slate-500 mb-8 leading-relaxed">
            Your payment was processed successfully. A confirmation email with your e-tickets has been sent.
          </p>

          <div className="bg-slate-50 rounded-2xl p-6 mb-8 text-left border border-slate-200/50">
            <div className="flex justify-between items-center pb-4 border-b border-slate-200/50 mb-4">
              <div>
                <span className="text-xs text-slate-400 block uppercase font-bold tracking-wider">PNR Reference</span>
                <span className="text-2xl font-black text-brand-600 tracking-widest">{confirmedBooking.pnr}</span>
              </div>
              <div className="text-right">
                <span className="text-xs text-slate-400 block uppercase font-bold tracking-wider">Total Paid</span>
                <span className="text-2xl font-black text-slate-800">₹{totalPrice.toFixed(2)}</span>
              </div>
            </div>

            <div className="space-y-3">
              <div>
                <span className="text-xs text-slate-400 block font-semibold">Flight</span>
                <span className="text-sm font-bold text-slate-800">
                  {currentFlight.flightNumber} ({currentFlight.origin} → {currentFlight.destination})
                </span>
              </div>
              <div>
                <span className="text-xs text-slate-400 block font-semibold">Travelers</span>
                <div className="space-y-1.5 mt-1">
                  {passengerDetails.map((p, idx) => (
                    <div key={idx} className="flex justify-between text-xs text-slate-700 bg-white p-2 rounded-lg border border-slate-100">
                      <span>{p.firstName} {p.lastName}</span>
                      <span className="font-bold text-slate-500">Seat {p.seatNumber}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <button
              onClick={() => navigate('/bookings')}
              className="bg-brand-600 hover:bg-brand-700 text-white font-semibold py-3 px-8 rounded-xl transition-all shadow-md flex items-center justify-center gap-1.5"
            >
              <Ticket className="h-5 w-5" />
              <span>Go to My Bookings</span>
            </button>
            <button
              onClick={() => navigate('/')}
              className="bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold py-3 px-8 rounded-xl transition-all"
            >
              Back to Home
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <form onSubmit={handleCheckout} className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        <div className="lg:col-span-2 space-y-8">
          
          <div className="bg-white border border-slate-100 rounded-3xl p-6 shadow-sm">
            <h2 className="text-xl font-bold text-slate-800 mb-6">Traveler Information</h2>
            
            <div className="space-y-6">
              {passengerDetails.map((p, idx) => (
                <div key={idx} className="p-4 bg-slate-50 border border-slate-200/50 rounded-2xl">
                  <div className="flex justify-between items-center mb-4">
                    <span className="text-xs font-bold text-slate-400 uppercase tracking-widest">Traveler #{idx + 1}</span>
                    <span className="text-xs font-bold text-brand-600 bg-brand-50 px-2 py-0.5 rounded-md">Seat {p.seatNumber}</span>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold text-slate-500 mb-1">First Name</label>
                      <input
                        type="text"
                        required
                        value={p.firstName}
                        onChange={(e) => handlePassengerChange(idx, 'firstName', e.target.value)}
                        className="block w-full px-3 py-2 border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:ring-1 focus:ring-brand-500 text-sm bg-white"
                        placeholder="John"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-500 mb-1">Last Name</label>
                      <input
                        type="text"
                        required
                        value={p.lastName}
                        onChange={(e) => handlePassengerChange(idx, 'lastName', e.target.value)}
                        className="block w-full px-3 py-2 border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:ring-1 focus:ring-brand-500 text-sm bg-white"
                        placeholder="Doe"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-500 mb-1">Email Address</label>
                      <input
                        type="email"
                        value={p.email || ''}
                        onChange={(e) => handlePassengerChange(idx, 'email', e.target.value)}
                        className="block w-full px-3 py-2 border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:ring-1 focus:ring-brand-500 text-sm bg-white"
                        placeholder="traveler@example.com"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-500 mb-1">Passport Number</label>
                      <input
                        type="text"
                        value={p.passportNumber || ''}
                        onChange={(e) => handlePassengerChange(idx, 'passportNumber', e.target.value)}
                        className="block w-full px-3 py-2 border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:ring-1 focus:ring-brand-500 text-sm bg-white"
                        placeholder="e.g. L00000000"
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-white border border-slate-100 rounded-3xl p-6 shadow-sm">
            <h2 className="text-xl font-bold text-slate-800 mb-6 flex items-center gap-2">
              <CreditCard className="h-5 w-5 text-brand-600" />
              <span>Simulated Payment</span>
            </h2>

            <div className="flex gap-4 mb-6">
              <button
                type="button"
                onClick={() => setPaymentMethod('credit_card')}
                className={`flex-1 py-3 border rounded-xl text-sm font-semibold transition-all ${
                  paymentMethod === 'credit_card'
                    ? 'border-brand-500 bg-brand-50/50 text-brand-700 shadow-sm'
                    : 'border-slate-200 hover:border-slate-300 text-slate-600 bg-white'
                }`}
              >
                Credit Card
              </button>
              <button
                type="button"
                onClick={() => setPaymentMethod('upi')}
                className={`flex-1 py-3 border rounded-xl text-sm font-semibold transition-all ${
                  paymentMethod === 'upi'
                    ? 'border-brand-500 bg-brand-50/50 text-brand-700 shadow-sm'
                    : 'border-slate-200 hover:border-slate-300 text-slate-600 bg-white'
                }`}
              >
                UPI ID
              </button>
            </div>

            {paymentMethod === 'credit_card' ? (
              <div className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1">Card Number</label>
                  <input
                    type="text"
                    required
                    value={cardDetails.number}
                    onChange={(e) => setCardDetails({ ...cardDetails, number: e.target.value })}
                    placeholder="4111 2222 3333 4444"
                    className="block w-full px-3 py-2 border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:ring-1 focus:ring-brand-500 text-sm bg-white"
                  />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-500 mb-1">Expiration Date</label>
                    <input
                      type="text"
                      required
                      value={cardDetails.expiry}
                      onChange={(e) => setCardDetails({ ...cardDetails, expiry: e.target.value })}
                      placeholder="MM/YY"
                      className="block w-full px-3 py-2 border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:ring-1 focus:ring-brand-500 text-sm bg-white"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-500 mb-1">CVV</label>
                    <input
                      type="password"
                      required
                      value={cardDetails.cvv}
                      onChange={(e) => setCardDetails({ ...cardDetails, cvv: e.target.value })}
                      placeholder="•••"
                      className="block w-full px-3 py-2 border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:ring-1 focus:ring-brand-500 text-sm bg-white"
                    />
                  </div>
                </div>
              </div>
            ) : (
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">UPI ID Address</label>
                <input
                  type="text"
                  required={paymentMethod === 'upi'}
                  value={upiId}
                  onChange={(e) => setUpiId(e.target.value)}
                  placeholder="e.g. traveler@upi"
                  className="block w-full px-3 py-2 border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:ring-1 focus:ring-brand-500 text-sm bg-white"
                />
              </div>
            )}
          </div>
        </div>

        <div className="space-y-6">
          <div className="bg-white border border-slate-100 rounded-3xl p-6 shadow-sm">
            <h3 className="text-lg font-bold text-slate-800 mb-6">Price Summary</h3>
            
            <div className="space-y-4 mb-6 border-b border-slate-100 pb-6 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-400">Flight Fare ({selectedSeats.length} traveler)</span>
                <span className="font-semibold text-slate-800">₹{subtotal.toFixed(2)}</span>
              </div>
              
              {appliedCoupon && (
                <div className="flex justify-between text-emerald-600 bg-emerald-50 p-2.5 rounded-xl border border-emerald-100">
                  <span className="flex items-center gap-1 font-semibold"><Sparkles className="h-4 w-4" /> Code {appliedCoupon.code}</span>
                  <span className="font-bold">-₹{discount.toFixed(2)}</span>
                </div>
              )}
            </div>

            <div className="mb-6">
              <label className="block text-xs font-semibold text-slate-500 uppercase mb-2">Have a promo code?</label>
              <div className="flex gap-2">
                <div className="relative flex-grow">
                  <Tag className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                  <input
                    type="text"
                    value={couponCode}
                    onChange={(e) => setCouponCode(e.target.value)}
                    placeholder="COUPONCODE"
                    className="block w-full pl-9 pr-3 py-2 border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:ring-1 focus:ring-brand-500 text-sm uppercase bg-slate-50/50"
                  />
                </div>
                <button
                  type="button"
                  onClick={handleApplyCoupon}
                  className="bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold py-2 px-4 rounded-xl text-xs transition-colors"
                >
                  Apply
                </button>
              </div>
              {couponError && <p className="text-xs text-red-500 mt-1">{couponError}</p>}
            </div>

            <div className="flex justify-between items-center mb-6 pt-4 border-t border-slate-50">
              <span className="text-slate-500 font-medium">Grand Total</span>
              <span className="text-3xl font-black text-slate-800">
                ₹{totalPrice.toFixed(2)}
              </span>
            </div>

            {error && (
              <div className="mb-4 bg-red-50 border-l-4 border-red-500 p-3 rounded-r-xl flex items-start gap-2 text-red-700 text-xs">
                <AlertCircle className="h-4 w-4 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-brand-600 hover:bg-brand-700 disabled:bg-brand-400 text-white font-bold py-3.5 rounded-xl transition-all shadow-md shadow-brand-100 flex items-center justify-center gap-1.5"
            >
              {loading ? 'Processing Transaction...' : 'Pay and Confirm Booking'}
            </button>
          </div>
        </div>

      </form>
    </div>
  );
}
