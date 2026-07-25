package ma.jadwal.solver.generation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ma.jadwal.solver.modele.GroupePlan;
import ma.jadwal.solver.modele.SeancePlan;
import ma.jadwal.solver.modele.SemainePlan;

/**
 * Génère les séances à planifier à partir des besoins de la maquette.
 * <p>
 * Garanties par construction :
 * <ul>
 *   <li>C-01 : les séances générées totalisent exactement le volume hebdomadaire par (groupe, matière) ;</li>
 *   <li>C-02 : le pattern retenu est le premier dont la somme == volume, sinon décomposition
 *       automatique en blocs de dureeMax puis reliquat &gt;= dureeMin (rééquilibré si nécessaire) ;</li>
 *   <li>C-03 : les durées générées respectent dureeMin/dureeMax de la matière
 *       (sauf incohérence min/max/volume, auquel cas un garde-fou émet un bloc unique) ;</li>
 *   <li>C-05 : quinzaine — si volumeUnitesB est non null, des séances semaine=A sont émises pour
 *       volumeUnites et des séances semaine=B pour volumeUnitesB, sinon semaine=TOUTES ;</li>
 *   <li>A-04/B-04 : en dédoublement, les séances de même rang de pattern des différents
 *       sous-groupes partagent un blocAlignementId commun (elles seront simultanées).</li>
 * </ul>
 * Ids déterministes : "gId-mId-i" (i = compteur par couple groupe planifiable / matière).
 * <p>
 * La factory dénormalise aussi sur chaque séance : maxParJourUnites, gapMinJours (F-02),
 * chargeMoyenneUnites (F-03), amplitudeMaxUnitesGroupe, nbJoursActifs et seuilCognitifDemiJournee.
 */
public final class SeanceFactory {

    private SeanceFactory() {
    }

    /** Génération avec les paramètres par défaut : 5 jours actifs, amplitude groupe 16, seuil cognitif 12. */
    public static List<SeancePlan> genererSeances(List<BesoinSeances> besoins) {
        return genererSeances(besoins, 5, 16, 12);
    }

    /** Génération avec les paramètres de l'établissement (utilisée par le mapper d'intégration). */
    public static List<SeancePlan> genererSeances(List<BesoinSeances> besoins, int nbJoursActifs,
            int amplitudeMaxUnitesGroupe, int seuilCognitifDemiJournee) {
        List<SeancePlan> seances = new ArrayList<>();
        Map<String, Integer> compteurs = new HashMap<>();

        for (BesoinSeances besoin : besoins) {
            genererPourBesoin(besoin, seances, compteurs);
        }

        denormaliser(seances, nbJoursActifs, amplitudeMaxUnitesGroupe, seuilCognitifDemiJournee);
        return seances;
    }

    // ------------------------------------------------------------------
    // Génération par besoin
    // ------------------------------------------------------------------

    private static void genererPourBesoin(BesoinSeances besoin, List<SeancePlan> seances,
            Map<String, Integer> compteurs) {
        List<SemaineVolume> volumes = new ArrayList<>();
        if (besoin.volumeUnitesB() == null) {
            volumes.add(new SemaineVolume(SemainePlan.TOUTES, besoin.volumeUnites()));
        } else {
            volumes.add(new SemaineVolume(SemainePlan.A, besoin.volumeUnites()));
            volumes.add(new SemaineVolume(SemainePlan.B, besoin.volumeUnitesB()));
        }

        for (SemaineVolume sv : volumes) {
            if (sv.volume() <= 0) {
                continue;
            }
            String dedoublement = besoin.dedoublement();
            boolean avecSousGroupes = !besoin.sousGroupes().isEmpty();
            if ("TOTAL".equals(dedoublement) && avecSousGroupes) {
                List<Integer> durees = decouper(sv.volume(), besoin);
                emettreAligne(besoin, sv.semaine(), durees, seances, compteurs, "T");
            } else if ("PARTIEL".equals(dedoublement) && avecSousGroupes) {
                int volumeEntier = (sv.volume() + 1) / 2;
                int volumeDedouble = sv.volume() / 2;
                for (int duree : decouper(volumeEntier, besoin)) {
                    seances.add(nouvelleSeance(besoin, besoin.groupe(), sv.semaine(), duree, null, compteurs));
                }
                if (volumeDedouble > 0) {
                    List<Integer> durees = decouper(volumeDedouble, besoin);
                    emettreAligne(besoin, sv.semaine(), durees, seances, compteurs, "P");
                }
            } else {
                for (int duree : decouper(sv.volume(), besoin)) {
                    seances.add(nouvelleSeance(besoin, besoin.groupe(), sv.semaine(), duree, null, compteurs));
                }
            }
        }
    }

    /** Émet les mêmes durées pour chaque sous-groupe ; même rang de pattern = même blocAlignementId (B-04). */
    private static void emettreAligne(BesoinSeances besoin, SemainePlan semaine, List<Integer> durees,
            List<SeancePlan> seances, Map<String, Integer> compteurs, String prefixe) {
        for (int rang = 0; rang < durees.size(); rang++) {
            String blocId = "bloc-" + prefixe + "-" + besoin.groupe().id() + "-" + besoin.matiere().id()
                    + "-" + semaine + "-" + rang;
            for (GroupePlan sousGroupe : besoin.sousGroupes()) {
                seances.add(nouvelleSeance(besoin, sousGroupe, semaine, durees.get(rang), blocId, compteurs));
            }
        }
    }

    private static SeancePlan nouvelleSeance(BesoinSeances besoin, GroupePlan groupe, SemainePlan semaine,
            int duree, String blocAlignementId, Map<String, Integer> compteurs) {
        String cle = groupe.id() + "-" + besoin.matiere().id();
        int index = compteurs.merge(cle, 1, Integer::sum) - 1;
        SeancePlan seance = new SeancePlan(cle + "-" + index, null, groupe, besoin.matiere(), duree,
                semaine, blocAlignementId, besoin.barretteId(), besoin.affectationEnseignantId(),
                besoin.maxParJourUnites());
        return seance;
    }

    // ------------------------------------------------------------------
    // Découpage d'un volume en durées de séances (C-02, C-03)
    // ------------------------------------------------------------------

    static List<Integer> decouper(int volume, BesoinSeances besoin) {
        return decouper(volume, besoin.patterns(),
                besoin.matiere().dureeMinUnites(), besoin.matiere().dureeMaxUnites());
    }

    /**
     * Retient le premier pattern dont la somme == volume ; sinon décomposition automatique en
     * blocs de dureeMax, le reliquat final restant toujours &gt;= dureeMin (le dernier bloc plein
     * est raccourci si nécessaire pour l'assurer).
     */
    static List<Integer> decouper(int volume, List<List<Integer>> patterns, int dureeMin, int dureeMax) {
        if (volume <= 0) {
            return List.of();
        }
        if (patterns != null) {
            for (List<Integer> pattern : patterns) {
                if (pattern != null && !pattern.isEmpty()
                        && pattern.stream().mapToInt(Integer::intValue).sum() == volume) {
                    return List.copyOf(pattern);
                }
            }
        }
        int max = dureeMax > 0 ? dureeMax : volume;
        int min = Math.max(1, dureeMin);
        List<Integer> blocs = new ArrayList<>();
        int reste = volume;
        while (reste > 0) {
            int bloc = Math.min(max, reste);
            int reliquat = reste - bloc;
            if (reliquat > 0 && reliquat < min) {
                bloc = reste - min;
            }
            if (bloc < min) {
                // Garde-fou : min/max incompatibles avec le volume restant, on émet tout d'un bloc.
                bloc = reste;
            }
            blocs.add(bloc);
            reste -= bloc;
        }
        return blocs;
    }

    // ------------------------------------------------------------------
    // Dénormalisation F-02 / F-03 / paramètres établissement
    // ------------------------------------------------------------------

    private static void denormaliser(List<SeancePlan> seances, int nbJoursActifs,
            int amplitudeMaxUnitesGroupe, int seuilCognitifDemiJournee) {
        // gapMinJours (F-02) : max(1, nbJoursActifs / nbSeances) par (groupe, matière, vue semaine).
        Map<String, Integer> comptesVueA = new HashMap<>();
        Map<String, Integer> comptesVueB = new HashMap<>();
        // chargeMoyenneUnites (F-03) : somme des durées par groupe (pire vue) / nbJoursActifs.
        Map<Long, Integer> chargeVueA = new HashMap<>();
        Map<Long, Integer> chargeVueB = new HashMap<>();

        for (SeancePlan s : seances) {
            String cle = s.getGroupe().id() + "-" + s.getMatiere().id();
            if (s.getSemaine() != SemainePlan.B) {
                comptesVueA.merge(cle, 1, Integer::sum);
                chargeVueA.merge(s.getGroupe().id(), s.getDureeUnites(), Integer::sum);
            }
            if (s.getSemaine() != SemainePlan.A) {
                comptesVueB.merge(cle, 1, Integer::sum);
                chargeVueB.merge(s.getGroupe().id(), s.getDureeUnites(), Integer::sum);
            }
        }

        int jours = Math.max(1, nbJoursActifs);
        for (SeancePlan s : seances) {
            String cle = s.getGroupe().id() + "-" + s.getMatiere().id();
            int nbSeances = s.getSemaine() == SemainePlan.B
                    ? comptesVueB.getOrDefault(cle, 1)
                    : comptesVueA.getOrDefault(cle, 1);
            s.setGapMinJours(Math.max(1, jours / Math.max(1, nbSeances)));

            int charge = Math.max(chargeVueA.getOrDefault(s.getGroupe().id(), 0),
                    chargeVueB.getOrDefault(s.getGroupe().id(), 0));
            s.setChargeMoyenneUnites((int) Math.round((double) charge / jours));

            s.setNbJoursActifs(nbJoursActifs);
            s.setAmplitudeMaxUnitesGroupe(amplitudeMaxUnitesGroupe);
            s.setSeuilCognitifDemiJournee(seuilCognitifDemiJournee);
        }
    }

    private record SemaineVolume(SemainePlan semaine, int volume) {
    }
}
