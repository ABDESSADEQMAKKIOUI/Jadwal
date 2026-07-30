package ma.jadwal.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Statistiques d'absentéisme d'une période.
 *
 * <p>{@code debut} et {@code fin} sont repris dans la réponse parce que le
 * serveur les complète quand la requête les omet (mois en cours).
 *
 * @param periode   granularité des séries : JOUR, SEMAINE ou MOIS
 * @param series    un point par regroupement, dans l'ordre chronologique, y
 *                  compris les regroupements sans aucune saisie
 * @param parGroupe classes du périmètre, triées par libellé
 */
public record StatistiquesAbsencesReponse(
        String periode,
        LocalDate debut,
        LocalDate fin,
        TotauxAbsencesReponse totaux,
        List<SerieAbsencesReponse> series,
        List<GroupeAbsencesReponse> parGroupe) {
}
