package ma.jadwal.solver.modele;

import java.util.Set;

/**
 * Matière planifiable (C-03 durées, E-02 type de salle, E-03 équipements, F-04 poids cognitif,
 * F-05 coefficient, G-05 flags de position).
 */
public record MatierePlan(long id, String libelle, int coefficient, int poidsCognitif, String typeSalleRequis,
        Set<String> equipementsRequis, int dureeMinUnites, int dureeMaxUnites,
        boolean eviterAvantDejeuner, boolean eviterFinJournee) {

    public MatierePlan {
        equipementsRequis = equipementsRequis == null ? Set.of() : Set.copyOf(equipementsRequis);
    }
}
