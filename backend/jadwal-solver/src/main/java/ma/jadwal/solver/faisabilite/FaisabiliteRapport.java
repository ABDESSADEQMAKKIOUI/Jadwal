package ma.jadwal.solver.faisabilite;

import java.util.List;

/**
 * Rapport complet du bilan de faisabilité avant calcul.
 *
 * @param global "OK", "AVERTISSEMENT" ou "ECHEC" (le pire statut des bilans).
 */
public record FaisabiliteRapport(String global, List<BilanFaisabilite> bilans) {

    public FaisabiliteRapport {
        bilans = bilans == null ? List.of() : List.copyOf(bilans);
    }
}
