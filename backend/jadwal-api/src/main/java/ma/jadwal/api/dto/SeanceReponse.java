package ma.jadwal.api.dto;

/**
 * Représentation JSON d'une séance planifiée (contrat SEANCE_JSON).
 * jour et indexDebut sont nuls tant que la séance n'a pas de créneau.
 */
public record SeanceReponse(
        Long id,
        Long groupeId,
        String groupeLibelle,
        Long matiereId,
        String matiereLibelle,
        String couleur,
        Long enseignantId,
        String enseignantNom,
        Long salleId,
        String salleNom,
        Long creneauId,
        String jour,
        Integer indexDebut,
        int dureeUnites,
        String semaine,
        boolean verrouillee) {
}
