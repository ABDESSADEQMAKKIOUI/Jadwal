package ma.jadwal.api.dto;

/**
 * Absentéisme d'une classe sur la période.
 *
 * @param effectif        élèves INSCRIT rattachés au groupe, dénominateur du taux
 * @param tauxAbsenteisme pourcentage arrondi au dixième
 */
public record GroupeAbsencesReponse(
        Long groupeId,
        String groupeLibelle,
        long effectif,
        long absences,
        double tauxAbsenteisme) {
}
