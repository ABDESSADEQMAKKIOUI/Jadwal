package ma.jadwal.solver.contrainte;

import java.util.List;
import java.util.Map;
import java.util.Set;

import ma.jadwal.solver.modele.CreneauPlan;
import ma.jadwal.solver.modele.EnseignantPlan;
import ma.jadwal.solver.modele.GroupePlan;
import ma.jadwal.solver.modele.IndispoPlan;
import ma.jadwal.solver.modele.JourPlan;
import ma.jadwal.solver.modele.MatierePlan;
import ma.jadwal.solver.modele.PreferencePlan;
import ma.jadwal.solver.modele.SallePlan;
import ma.jadwal.solver.modele.SeancePlan;

/**
 * Fabrique d'objets de test avec des valeurs par défaut neutres : chaque test ne fait varier
 * que ce qui concerne SA règle (la vérification est mono-contrainte, pas d'interférences).
 */
public final class FabriqueTest {

    private FabriqueTest() {
    }

    public static CreneauPlan creneau(long id, JourPlan jour, int indexDebut, int unitesDisponibles,
            boolean matin) {
        return new CreneauPlan(id, jour, indexDebut, unitesDisponibles, matin);
    }

    public static SallePlan salle(long id, int capacite) {
        return new SallePlan(id, "Salle " + id, capacite, "STANDARD", Set.of(), "A");
    }

    public static SallePlan salle(long id, int capacite, String type, Set<String> equipements,
            String batiment) {
        return new SallePlan(id, "Salle " + id, capacite, type, equipements, batiment);
    }

    /** Enseignant très permissif : quota 100, pas de limite de rythme, habilité « tous niveaux ». */
    public static EnseignantPlan enseignant(long id, long... matiereIds) {
        Map<Long, Set<Long>> habilitations = new java.util.HashMap<>();
        for (long matiereId : matiereIds) {
            habilitations.put(matiereId, Set.of());
        }
        return new EnseignantPlan(id, "Enseignant " + id, 100, 100, null, false, habilitations,
                List.of(), List.of());
    }

    public static EnseignantPlan enseignant(long id, int quota, int maxConsecutif, Integer amplitudeMax,
            boolean vacataire, Map<Long, Set<Long>> habilitations, List<IndispoPlan> indispos,
            List<PreferencePlan> preferences) {
        return new EnseignantPlan(id, "Enseignant " + id, quota, maxConsecutif, amplitudeMax, vacataire,
                habilitations, indispos, preferences);
    }

    public static GroupePlan groupe(long id) {
        return new GroupePlan(id, "Groupe " + id, 30, null, 1, 1, null);
    }

    public static GroupePlan sousGroupe(long id, long parentId) {
        return new GroupePlan(id, "Sous-groupe " + id, 15, parentId, 1, 1, null);
    }

    /** Matière neutre : coefficient 2, poids cognitif 2, durées 2..4, aucun besoin de salle. */
    public static MatierePlan matiere(long id) {
        return new MatierePlan(id, "Matière " + id, 2, 2, null, Set.of(), 2, 4, false, false);
    }

    public static SeancePlan seance(String id, GroupePlan groupe, MatierePlan matiere, int duree,
            CreneauPlan creneau, SallePlan salle, EnseignantPlan enseignant) {
        SeancePlan s = new SeancePlan(id, null, groupe, matiere, duree, null, null, null, null, 0);
        s.setCreneau(creneau);
        s.setSalle(salle);
        s.setEnseignant(enseignant);
        return s;
    }
}
