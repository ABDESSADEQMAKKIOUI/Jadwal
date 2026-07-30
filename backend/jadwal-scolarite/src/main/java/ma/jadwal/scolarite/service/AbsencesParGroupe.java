package ma.jadwal.scolarite.service;

/**
 * Absentéisme d'une classe sur la période observée.
 *
 * @param effectif         élèves INSCRIT rattachés au groupe
 * @param absences         saisies de type ABSENCE (retards et exclusions exclus)
 * @param tauxAbsenteisme  pourcentage arrondi au dixième, voir
 *                         {@link AbsenceService#synthese}
 */
public record AbsencesParGroupe(
        Long groupeId,
        String groupeLibelle,
        long effectif,
        long absences,
        double tauxAbsenteisme) {
}
