package ma.jadwal.solver.contrainte;

import java.util.Set;

import org.junit.jupiter.api.Test;

import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;

import ma.jadwal.solver.modele.EmploiDuTempsPlan;
import ma.jadwal.solver.modele.GroupePlan;
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
 * Contraintes de grille et de confort des groupes : G-01, G-02, G-03 (hard2), G-04, G-05, G-06 (soft0).
 */
class ReglesGrilleGroupeTest {

    private final ConstraintVerifier<ReglesEmploiDuTemps, EmploiDuTempsPlan> verifier =
            ConstraintVerifier.build(new ReglesEmploiDuTemps(), EmploiDuTempsPlan.class, SeancePlan.class);

    @Test
    void g01_seanceNeDebordePasDeLaPlageContigue() {
        // 2 unités disponibles avant la pause, séance de 4 unités : débordement de 2.
        var s = seance("s1", groupe(1), matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 6, 2, true), salle(1, 30), enseignant(9, 1));
        verifier.verifyThat(ReglesEmploiDuTemps::debordementPlage)
                .given(s)
                .penalizesBy(2);

        s.setCreneau(creneau(2, JourPlan.LUNDI, 0, 8, true));
        verifier.verifyThat(ReglesEmploiDuTemps::debordementPlage)
                .given(s)
                .penalizesBy(0);
    }

    @Test
    void g02_amplitudeJournaliereDuGroupePlafonnee() {
        var g = groupe(1);
        var s1 = seance("s1", g, matiere(1), 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        var s2 = seance("s2", g, matiere(2), 2,
                creneau(2, JourPlan.LUNDI, 10, 4, false), salle(1, 30), enseignant(10, 2));
        s1.setAmplitudeMaxUnitesGroupe(8);
        s2.setAmplitudeMaxUnitesGroupe(8);

        // Amplitude 12 (de 0 à 12) pour un max de 8 : excédent de 4.
        verifier.verifyThat(ReglesEmploiDuTemps::amplitudeGroupe)
                .given(s1, s2)
                .penalizesBy(4);

        // Journée resserrée : amplitude 6.
        s2.setCreneau(creneau(3, JourPlan.LUNDI, 4, 4, true));
        verifier.verifyThat(ReglesEmploiDuTemps::amplitudeGroupe)
                .given(s1, s2)
                .penalizesBy(0);
    }

    @Test
    void g03_plusLongueSuiteContigueDuGroupePlafonneeA8() {
        var g = groupe(1);
        // [0,4) + [4,8) + [8,10) = suite contiguë de 10 unités : excédent de 2 au-delà de 8.
        var s1 = seance("s1", g, matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 0, 12, true), salle(1, 30), enseignant(9, 1));
        var s2 = seance("s2", g, matiere(2), 4,
                creneau(2, JourPlan.LUNDI, 4, 8, true), salle(1, 30), enseignant(10, 2));
        var s3 = seance("s3", g, matiere(3), 2,
                creneau(3, JourPlan.LUNDI, 8, 4, true), salle(1, 30), enseignant(11, 3));
        verifier.verifyThat(ReglesEmploiDuTemps::suiteContigueGroupe)
                .given(s1, s2, s3)
                .penalizesBy(2);

        // Avec une pause au milieu, la plus longue suite est de 8 : autorisé.
        s3.setCreneau(creneau(4, JourPlan.LUNDI, 10, 2, false));
        verifier.verifyThat(ReglesEmploiDuTemps::suiteContigueGroupe)
                .given(s1, s2, s3)
                .penalizesBy(0);
    }

    @Test
    void g04_trousDesGroupesPenalises() {
        var g = groupe(1);
        // [0,2) puis [6,8) : 4 unités de trou entre les deux.
        var s1 = seance("s1", g, matiere(1), 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        var s2 = seance("s2", g, matiere(2), 2,
                creneau(2, JourPlan.LUNDI, 6, 2, true), salle(1, 30), enseignant(10, 2));
        verifier.verifyThat(ReglesEmploiDuTemps::trousGroupes)
                .given(s1, s2)
                .penalizesBy(4);

        // Séances enchaînées : aucun trou.
        s2.setCreneau(creneau(3, JourPlan.LUNDI, 2, 6, true));
        verifier.verifyThat(ReglesEmploiDuTemps::trousGroupes)
                .given(s1, s2)
                .penalizesBy(0);
    }

    @Test
    void g05_matiereEviteeEnFinDeDemiJournee() {
        // EPS à éviter juste avant le déjeuner : le créneau [4,8) du matin est épuisé par la séance.
        var eps = new MatierePlan(1, "EPS", 2, 2, null, Set.of(), 2, 4, true, false);
        var s1 = seance("s1", groupe(1), eps, 4,
                creneau(1, JourPlan.LUNDI, 4, 4, true), salle(1, 30), enseignant(9, 1));
        verifier.verifyThat(ReglesEmploiDuTemps::positionMatiere)
                .given(s1)
                .penalizesBy(1);

        // Matière à éviter en fin de journée, séance qui termine la journée (après-midi).
        var arts = new MatierePlan(2, "Arts", 2, 2, null, Set.of(), 2, 4, false, true);
        var s2 = seance("s2", groupe(1), arts, 4,
                creneau(2, JourPlan.LUNDI, 12, 4, false), salle(1, 30), enseignant(9, 2));
        verifier.verifyThat(ReglesEmploiDuTemps::positionMatiere)
                .given(s2)
                .penalizesBy(1);

        // La même séance EPS plus tôt dans la matinée ne termine pas la demi-journée.
        var s3 = seance("s3", groupe(1), eps, 4,
                creneau(3, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        verifier.verifyThat(ReglesEmploiDuTemps::positionMatiere)
                .given(s3)
                .penalizesBy(0);
    }

    @Test
    void g06_chargeJournaliereMaxDuNiveauRespectee() {
        // Niveau limité à 4 unités par jour.
        var g = new GroupePlan(1, "CP-A", 30, null, 1, 1, 4);
        var s1 = seance("s1", g, matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        var s2 = seance("s2", g, matiere(2), 2,
                creneau(2, JourPlan.LUNDI, 4, 4, true), salle(1, 30), enseignant(10, 2));

        // 6 unités pour un plafond de 4 : excédent de 2.
        verifier.verifyThat(ReglesEmploiDuTemps::chargeMaxNiveau)
                .given(s1, s2)
                .penalizesBy(2);

        // Réparti sur deux jours : chaque jour est dans la limite.
        s2.setCreneau(creneau(3, JourPlan.MARDI, 0, 8, true));
        verifier.verifyThat(ReglesEmploiDuTemps::chargeMaxNiveau)
                .given(s1, s2)
                .penalizesBy(0);
    }
}
