package ma.jadwal.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PaiementReponse(
        Long id,
        Long abonnementId,
        Long etablissementId,
        String etablissementNom,
        String planNom,
        BigDecimal montant,
        String mode,
        String reference,
        LocalDate datePaiement,
        String statut,
        String note,
        Instant dateCreation) {
}
