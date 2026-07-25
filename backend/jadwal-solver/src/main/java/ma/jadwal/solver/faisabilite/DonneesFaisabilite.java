package ma.jadwal.solver.faisabilite;

import java.util.List;
import java.util.Map;

import ma.jadwal.solver.generation.BesoinSeances;
import ma.jadwal.solver.modele.EnseignantPlan;
import ma.jadwal.solver.modele.SallePlan;

/**
 * Données d'entrée du bilan de faisabilité (H-01..H-06), préparées par le mapper d'intégration.
 *
 * @param unitesCoursParSemaine                  nombre total d'unités COURS de la grille sur une semaine.
 * @param unitesCoursParSemaineParEnseignantMixte unités COURS restant libres pour chaque enseignant MIXTE
 *                                               après déduction de ses indisponibilités ETAT (buffer inclus, D-05).
 * @param nomsMatieres                           libellés par id de matière, pour des messages lisibles (H-06).
 * @param quotasEcoleMixtes                      quota école attendu par enseignant MIXTE (approximation H-05).
 */
public record DonneesFaisabilite(int unitesCoursParSemaine,
        Map<Long, Integer> unitesCoursParSemaineParEnseignantMixte,
        List<BesoinSeances> besoins,
        List<EnseignantPlan> enseignants,
        List<SallePlan> salles,
        Map<Long, String> nomsMatieres,
        Map<Long, Integer> quotasEcoleMixtes) {

    public DonneesFaisabilite {
        unitesCoursParSemaineParEnseignantMixte = unitesCoursParSemaineParEnseignantMixte == null
                ? Map.of() : Map.copyOf(unitesCoursParSemaineParEnseignantMixte);
        besoins = besoins == null ? List.of() : List.copyOf(besoins);
        enseignants = enseignants == null ? List.of() : List.copyOf(enseignants);
        salles = salles == null ? List.of() : List.copyOf(salles);
        nomsMatieres = nomsMatieres == null ? Map.of() : Map.copyOf(nomsMatieres);
        quotasEcoleMixtes = quotasEcoleMixtes == null ? Map.of() : Map.copyOf(quotasEcoleMixtes);
    }
}
