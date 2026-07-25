package ma.jadwal.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AbonnementReponse(
        Long id,
        Long etablissementId,
        String etablissementNom,
        Long planId,
        String planNom,
        BigDecimal prixAnnuel,
        LocalDate dateDebut,
        LocalDate dateFin,
        String statut,
        BigDecimal totalPaye) {
}
