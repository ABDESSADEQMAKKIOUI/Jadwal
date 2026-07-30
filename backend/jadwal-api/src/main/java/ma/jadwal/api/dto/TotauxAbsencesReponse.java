package ma.jadwal.api.dto;

/**
 * Cumuls d'absentéisme d'une période.
 *
 * @param absences        saisies de type ABSENCE
 * @param retards         saisies de type RETARD, jamais comptées dans le taux
 * @param exclusions      saisies de type EXCLUSION, jamais comptées dans le taux
 * @param tauxAbsenteisme pourcentage arrondi au dixième ; formule documentée sur
 *                        {@code AbsenceService.synthese}
 * @param elevesInscrits  dénominateur du taux : élèves INSCRIT du périmètre
 * @param joursOuvrables  jours de la période hors dimanche
 */
public record TotauxAbsencesReponse(
        long absences,
        long retards,
        long exclusions,
        long justifiees,
        long nonJustifiees,
        double tauxAbsenteisme,
        long elevesInscrits,
        long joursOuvrables) {
}
