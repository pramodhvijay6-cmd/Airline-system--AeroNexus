import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice';
import bookingReducer from './slices/bookingSlice';
import flightReducer from './slices/flightSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    booking: bookingReducer,
    flights: flightReducer,
  },
});
