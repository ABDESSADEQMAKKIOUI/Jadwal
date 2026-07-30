package ma.jadwal.scolarite.depot;

/**
 * Projection d'agrégat : nombre d'évènements comptabilisés pour un élève.
 * Ne porte que des identifiants techniques, jamais de nom.
 */
public record CompteurAbsencesEleve(Long eleveId, Long nombre) {
}
