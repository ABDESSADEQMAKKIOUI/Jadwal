package ma.jadwal.api.dto;

/**
 * Un point de la série temporelle d'absentéisme.
 *
 * @param cle     clé triable du regroupement : {@code yyyy-MM-dd} pour un jour ou
 *                une semaine (son lundi), {@code yyyy-MM} pour un mois
 * @param libelle libellé court destiné à l'axe d'un graphique
 */
public record SerieAbsencesReponse(
        String cle,
        String libelle,
        long absences,
        long retards) {
}
