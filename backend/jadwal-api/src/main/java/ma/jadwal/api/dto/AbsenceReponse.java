package ma.jadwal.api.dto;

import java.time.LocalDate;

/**
 * Une saisie de vie scolaire telle que l'écran de suivi l'affiche.
 *
 * <p>N'expose que ce que la liste doit montrer : ni tuteur, ni date de
 * naissance, ni auteur de la saisie ne sortent par ici.
 *
 * @param eleveNom      « Nom Prénom », pour un affichage direct
 * @param groupeLibelle classe de l'élève, {@code null} s'il n'en a pas
 * @param demiJournee   MATIN, APRES_MIDI ou JOURNEE
 * @param type          ABSENCE, RETARD ou EXCLUSION
 */
public record AbsenceReponse(
        Long id,
        Long eleveId,
        String eleveNom,
        String codeMassar,
        String groupeLibelle,
        LocalDate dateAbsence,
        String demiJournee,
        String type,
        boolean justifiee,
        String motif) {
}
