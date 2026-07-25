import { redirect } from 'next/navigation';
import { getSession } from '@/lib/session';

export default async function PageAccueil() {
  const session = await getSession();
  if (!session) {
    redirect('/login');
  }
  if (session.role === 'SUPER_ADMIN') {
    redirect('/admin');
  }
  redirect('/ecole');
}
