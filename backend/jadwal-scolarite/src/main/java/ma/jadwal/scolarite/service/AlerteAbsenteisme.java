package ma.jadwal.scolarite.service;

/**
 * Élève dont les absences non justifiées atteignent le seuil d'alerte sur la
 * période observée. Destiné à l'affichage direct dans l'écran de suivi.
 */
public record AlerteAbsenteisme(
        Long eleveId,
        String codeMassar,
        String nom,
        String prenom,
        Long groupeId,
        String groupeLibelle,
        long absencesNonJustifiees) {
}
