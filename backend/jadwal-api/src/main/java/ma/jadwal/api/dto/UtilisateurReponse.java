package ma.jadwal.api.dto;

public record UtilisateurReponse(
        Long id,
        String email,
        String nomComplet,
        String role,
        Long etablissementId,
        String etablissementNom) {
}
