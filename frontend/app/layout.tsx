import type { Metadata } from 'next';
import type { CSSProperties, ReactNode } from 'react';
import { Inter } from 'next/font/google';
import './globals.css';
import Providers from './providers';

/* Police de marque du design system Ynexis.
   tokens/fonts.css charge Inter via un @import distant, mais globals.css place
   `@import "tailwindcss"` avant lui : après inlining, l'@import se retrouve
   derrière les règles Tailwind — position invalide en CSS — et le build le
   supprime. Résultat : Inter n'arrivait jamais dans le navigateur.
   next/font auto-héberge la fonte (aucune requête tierce, pas de FOUT) et
   expose --font-inter. tokens/ et globals.css sont intouchables : on
   redéfinit donc --font-sans ici, sur <html>, où la valeur en ligne prime sur
   le :root de tokens/typography.css et couvre TOUTES les routes (y compris
   /login et /, qui ne passent pas par AppShell). */
const inter = Inter({
  subsets: ['latin'],
  weight: ['400', '500', '600', '700'],
  variable: '--font-inter',
  display: 'swap',
});

/* Même pile que tokens/typography.css, « Inter » remplacé par la fonte
   réellement chargée. */
const policeDeMarque = {
  '--font-sans':
    "var(--font-inter), -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Helvetica Neue', Arial, 'Noto Sans Arabic', sans-serif",
} as CSSProperties;

export const metadata: Metadata = {
  title: 'JADWAL',
  description:
    'JADWAL — plateforme de génération d’emplois du temps scolaires.',
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="fr" className={inter.variable} style={policeDeMarque}>
      <body className="min-h-screen antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
