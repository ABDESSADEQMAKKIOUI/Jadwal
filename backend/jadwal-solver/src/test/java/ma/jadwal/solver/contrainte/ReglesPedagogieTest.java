package ma.jadwal.solver.contrainte;

import java.util.Set;

import org.junit.jupiter.api.Test;

import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;

import ma.jadwal.solver.modele.EmploiDuTempsPlan;
import ma.jadwal.solver.modele.JourPlan;
import ma.jadwal.solver.modele.MatierePlan;
import ma.jadwal.solver.modele.SeancePlan;
import ma.jadwal.solver.modele.SemainePlan;

import static ma.jadwal.solver.contrainte.FabriqueTest.creneau;
import static ma.jadwal.solver.contrainte.FabriqueTest.enseignant;
import static ma.jadwal.solver.contrainte.FabriqueTest.groupe;
import static ma.jadwal.solver.contrainte.FabriqueTest.matiere;
import static ma.jadwal.solver.contrainte.FabriqueTest.salle;
import static ma.jadwal.solver.contrainte.FabriqueTest.seance;

/**
 * Contraintes pédagogiques (niveau soft1) : F-02, F-03, F-04, F-05, F-06.
 */
class ReglesPedagogieTest {

    private final ConstraintVerifier<ReglesEmploiDuTemps, EmploiDuTempsPlan> verifier =
            ConstraintVerifier.build(new ReglesEmploiDuTemps(), EmploiDuTempsPlan.class, SeancePlan.class);

    @Test
    void f02_espacementMinimalEntreSeancesDuMemeCouple() {
        var g = groupe(1);
        var m = matiere(1);
        var s1 = seance("s1", g, m, 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        var s2 = seance("s2", g, m, 2,
                creneau(2, JourPlan.MARDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        s1.setGapMinJours(2);
        s2.setGapMinJours(2);

        // Lundi puis mardi : écart de 1 jour pour un minimum de 2 -> pénalité de 1.
        verifier.verifyThat(ReglesEmploiDuTemps::espacementSeances)
                .given(s1, s2)
                .penalizesBy(1);

        // Lundi puis jeudi : écart de 3 jours -> respecté.
        s2.setCreneau(creneau(3, JourPlan.JEUDI, 0, 8, true));
        verifier.verifyThat(ReglesEmploiDuTemps::espacementSeances)
                .given(s1, s2)
                .penalizesBy(0);
    }

    @Test
    void f03_chargeJournaliereHomogeneAutourDeLaMoyenne() {
        var g = groupe(1);
        var s1 = seance("s1", g, matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        var s2 = seance("s2", g, matiere(2), 4,
                creneau(2, JourPlan.LUNDI, 4, 4, true), salle(1, 30), enseignant(10, 2));
        s1.setChargeMoyenneUnites(4);
        s2.setChargeMoyenneUnites(4);

        // Charge du jour 8 pour une moyenne de 4 : |8-4| - 2 de tolérance = 2.
        verifier.verifyThat(ReglesEmploiDuTemps::equilibreChargeJournaliere)
                .given(s1, s2)
                .penalizesBy(2);

        // Charge du jour 4 = moyenne : rien à signaler.
        verifier.verifyThat(ReglesEmploiDuTemps::equilibreChargeJournaliere)
                .given(s1)
                .penalizesBy(0);
    }

    @Test
    void f04_poidsCognitifParDemiJourneePlafonne() {
        var g = groupe(1);
        // Poids cognitif 4 sur 4 unités le matin : 16 pour un seuil de 12 -> excédent 4.
        var math = new MatierePlan(1, "Mathématiques", 4, 4, null, Set.of(), 2, 4, false, false);
        var s = seance("s1", g, math, 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        verifier.verifyThat(ReglesEmploiDuTemps::poidsCognitifDemiJournee)
                .given(s)
                .penalizesBy(4);

        // Matière légère (poids 2 sur 4 unités = 8) : sous le seuil.
        var s2 = seance("s2", g, matiere(2), 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 2));
        verifier.verifyThat(ReglesEmploiDuTemps::poidsCognitifDemiJournee)
                .given(s2)
                .penalizesBy(0);
    }

    @Test
    void f05_matiereForteCommenceEnDebutDeJournee() {
        // Coefficient 5 >= 4 et début à l'index 4 (au-delà des 4 premières unités).
        var math = new MatierePlan(1, "Mathématiques", 5, 4, null, Set.of(), 2, 4, false, false);
        var s = seance("s1", groupe(1), math, 2,
                creneau(1, JourPlan.LUNDI, 4, 4, true), salle(1, 30), enseignant(9, 1));
        verifier.verifyThat(ReglesEmploiDuTemps::matiereForteEnDebutDeJournee)
                .given(s)
                .penalizesBy(2);

        // La même matière en tout début de journée : rien.
        s.setCreneau(creneau(2, JourPlan.LUNDI, 0, 8, true));
        verifier.verifyThat(ReglesEmploiDuTemps::matiereForteEnDebutDeJournee)
                .given(s)
                .penalizesBy(0);
    }

    @Test
    void f06_alignementDesSemainesAEtBRecompense() {
        var g = groupe(1);
        var m = matiere(1);
        var memeCreneau = creneau(1, JourPlan.LUNDI, 0, 8, true);
        var s1 = seance("s1", g, m, 2, memeCreneau, salle(1, 30), enseignant(9, 1));
        var s2 = seance("s2", g, m, 2, memeCreneau, salle(1, 30), enseignant(9, 1));
        s1.setSemaine(SemainePlan.A);
        s2.setSemaine(SemainePlan.B);

        // Paire A/B alignée jour + index : récompensée.
        verifier.verifyThat(ReglesEmploiDuTemps::alignementQuinzaine)
                .given(s1, s2)
                .rewardsWith(1);

        // Index différents : pas de récompense.
        s2.setCreneau(creneau(2, JourPlan.LUNDI, 2, 6, true));
        verifier.verifyThat(ReglesEmploiDuTemps::alignementQuinzaine)
                .given(s1, s2)
                .rewardsWith(0);
    }
}
