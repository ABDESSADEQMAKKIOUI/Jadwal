package ma.jadwal.api.controleur;

import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.common.exception.OperationNonAutoriseeException;

/**
 * L'etablissementId provient EXCLUSIVEMENT du JWT (jamais de la requête).
 */
final class ContexteEcole {

    private ContexteEcole() {
    }

    static Long etablissementId(UtilisateurCourant courant) {
        Long etablissementId = courant == null ? null : courant.etablissementId();
        if (etablissementId == null) {
            throw new OperationNonAutoriseeException("Aucun établissement associé à ce compte");
        }
        return etablissementId;
    }
}
