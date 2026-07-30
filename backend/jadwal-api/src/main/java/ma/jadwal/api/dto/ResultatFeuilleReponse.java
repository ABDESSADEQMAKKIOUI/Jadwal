package ma.jadwal.api.dto;

/**
 * Effet d'une saisie en lot. Rejouer la même feuille renvoie
 * {@code crees = 0} et {@code supprimes = 0} : l'opération est idempotente.
 *
 * @param supprimes saisies effacées parce qu'elles ne figurent plus sur la
 *                  feuille — l'élève a été déclaré présent
 */
public record ResultatFeuilleReponse(
        int crees,
        int misAJour,
        int supprimes) {
}
