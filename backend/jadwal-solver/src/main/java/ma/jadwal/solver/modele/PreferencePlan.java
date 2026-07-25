package ma.jadwal.solver.modele;

/**
 * Plage de préférence d'un enseignant (D-10) : eviter = true pour une plage EVITER,
 * false pour une plage PREFERER.
 */
public record PreferencePlan(JourPlan jour, int indexDebut, int dureeUnites, boolean eviter) {

    /** Fin exclusive de la plage. */
    public int finExclu() {
        return indexDebut + dureeUnites;
    }
}
