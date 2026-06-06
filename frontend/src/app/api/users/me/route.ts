import { NextRequest } from 'next/server';
import { resolveUser } from '@/lib/auth-server';

export async function GET(req: NextRequest) {
  const user = await resolveUser(req);
  const { passwordHash, ...safe } = user as any;
  return Response.json(safe);
}
