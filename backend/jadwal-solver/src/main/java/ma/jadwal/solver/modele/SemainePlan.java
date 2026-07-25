package ma.jadwal.solver.modele;

/**
 * Semaine d'application d'une séance (C-05 quinzaine).
 * TOUTES = toutes les semaines, A = semaine A uniquement, B = semaine B uniquement.
 */
public enum SemainePlan {
    TOUTES,
    A,
    B;

    /**
     * Deux séances ne peuvent entrer en collision temporelle que si leurs semaines se chevauchent :
     * TOUTES chevauche tout, A ne chevauche que TOUTES et A, B ne chevauche que TOUTES et B.
     */
    public boolean chevauche(SemainePlan autre) {
        return this == TOUTES || autre == TOUTES || this == autre;
    }
}
