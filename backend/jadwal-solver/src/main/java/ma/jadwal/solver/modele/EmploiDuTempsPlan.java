package ma.jadwal.solver.modele;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.BendableScore;

/**
 * Solution de planification : l'emploi du temps complet d'un établissement.
 * <p>
 * Score {@link BendableScore} à 3 niveaux durs et 3 niveaux souples :
 * <ul>
 *   <li>hard0 : unicité (B-01..B-05)</li>
 *   <li>hard1 : volumes / structure (C-06, F-01, F-07)</li>
 *   <li>hard2 : ressources (D-01, D-02, D-03, D-07, E-01, E-02, E-03, G-01, G-02, G-03)</li>
 *   <li>soft0 : confort des groupes (G-04, G-05, G-06)</li>
 *   <li>soft1 : pédagogie (F-02, F-03, F-04, F-05, F-06)</li>
 *   <li>soft2 : confort des enseignants (D-08, D-09, D-10, E-04)</li>
 * </ul>
 * Les pondérations des contraintes souples sont paramétrables par établissement (I-01)
 * via {@link ConstraintWeightOverrides}.
 */
@PlanningSolution
public class EmploiDuTempsPlan {

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<CreneauPlan> creneaux = new ArrayList<>();

    // Salles et enseignants restent des faits du problème, mais le domaine de chaque variable est
    // porté par la séance elle-même (SeancePlan.sallesPossibles / enseignantsPossibles) : le
    // solveur n'explore que les valeurs réellement admissibles (E-01/E-02/E-03, C-06/D-02).
    @ProblemFactCollectionProperty
    private List<SallePlan> salles = new ArrayList<>();

    @ProblemFactCollectionProperty
    private List<EnseignantPlan> enseignants = new ArrayList<>();

    @PlanningEntityCollectionProperty
    private List<SeancePlan> seances = new ArrayList<>();

    /** I-01 : pondérations par établissement ; clés = codes de contraintes ("G-04", ...). */
    private ConstraintWeightOverrides<BendableScore> ponderations = ConstraintWeightOverrides.none();

    @PlanningScore(bendableHardLevelsSize = 3, bendableSoftLevelsSize = 3)
    private BendableScore score;

    // --- Paramètres simples de l'établissement (informatifs ; les contraintes lisent
    // les champs dénormalisés portés par chaque SeancePlan) ---
    private int amplitudeMaxUnitesGroupe = 16;
    private int nbJoursActifs = 5;
    private int seuilCognitifDemiJournee = 12;

    public EmploiDuTempsPlan() {
        // Requis par Timefold.
    }

    public EmploiDuTempsPlan(List<CreneauPlan> creneaux, List<SallePlan> salles,
            List<EnseignantPlan> enseignants, List<SeancePlan> seances) {
        this.creneaux = creneaux;
        this.salles = salles;
        this.enseignants = enseignants;
        this.seances = seances;
    }

    public List<CreneauPlan> getCreneaux() {
        return creneaux;
    }

    public void setCreneaux(List<CreneauPlan> creneaux) {
        this.creneaux = creneaux;
    }

    public List<SallePlan> getSalles() {
        return salles;
    }

    public void setSalles(List<SallePlan> salles) {
        this.salles = salles;
    }

    public List<EnseignantPlan> getEnseignants() {
        return enseignants;
    }

    public void setEnseignants(List<EnseignantPlan> enseignants) {
        this.enseignants = enseignants;
    }

    public List<SeancePlan> getSeances() {
        return seances;
    }

    public void setSeances(List<SeancePlan> seances) {
        this.seances = seances;
    }

    public ConstraintWeightOverrides<BendableScore> getPonderations() {
        return ponderations;
    }

    public void setPonderations(ConstraintWeightOverrides<BendableScore> ponderations) {
        this.ponderations = ponderations;
    }

    public BendableScore getScore() {
        return score;
    }

    public void setScore(BendableScore score) {
        this.score = score;
    }

    public int getAmplitudeMaxUnitesGroupe() {
        return amplitudeMaxUnitesGroupe;
    }

    public void setAmplitudeMaxUnitesGroupe(int amplitudeMaxUnitesGroupe) {
        this.amplitudeMaxUnitesGroupe = amplitudeMaxUnitesGroupe;
    }

    public int getNbJoursActifs() {
        return nbJoursActifs;
    }

    public void setNbJoursActifs(int nbJoursActifs) {
        this.nbJoursActifs = nbJoursActifs;
    }

    public int getSeuilCognitifDemiJournee() {
        return seuilCognitifDemiJournee;
    }

    public void setSeuilCognitifDemiJournee(int seuilCognitifDemiJournee) {
        this.seuilCognitifDemiJournee = seuilCognitifDemiJournee;
    }
}
