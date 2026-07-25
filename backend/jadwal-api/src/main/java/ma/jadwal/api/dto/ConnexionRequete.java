package ma.jadwal.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ConnexionRequete(
        @NotBlank(message = "L'email est obligatoire") String email,
        @NotBlank(message = "Le mot de passe est obligatoire") String motDePasse) {
}
