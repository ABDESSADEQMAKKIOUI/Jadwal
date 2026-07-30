package ma.jadwal.scolarite.service;

import java.time.LocalDate;
import java.util.List;

/**
 * Vue complète de l'absentéisme d'une période : totaux, série temporelle à la
 * granularité demandée, et répartition par classe. C'est l'unique agrégat que
 * l'écran de suivi consomme.
 *
 * @param totaux    cumuls de la période, dont le taux global
 * @param series    un point par regroupement, dans l'ordre chronologique
 * @param parGroupe classes concernées, triées par libellé
 */
public record SyntheseAbsences(
        PeriodeStatistiques periode,
        LocalDate debut,
        LocalDate fin,
        StatistiquesAbsences totaux,
        List<SerieAbsences> series,
        List<AbsencesParGroupe> parGroupe) {
}
