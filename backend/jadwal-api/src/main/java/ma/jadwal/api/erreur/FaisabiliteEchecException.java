package ma.jadwal.api.erreur;

import ma.jadwal.solver.faisabilite.FaisabiliteRapport;

/**
 * Levée quand une génération est demandée alors que le bilan de faisabilité est en ECHEC.
 * Convertie en réponse 422 avec le rapport complet.
 */
public class FaisabiliteEchecException extends RuntimeException {

    private final transient FaisabiliteRapport rapport;

    public FaisabiliteEchecException(FaisabiliteRapport rapport) {
        super("Faisabilité en échec");
        this.rapport = rapport;
    }

    public FaisabiliteRapport getRapport() {
        return rapport;
    }
}
