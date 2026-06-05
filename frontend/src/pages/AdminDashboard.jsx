import React, { useEffect, useState } from 'react';
import api from '../api';
import { Plane, History, TrendingUp, AlertTriangle, Plus, Trash2 } from 'lucide-react';

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('analytics');
  
  const [stats, setStats] = useState(null);
  const [userStats, setUserStats] = useState(null);
  const [auditLogs, setAuditLogs] = useState([]);
  const [flights, setFlights] = useState([]);
  const [loading, setLoading] = useState(true);

  const [showFlightModal, setShowFlightModal] = useState(false);
  const [flightForm, setFlightForm] = useState({
    flightNumber: '',
    routeId: 1,
    aircraftId: 1,
    departureTime: '',
    arrivalTime: '',
    basePrice: '',
  });

  const fetchData = async () => {
    setLoading(true);
    try {
      const statsRes = await api.get('/analytics');
      const userRes = await api.get('/admin/users/stats');
      const logsRes = await api.get('/admin/audit-logs');
      const flightsRes = await api.get('/flights');

      if (statsRes.data.success) setStats(statsRes.data.data);
      if (userRes.data.success) setUserStats(userRes.data.data);
      if (logsRes.data.success) setAuditLogs(logsRes.data.data);
      if (flightsRes.data.success) setFlights(flightsRes.data.data);
    } catch (err) {
      console.error('Error fetching admin data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCreateFlight = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...flightForm,
        basePrice: parseFloat(flightForm.basePrice),
        routeId: parseInt(flightForm.routeId),
        aircraftId: parseInt(flightForm.aircraftId),
        departureTime: new Date(flightForm.departureTime).toISOString().slice(0, 19),
        arrivalTime: new Date(flightForm.arrivalTime).toISOString().slice(0, 19),
      };
      const res = await api.post('/flights', payload);
      if (res.data.success) {
        setShowFlightModal(false);
        fetchData();
      }
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to create flight');
    }
  };

  const handleDeleteFlight = async (id) => {
    if (!window.confirm('Are you sure you want to delete this flight?')) return;
    try {
      const res = await api.delete(`/flights/${id}`);
      if (res.data.success) {
        fetchData();
      }
    } catch (err) {
      alert(err.response?.data?.message || 'Delete failed');
    }
  };

  if (loading) {
    return <div className="p-12 text-center text-slate-500">Loading admin console...</div>;
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8">
        <div>
          <h2 className="text-3xl font-black text-slate-800">Control Panel</h2>
          <p className="text-sm text-slate-500">System management, analytics, and activity audit logs</p>
        </div>

        {/* Tab Selection */}
        <div className="flex gap-2 bg-slate-100 p-1 rounded-xl">
          <button
            onClick={() => setActiveTab('analytics')}
            className={`flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-semibold transition-all ${
              activeTab === 'analytics' ? 'bg-white text-brand-600 shadow-sm' : 'text-slate-600 hover:text-slate-800'
            }`}
          >
            <TrendingUp className="h-4 w-4" />
            <span>Analytics</span>
          </button>
          <button
            onClick={() => setActiveTab('flights')}
            className={`flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-semibold transition-all ${
              activeTab === 'flights' ? 'bg-white text-brand-600 shadow-sm' : 'text-slate-600 hover:text-slate-800'
            }`}
          >
            <Plane className="h-4 w-4" />
            <span>Flights</span>
          </button>
          <button
            onClick={() => setActiveTab('audit')}
            className={`flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-semibold transition-all ${
              activeTab === 'audit' ? 'bg-white text-brand-600 shadow-sm' : 'text-slate-600 hover:text-slate-800'
            }`}
          >
            <History className="h-4 w-4" />
            <span>Audit Logs</span>
          </button>
        </div>
      </div>

      {activeTab === 'analytics' && stats && (
        <div className="space-y-8">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            <div className="bg-white border border-slate-100 p-6 rounded-2xl shadow-sm">
              <span className="text-xs font-bold text-slate-400 block uppercase tracking-wider mb-2">Total Revenue</span>
              <span className="text-2xl font-black text-slate-800">₹{stats.totalRevenue.toFixed(2)}</span>
            </div>
            <div className="bg-white border border-slate-100 p-6 rounded-2xl shadow-sm">
              <span className="text-xs font-bold text-slate-400 block uppercase tracking-wider mb-2">Occupancy Rate</span>
              <span className="text-2xl font-black text-slate-800">{stats.averageOccupancyPercentage.toFixed(1)}%</span>
            </div>
            <div className="bg-white border border-slate-100 p-6 rounded-2xl shadow-sm">
              <span className="text-xs font-bold text-slate-400 block uppercase tracking-wider mb-2">Total Bookings</span>
              <span className="text-2xl font-black text-slate-800">{stats.totalBookingsCount}</span>
            </div>
            {userStats && (
              <div className="bg-white border border-slate-100 p-6 rounded-2xl shadow-sm">
                <span className="text-xs font-bold text-slate-400 block uppercase tracking-wider mb-2">Registered Users</span>
                <span className="text-2xl font-black text-slate-800">{userStats.totalUsers}</span>
              </div>
            )}
          </div>

          <div className="bg-white border border-slate-100 rounded-3xl p-6 shadow-sm">
            <h3 className="text-lg font-bold text-slate-800 mb-4">Daily Booking Volume</h3>
            <div className="space-y-3">
              {Object.entries(stats.bookingTrendsLast7Days).map(([date, count]) => (
                <div key={date} className="flex justify-between items-center text-sm border-b border-slate-50 py-2">
                  <span className="text-slate-500 font-medium">{date}</span>
                  <span className="font-bold text-slate-800">{count} bookings</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {activeTab === 'flights' && (
        <div className="space-y-6">
          <div className="flex justify-between items-center">
            <h3 className="text-lg font-bold text-slate-800">Flight Schedules</h3>
            <button
              onClick={() => setShowFlightModal(true)}
              className="bg-brand-600 hover:bg-brand-700 text-white font-semibold py-2 px-4 rounded-xl text-sm flex items-center gap-1"
            >
              <Plus className="h-4 w-4" /> Create Flight
            </button>
          </div>

          <div className="bg-white border border-slate-100 rounded-3xl shadow-sm overflow-hidden">
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 font-semibold uppercase text-xs">
                  <th className="p-4">Flight</th>
                  <th className="p-4">Route</th>
                  <th className="p-4">Aircraft</th>
                  <th className="p-4">Departure</th>
                  <th className="p-4">Base Price</th>
                  <th className="p-4">Status</th>
                  <th className="p-4 text-center">Actions</th>
                </tr>
              </thead>
              <tbody>
                {flights.map((flight) => (
                  <tr key={flight.id} className="border-b border-slate-50 hover:bg-slate-50/50">
                    <td className="p-4 font-bold text-brand-600">{flight.flightNumber}</td>
                    <td className="p-4">{flight.origin} → {flight.destination}</td>
                    <td className="p-4">{flight.aircraftModel}</td>
                    <td className="p-4">{new Date(flight.departureTime).toLocaleString()}</td>
                    <td className="p-4 font-semibold">₹{flight.basePrice.toFixed(2)}</td>
                    <td className="p-4">
                      <span className={`px-2.5 py-0.5 rounded-full text-xs font-semibold uppercase ${
                        flight.status === 'SCHEDULED' ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'
                      }`}>
                        {flight.status}
                      </span>
                    </td>
                    <td className="p-4 text-center">
                      <button
                        onClick={() => handleDeleteFlight(flight.id)}
                        className="text-red-500 hover:text-red-700 p-1"
                        title="Delete flight"
                      >
                        <Trash2 className="h-4.5 w-4.5" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {activeTab === 'audit' && (
        <div className="bg-white border border-slate-100 rounded-3xl p-6 shadow-sm">
          <h3 className="text-lg font-bold text-slate-800 mb-6 flex items-center gap-1.5"><AlertTriangle className="h-5 w-5 text-amber-500" /> Administrative Audit Records</h3>
          
          <div className="space-y-4 max-h-[600px] overflow-y-auto pr-2">
            {auditLogs.map((log) => (
              <div key={log.id} className="bg-slate-50 border border-slate-200/50 rounded-2xl p-4 text-xs">
                <div className="flex justify-between items-start gap-4 mb-2">
                  <div>
                    <span className="bg-slate-200 text-slate-700 font-bold px-2 py-0.5 rounded uppercase tracking-wider text-[10px]">
                      {log.action}
                    </span>
                    <span className="text-slate-400 ml-2">on {log.entityName} (ID: {log.entityId})</span>
                  </div>
                  <span className="text-slate-400">{new Date(log.timestamp).toLocaleString()}</span>
                </div>
                <div className="space-y-1">
                  <p className="text-slate-600"><span className="font-semibold text-slate-700">Triggered by:</span> {log.user ? log.user.username : 'SYSTEM'}</p>
                  {log.changeLog && <p className="text-slate-500 font-mono text-[10px] bg-white p-2 rounded-lg border border-slate-100 mt-1">{log.changeLog}</p>}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Create Flight Modal */}
      {showFlightModal && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl border border-slate-100 shadow-xl max-w-lg w-full p-6 relative overflow-hidden">
            <button
              onClick={() => setShowFlightModal(false)}
              className="absolute top-4 right-4 text-slate-400 hover:text-slate-600 text-lg font-bold"
            >
              ✕
            </button>
            <h3 className="text-lg font-bold text-slate-800 mb-6">Schedule New Flight</h3>
            
            <form onSubmit={handleCreateFlight} className="space-y-4 text-xs">
              <div>
                <label className="block font-semibold text-slate-500 mb-1">Flight Number</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. SF-102"
                  value={flightForm.flightNumber}
                  onChange={(e) => setFlightForm({ ...flightForm, flightNumber: e.target.value })}
                  className="block w-full px-3 py-2 border border-slate-200 rounded-xl bg-slate-50/50"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block font-semibold text-slate-500 mb-1">Route ID</label>
                  <input
                    type="number"
                    required
                    value={flightForm.routeId}
                    onChange={(e) => setFlightForm({ ...flightForm, routeId: e.target.value })}
                    className="block w-full px-3 py-2 border border-slate-200 rounded-xl bg-slate-50/50"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-500 mb-1">Aircraft ID</label>
                  <input
                    type="number"
                    required
                    value={flightForm.aircraftId}
                    onChange={(e) => setFlightForm({ ...flightForm, aircraftId: e.target.value })}
                    className="block w-full px-3 py-2 border border-slate-200 rounded-xl bg-slate-50/50"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block font-semibold text-slate-500 mb-1">Departure Time</label>
                  <input
                    type="datetime-local"
                    required
                    value={flightForm.departureTime}
                    onChange={(e) => setFlightForm({ ...flightForm, departureTime: e.target.value })}
                    className="block w-full px-3 py-2 border border-slate-200 rounded-xl bg-slate-50/50"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-500 mb-1">Arrival Time</label>
                  <input
                    type="datetime-local"
                    required
                    value={flightForm.arrivalTime}
                    onChange={(e) => setFlightForm({ ...flightForm, arrivalTime: e.target.value })}
                    className="block w-full px-3 py-2 border border-slate-200 rounded-xl bg-slate-50/50"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-500 mb-1">Base Price ($)</label>
                <input
                  type="number"
                  step="0.01"
                  required
                  placeholder="299.99"
                  value={flightForm.basePrice}
                  onChange={(e) => setFlightForm({ ...flightForm, basePrice: e.target.value })}
                  className="block w-full px-3 py-2 border border-slate-200 rounded-xl bg-slate-50/50"
                />
              </div>

              <div className="pt-4">
                <button
                  type="submit"
                  className="w-full bg-brand-600 hover:bg-brand-700 text-white font-semibold py-2.5 rounded-xl transition-all shadow-md"
                >
                  Create and Publish Flight
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
