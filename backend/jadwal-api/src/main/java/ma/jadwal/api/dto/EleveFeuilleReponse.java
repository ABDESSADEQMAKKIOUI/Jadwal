package ma.jadwal.api.dto;

/**
 * Une ligne de feuille d'appel : l'élève, et sa saisie du jour si elle existe.
 *
 * @param statut  statut d'inscription, pour distinguer un élève parti d'un
 *                élève à appeler
 * @param absence {@code null} quand l'élève est présent, c'est-à-dire quand
 *                aucune saisie ne le concerne dans ce contexte
 */
public record EleveFeuilleReponse(
        Long eleveId,
        String nom,
        String prenom,
        String codeMassar,
        String statut,
        SaisieAbsenceReponse absence) {
}
