package ma.jadwal.scolarite.service;

/**
 * Un point d'une série temporelle d'absentéisme.
 *
 * @param cle       clé triable du regroupement ({@code yyyy-MM-dd} ou {@code yyyy-MM})
 * @param libelle   libellé court pour l'axe d'un graphique
 * @param absences  nombre de saisies de type ABSENCE du regroupement
 * @param retards   nombre de saisies de type RETARD du regroupement
 */
public record SerieAbsences(
        String cle,
        String libelle,
        long absences,
        long retards) {
}
