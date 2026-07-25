package ma.jadwal.solver.faisabilite;

/**
 * Résultat d'un contrôle de faisabilité.
 *
 * @param code    code du contrôle ("H-01".."H-05").
 * @param libelle intitulé français du contrôle.
 * @param statut  "OK", "AVERTISSEMENT" ou "ECHEC".
 * @param message message principal, chiffré et actionnable en français (H-06).
 * @param details détails ligne par ligne (un élément par groupe/matière/salle concerné), peut être vide.
 */
public record BilanFaisabilite(String code, String libelle, String statut, String message, String details) {

    public static final String STATUT_OK = "OK";
    public static final String STATUT_AVERTISSEMENT = "AVERTISSEMENT";
    public static final String STATUT_ECHEC = "ECHEC";
}
