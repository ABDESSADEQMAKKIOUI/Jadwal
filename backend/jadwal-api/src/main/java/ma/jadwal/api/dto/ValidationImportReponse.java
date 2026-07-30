package ma.jadwal.api.dto;

/**
 * Résultat de l'écriture d'un import Massar.
 *
 * @param crees    élèves créés
 * @param misAJour élèves déjà inscrits et mis à jour
 * @param ignores  lignes volontairement écartées (erreur, doublon dans le lot,
 *                 ou élève existant sans mise à jour demandée)
 */
public record ValidationImportReponse(int crees, int misAJour, int ignores) {
}
