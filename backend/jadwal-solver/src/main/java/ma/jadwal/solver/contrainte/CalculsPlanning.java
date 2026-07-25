package ma.jadwal.solver.contrainte;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ma.jadwal.solver.modele.IndispoPlan;
import ma.jadwal.solver.modele.PreferencePlan;
import ma.jadwal.solver.modele.SeancePlan;
import ma.jadwal.solver.modele.SemainePlan;

/**
 * Calculs partagés par les contraintes ({@link ReglesEmploiDuTemps}) et la validation
 * synchrone ({@code ValidationConflits}).
 * <p>
 * Gestion de la quinzaine : les agrégats journaliers (suite contiguë, amplitude, trous,
 * charge, changements de bâtiment) sont calculés séparément pour la « vue semaine A »
 * (séances TOUTES + A) et la « vue semaine B » (séances TOUTES + B), et le MAXIMUM des
 * deux vues est retenu, afin de ne jamais compter deux fois les séances TOUTES.
 */
public final class CalculsPlanning {

    private CalculsPlanning() {
    }

    // ------------------------------------------------------------------
    // Chevauchements élémentaires
    // ------------------------------------------------------------------

    /** Chevauchement temporel complet : même jour, intervalles qui s'intersectent, semaines compatibles. */
    public static boolean seChevauchent(SeancePlan a, SeancePlan b) {
        if (a.getCreneau() == null || b.getCreneau() == null) {
            return false;
        }
        return a.getCreneau().jour() == b.getCreneau().jour()
                && a.getCreneau().indexDebut() < b.finExclu()
                && b.getCreneau().indexDebut() < a.finExclu()
                && a.getSemaine().chevauche(b.getSemaine());
    }

    /** F-07 : intervalles adjacents (qui se touchent) ou chevauchants, même jour supposé acquis. */
    public static boolean adjacentesOuChevauchantes(SeancePlan a, SeancePlan b) {
        return a.getCreneau().indexDebut() <= b.finExclu()
                && b.getCreneau().indexDebut() <= a.finExclu();
    }

    /** D-03 : la séance chevauche l'indisponibilité (même jour, intervalles, semaines compatibles). */
    public static boolean conflitIndispo(SeancePlan s, IndispoPlan indispo) {
        if (s.getCreneau() == null) {
            return false;
        }
        return s.getCreneau().jour() == indispo.jour()
                && s.getCreneau().indexDebut() < indispo.finExclu()
                && indispo.indexDebut() < s.finExclu()
                && indispo.semaine().chevauche(s.getSemaine());
    }

    /**
     * D-10 : impact signé des préférences de l'enseignant de la séance :
     * négatif (pénalité) pour chaque unité sur une plage EVITER,
     * positif (récompense) pour chaque unité sur une plage PREFERER.
     */
    public static long impactPreferences(SeancePlan s) {
        if (s.getCreneau() == null || s.getEnseignant() == null) {
            return 0;
        }
        long total = 0;
        for (PreferencePlan p : s.getEnseignant().getPreferences()) {
            if (p.jour() != s.getCreneau().jour()) {
                continue;
            }
            int chevauchement = Math.min(s.finExclu(), p.finExclu())
                    - Math.max(s.getCreneau().indexDebut(), p.indexDebut());
            if (chevauchement > 0) {
                total += p.eviter() ? -chevauchement : chevauchement;
            }
        }
        return total;
    }

    // ------------------------------------------------------------------
    // Agrégats journaliers / hebdomadaires par vue de semaine
    // ------------------------------------------------------------------

    /** Somme des durées (max des vues semaine A et B). Utilisé par D-01, F-01, F-03, G-06. */
    public static long charge(List<SeancePlan> seances) {
        return maxParVue(seances, CalculsPlanning::sommeDurees);
    }

    /** F-04 : somme des poids cognitifs pondérés par la durée (max des vues A et B). */
    public static long chargeCognitive(List<SeancePlan> seances) {
        return maxParVue(seances, vue -> {
            long total = 0;
            for (SeancePlan s : vue) {
                total += (long) s.getMatiere().poidsCognitif() * s.getDureeUnites();
            }
            return total;
        });
    }

    /** D-07 / G-03 : excédent de la plus longue suite contiguë d'unités occupées au-delà de max. */
    public static long excesConsecutif(List<SeancePlan> seances, int maxUnites) {
        if (maxUnites <= 0) {
            return 0;
        }
        return maxParVue(seances, vue -> {
            long plusLongue = 0;
            for (int[] intervalle : intervallesFusionnes(vue)) {
                plusLongue = Math.max(plusLongue, intervalle[1] - intervalle[0]);
            }
            return Math.max(0, plusLongue - maxUnites);
        });
    }

    /** D-07 / G-02 : excédent de l'amplitude journalière (fin dernière - début première) au-delà de max. */
    public static long excesAmplitude(List<SeancePlan> seances, Integer maxUnites) {
        if (maxUnites == null || maxUnites <= 0) {
            return 0;
        }
        return maxParVue(seances, vue -> Math.max(0, amplitude(vue) - maxUnites));
    }

    /** G-04 / D-09 : unités libres entre la première et la dernière séance du jour. */
    public static long trous(List<SeancePlan> seances) {
        return maxParVue(seances, vue -> {
            long occupees = 0;
            for (int[] intervalle : intervallesFusionnes(vue)) {
                occupees += intervalle[1] - intervalle[0];
            }
            return amplitude(vue) - occupees;
        });
    }

    /** E-04 : nombre de changements de bâtiment entre séances successives de la journée. */
    public static long changementsBatiment(List<SeancePlan> seances) {
        return maxParVue(seances, vue -> {
            List<SeancePlan> triees = new ArrayList<>(vue);
            triees.sort(Comparator.comparingInt(s -> s.getCreneau().indexDebut()));
            long changements = 0;
            for (int i = 1; i < triees.size(); i++) {
                String precedent = triees.get(i - 1).getSalle() == null ? null : triees.get(i - 1).getSalle().batiment();
                String courant = triees.get(i).getSalle() == null ? null : triees.get(i).getSalle().batiment();
                if (precedent != null && courant != null && !precedent.equals(courant)) {
                    changements++;
                }
            }
            return changements;
        });
    }

    /** F-01 : excédent des durées cumulées d'un couple (groupe, matière) sur un jour au-delà du plafond. */
    public static long excesMaxParJour(List<SeancePlan> seances) {
        int max = seances.get(0).getMaxParJourUnites();
        if (max <= 0) {
            return 0;
        }
        return Math.max(0, charge(seances) - max);
    }

    // ------------------------------------------------------------------
    // Interne
    // ------------------------------------------------------------------

    private interface CalculVue {
        long calculer(List<SeancePlan> vue);
    }

    private static long maxParVue(List<SeancePlan> seances, CalculVue calcul) {
        long vueA = calcul.calculer(filtrerVue(seances, SemainePlan.A));
        long vueB = calcul.calculer(filtrerVue(seances, SemainePlan.B));
        return Math.max(vueA, vueB);
    }

    private static List<SeancePlan> filtrerVue(List<SeancePlan> seances, SemainePlan vue) {
        List<SeancePlan> resultat = new ArrayList<>(seances.size());
        for (SeancePlan s : seances) {
            if (s.getCreneau() != null && s.getSemaine().chevauche(vue)) {
                resultat.add(s);
            }
        }
        return resultat;
    }

    private static long sommeDurees(List<SeancePlan> vue) {
        long total = 0;
        for (SeancePlan s : vue) {
            total += s.getDureeUnites();
        }
        return total;
    }

    private static long amplitude(List<SeancePlan> vue) {
        if (vue.isEmpty()) {
            return 0;
        }
        int debutMin = Integer.MAX_VALUE;
        int finMax = Integer.MIN_VALUE;
        for (SeancePlan s : vue) {
            debutMin = Math.min(debutMin, s.getCreneau().indexDebut());
            finMax = Math.max(finMax, s.finExclu());
        }
        return finMax - debutMin;
    }

    /** Intervalles [debut, fin) triés et fusionnés (les chevauchements internes sont absorbés). */
    private static List<int[]> intervallesFusionnes(List<SeancePlan> vue) {
        List<int[]> intervalles = new ArrayList<>(vue.size());
        for (SeancePlan s : vue) {
            intervalles.add(new int[] { s.getCreneau().indexDebut(), s.finExclu() });
        }
        intervalles.sort(Comparator.comparingInt(i -> i[0]));
        List<int[]> fusionnes = new ArrayList<>();
        for (int[] courant : intervalles) {
            if (!fusionnes.isEmpty() && courant[0] <= fusionnes.get(fusionnes.size() - 1)[1]) {
                int[] dernier = fusionnes.get(fusionnes.size() - 1);
                dernier[1] = Math.max(dernier[1], courant[1]);
            } else {
                fusionnes.add(new int[] { courant[0], courant[1] });
            }
        }
        return fusionnes;
    }
}
