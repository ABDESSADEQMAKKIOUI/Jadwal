package ma.jadwal.scolarite.service;

/**
 * Agrégats d'effectifs pour le tableau de bord de la vie scolaire.
 */
public record StatistiquesEleves(
        long total,
        long inscrits,
        long partis,
        long redoublants,
        long sansGroupe) {
}
