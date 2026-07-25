package ma.jadwal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreationPlanRequete(
        @NotBlank(message = "Le code est obligatoire") String code,
        @NotBlank(message = "Le nom est obligatoire") String nom,
        @NotNull(message = "Le prix annuel est obligatoire")
        @Positive(message = "Le prix annuel doit être positif") BigDecimal prixAnnuel,
        String description) {
}
