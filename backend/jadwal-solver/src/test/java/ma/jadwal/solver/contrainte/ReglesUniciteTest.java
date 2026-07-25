package ma.jadwal.solver.contrainte;

import org.junit.jupiter.api.Test;

import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;

import ma.jadwal.solver.modele.EmploiDuTempsPlan;
import ma.jadwal.solver.modele.JourPlan;
import ma.jadwal.solver.modele.SeancePlan;
import ma.jadwal.solver.modele.SemainePlan;

import static ma.jadwal.solver.contrainte.FabriqueTest.creneau;
import static ma.jadwal.solver.contrainte.FabriqueTest.enseignant;
import static ma.jadwal.solver.contrainte.FabriqueTest.groupe;
import static ma.jadwal.solver.contrainte.FabriqueTest.matiere;
import static ma.jadwal.solver.contrainte.FabriqueTest.salle;
import static ma.jadwal.solver.contrainte.FabriqueTest.seance;
import static ma.jadwal.solver.contrainte.FabriqueTest.sousGroupe;

/**
 * Contraintes d'unicité (niveau hard0) : B-01, B-02, B-03, B-04, B-05.
 */
class ReglesUniciteTest {

    private final ConstraintVerifier<ReglesEmploiDuTemps, EmploiDuTempsPlan> verifier =
            ConstraintVerifier.build(new ReglesEmploiDuTemps(), EmploiDuTempsPlan.class, SeancePlan.class);

    @Test
    void b01_enseignantNePeutPasAvoirDeuxSeancesQuiSeChevauchent() {
        var e = enseignant(9, 1, 2);
        var s1 = seance("s1", groupe(1), matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), e);
        var s2 = seance("s2", groupe(2), matiere(2), 2,
                creneau(2, JourPlan.LUNDI, 2, 6, true), salle(2, 30), e);

        verifier.verifyThat(ReglesEmploiDuTemps::conflitEnseignant)
                .given(s1, s2)
                .penalizesBy(1);

        // Pas de chevauchement : [0,4) puis [4,6).
        var s3 = seance("s3", groupe(2), matiere(2), 2,
                creneau(3, JourPlan.LUNDI, 4, 4, true), salle(2, 30), e);
        verifier.verifyThat(ReglesEmploiDuTemps::conflitEnseignant)
                .given(s1, s3)
                .penalizesBy(0);

        // Semaines incompatibles : A ne chevauche pas B.
        s1.setSemaine(SemainePlan.A);
        s2.setSemaine(SemainePlan.B);
        verifier.verifyThat(ReglesEmploiDuTemps::conflitEnseignant)
                .given(s1, s2)
                .penalizesBy(0);
    }

    @Test
    void b02_groupeNePeutPasAvoirDeuxSeancesQuiSeChevauchent_hierarchieIncluse() {
        var parent = groupe(1);
        var sg1 = sousGroupe(11, 1);
        var sg2 = sousGroupe(12, 1);

        // Même groupe.
        var s1 = seance("s1", parent, matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        var s2 = seance("s2", parent, matiere(2), 2,
                creneau(2, JourPlan.LUNDI, 2, 6, true), salle(2, 30), enseignant(10, 2));
        verifier.verifyThat(ReglesEmploiDuTemps::conflitGroupe)
                .given(s1, s2)
                .penalizesBy(1);

        // Sous-groupe contre groupe parent.
        var s3 = seance("s3", sg1, matiere(2), 2,
                creneau(2, JourPlan.LUNDI, 2, 6, true), salle(2, 30), enseignant(10, 2));
        verifier.verifyThat(ReglesEmploiDuTemps::conflitGroupe)
                .given(s1, s3)
                .penalizesBy(1);

        // Deux sous-groupes frères peuvent être simultanés (dédoublement A-04).
        var s4 = seance("s4", sg1, matiere(1), 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        var s5 = seance("s5", sg2, matiere(1), 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(2, 30), enseignant(10, 1));
        verifier.verifyThat(ReglesEmploiDuTemps::conflitGroupe)
                .given(s4, s5)
                .penalizesBy(0);
    }

    @Test
    void b03_salleNePeutPasAccueillirDeuxSeancesQuiSeChevauchent() {
        var salleCommune = salle(1, 30);
        var s1 = seance("s1", groupe(1), matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salleCommune, enseignant(9, 1));
        var s2 = seance("s2", groupe(2), matiere(2), 2,
                creneau(2, JourPlan.LUNDI, 2, 6, true), salleCommune, enseignant(10, 2));

        verifier.verifyThat(ReglesEmploiDuTemps::conflitSalle)
                .given(s1, s2)
                .penalizesBy(1);

        // Jours différents : pas de conflit.
        var s3 = seance("s3", groupe(2), matiere(2), 2,
                creneau(3, JourPlan.MARDI, 2, 6, true), salleCommune, enseignant(10, 2));
        verifier.verifyThat(ReglesEmploiDuTemps::conflitSalle)
                .given(s1, s3)
                .penalizesBy(0);
    }

    @Test
    void b04_seancesDuMemeBlocDeDedoublementDoiventEtreSimultanees() {
        var s1 = seance("s1", sousGroupe(11, 1), matiere(1), 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        var s2 = seance("s2", sousGroupe(12, 1), matiere(1), 2,
                creneau(2, JourPlan.LUNDI, 2, 6, true), salle(2, 30), enseignant(10, 1));
        s1.setBlocAlignementId("bloc-1");
        s2.setBlocAlignementId("bloc-1");

        // Index de début différents -> violation.
        verifier.verifyThat(ReglesEmploiDuTemps::alignementBloc)
                .given(s1, s2)
                .penalizesBy(1);

        // Même jour + même index -> aligné.
        s2.setCreneau(creneau(1, JourPlan.LUNDI, 0, 8, true));
        verifier.verifyThat(ReglesEmploiDuTemps::alignementBloc)
                .given(s1, s2)
                .penalizesBy(0);

        // Sans bloc, aucune exigence.
        s1.setBlocAlignementId(null);
        s2.setBlocAlignementId(null);
        s2.setCreneau(creneau(2, JourPlan.LUNDI, 2, 6, true));
        verifier.verifyThat(ReglesEmploiDuTemps::alignementBloc)
                .given(s1, s2)
                .penalizesBy(0);
    }

    @Test
    void b05_seancesDeLaMemeBarretteDoiventEtreSimultanees() {
        var s1 = seance("s1", groupe(1), matiere(1), 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        var s2 = seance("s2", groupe(2), matiere(1), 2,
                creneau(2, JourPlan.MARDI, 0, 8, true), salle(2, 30), enseignant(10, 1));
        s1.setBarretteId(5L);
        s2.setBarretteId(5L);

        // Jours différents -> violation.
        verifier.verifyThat(ReglesEmploiDuTemps::alignementBarrette)
                .given(s1, s2)
                .penalizesBy(1);

        // Même jour + même index -> aligné.
        s2.setCreneau(creneau(1, JourPlan.LUNDI, 0, 8, true));
        verifier.verifyThat(ReglesEmploiDuTemps::alignementBarrette)
                .given(s1, s2)
                .penalizesBy(0);
    }
}
