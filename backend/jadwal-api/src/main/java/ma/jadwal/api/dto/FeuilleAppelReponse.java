package ma.jadwal.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Feuille d'appel d'un groupe : tous les élèves rattachés, chacun avec sa saisie
 * du contexte demandé ou {@code null} s'il est présent.
 *
 * @param seanceId séance appelée, {@code null} pour un appel de demi-journée
 */
public record FeuilleAppelReponse(
        GroupeFeuilleReponse groupe,
        LocalDate date,
        String demiJournee,
        Long seanceId,
        List<EleveFeuilleReponse> eleves) {
}
