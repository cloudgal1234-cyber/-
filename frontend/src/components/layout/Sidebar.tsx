'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';

const navItems = [
  { href: '/dashboard', label: 'לוח בקרה', icon: '🏠' },
  { href: '/campaigns', label: 'קמפיינים', icon: '📁' },
  { href: '/campaigns/new', label: 'קמפיין חדש', icon: '✨' },
  { href: '/gallery', label: 'גלריה', icon: '🖼️' },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="flex h-full w-60 flex-col border-l border-gray-200 bg-white px-4 py-6">
      <Link href="/" className="mb-8 px-2 text-xl font-bold text-brand-600">
        Presenter AI
      </Link>

      <nav className="flex-1 space-y-1">
        {navItems.map(({ href, label, icon }) => (
          <Link
            key={href}
            href={href}
            className={cn(
              'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition',
              pathname === href || pathname.startsWith(href + '/')
                ? 'bg-brand-50 text-brand-700'
                : 'text-gray-600 hover:bg-gray-100'
            )}
          >
            <span>{icon}</span>
            {label}
          </Link>
        ))}
      </nav>
    </aside>
  );
}
