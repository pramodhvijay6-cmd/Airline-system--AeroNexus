import { createSlice } from '@reduxjs/toolkit';

const flightSlice = createSlice({
  name: 'flights',
  initialState: {
    searchParams: {
      origin: '',
      destination: '',
      departureDate: '',
    },
    searchResults: [],
    loading: false,
    error: null,
  },
  reducers: {
    setSearchParams(state, action) {
      state.searchParams = action.payload;
    },
    setSearchResults(state, action) {
      state.searchResults = action.payload;
    },
    setFlightLoading(state, action) {
      state.loading = action.payload;
    },
    setFlightError(state, action) {
      state.error = action.payload;
    }
  }
});

export const { setSearchParams, setSearchResults, setFlightLoading, setFlightError } = flightSlice.actions;
export default flightSlice.reducer;
