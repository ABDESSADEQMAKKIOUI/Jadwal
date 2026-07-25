package ma.jadwal.api.dto;

import java.util.List;

/**
 * Analyse du score d'une génération, contrainte par contrainte (I-06).
 */
public record AnalyseReponse(String score, List<ContrainteAnalyse> contraintes) {

    public record ContrainteAnalyse(String regle, String libelle, long nombreViolations) {
    }
}
