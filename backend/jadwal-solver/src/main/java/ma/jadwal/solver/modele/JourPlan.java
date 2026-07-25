package ma.jadwal.solver.modele;

/**
 * Jour de la semaine, dans l'ordre marocain usuel (lundi -&gt; dimanche).
 */
public enum JourPlan {
    LUNDI,
    MARDI,
    MERCREDI,
    JEUDI,
    VENDREDI,
    SAMEDI,
    DIMANCHE;

    /** Position du jour dans la semaine (0 = lundi), utilisée pour les calculs d'écart (F-02). */
    public int ordreSemaine() {
        return ordinal();
    }
}
