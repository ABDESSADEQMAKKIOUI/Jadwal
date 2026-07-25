package ma.jadwal.common;

/**
 * Utilisateur authentifié porté par le contexte de sécurité.
 * L'etablissementId est nul pour un SUPER_ADMIN.
 */
public record UtilisateurCourant(Long id, String email, String role, Long etablissementId) {
}
