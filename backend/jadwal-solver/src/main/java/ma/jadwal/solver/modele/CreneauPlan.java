package ma.jadwal.solver.modele;

/**
 * Créneau de départ possible d'une séance. Seuls les créneaux de type COURS sont passés au solveur (A-07).
 *
 * @param unitesDisponibles nombre d'unités de 30 min contiguës de type COURS à partir de ce créneau
 *                          (jusqu'à la prochaine pause bloquée type DEJEUNER ou la fin de journée) — G-01.
 * @param matin             vrai si le créneau est avant la plage DEJEUNER (ou avant la moitié de journée
 *                          s'il n'y a pas de déjeuner) — utilisé par F-04 et G-05.
 */
public record CreneauPlan(long id, JourPlan jour, int indexDebut, int unitesDisponibles, boolean matin) {
}
