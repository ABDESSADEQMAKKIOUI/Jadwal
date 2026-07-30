package ma.jadwal.api.dto;

/**
 * En-tête de groupe d'une feuille d'appel.
 *
 * @param effectif capacité déclarée du groupe, à ne pas confondre avec le nombre
 *                 d'élèves réellement rattachés (la longueur de la liste
 *                 d'élèves de la feuille)
 */
public record GroupeFeuilleReponse(
        Long id,
        String libelle,
        Long niveauId,
        String niveauLibelle,
        int effectif) {
}
