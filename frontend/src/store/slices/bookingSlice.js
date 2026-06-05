import { createSlice } from '@reduxjs/toolkit';

const bookingSlice = createSlice({
  name: 'booking',
  initialState: {
    currentFlight: null,
    passengers: [],
    selectedSeats: [],
    coupon: null,
    totalPrice: 0,
    activeBooking: null,
    loading: false,
    error: null,
  },
  reducers: {
    setBookingFlight(state, action) {
      state.currentFlight = action.payload;
      state.passengers = [];
      state.selectedSeats = [];
      state.coupon = null;
      state.totalPrice = action.payload ? action.payload.currentPrice : 0;
    },
    updateSelectedSeats(state, action) {
      state.selectedSeats = action.payload;
    },
    setBookingCoupon(state, action) {
      state.coupon = action.payload;
    },
    setPassengersInfo(state, action) {
      state.passengers = action.payload;
    },
    setBookingResponse(state, action) {
      state.activeBooking = action.payload;
    },
    clearBooking(state) {
      state.currentFlight = null;
      state.passengers = [];
      state.selectedSeats = [];
      state.coupon = null;
      state.totalPrice = 0;
      state.activeBooking = null;
    }
  }
});

export const {
  setBookingFlight,
  updateSelectedSeats,
  setBookingCoupon,
  setPassengersInfo,
  setBookingResponse,
  clearBooking
} = bookingSlice.actions;
export default bookingSlice.reducer;
