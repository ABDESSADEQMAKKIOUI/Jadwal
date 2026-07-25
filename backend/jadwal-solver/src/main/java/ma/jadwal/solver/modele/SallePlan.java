package ma.jadwal.solver.modele;

import java.util.Set;

/**
 * Salle planifiable (E-01 capacité, E-02 type, E-03 équipements, E-04 bâtiment).
 */
public record SallePlan(long id, String nom, int capacite, String type, Set<String> equipements, String batiment) {

    public SallePlan {
        equipements = equipements == null ? Set.of() : Set.copyOf(equipements);
    }
}
