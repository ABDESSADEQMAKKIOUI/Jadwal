package ma.jadwal.solver.contrainte;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;

import ma.jadwal.solver.modele.EmploiDuTempsPlan;
import ma.jadwal.solver.modele.JourPlan;
import ma.jadwal.solver.modele.PreferencePlan;
import ma.jadwal.solver.modele.SeancePlan;

import static ma.jadwal.solver.contrainte.FabriqueTest.creneau;
import static ma.jadwal.solver.contrainte.FabriqueTest.enseignant;
import static ma.jadwal.solver.contrainte.FabriqueTest.groupe;
import static ma.jadwal.solver.contrainte.FabriqueTest.matiere;
import static ma.jadwal.solver.contrainte.FabriqueTest.salle;
import static ma.jadwal.solver.contrainte.FabriqueTest.seance;

/**
 * Contraintes de confort des enseignants (niveau soft2) : D-08, D-09, D-10.
 * (E-04 est testée dans ReglesSalleTest.)
 */
class ReglesConfortTest {

    private final ConstraintVerifier<ReglesEmploiDuTemps, EmploiDuTempsPlan> verifier =
            ConstraintVerifier.build(new ReglesEmploiDuTemps(), EmploiDuTempsPlan.class, SeancePlan.class);

    @Test
    void d08_minimiserJoursDePresenceDesVacataires() {
        var vacataire = enseignant(9, 100, 100, null, true, Map.of(1L, Set.of()), List.of(), List.of());
        var s1 = seance("s1", groupe(1), matiere(1), 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), vacataire);
        var s2 = seance("s2", groupe(2), matiere(1), 2,
                creneau(2, JourPlan.MARDI, 0, 8, true), salle(1, 30), vacataire);

        // Présent 2 jours : pénalité de (2 - 1) = 1.
        verifier.verifyThat(ReglesEmploiDuTemps::joursPresenceVacataires)
                .given(s1, s2)
                .penalizesBy(1);

        // Tout regroupé sur un seul jour : aucune pénalité.
        s2.setCreneau(creneau(3, JourPlan.LUNDI, 2, 6, true));
        verifier.verifyThat(ReglesEmploiDuTemps::joursPresenceVacataires)
                .given(s1, s2)
                .penalizesBy(0);

        // Un permanent sur 2 jours n'est pas concerné.
        var permanent = enseignant(10, 1);
        var s3 = seance("s3", groupe(1), matiere(1), 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), permanent);
        var s4 = seance("s4", groupe(2), matiere(1), 2,
                creneau(2, JourPlan.MARDI, 0, 8, true), salle(1, 30), permanent);
        verifier.verifyThat(ReglesEmploiDuTemps::joursPresenceVacataires)
                .given(s3, s4)
                .penalizesBy(0);
    }

    @Test
    void d09_minimiserTrousDesEnseignants() {
        var e = enseignant(9, 1, 2);
        // [0,2) puis [6,8) : 4 unités de trou.
        var s1 = seance("s1", groupe(1), matiere(1), 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), e);
        var s2 = seance("s2", groupe(2), matiere(2), 2,
                creneau(2, JourPlan.LUNDI, 6, 2, true), salle(2, 30), e);
        verifier.verifyThat(ReglesEmploiDuTemps::trousEnseignants)
                .given(s1, s2)
                .penalizesBy(4);

        // Séances enchaînées : aucun trou.
        s2.setCreneau(creneau(3, JourPlan.LUNDI, 2, 6, true));
        verifier.verifyThat(ReglesEmploiDuTemps::trousEnseignants)
                .given(s1, s2)
                .penalizesBy(0);
    }

    @Test
    void d10_plagesEviterPenaliseesEtPrefererRecompensees() {
        // Plage EVITER lundi [0,4) : séance [2,6) -> 2 unités dessus.
        var eviteur = enseignant(9, 100, 100, null, false, Map.of(1L, Set.of()), List.of(),
                List.of(new PreferencePlan(JourPlan.LUNDI, 0, 4, true)));
        var s1 = seance("s1", groupe(1), matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 2, 6, true), salle(1, 30), eviteur);
        verifier.verifyThat(ReglesEmploiDuTemps::preferencesEnseignant)
                .given(s1)
                .penalizesBy(2);

        // Plage PREFERER lundi [0,4) : séance [2,6) -> 2 unités récompensées.
        var prefereur = enseignant(10, 100, 100, null, false, Map.of(1L, Set.of()), List.of(),
                List.of(new PreferencePlan(JourPlan.LUNDI, 0, 4, false)));
        var s2 = seance("s2", groupe(1), matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 2, 6, true), salle(1, 30), prefereur);
        verifier.verifyThat(ReglesEmploiDuTemps::preferencesEnseignant)
                .given(s2)
                .rewardsWith(2);

        // Séance hors de toute plage de préférence : neutre.
        var s3 = seance("s3", groupe(1), matiere(1), 2,
                creneau(2, JourPlan.MARDI, 0, 8, true), salle(1, 30), eviteur);
        verifier.verifyThat(ReglesEmploiDuTemps::preferencesEnseignant)
                .given(s3)
                .penalizesBy(0);
    }
}
