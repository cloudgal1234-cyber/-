'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import api from '@/lib/api';
import { formatDate, MEDIA_TYPE_LABELS, STATUS_COLORS, STATUS_LABELS } from '@/lib/utils';

interface Stats { campaigns: number; generations: number; }
interface RecentGen { id: string; mediaType: string; status: string; createdAt: string; campaign: { title: string }; thumbnailUrl?: string; }

export default function DashboardPage() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [recent, setRecent] = useState<RecentGen[]>([]);

  useEffect(() => {
    Promise.all([
      api.get('/api/campaigns').then((r) => r.data),
      api.get('/api/generations?limit=6').then((r) => r.data),
    ]).then(([campaigns, gens]) => {
      setStats({ campaigns: campaigns.length, generations: gens.total });
      setRecent(gens.items);
    });
  }, []);

  return (
    <div className="p-8 space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">ברוך הבא 👋</h1>
        <p className="text-gray-500 text-sm mt-1">הנה מה שקורה עם התוכן שלך</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {[
          { label: 'קמפיינים', value: stats?.campaigns ?? '—', sub: 'נוצרו', color: 'text-gray-900' },
          { label: 'יצירות', value: stats?.generations ?? '—', sub: 'נכסים נוצרו', color: 'text-gray-900' },
        ].map((s) => (
          <div key={s.label} className="card p-5">
            <p className="text-sm text-gray-500">{s.label}</p>
            <p className={`text-3xl font-bold mt-1 ${s.color}`}>{s.value}</p>
            <p className="text-xs text-gray-400 mt-0.5">{s.sub}</p>
          </div>
        ))}
      </div>

      <div className="flex gap-3">
        <Link href="/campaigns/new" className="btn-primary">+ קמפיין חדש</Link>
        <Link href="/gallery" className="btn-secondary">לגלריה</Link>
      </div>

      <div>
        <h2 className="text-lg font-semibold mb-4">יצירות אחרונות</h2>
        {recent.length === 0 ? (
          <div className="card p-10 text-center text-gray-400">
            <p className="text-4xl mb-2">🎬</p>
            <p>אין יצירות עדיין. <Link href="/campaigns/new" className="text-brand-600 hover:underline">צור את הקמפיין הראשון שלך</Link>.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {recent.map((g) => (
              <div key={g.id} className="card overflow-hidden">
                <div className="h-36 bg-gradient-to-br from-brand-100 to-brand-200 flex items-center justify-center text-4xl">
                  {g.mediaType === 'VIDEO' || g.mediaType === 'UGC' ? '🎬' : g.mediaType === 'IMAGE' ? '📸' : '🎵'}
                </div>
                <div className="p-4">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-sm font-medium">{MEDIA_TYPE_LABELS[g.mediaType]}</span>
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_COLORS[g.status]}`}>{STATUS_LABELS[g.status] ?? g.status}</span>
                  </div>
                  <p className="text-xs text-gray-400">{g.campaign.title}</p>
                  <p className="text-xs text-gray-300 mt-1">{formatDate(g.createdAt)}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
