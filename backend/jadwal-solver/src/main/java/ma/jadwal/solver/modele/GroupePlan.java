package ma.jadwal.solver.modele;

/**
 * Groupe planifiable : classe entière ou sous-groupe de dédoublement (A-04).
 *
 * @param parentId            identifiant du groupe parent si ce groupe est un sous-groupe, sinon null (B-02).
 * @param chargeMaxUnitesJour charge journalière maximale du niveau (G-06), null si non définie.
 */
public record GroupePlan(long id, String libelle, int effectif, Long parentId, long niveauId,
        int niveauOrdre, Integer chargeMaxUnitesJour) {

    /** B-02 : deux groupes sont liés s'ils sont identiques ou en relation parent/sous-groupe. */
    public boolean estLieA(GroupePlan autre) {
        if (autre == null) {
            return false;
        }
        return id == autre.id
                || (parentId != null && parentId == autre.id())
                || (autre.parentId() != null && autre.parentId() == id);
    }
}
