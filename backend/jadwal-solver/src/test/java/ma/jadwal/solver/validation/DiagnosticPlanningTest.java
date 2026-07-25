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
 * Diagnostic d'un planning complet (I-06) : l'analyse de score native de Timefold étant une
 * fonctionnalité commerciale, ces comptages remplacent {@code SolutionManager.analyze}.
 */
class DiagnosticPlanningTest {

    private final DiagnosticPlanning diagnostic = new DiagnosticPlanning();

    @Test
    void planningValideNeRemonteAucuneViolation() {
        var a = seance("s1", groupe(1), matiere(1), 2,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        var b = seance("s2", groupe(2), matiere(1), 2,
                creneau(2, JourPlan.LUNDI, 2, 6, true), salle(2, 30), enseignant(10, 1));

        assertThat(diagnostic.analyser(List.of(a, b))).isEmpty();
    }

    @Test
    void comptabiliseConflitsEnseignantGroupeEtSalle() {
        var prof = enseignant(9, 1);
        var salleCommune = salle(1, 30);
        // Deux séances du MÊME groupe, MÊME enseignant, MÊME salle, qui se chevauchent :
        // une violation de B-01, une de B-02 et une de B-03.
        var a = seance("s1", groupe(1), matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salleCommune, prof);
        var b = seance("s2", groupe(1), matiere(1), 4,
                creneau(2, JourPlan.LUNDI, 2, 6, true), salleCommune, prof);

        List<DiagnosticPlanning.ViolationRegle> violations = diagnostic.analyser(List.of(a, b));

        assertThat(violations).extracting(DiagnosticPlanning.ViolationRegle::regle)
                .contains("B-01", "B-02", "B-03");
        assertThat(violations).allSatisfy(v -> {
            assertThat(v.libelle()).isNotBlank();
            assertThat(v.nombreViolations()).isPositive();
        });
    }

    @Test
    void comptabiliseHierarchieCapaciteEtDebordement() {
        // Sous-groupe et groupe parent en même temps -> B-02.
        // Salle de capacité 10 pour un groupe de 30 -> E-01.
        // Séance de 4 unités sur un créneau n'en offrant que 2 -> G-01.
        var existante = seance("s1", sousGroupe(11, 1), matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), enseignant(9, 1));
        var fautive = seance("s2", groupe(1), matiere(1), 4,
                creneau(2, JourPlan.LUNDI, 2, 2, true), salle(2, 10), enseignant(9, 1));

        List<DiagnosticPlanning.ViolationRegle> violations = diagnostic.analyser(List.of(existante, fautive));

        assertThat(violations).extracting(DiagnosticPlanning.ViolationRegle::regle)
                .contains("B-02", "E-01", "G-01");
    }

    @Test
    void trieLesReglesDeLaPlusVioleeALaMoinsViolee() {
        var prof = enseignant(9, 1);
        // Trois séances du même groupe qui se chevauchent deux à deux : 3 paires -> B-02 = 3.
        // Chacune dans une salle distincte, avec des enseignants distincts : B-01/B-03 non déclenchés.
        SeancePlan a = seance("s1", groupe(1), matiere(1), 4,
                creneau(1, JourPlan.LUNDI, 0, 8, true), salle(1, 30), prof);
        SeancePlan b = seance("s2", groupe(1), matiere(1), 4,
                creneau(2, JourPlan.LUNDI, 1, 7, true), salle(2, 30), enseignant(10, 1));
        SeancePlan c = seance("s3", groupe(1), matiere(1), 4,
                creneau(3, JourPlan.LUNDI, 2, 6, true), salle(3, 30), enseignant(11, 1));

        List<DiagnosticPlanning.ViolationRegle> violations = diagnostic.analyser(List.of(a, b, c));

        assertThat(violations).isNotEmpty();
        assertThat(violations.get(0).nombreViolations())
                .isGreaterThanOrEqualTo(violations.get(violations.size() - 1).nombreViolations());
        var b02 = violations.stream().filter(v -> v.regle().equals("B-02")).findFirst().orElseThrow();
        assertThat(b02.nombreViolations()).isEqualTo(3);
    }
}
