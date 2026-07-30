package ma.jadwal.scolarite.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Granularité des séries d'absentéisme. La semaine est celle de la norme ISO
 * (lundi → dimanche), ce qui colle à la semaine scolaire marocaine puisque le
 * dimanche est le seul jour non ouvré.
 */
public enum PeriodeStatistiques {

    JOUR,
    SEMAINE,
    MOIS;

    /**
     * Lecture d'un code venu de la requête. Valeur absente = {@link #JOUR}.
     *
     * @throws IllegalArgumentException si le code ne désigne aucune granularité,
     *                                  ce que la couche API traduit en 400
     */
    public static PeriodeStatistiques depuisCode(String code) {
        if (code == null || code.isBlank()) {
            return JOUR;
        }
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Période inconnue : " + code + " (valeurs acceptées : JOUR, SEMAINE, MOIS)");
        }
    }

    /**
     * Premier jour du regroupement auquel appartient une date : le jour lui-même,
     * le lundi de sa semaine ISO, ou le premier du mois.
     */
    public LocalDate debutDuRegroupement(LocalDate jour) {
        return switch (this) {
            case JOUR -> jour;
            case SEMAINE -> jour.with(DayOfWeek.MONDAY);
            case MOIS -> jour.withDayOfMonth(1);
        };
    }

    /**
     * Clé stable et triable d'un regroupement : {@code yyyy-MM-dd} pour un jour
     * ou une semaine (le lundi), {@code yyyy-MM} pour un mois. L'ordre
     * lexicographique des clés est l'ordre chronologique.
     */
    public String cle(LocalDate jour) {
        LocalDate debut = debutDuRegroupement(jour);
        return this == MOIS
                ? String.format(Locale.ROOT, "%04d-%02d", debut.getYear(), debut.getMonthValue())
                : debut.toString();
    }

    /**
     * Libellé court destiné à un axe de graphique.
     */
    public String libelle(LocalDate jour) {
        LocalDate debut = debutDuRegroupement(jour);
        return switch (this) {
            case JOUR -> String.format(Locale.ROOT, "%02d/%02d", debut.getDayOfMonth(), debut.getMonthValue());
            case SEMAINE -> String.format(Locale.ROOT, "sem. %02d/%02d",
                    debut.getDayOfMonth(), debut.getMonthValue());
            case MOIS -> String.format(Locale.ROOT, "%02d/%04d", debut.getMonthValue(), debut.getYear());
        };
    }
}
