package ma.jadwal.solver.generation;

import java.util.List;

import ma.jadwal.solver.modele.GroupePlan;
import ma.jadwal.solver.modele.MatierePlan;

/**
 * Besoin de séances issu de la maquette pédagogique pour un couple (groupe, matière).
 *
 * @param volumeUnites            volume hebdomadaire en unités de 30 min (A-05) ; semaine A si quinzaine.
 * @param volumeUnitesB           volume de la semaine B si quinzaine (C-05), sinon null.
 * @param patterns                patterns de découpage autorisés (C-02), ex 8 -&gt; [[4,4],[4,2,2]] ; nullable.
 * @param maxParJourUnites        plafond journalier F-01, dénormalisé sur chaque séance ; 0 = illimité.
 * @param dedoublement            "AUCUN", "PARTIEL" (moitié classe entière + moitié par sous-groupe)
 *                                ou "TOTAL" (tout le volume par sous-groupe) — A-04.
 * @param sousGroupes             sous-groupes de dédoublement (vide si AUCUN).
 * @param barretteId              identifiant de barrette (B-05), nullable.
 * @param affectationEnseignantId enseignant imposé (C-06), nullable.
 */
public record BesoinSeances(GroupePlan groupe, MatierePlan matiere, int volumeUnites, Integer volumeUnitesB,
        List<List<Integer>> patterns, int maxParJourUnites, String dedoublement,
        List<GroupePlan> sousGroupes, Long barretteId, Long affectationEnseignantId) {

    public BesoinSeances {
        dedoublement = dedoublement == null ? "AUCUN" : dedoublement;
        sousGroupes = sousGroupes == null ? List.of() : List.copyOf(sousGroupes);
    }
}
