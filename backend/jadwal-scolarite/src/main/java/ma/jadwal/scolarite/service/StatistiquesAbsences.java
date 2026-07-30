package ma.jadwal.scolarite.service;

import java.time.LocalDate;

/**
 * Agrégats d'absentéisme sur une période.
 *
 * @param demiJourneesAbsence demi-journées d'absence cumulées (une absence
 *                            JOURNEE en vaut deux)
 * @param demiJourneesDues    demi-journées théoriquement dues sur la période
 *                            (élèves inscrits × jours ouvrables × 2)
 * @param tauxAbsenteisme     pourcentage arrondi au centième
 */
public record StatistiquesAbsences(
        LocalDate debut,
        LocalDate fin,
        long elevesInscrits,
        long joursOuvrables,
        long totalAbsences,
        long absencesJustifiees,
        long absencesNonJustifiees,
        long retards,
        long exclusions,
        long demiJourneesAbsence,
        long demiJourneesDues,
        double tauxAbsenteisme) {
}
