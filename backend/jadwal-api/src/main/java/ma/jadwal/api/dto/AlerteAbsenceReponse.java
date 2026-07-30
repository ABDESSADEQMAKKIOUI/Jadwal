package ma.jadwal.api.dto;

/**
 * Élève dont les absences non justifiées atteignent le seuil d'alerte sur la
 * période : la liste des convocations à préparer.
 *
 * @param eleveNom      « Nom Prénom », pour un affichage direct
 * @param nonJustifiees absences non justifiées décomptées sur la période
 */
public record AlerteAbsenceReponse(
        Long eleveId,
        String eleveNom,
        String codeMassar,
        Long groupeId,
        String groupeLibelle,
        long nonJustifiees) {
}
