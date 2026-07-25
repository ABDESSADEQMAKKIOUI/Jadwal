package ma.jadwal.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AbonnementEcoleReponse(
        Long id,
        String planNom,
        BigDecimal prixAnnuel,
        LocalDate dateDebut,
        LocalDate dateFin,
        String statut,
        BigDecimal totalPaye,
        BigDecimal resteAPayer) {
}
