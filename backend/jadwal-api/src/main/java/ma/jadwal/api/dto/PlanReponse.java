package ma.jadwal.api.dto;

import java.math.BigDecimal;

public record PlanReponse(
        Long id,
        String code,
        String nom,
        BigDecimal prixAnnuel,
        String description,
        boolean actif) {
}
