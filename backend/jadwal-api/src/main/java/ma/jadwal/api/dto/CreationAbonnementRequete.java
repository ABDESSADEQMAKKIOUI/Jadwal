package ma.jadwal.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreationAbonnementRequete(
        @NotNull(message = "L'établissement est obligatoire") Long etablissementId,
        @NotNull(message = "Le plan est obligatoire") Long planId,
        @NotNull(message = "La date de début est obligatoire") LocalDate dateDebut,
        @NotNull(message = "La date de fin est obligatoire") LocalDate dateFin) {
}
