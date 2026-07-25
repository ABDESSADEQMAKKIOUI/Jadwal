package ma.jadwal.api.dto;

import java.math.BigDecimal;

public record TableauBordAdminReponse(
        long nbEtablissements,
        long nbAbonnementsActifs,
        long nbPaiementsEnAttente,
        BigDecimal totalEncaisseAnnee) {
}
