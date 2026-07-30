package ma.jadwal.scolarite.entite;

/**
 * Nature de l'évènement saisi sur la feuille d'appel.
 * Seul {@link #ABSENCE} entre dans le calcul du taux d'absentéisme.
 */
public enum TypeAbsence {
    ABSENCE,
    RETARD,
    EXCLUSION
}
