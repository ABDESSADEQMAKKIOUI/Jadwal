package ma.jadwal.solver.modele;

/**
 * Indisponibilité VALIDÉE d'un enseignant (D-03/D-06).
 * Les indisponibilités de source ETAT sont DÉJÀ étendues du bufferTrajetUnites avant/après
 * par le mapper en amont (D-05) : le solveur les consomme telles quelles.
 */
public record IndispoPlan(JourPlan jour, int indexDebut, int dureeUnites, SemainePlan semaine) {

    /** Fin exclusive de l'intervalle indisponible. */
    public int finExclu() {
        return indexDebut + dureeUnites;
    }
}
