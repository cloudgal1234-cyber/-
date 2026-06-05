'use client';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { useAuthStore } from '@/store';

interface FormData { email: string; password: string; }

const GITHUB_AUTH_URL = '/api/auth/github';
const GOOGLE_AUTH_URL = '/api/auth/google';

export default function LoginPage() {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>();
  const { login } = useAuthStore();
  const router = useRouter();
  const [magicEmail, setMagicEmail] = useState('');
  const [magicSent, setMagicSent] = useState(false);
  const [magicLoading, setMagicLoading] = useState(false);

  const onSubmit = async (data: FormData) => {
    try {
      await login(data.email, data.password);
      toast.success('ברוך שובך!');
      router.push('/dashboard');
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'הכניסה נכשלה');
    }
  };

  const sendMagicLink = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!magicEmail) return;
    setMagicLoading(true);
    try {
      await api.post('/api/auth/magic-link', { email: magicEmail });
      setMagicSent(true);
      toast.success('קישור נשלח למייל שלך!');
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'שגיאה בשליחת הקישור');
    } finally {
      setMagicLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="card p-8 w-full max-w-md">
        <div className="mb-8 text-center">
          <Link href="/" className="text-2xl font-bold text-brand-600">Presenter AI</Link>
          <p className="text-gray-500 mt-2 text-sm">כניסה לחשבון</p>
        </div>

        {/* Social logins */}
        <div className="flex flex-col gap-2 mb-5">
          <a href={GITHUB_AUTH_URL}
            className="flex items-center justify-center gap-3 w-full rounded-lg border border-gray-300 bg-white px-4 py-2.5 text-sm font-semibold text-gray-700 hover:bg-gray-50 transition active:scale-95">
            <GitHubIcon />
            כניסה עם GitHub
          </a>
          <a href={GOOGLE_AUTH_URL}
            className="flex items-center justify-center gap-3 w-full rounded-lg border border-gray-300 bg-white px-4 py-2.5 text-sm font-semibold text-gray-700 hover:bg-gray-50 transition active:scale-95">
            <GoogleIcon />
            כניסה עם Google
          </a>
        </div>

        {/* Magic link */}
        <div className="mb-5">
          <div className="flex items-center gap-3 mb-3">
            <div className="flex-1 h-px bg-gray-200" />
            <span className="text-xs text-gray-400">כניסה ללא סיסמה</span>
            <div className="flex-1 h-px bg-gray-200" />
          </div>
          {magicSent ? (
            <div className="text-center py-4">
              <p className="text-2xl mb-2">📬</p>
              <p className="text-sm font-medium text-gray-700">בדוק את המייל שלך</p>
              <p className="text-xs text-gray-400 mt-1">שלחנו קישור כניסה אל <strong>{magicEmail}</strong></p>
              <button onClick={() => { setMagicSent(false); setMagicEmail(''); }}
                className="text-xs text-brand-600 hover:underline mt-3">שלח שוב</button>
            </div>
          ) : (
            <form onSubmit={sendMagicLink} className="flex gap-2">
              <input
                type="email"
                className="input flex-1"
                placeholder="your@email.com"
                value={magicEmail}
                onChange={(e) => setMagicEmail(e.target.value)}
                required
              />
              <button type="submit" disabled={magicLoading}
                className="btn-primary whitespace-nowrap px-4 disabled:opacity-50">
                {magicLoading ? '…' : 'שלח קישור'}
              </button>
            </form>
          )}
        </div>

        {/* Email + password */}
        <div className="flex items-center gap-3 mb-5">
          <div className="flex-1 h-px bg-gray-200" />
          <span className="text-xs text-gray-400">או כנס עם סיסמה</span>
          <div className="flex-1 h-px bg-gray-200" />
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">אימייל</label>
            <input type="email" className="input" placeholder="you@example.com"
              {...register('email', { required: 'חובה להזין אימייל' })} />
            {errors.email && <p className="text-red-500 text-xs mt-1">{errors.email.message}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">סיסמה</label>
            <input type="password" className="input" placeholder="••••••••"
              {...register('password', { required: 'חובה להזין סיסמה' })} />
            {errors.password && <p className="text-red-500 text-xs mt-1">{errors.password.message}</p>}
          </div>
          <button type="submit" disabled={isSubmitting} className="btn-primary w-full py-2.5">
            {isSubmitting ? 'נכנס…' : 'כניסה'}
          </button>
        </form>

      </div>
    </div>
  );
}

function GitHubIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5 fill-current text-gray-800" aria-hidden="true">
      <path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12" />
    </svg>
  );
}

function GoogleIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true">
      <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
      <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
      <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
      <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
    </svg>
  );
}
