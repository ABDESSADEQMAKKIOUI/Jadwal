package ma.jadwal.solver.contrainte;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;

import ma.jadwal.solver.modele.EmploiDuTempsPlan;
import ma.jadwal.solver.modele.GroupePlan;
import ma.jadwal.solver.modele.IndispoPlan;
import ma.jadwal.solver.modele.JourPlan;
import ma.jadwal.solver.modele.SeancePlan;
import ma.jadwal.solver.modele.SemainePlan;

import static ma.jadwal.solver.contrainte.FabriqueTest.creneau;
import static ma.jadwal.solver.contrainte.FabriqueTest.enseignant;
import static ma.jadwal.solver.contrainte.FabriqueTest.groupe;
import static ma.jadwal.solver.contrainte.FabriqueTest.matiere;
import static ma.jadwal.solver.contrainte.FabriqueTest.salle;
import static ma.jadwal.solver.contrainte.FabriqueTest.seance;

/**
 * Contraintes dures des enseignants (niveau hard2) : D-01, D-02, D-03, D-07.
 */
class ReglesEnseignantTest {

    private final ConstraintVerifier<ReglesEmploiDuTemps, EmploiDuTempsPlan> verifier =
            ConstraintVerifier.build(new ReglesEmploiDuTemps(), EmploiDuTempsPlan.class, SeancePlan.class);

    @Test
    void d01_quotaHebdomadaireRespecte() {
        // Quota de 4 unités seulement.
        var e = enseignant(9, 4, 100, null, false, Map.of(1L, Set.of()), List.of(), List.of());
        var s1 = seance("s1", groupe(1), matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), e);
        var s2 = seance("s2", groupe(2), matiere(1), 2,
                creneau(2, JourPlan.MARDI, 0, 8, true), salle(1, 30), e);

        // 4 + 2 = 6 unités pour un quota de 4 : dépassement de 2.
        verifier.verifyThat(ReglesEmploiDuTemps::quotaEnseignant)
                .given(s1, s2)
                .penalizesBy(2);

        // Une seule séance de 4 unités : dans le quota.
        verifier.verifyThat(ReglesEmploiDuTemps::quotaEnseignant)
                .given(s1)
                .penalizesBy(0);
    }

    @Test
    void d02_matieresHabiliteesEtNiveauxAutorises() {
        // Habilité seulement pour la matière 1 (tous niveaux).
        var e = enseignant(9, 1);
        var s = seance("s1", groupe(1), matiere(2), 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), e);
        verifier.verifyThat(ReglesEmploiDuTemps::habilitationEnseignant)
                .given(s)
                .penalizesBy(1);

        // Habilité pour la matière 3 mais uniquement au niveau 5 ; le groupe est de niveau 1.
        var eNiveau = enseignant(10, 100, 100, null, false, Map.of(3L, Set.of(5L)), List.of(), List.of());
        var s2 = seance("s2", groupe(1), matiere(3), 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), eNiveau);
        verifier.verifyThat(ReglesEmploiDuTemps::habilitationEnseignant)
                .given(s2)
                .penalizesBy(1);

        // Groupe du bon niveau (5) : autorisé.
        var groupeNiveau5 = new GroupePlan(2, "2NDE-A", 30, null, 5, 5, null);
        var s3 = seance("s3", groupeNiveau5, matiere(3), 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), eNiveau);
        verifier.verifyThat(ReglesEmploiDuTemps::habilitationEnseignant)
                .given(s3)
                .penalizesBy(0);
    }

    @Test
    void d03_aucuneSeanceSurIndisponibiliteValidee() {
        var indispo = new IndispoPlan(JourPlan.LUNDI, 0, 4, SemainePlan.TOUTES);
        var e = enseignant(9, 100, 100, null, false, Map.of(1L, Set.of()), List.of(indispo), List.of());

        // Séance [2,6) chevauchant l'indispo [0,4).
        var s = seance("s1", groupe(1), matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 2, 6, true), salle(1, 30), e);
        verifier.verifyThat(ReglesEmploiDuTemps::indisponibiliteEnseignant)
                .given(s)
                .penalizesBy(1);

        // Séance après l'indispo : autorisée.
        var s2 = seance("s2", groupe(1), matiere(1), 4,
                creneau(2, JourPlan.LUNDI, 4, 4, true), salle(1, 30), e);
        verifier.verifyThat(ReglesEmploiDuTemps::indisponibiliteEnseignant)
                .given(s2)
                .penalizesBy(0);

        // Indispo semaine A seulement : une séance semaine B est autorisée.
        var indispoA = new IndispoPlan(JourPlan.LUNDI, 0, 4, SemainePlan.A);
        var eQuinzaine = enseignant(10, 100, 100, null, false, Map.of(1L, Set.of()),
                List.of(indispoA), List.of());
        var s3 = seance("s3", groupe(1), matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 2, 6, true), salle(1, 30), eQuinzaine);
        s3.setSemaine(SemainePlan.B);
        verifier.verifyThat(ReglesEmploiDuTemps::indisponibiliteEnseignant)
                .given(s3)
                .penalizesBy(0);
    }

    @Test
    void d07_heuresConsecutivesEtAmplitudeJournaliere() {
        // Max 4 unités consécutives : [0,4) + [4,6) = suite de 6, excédent de 2.
        var e = enseignant(9, 100, 4, null, false, Map.of(1L, Set.of()), List.of(), List.of());
        var s1 = seance("s1", groupe(1), matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), e);
        var s2 = seance("s2", groupe(2), matiere(1), 2,
                creneau(2, JourPlan.LUNDI, 4, 4, true), salle(2, 30), e);
        verifier.verifyThat(ReglesEmploiDuTemps::rythmeEnseignant)
                .given(s1, s2)
                .penalizesBy(2);

        // Amplitude max 8 : séances [0,2) et [10,12) -> amplitude 12, excédent de 4.
        var eAmplitude = enseignant(10, 100, 100, 8, false, Map.of(1L, Set.of()), List.of(), List.of());
        var s3 = seance("s3", groupe(1), matiere(1), 2,
                creneau(3, JourPlan.MARDI, 0, 8, true), salle(1, 30), eAmplitude);
        var s4 = seance("s4", groupe(2), matiere(1), 2,
                creneau(4, JourPlan.MARDI, 10, 4, false), salle(2, 30), eAmplitude);
        verifier.verifyThat(ReglesEmploiDuTemps::rythmeEnseignant)
                .given(s3, s4)
                .penalizesBy(4);

        // Journée compacte dans les limites : aucun excédent.
        var s5 = seance("s5", groupe(1), matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), eAmplitude);
        verifier.verifyThat(ReglesEmploiDuTemps::rythmeEnseignant)
                .given(s5)
                .penalizesBy(0);
    }
}
