import React, { useEffect, useState } from 'react';
import api from '../api';
import { Award, Star, History, Gift, Shield } from 'lucide-react';

export default function Profile() {
  const [loyalty, setLoyalty] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get('/loyalty')
      .then((res) => {
        if (res.data.success) {
          setLoyalty(res.data.data);
        }
      })
      .catch((err) => console.error('Error fetching loyalty stats', err))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="max-w-md mx-auto text-center py-12 animate-pulse">
        <div className="h-32 bg-slate-50 border border-slate-100 rounded-3xl mb-4"></div>
      </div>
    );
  }

  const balance = loyalty?.currentBalance || 0;
  const tier = loyalty?.tier || 'BRONZE';
  const transactions = loyalty?.transactions || [];

  let nextTier = 'SILVER';
  let targetPoints = 1000;
  let prevPoints = 0;

  if (tier === 'SILVER') {
    nextTier = 'GOLD';
    targetPoints = 5000;
    prevPoints = 1000;
  } else if (tier === 'GOLD') {
    nextTier = 'PLATINUM';
    targetPoints = 10000;
    prevPoints = 5000;
  } else if (tier === 'PLATINUM') {
    nextTier = 'MAXIMUM';
    targetPoints = 10000;
    prevPoints = 0;
  }

  const progressPercentage = tier === 'PLATINUM' 
    ? 100 
    : Math.min(100, Math.max(0, ((balance - prevPoints) / (targetPoints - prevPoints)) * 100));

  return (
    <div className="max-w-4xl mx-auto px-4 py-12">
      {/* Loyalty Status Hero Card */}
      <div className="bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 rounded-3xl p-8 text-white shadow-lg relative overflow-hidden mb-8">
        <div className="absolute top-0 right-0 w-32 h-32 bg-indigo-500/10 rounded-full blur-2xl"></div>
        <div className="flex items-center gap-4 mb-6">
          <div className="bg-white/10 p-3 rounded-2xl border border-white/10 text-amber-400">
            <Award className="h-8 w-8" />
          </div>
          <div>
            <span className="text-xs font-semibold uppercase tracking-wider text-indigo-300">Loyalty Program Member</span>
            <h2 className="text-2xl font-bold tracking-tight">{tier} MEMBER</h2>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-end">
          <div>
            <span className="text-sm text-indigo-200 block font-medium">Current Balance</span>
            <span className="text-5xl font-black tracking-tight">{balance} <span className="text-sm font-semibold text-indigo-300">points</span></span>
          </div>

          {tier !== 'PLATINUM' && (
            <div className="space-y-2">
              <div className="flex justify-between text-xs font-semibold text-indigo-200">
                <span>Next Tier: {nextTier}</span>
                <span>{balance} / {targetPoints} pts</span>
              </div>
              <div className="w-full bg-white/10 rounded-full h-2.5 overflow-hidden border border-white/5">
                <div 
                  className="bg-brand-500 h-2.5 rounded-full transition-all duration-500" 
                  style={{ width: `${progressPercentage}%` }}
                ></div>
              </div>
              <span className="block text-[10px] text-indigo-300 italic">
                Earn {targetPoints - balance} more points to reach {nextTier} tier!
              </span>
            </div>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        
        <div className="bg-white border border-slate-100 rounded-3xl p-6 shadow-sm h-fit space-y-4">
          <h3 className="text-base font-bold text-slate-800 flex items-center gap-1.5"><Shield className="h-4 w-4 text-brand-600" /> Tier Benefits</h3>
          <div className="space-y-3 text-xs text-slate-600">
            <div className="flex items-start gap-2">
              <Star className="h-4 w-4 text-amber-500 fill-amber-500 shrink-0" />
              <span>Bronze: 1 point per $10 spent on all bookings.</span>
            </div>
            <div className="flex items-start gap-2">
              <Star className="h-4 w-4 text-slate-400 fill-slate-400 shrink-0" />
              <span>Silver: 10% bonus points on bookings.</span>
            </div>
            <div className="flex items-start gap-2">
              <Star className="h-4 w-4 text-yellow-500 fill-yellow-500 shrink-0" />
              <span>Gold: 25% bonus points and priority waitlisting.</span>
            </div>
            <div className="flex items-start gap-2">
              <Star className="h-4 w-4 text-indigo-400 fill-indigo-400 shrink-0" />
              <span>Platinum: 50% bonus points and complimentary baggage checks.</span>
            </div>
          </div>
        </div>

        <div className="md:col-span-2 bg-white border border-slate-100 rounded-3xl p-6 shadow-sm">
          <h3 className="text-base font-bold text-slate-800 flex items-center gap-1.5 mb-6">
            <History className="h-4 w-4 text-brand-600" />
            <span>Points Transaction Ledger</span>
          </h3>

          {transactions.length > 0 ? (
            <div className="space-y-4 max-h-[400px] overflow-y-auto pr-2">
              {transactions.map((tx) => (
                <div key={tx.id} className="flex justify-between items-center py-2.5 border-b border-slate-50 last:border-b-0 text-xs">
                  <div>
                    <span className="font-semibold text-slate-800 block">
                      {tx.type === 'EARNED' ? 'Earned points from booking' : 'Redeemed points at checkout'}
                    </span>
                    <span className="text-[10px] text-slate-400">{new Date(tx.transactionDate).toLocaleString()}</span>
                  </div>
                  <div>
                    <span className={`font-extrabold text-sm ${
                      tx.type === 'EARNED' ? 'text-emerald-600' : 'text-red-600'
                    }`}>
                      {tx.type === 'EARNED' ? '+' : '-'}{tx.points}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center text-slate-400 py-12">
              <Gift className="h-10 w-10 text-slate-200 mx-auto mb-3" />
              <p className="text-xs">No reward transactions recorded yet.</p>
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
