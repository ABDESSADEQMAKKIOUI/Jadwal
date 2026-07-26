'use client';

import { useRouter } from 'next/navigation';
import { useState, type FormEvent } from 'react';
import { extraireMessageErreur } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export default function PageConnexion() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [motDePasse, setMotDePasse] = useState('');
  const [erreur, setErreur] = useState<string | null>(null);
  const [chargement, setChargement] = useState(false);

  async function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    setErreur(null);
    setChargement(true);
    try {
      const reponse = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, motDePasse }),
      });

      let donnees: unknown = null;
      try {
        donnees = await reponse.json();
      } catch {
        donnees = null;
      }

      if (!reponse.ok) {
        setErreur(extraireMessageErreur(donnees, 'Identifiants invalides.'));
        return;
      }

      const { utilisateur } = donnees as { utilisateur: { role: string } };
      router.push(utilisateur.role === 'SUPER_ADMIN' ? '/admin' : '/ecole');
      router.refresh();
    } catch {
      setErreur('Impossible de contacter le serveur.');
    } finally {
      setChargement(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <div className="w-full max-w-sm rounded-xl border border-line-subtle bg-surface-card p-8 shadow-sm">
        <div className="mb-6 flex flex-col items-center gap-3">
          {/* Symbole + mot composés en HTML : voir la note dans app-shell.tsx. */}
          <span className="inline-flex items-center gap-2.5">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src="/jadwal-mark.svg" alt="" className="h-8 w-8" />
            <span className="text-2xl font-semibold tracking-[0.14em] text-ink-strong">
              JADWAL
            </span>
          </span>
          <div className="text-center">
            <h1 className="text-lg font-semibold text-ink-strong">Connexion</h1>
            <p className="mt-1 text-sm text-ink-muted">
              Plateforme de gestion des emplois du temps scolaires
            </p>
          </div>
        </div>

        <form onSubmit={soumettre} className="space-y-4">
          <div>
            <Label htmlFor="email">Adresse e-mail</Label>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              placeholder="vous@exemple.ma"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div>
            <Label htmlFor="motDePasse">Mot de passe</Label>
            <Input
              id="motDePasse"
              type="password"
              autoComplete="current-password"
              placeholder="••••••••"
              value={motDePasse}
              onChange={(e) => setMotDePasse(e.target.value)}
              required
            />
          </div>

          {erreur !== null && (
            <p
              role="alert"
              className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700"
            >
              {erreur}
            </p>
          )}

          <Button type="submit" className="w-full" disabled={chargement}>
            {chargement ? 'Connexion en cours…' : 'Se connecter'}
          </Button>
        </form>
      </div>
    </div>
  );
}
