package ma.jadwal.api.dto;

/**
 * Saisie déjà enregistrée pour un élève sur une feuille d'appel.
 *
 * @param demiJournee contexte réel de la saisie, rappelé ici parce qu'une
 *                    absence sur la journée entière peut apparaître sur la
 *                    feuille d'une demi-journée
 */
public record SaisieAbsenceReponse(
        Long id,
        String type,
        String demiJournee,
        boolean justifiee,
        String motif) {
}
