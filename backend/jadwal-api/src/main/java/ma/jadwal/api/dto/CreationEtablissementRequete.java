package ma.jadwal.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreationEtablissementRequete(
        @NotBlank(message = "Le nom est obligatoire") String nom,
        @NotBlank(message = "Le code est obligatoire") String code,
        String ville,
        String telephone,
        String email) {
}
