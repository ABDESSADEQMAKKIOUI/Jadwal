# JADWAL — brief de création du logo

Document à remettre à un graphiste (ou à utiliser comme consigne pour un outil de génération
d'images). Le logo actuel (`frontend/public/jadwal-teal.svg`) est un **placeholder** : un rectangle
teal avec le mot « JADWAL » en Inter. Le design system Ynexis le signale lui-même comme
*« Wordmark + mark — placeholder, pending official asset »*.

---

## 1. Le produit en une phrase

**JADWAL est la plateforme qui construit automatiquement les emplois du temps d'un établissement
scolaire** — ce qui prenait plusieurs semaines sous Excel est calculé en quelques minutes, en
respectant l'intégralité des règles pédagogiques, contractuelles et réglementaires de l'école.

## 2. Le nom

**JADWAL** vient de l'arabe **جدول** : « tableau », « grille », « emploi du temps ». Le nom dit
donc littéralement l'objet du produit, et il est immédiatement compris au Maroc — marché visé.
Prononcé « jad-wal ». À écrire en capitales : **JADWAL**.

## 3. À qui il parle

Un outil de travail professionnel, utilisé quotidiennement par :

- **directeurs et directrices d'établissement** (validation, publication, tableaux de bord),
- **responsables de planification** (le cœur de cible : référentiel, maquettes, génération),
- enseignants (consultation, indisponibilités), élèves et parents (consultation).

Interface **en français**, données parfois **en arabe** (noms, libellés) — le logo doit donc rester
neutre culturellement et fonctionner à côté de texte arabe sans dissonance.

## 4. Positionnement et ton

Le registre du produit est celui d'un **outil d'administration SaaS moderne** : sobre, fiable,
dense en données mais **calme** — la référence est Linear ou Vercel, pas une application grand
public colorée. Le logo doit exprimer :

| Oui | Non |
|---|---|
| rigueur, structure, fiabilité | fantaisie, ludique, enfantin |
| clarté, lisibilité | complexité décorative |
| calme, maîtrise | urgence, dynamisme agressif |
| institutionnel sans être austère | administratif triste |

Un mot d'ordre : **c'est un outil qui remet de l'ordre dans le chaos**. Le logo peut le raconter.

## 5. Le système de marque existant — à respecter

JADWAL fait partie de la maison **Ynexis** et partage son design system (le même que le tableau de
bord Ynexis AI Call Center). Les valeurs ci-dessous ne sont pas négociables :

**Couleurs**

| Rôle | Valeur | Usage |
|---|---|---|
| Teal de marque « Ocean Green » | `#47a398` | couleur principale du logo |
| Teal foncé | `#3a8a80` | variante, contraste |
| Teal clair | `#a8e1d7` | accent secondaire |
| Teal très clair | `#eef9f7` | sur fond sombre |
| Corail « Froly » | `#f2847b` | accent, **à utiliser avec parcimonie voire pas du tout** |
| Encre / fond sombre | `#191f1e` | fond de la barre latérale |
| Fond clair de l'application | `#f7f9f9` | fond des écrans |

**Typographie** : **Inter** (400/500/600/700) est la police de toute l'interface. Si le logo comporte
un mot-symbole, Inter — ou un dessin qui s'accorde avec Inter — est le choix cohérent. Éviter les
serifs et les polices géométriques trop marquées.

**Formes** : rayons de coin modestes (4 à 12 px à l'échelle de l'interface), ombres douces,
traits nets. Le design system est « border-led » : les contours structurent, pas les dégradés.

## 6. Déclinaisons requises et contraintes techniques réelles

Ce sont les emplacements où le logo est **effectivement** utilisé dans le produit aujourd'hui. Les
tailles sont celles du code, pas des hypothèses.

| Déclinaison | Où | Contrainte dure |
|---|---|---|
| **Logo horizontal, version claire** | barre latérale, **hauteur 26 px** sur fond `#191f1e` | doit rester lisible à 26 px de haut, sur fond quasi noir |
| **Logo horizontal, version claire** | écran de connexion, hauteur 40 px sur fond `#f7f9f9` | doit aussi fonctionner sur fond clair |
| **Symbole seul (carré)** | en-tête des **emplois du temps exportés en PDF** | imprimé, souvent **en noir et blanc** sur imprimante d'école → doit survivre au monochrome |
| **Favicon / icône d'application** | onglet du navigateur | lisible à **16 et 32 px** |

Conséquences concrètes pour le dessin :

1. **Deux verrous de lisibilité** : 26 px de haut pour le logo complet, 16 px pour le symbole. Pas
   de trait fin, pas de détail sous ~2 px, pas de texte secondaire (baseline, slogan) intégré.
2. **Deux fonds** : sombre `#191f1e` et clair `#f7f9f9`. Prévoir une version pour chacun (le teal
   `#47a398` passe sur les deux, mais du texte noir ou blanc pur ne passe que sur un).
3. **Monochrome obligatoire** : une version noire et une version blanche, sans perte de sens. Si le
   concept repose sur deux couleurs pour être compris, il est à revoir.
4. **Symbole autonome** : il faut un symbole qui vit seul, carré, sans le mot. C'est lui qui va dans
   les PDF et le favicon. Aujourd'hui le PDF affiche un monogramme d'initiales faute de mieux.

## 7. Pistes visuelles

Non prescriptives — des directions cohérentes avec le nom et le produit :

- **La grille.** Un emploi du temps *est* une grille jours × heures. Une grille stylisée, dont
  quelques cases sont remplies en teal, dit à la fois « planning » et « cases qui se remplissent
  toutes seules ». C'est la piste la plus directe et la plus lisible en petit.
- **Le J structurant.** Le J de JADWAL dessiné comme une colonne de grille, ou une case qui
  descend — le monogramme devient le symbole.
- **L'ordre depuis le désordre.** Des blocs désalignés à gauche qui s'alignent à droite : raconte
  exactement la promesse du produit. Risque : peu lisible à 16 px, à tester tôt.
- **Le جدول discret.** Une allusion à la calligraphie arabe du mot, intégrée sans devenir un logo
  « oriental » — le produit est un outil professionnel, pas un objet culturel.

## 8. À éviter

- La page de calendrier avec un coin corné, et l'horloge / le sablier : clichés, et déjà partout.
- Les dégradés, les ombres portées, les effets 3D : contraires au design system (plat, contours).
- Plus de deux couleurs.
- Le chapeau de diplômé, le tableau noir, la pomme, le crayon : JADWAL est un outil de gestion
  destiné à l'administration, pas une application d'apprentissage.
- Un symbole qui ne fonctionne qu'accompagné du mot.

## 9. Livrables attendus

- Logo horizontal (mot + symbole) : **SVG**, versions pour fond clair et fond sombre.
- Symbole seul, cadrage carré : **SVG**, versions couleur, noir, blanc.
- Favicon : SVG + PNG 32×32 et 16×16.
- Zone de protection et taille minimale documentées.
- Fichier source éditable.

Les fichiers atterrissent dans `frontend/public/` (le code référence `/jadwal-teal.svg` depuis la
barre latérale et l'écran de connexion) et, pour le PDF, dans le générateur d'export
(`jadwal-export`), qui utilise aujourd'hui un monogramme de repli.

## 10. Prompts pour Gemini (génération d'images)

**À savoir avant de générer.** Un générateur d'images produit une **image matricielle**, alors que
le produit a besoin de **SVG** (lisible à 26 px, imprimable en noir et blanc). Et le rendu de texte
par ces modèles reste approximatif : lettres déformées, orthographe fantaisiste. La méthode qui
fonctionne :

1. générer le **symbole seul, sans aucun texte** (c'est là que le modèle est bon) ;
2. choisir une piste, la **redessiner en vectoriel** (Figma, Illustrator, Inkscape) pour obtenir un
   SVG propre et le teal exact `#47a398` ;
3. composer le mot « JADWAL » en **Inter demi-gras** à côté — pas besoin du modèle pour ça.

### Prompt 1 — le symbole (recommandé, format 1:1)

> Minimalist flat vector app icon for a school timetable scheduling software. The symbol is a
> stylized weekly timetable grid: a 4×4 arrangement of small squares with slightly rounded corners;
> four of the cells are filled solid and the remaining cells are drawn as thin outlines only, so the
> icon reads as a schedule filling itself in. Single flat color, teal green (hex #47a398), on a pure
> white background. Absolutely flat: no gradient, no drop shadow, no 3D, no bevel, no texture, no
> reflection. Uniform medium stroke weight, precise geometry, generous even margin around the
> symbol, perfectly centered. Designed to stay legible at 16 pixels. No text, no letters, no words,
> no numbers anywhere in the image.

### Prompt 2 — monogramme « J » (format 1:1)

> Minimalist flat vector logo mark: the capital letter J constructed entirely from timetable grid
> cells — a vertical column of three small rounded squares with the bottom one extending left to
> form the hook of the J. Geometric, built on a strict square grid, single flat teal green color
> (hex #47a398) on pure white. No gradient, no shadow, no 3D, no outline glow. Sharp precise
> construction, slightly rounded corners, centered with generous margin. Must read clearly at very
> small size. Only the single letter J as a geometric construction — no other text or words.

### Prompt 3 — « l'ordre depuis le désordre » (format 1:1)

> Minimalist flat vector logo mark for scheduling software: on the left, three small rectangles
> scattered and misaligned at slight angles; on the right, the same three rectangles perfectly
> aligned into a neat vertical stack. The composition reads left to right as chaos resolving into
> order. Single flat teal green color (hex #47a398) on pure white background. Completely flat
> design, no gradient, no shadow, no 3D. Clean geometry, rounded corners, balanced negative space,
> centered. No text, no letters, no words.

### Prompt 4 — variante sur fond sombre (format 1:1)

Reprendre le prompt retenu en remplaçant la ligne de couleur par :

> Single flat pale teal color (hex #a8e1d7) on a solid very dark background (hex #191f1e).

### Prompt 5 — logo complet avec le mot (format 4:1, à vos risques)

Le modèle se trompera souvent sur les lettres : à n'utiliser que pour explorer une mise en page,
jamais comme livrable.

> Horizontal logo lockup for a software product. On the left, a small flat geometric symbol: a grid
> of rounded squares with a few cells filled. On the right, the single word "JADWAL" in uppercase
> sans-serif letters, semi-bold, slightly letter-spaced, optically aligned with the symbol. Single
> flat teal green color (hex #47a398) on pure white. Flat vector style, no gradient, no shadow, no
> 3D. Wide horizontal composition with generous margins. Spell the word exactly J-A-D-W-A-L.

### Consignes de tri

Générer 4 à 8 variantes par prompt, puis écarter sans regret tout résultat qui :
présente un dégradé ou une ombre · comporte du texte parasite · devient illisible réduit à 16 px
(réduire l'image pour vérifier) · ne survit pas converti en noir et blanc · a besoin de plus de deux
couleurs pour être compris.

## 11. Version courte pour un générateur d'images

> Logo pour « JADWAL », plateforme SaaS marocaine de génération automatique d'emplois du temps
> scolaires. Style : outil d'administration professionnel, sobre et structuré, registre Linear /
> Vercel. Symbole : grille d'emploi du temps stylisée dont quelques cases sont remplies, ou
> monogramme « J » construit comme une colonne de grille. Couleur unique teal Ocean Green #47a398
> sur fond blanc, et variante claire sur fond #191f1e. Formes géométriques nettes, angles
> légèrement arrondis, aucun dégradé, aucune ombre, deux couleurs maximum. Mot-symbole « JADWAL »
> en capitales, police Inter demi-gras, légèrement espacé. Doit rester lisible à 26 pixels de haut
> et fonctionner en noir et blanc. Pas d'horloge, pas de sablier, pas de page de calendrier cornée,
> pas de chapeau de diplômé.
