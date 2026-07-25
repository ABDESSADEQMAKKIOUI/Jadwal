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
 * Contraintes de volumes / structure (niveau hard1) : C-06, F-01, F-07.
 */
class ReglesStructureTest {

    private final ConstraintVerifier<ReglesEmploiDuTemps, EmploiDuTempsPlan> verifier =
            ConstraintVerifier.build(new ReglesEmploiDuTemps(), EmploiDuTempsPlan.class, SeancePlan.class);

    @Test
    void c06_enseignantImposeParAffectation() {
        var s = seance("s1", groupe(1), matiere(1), 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        s.setAffectationEnseignantId(99L);

        // L'enseignant affecté (9) n'est pas l'enseignant imposé (99).
        verifier.verifyThat(ReglesEmploiDuTemps::enseignantImpose)
                .given(s)
                .penalizesBy(1);

        // L'enseignant imposé est respecté.
        s.setAffectationEnseignantId(9L);
        verifier.verifyThat(ReglesEmploiDuTemps::enseignantImpose)
                .given(s)
                .penalizesBy(0);
    }

    @Test
    void f01_dureesCumuleesParJourPlafonnees() {
        var g = groupe(1);
        var m = matiere(1);
        var s1 = seance("s1", g, m, 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        var s2 = seance("s2", g, m, 2,
                creneau(2, JourPlan.LUNDI, 4, 4, true), salle(1, 30), enseignant(9, 1));
        s1.setMaxParJourUnites(4);
        s2.setMaxParJourUnites(4);

        // 4 + 2 = 6 unités le même jour pour un plafond de 4 : excédent de 2.
        verifier.verifyThat(ReglesEmploiDuTemps::maxParJour)
                .given(s1, s2)
                .penalizesBy(2);

        // Sur deux jours, chaque jour respecte le plafond.
        s2.setCreneau(creneau(3, JourPlan.MARDI, 4, 4, true));
        verifier.verifyThat(ReglesEmploiDuTemps::maxParJour)
                .given(s1, s2)
                .penalizesBy(0);
    }

    @Test
    void f07_seancesAdjacentesDuMemeCoupleInterditesSaufBlocsLongs() {
        var g = groupe(1);
        // Matière à séances courtes (dureeMax < 4) : blocs adjacents interdits.
        var matiereCourte = new MatierePlan(1, "Arabe", 2, 2, null, Set.of(), 2, 2, false, false);
        var s1 = seance("s1", g, matiereCourte, 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        var s2 = seance("s2", g, matiereCourte, 2,
                creneau(2, JourPlan.LUNDI, 2, 6, true), salle(1, 30), enseignant(9, 1));

        verifier.verifyThat(ReglesEmploiDuTemps::seancesAdjacentesInterdites)
                .given(s1, s2)
                .penalizesBy(1);

        // Matière autorisant les blocs de 2h (dureeMax >= 4) : autorisé.
        var matiereLongue = new MatierePlan(2, "Mathématiques", 4, 4, null, Set.of(), 2, 4, false, false);
        var s3 = seance("s3", g, matiereLongue, 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 2));
        var s4 = seance("s4", g, matiereLongue, 2,
                creneau(2, JourPlan.LUNDI, 2, 6, true), salle(1, 30), enseignant(9, 2));
        verifier.verifyThat(ReglesEmploiDuTemps::seancesAdjacentesInterdites)
                .given(s3, s4)
                .penalizesBy(0);

        // Séances éloignées dans la journée : autorisé même pour une matière courte.
        var s5 = seance("s5", g, matiereCourte, 2,
                creneau(3, JourPlan.LUNDI, 6, 2, true), salle(1, 30), enseignant(9, 1));
        verifier.verifyThat(ReglesEmploiDuTemps::seancesAdjacentesInterdites)
                .given(s1, s5)
                .penalizesBy(0);
    }
}
