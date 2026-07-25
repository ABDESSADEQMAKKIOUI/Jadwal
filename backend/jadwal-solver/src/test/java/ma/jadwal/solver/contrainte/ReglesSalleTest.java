package ma.jadwal.solver.contrainte;

import java.util.Set;

import org.junit.jupiter.api.Test;

import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;

import ma.jadwal.solver.modele.EmploiDuTempsPlan;
import ma.jadwal.solver.modele.JourPlan;
import ma.jadwal.solver.modele.MatierePlan;
import ma.jadwal.solver.modele.SeancePlan;

import static ma.jadwal.solver.contrainte.FabriqueTest.creneau;
import static ma.jadwal.solver.contrainte.FabriqueTest.enseignant;
import static ma.jadwal.solver.contrainte.FabriqueTest.groupe;
import static ma.jadwal.solver.contrainte.FabriqueTest.matiere;
import static ma.jadwal.solver.contrainte.FabriqueTest.salle;
import static ma.jadwal.solver.contrainte.FabriqueTest.seance;

/**
 * Contraintes de salles : E-01, E-02, E-03 (hard2) et E-04 (soft2).
 */
class ReglesSalleTest {

    private final ConstraintVerifier<ReglesEmploiDuTemps, EmploiDuTempsPlan> verifier =
            ConstraintVerifier.build(new ReglesEmploiDuTemps(), EmploiDuTempsPlan.class, SeancePlan.class);

    @Test
    void e01_capaciteSalleCouvreEffectifGroupe() {
        // Salle de 20 places pour un groupe de 30 élèves.
        var s = seance("s1", groupe(1), matiere(1), 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 20), enseignant(9, 1));
        verifier.verifyThat(ReglesEmploiDuTemps::capaciteSalle)
                .given(s)
                .penalizesBy(1);

        s.setSalle(salle(2, 35));
        verifier.verifyThat(ReglesEmploiDuTemps::capaciteSalle)
                .given(s)
                .penalizesBy(0);
    }

    @Test
    void e02_typeDeSalleRequisRespecte() {
        var svt = new MatierePlan(1, "SVT", 4, 3, "LABO", Set.of(), 2, 4, false, false);
        var s = seance("s1", groupe(1), svt, 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));

        // Salle STANDARD alors que la matière exige un LABO.
        verifier.verifyThat(ReglesEmploiDuTemps::typeSalle)
                .given(s)
                .penalizesBy(1);

        s.setSalle(salle(2, 30, "LABO", Set.of(), "A"));
        verifier.verifyThat(ReglesEmploiDuTemps::typeSalle)
                .given(s)
                .penalizesBy(0);
    }

    @Test
    void e03_equipementsRequisPresentsDansLaSalle() {
        var physique = new MatierePlan(1, "Physique", 4, 3, null, Set.of("PROJECTEUR"), 2, 4, false, false);
        var s = seance("s1", groupe(1), physique, 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));

        // La salle n'a pas de projecteur.
        verifier.verifyThat(ReglesEmploiDuTemps::equipementsSalle)
                .given(s)
                .penalizesBy(1);

        s.setSalle(salle(2, 30, "STANDARD", Set.of("PROJECTEUR", "TABLEAU"), "A"));
        verifier.verifyThat(ReglesEmploiDuTemps::equipementsSalle)
                .given(s)
                .penalizesBy(0);
    }

    @Test
    void e04_minimiserChangementsDeBatimentDansLaJournee() {
        var g = groupe(1);
        var batA1 = salle(1, 30, "STANDARD", Set.of(), "A");
        var batB = salle(2, 30, "STANDARD", Set.of(), "B");
        var batA2 = salle(3, 30, "STANDARD", Set.of(), "A");

        // A -> B -> A : 2 changements de bâtiment dans la journée.
        var s1 = seance("s1", g, matiere(1), 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), batA1, enseignant(9, 1));
        var s2 = seance("s2", g, matiere(2), 2,
                creneau(2, JourPlan.LUNDI, 2, 6, true), batB, enseignant(10, 2));
        var s3 = seance("s3", g, matiere(3), 2,
                creneau(3, JourPlan.LUNDI, 4, 4, true), batA2, enseignant(11, 3));
        verifier.verifyThat(ReglesEmploiDuTemps::changementsBatimentGroupe)
                .given(s1, s2, s3)
                .penalizesBy(2);

        // Tout dans le même bâtiment : aucun changement.
        s2.setSalle(batA2);
        s3.setSalle(batA1);
        verifier.verifyThat(ReglesEmploiDuTemps::changementsBatimentGroupe)
                .given(s1, s2, s3)
                .penalizesBy(0);
    }
}
