package ma.jadwal.api.dto;

/**
 * Créneau de la grille horaire exposé aux vues planning.
 */
public record CreneauReponse(
        Long id,
        String jour,
        int indexDebut,
        String type,
        int unitesDisponibles) {
}
