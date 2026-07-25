package ma.jadwal.solver.validation;

import java.util.List;

import org.junit.jupiter.api.Test;

import ma.jadwal.solver.modele.JourPlan;
import ma.jadwal.solver.modele.SeancePlan;

import static ma.jadwal.solver.contrainte.FabriqueTest.creneau;
import static ma.jadwal.solver.contrainte.FabriqueTest.enseignant;
import static ma.jadwal.solver.contrainte.FabriqueTest.groupe;
import static ma.jadwal.solver.contrainte.FabriqueTest.matiere;
import static ma.jadwal.solver.contrainte.FabriqueTest.salle;
import static ma.jadwal.solver.contrainte.FabriqueTest.seance;
import static ma.jadwal.solver.contrainte.FabriqueTest.sousGroupe;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validation synchrone d'un déplacement manuel (I-04) contre les règles dures.
 */
class ValidationConflitsTest {

    private final ValidationConflits validation = new ValidationConflits();

    @Test
    void detecteConflitsEnseignantEtSalleSurChevauchement() {
        var e = enseignant(9, 1);
        var salleCommune = salle(1, 30);
        var existante = seance("s1", groupe(1), matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salleCommune, e);

        // Candidate [2,6) le même jour, même enseignant et même salle.
        var candidate = seance("s2", groupe(2), matiere(1), 4,
                creneau(2, JourPlan.LUNDI, 2, 6, true), salleCommune, e);

        List<ConflitDur> conflits = validation.verifier(candidate, List.of(existante));

        assertThat(conflits).extracting(ConflitDur::regle).containsExactlyInAnyOrder("B-01", "B-03");
        assertThat(conflits).allSatisfy(c -> assertThat(c.message()).isNotBlank());
    }

    @Test
    void detecteConflitGroupeAvecHierarchieEtDebordementDePlage() {
        // Le sous-groupe 11 a déjà une séance : le groupe parent 1 ne peut pas être en cours en même temps.
        var existante = seance("s1", sousGroupe(11, 1), matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        var candidate = seance("s2", groupe(1), matiere(2), 4,
                creneau(2, JourPlan.LUNDI, 2, 2, true), salle(2, 30), enseignant(10, 2));

        List<ConflitDur> conflits = validation.verifier(candidate, List.of(existante));

        // B-02 (hiérarchie) + G-01 (4 unités sur un créneau n'en offrant que 2).
        assertThat(conflits).extracting(ConflitDur::regle).containsExactlyInAnyOrder("B-02", "G-01");
    }

    @Test
    void placementValideNeRemonteAucunConflit() {
        var existante = seance("s1", groupe(1), matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));

        // Autre jour, autre salle, autre enseignant, plage suffisante.
        var candidate = seance("s2", groupe(2), matiere(1), 4,
                creneau(2, JourPlan.MARDI, 0, 8, true), salle(2, 30), enseignant(10, 1));

        assertThat(validation.verifier(candidate, List.of(existante))).isEmpty();
    }

    @Test
    void ignoreLaVersionExistanteDeLaSeanceDeplacee() {
        var e = enseignant(9, 1);
        // La séance "s1" est déplacée : son ancienne version dans les existantes est ignorée.
        var ancienne = seance("s1", groupe(1), matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), e);
        var deplacee = seance("s1", groupe(1), matiere(1), 4,
                creneau(2, JourPlan.LUNDI, 2, 6, true), salle(1, 30), e);

        assertThat(validation.verifier(deplacee, List.of(ancienne))).isEmpty();
    }
}
