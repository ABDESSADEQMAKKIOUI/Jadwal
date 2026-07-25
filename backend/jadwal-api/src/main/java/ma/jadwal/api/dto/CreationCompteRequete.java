package ma.jadwal.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreationCompteRequete(
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email,
        @NotBlank(message = "Le nom complet est obligatoire") String nomComplet,
        @NotBlank(message = "Le mot de passe est obligatoire") String motDePasse) {
}
