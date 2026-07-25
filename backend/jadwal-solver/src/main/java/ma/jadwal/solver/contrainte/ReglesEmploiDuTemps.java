package ma.jadwal.solver.contrainte;

import java.util.List;

import ai.timefold.solver.core.api.score.BendableScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;

import ma.jadwal.solver.modele.EnseignantPlan;
import ma.jadwal.solver.modele.SeancePlan;
import ma.jadwal.solver.modele.SemainePlan;

/**
 * Toutes les contraintes du cahier des règles JADWAL, sur un {@link BendableScore}(3 hard, 3 soft).
 * <p>
 * Répartition des niveaux :
 * <ul>
 *   <li>hard0 (unicité) : B-01, B-02, B-03, B-04, B-05</li>
 *   <li>hard1 (volumes/structure) : C-06, F-01, F-07</li>
 *   <li>hard2 (ressources) : D-01, D-02, D-03, D-07, E-01, E-02, E-03, G-01, G-02, G-03</li>
 *   <li>soft0 (groupes) : G-04 (poids défaut 10), G-05, G-06</li>
 *   <li>soft1 (pédagogie) : F-02, F-03, F-04, F-05, F-06</li>
 *   <li>soft2 (confort enseignants) : D-08, D-09, D-10, E-04</li>
 * </ul>
 * Les noms de contraintes sont exactement les codes du cahier des règles ; ce sont les clés
 * attendues par {@code ConstraintWeightOverrides} (I-01).
 * <p>
 * I-02 (relaxation automatique) n'est volontairement PAS implémentée dans ce lot.
 */
public class ReglesEmploiDuTemps implements ConstraintProvider {

    private static final BendableScore UNICITE = BendableScore.ofHard(3, 3, 0, 1);
    private static final BendableScore STRUCTURE = BendableScore.ofHard(3, 3, 1, 1);
    private static final BendableScore RESSOURCE = BendableScore.ofHard(3, 3, 2, 1);
    private static final BendableScore GROUPE = BendableScore.ofSoft(3, 3, 0, 1);
    private static final BendableScore GROUPE_FORT = BendableScore.ofSoft(3, 3, 0, 10);
    private static final BendableScore PEDAGOGIE = BendableScore.ofSoft(3, 3, 1, 1);
    private static final BendableScore CONFORT = BendableScore.ofSoft(3, 3, 2, 1);

    @Override
    public Constraint[] defineConstraints(ConstraintFactory f) {
        return new Constraint[] {
                // hard0 — unicité
                conflitEnseignant(f),
                conflitGroupe(f),
                conflitSalle(f),
                alignementBloc(f),
                alignementBarrette(f),
                // hard1 — volumes / structure
                enseignantImpose(f),
                maxParJour(f),
                seancesAdjacentesInterdites(f),
                // hard2 — ressources
                quotaEnseignant(f),
                habilitationEnseignant(f),
                indisponibiliteEnseignant(f),
                rythmeEnseignant(f),
                capaciteSalle(f),
                typeSalle(f),
                equipementsSalle(f),
                debordementPlage(f),
                amplitudeGroupe(f),
                suiteContigueGroupe(f),
                // soft0 — groupes
                trousGroupes(f),
                positionMatiere(f),
                chargeMaxNiveau(f),
                // soft1 — pédagogie
                espacementSeances(f),
                equilibreChargeJournaliere(f),
                poidsCognitifDemiJournee(f),
                matiereForteEnDebutDeJournee(f),
                alignementQuinzaine(f),
                // soft2 — confort enseignants
                joursPresenceVacataires(f),
                trousEnseignants(f),
                preferencesEnseignant(f),
                changementsBatimentGroupe(f),
        };
    }

    // ------------------------------------------------------------------
    // hard0 — unicité
    // ------------------------------------------------------------------

    /** B-01 : un enseignant ne peut assurer deux séances qui se chevauchent. */
    public Constraint conflitEnseignant(ConstraintFactory f) {
        return f.forEachUniquePair(SeancePlan.class,
                        Joiners.equal((SeancePlan s) -> s.getEnseignant().getId()),
                        Joiners.overlapping(SeancePlan::axeDebut, SeancePlan::axeFin))
                .filter((a, b) -> a.getSemaine().chevauche(b.getSemaine()))
                .penalize(UNICITE)
                .asConstraint("B-01");
    }

    /**
     * B-02 : un groupe ne peut avoir deux séances qui se chevauchent, EN INCLUANT la hiérarchie :
     * une séance d'un sous-groupe chevauche une séance de son groupe parent (et réciproquement).
     * Deux sous-groupes frères peuvent, eux, être simultanés (c'est le principe du dédoublement A-04).
     */
    public Constraint conflitGroupe(ConstraintFactory f) {
        return f.forEachUniquePair(SeancePlan.class,
                        Joiners.overlapping(SeancePlan::axeDebut, SeancePlan::axeFin))
                .filter((a, b) -> a.getGroupe().estLieA(b.getGroupe())
                        && a.getSemaine().chevauche(b.getSemaine()))
                .penalize(UNICITE)
                .asConstraint("B-02");
    }

    /** B-03 : une salle ne peut accueillir deux séances qui se chevauchent. */
    public Constraint conflitSalle(ConstraintFactory f) {
        return f.forEachUniquePair(SeancePlan.class,
                        Joiners.equal((SeancePlan s) -> s.getSalle().id()),
                        Joiners.overlapping(SeancePlan::axeDebut, SeancePlan::axeFin))
                .filter((a, b) -> a.getSemaine().chevauche(b.getSemaine()))
                .penalize(UNICITE)
                .asConstraint("B-03");
    }

    /** B-04 : les séances d'un même bloc de dédoublement doivent partager jour et index de début. */
    public Constraint alignementBloc(ConstraintFactory f) {
        return f.forEachUniquePair(SeancePlan.class,
                        Joiners.equal((SeancePlan s) -> s.getBlocAlignementId() != null
                                ? "B:" + s.getBlocAlignementId()
                                : "N:" + s.getId()))
                .filter(ReglesEmploiDuTemps::debutsDifferents)
                .penalize(UNICITE)
                .asConstraint("B-04");
    }

    /** B-05 : les séances d'une même barrette doivent partager jour et index de début. */
    public Constraint alignementBarrette(ConstraintFactory f) {
        return f.forEachUniquePair(SeancePlan.class,
                        Joiners.equal((SeancePlan s) -> s.getBarretteId() != null
                                ? "B:" + s.getBarretteId()
                                : "N:" + s.getId()))
                .filter(ReglesEmploiDuTemps::debutsDifferents)
                .penalize(UNICITE)
                .asConstraint("B-05");
    }

    private static boolean debutsDifferents(SeancePlan a, SeancePlan b) {
        return a.getCreneau().jour() != b.getCreneau().jour()
                || a.getCreneau().indexDebut() != b.getCreneau().indexDebut();
    }

    // ------------------------------------------------------------------
    // hard1 — volumes / structure
    // ------------------------------------------------------------------

    /** C-06 : si une affectation (groupe, matière, enseignant) est imposée, l'enseignant doit être celui-là. */
    public Constraint enseignantImpose(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .filter(s -> s.getAffectationEnseignantId() != null
                        && s.getAffectationEnseignantId().longValue() != s.getEnseignant().getId())
                .penalize(STRUCTURE)
                .asConstraint("C-06");
    }

    /** F-01 : durées cumulées d'un couple (groupe, matière) sur un jour &lt;= maxParJourUnites. */
    public Constraint maxParJour(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .groupBy(s -> s.getGroupe().id() + "|" + s.getMatiere().id() + "|"
                                + s.getCreneau().jour() + "|" + s.getSemaine(),
                        ConstraintCollectors.toList())
                .filter((cle, seances) -> CalculsPlanning.excesMaxParJour(seances) > 0)
                .penalize(STRUCTURE, (cle, seances) -> CalculsPlanning.excesMaxParJour(seances))
                .asConstraint("F-01");
    }

    /**
     * F-07 : deux séances du même couple (groupe, matière) le même jour, adjacentes ou
     * chevauchantes, sont interdites sauf si la matière autorise des blocs longs
     * (dureeMaxUnites &gt;= 4, blocs de 2h).
     */
    public Constraint seancesAdjacentesInterdites(ConstraintFactory f) {
        return f.forEachUniquePair(SeancePlan.class,
                        Joiners.equal((SeancePlan s) -> s.getGroupe().id()),
                        Joiners.equal((SeancePlan s) -> s.getMatiere().id()),
                        Joiners.equal((SeancePlan s) -> s.getCreneau().jour()))
                .filter((a, b) -> a.getMatiere().dureeMaxUnites() < 4
                        && a.getSemaine().chevauche(b.getSemaine())
                        && CalculsPlanning.adjacentesOuChevauchantes(a, b))
                .penalize(STRUCTURE)
                .asConstraint("F-07");
    }

    // ------------------------------------------------------------------
    // hard2 — ressources
    // ------------------------------------------------------------------

    /** D-01 : la somme des durées des séances d'un enseignant ne dépasse pas son quota hebdomadaire. */
    public Constraint quotaEnseignant(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .groupBy(SeancePlan::getEnseignant, ConstraintCollectors.toList())
                .filter((enseignant, seances) ->
                        CalculsPlanning.charge(seances) > enseignant.getQuotaHebdoUnites())
                .penalize(RESSOURCE, (enseignant, seances) ->
                        CalculsPlanning.charge(seances) - enseignant.getQuotaHebdoUnites())
                .asConstraint("D-01");
    }

    /** D-02 : un enseignant n'assure que des matières habilitées (et des niveaux autorisés). */
    public Constraint habilitationEnseignant(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .filter(s -> !s.getEnseignant().estHabilite(s.getMatiere().id(), s.getGroupe().niveauId()))
                .penalize(RESSOURCE)
                .asConstraint("D-02");
    }

    /** D-03/D-06 : aucune séance sur une indisponibilité validée de l'enseignant. */
    public Constraint indisponibiliteEnseignant(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .filter(s -> s.getEnseignant().getIndisponibilites().stream()
                        .anyMatch(indispo -> CalculsPlanning.conflitIndispo(s, indispo)))
                .penalize(RESSOURCE)
                .asConstraint("D-03");
    }

    /**
     * D-07 : plus longue suite contiguë d'unités occupées d'un enseignant dans une journée
     * &lt;= maxConsecutifUnites, et amplitude journalière &lt;= amplitudeMaxUnites si définie.
     */
    public Constraint rythmeEnseignant(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .groupBy(SeancePlan::getEnseignant, s -> s.getCreneau().jour(),
                        ConstraintCollectors.toList())
                .filter((enseignant, jour, seances) -> excesRythme(enseignant, seances) > 0)
                .penalize(RESSOURCE, (enseignant, jour, seances) -> excesRythme(enseignant, seances))
                .asConstraint("D-07");
    }

    private static long excesRythme(EnseignantPlan enseignant, List<SeancePlan> seances) {
        return CalculsPlanning.excesConsecutif(seances, enseignant.getMaxConsecutifUnites())
                + CalculsPlanning.excesAmplitude(seances, enseignant.getAmplitudeMaxUnites());
    }

    /** E-01 : la capacité de la salle couvre l'effectif du groupe. */
    public Constraint capaciteSalle(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .filter(s -> s.getSalle().capacite() < s.getGroupe().effectif())
                .penalize(RESSOURCE)
                .asConstraint("E-01");
    }

    /** E-02 : si la matière exige un type de salle, la salle doit être de ce type. */
    public Constraint typeSalle(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .filter(s -> s.getMatiere().typeSalleRequis() != null
                        && !s.getMatiere().typeSalleRequis().equals(s.getSalle().type()))
                .penalize(RESSOURCE)
                .asConstraint("E-02");
    }

    /** E-03 : les équipements requis par la matière sont tous présents dans la salle. */
    public Constraint equipementsSalle(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .filter(s -> !s.getSalle().equipements().containsAll(s.getMatiere().equipementsRequis()))
                .penalize(RESSOURCE)
                .asConstraint("E-03");
    }

    /**
     * G-01 : une séance ne déborde jamais de la plage COURS contiguë de son créneau de départ
     * (couvre la pause déjeuner bloquée et la fin de journée).
     */
    public Constraint debordementPlage(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .filter(s -> s.getDureeUnites() > s.getCreneau().unitesDisponibles())
                .penalize(RESSOURCE, s -> s.getDureeUnites() - s.getCreneau().unitesDisponibles())
                .asConstraint("G-01");
    }

    /** G-02 : amplitude journalière d'un groupe &lt;= amplitudeMaxUnitesGroupe (paramètre établissement, défaut 16). */
    public Constraint amplitudeGroupe(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .groupBy(SeancePlan::getGroupe, s -> s.getCreneau().jour(),
                        ConstraintCollectors.toList())
                .filter((groupe, jour, seances) -> CalculsPlanning.excesAmplitude(
                        seances, seances.get(0).getAmplitudeMaxUnitesGroupe()) > 0)
                .penalize(RESSOURCE, (groupe, jour, seances) -> CalculsPlanning.excesAmplitude(
                        seances, seances.get(0).getAmplitudeMaxUnitesGroupe()))
                .asConstraint("G-02");
    }

    /** G-03 : plus longue suite contiguë d'unités occupées d'un groupe &lt;= 8 (4h). */
    public Constraint suiteContigueGroupe(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .groupBy(SeancePlan::getGroupe, s -> s.getCreneau().jour(),
                        ConstraintCollectors.toList())
                .filter((groupe, jour, seances) -> CalculsPlanning.excesConsecutif(seances, 8) > 0)
                .penalize(RESSOURCE, (groupe, jour, seances) -> CalculsPlanning.excesConsecutif(seances, 8))
                .asConstraint("G-03");
    }

    // ------------------------------------------------------------------
    // soft0 — groupes
    // ------------------------------------------------------------------

    /** G-04 : trous des groupes fortement pénalisés (poids par défaut 10, paramétrable I-01). */
    public Constraint trousGroupes(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .groupBy(SeancePlan::getGroupe, s -> s.getCreneau().jour(),
                        ConstraintCollectors.toList())
                .filter((groupe, jour, seances) -> CalculsPlanning.trous(seances) > 0)
                .penalize(GROUPE_FORT, (groupe, jour, seances) -> CalculsPlanning.trous(seances))
                .asConstraint("G-04");
    }

    /**
     * G-05 : séance d'une matière eviterAvantDejeuner=true qui se termine juste avant la plage
     * DEJEUNER, ou eviterFinJournee=true qui termine la journée. Une séance se termine en fin de
     * plage exactement quand sa durée épuise les unités disponibles de son créneau de départ.
     */
    public Constraint positionMatiere(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .filter(s -> s.getDureeUnites() == s.getCreneau().unitesDisponibles()
                        && ((s.getMatiere().eviterAvantDejeuner() && s.getCreneau().matin())
                                || (s.getMatiere().eviterFinJournee() && !s.getCreneau().matin())))
                .penalize(GROUPE)
                .asConstraint("G-05");
    }

    /** G-06 : si le niveau définit une charge journalière maximale, pénaliser l'excédent. */
    public Constraint chargeMaxNiveau(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .groupBy(SeancePlan::getGroupe, s -> s.getCreneau().jour(),
                        ConstraintCollectors.toList())
                .filter((groupe, jour, seances) -> groupe.chargeMaxUnitesJour() != null
                        && CalculsPlanning.charge(seances) > groupe.chargeMaxUnitesJour())
                .penalize(GROUPE, (groupe, jour, seances) ->
                        CalculsPlanning.charge(seances) - groupe.chargeMaxUnitesJour())
                .asConstraint("G-06");
    }

    // ------------------------------------------------------------------
    // soft1 — pédagogie
    // ------------------------------------------------------------------

    /**
     * F-02 : deux séances du même couple (groupe, matière) sur des jours différents doivent être
     * espacées d'au moins gapMinJours (pré-calculé par la factory sur chaque séance).
     */
    public Constraint espacementSeances(ConstraintFactory f) {
        return f.forEachUniquePair(SeancePlan.class,
                        Joiners.equal((SeancePlan s) -> s.getGroupe().id()),
                        Joiners.equal((SeancePlan s) -> s.getMatiere().id()))
                .filter((a, b) -> a.getCreneau().jour() != b.getCreneau().jour()
                        && a.getSemaine().chevauche(b.getSemaine())
                        && ecartJours(a, b) < a.getGapMinJours())
                .penalize(PEDAGOGIE, (a, b) -> a.getGapMinJours() - ecartJours(a, b))
                .asConstraint("F-02");
    }

    private static int ecartJours(SeancePlan a, SeancePlan b) {
        return Math.abs(a.getCreneau().jour().ordreSemaine() - b.getCreneau().jour().ordreSemaine());
    }

    /**
     * F-03 : charge journalière homogène d'un groupe : pénaliser |charge(jour) - chargeMoyenne|
     * au-delà d'une tolérance de 2 unités.
     */
    public Constraint equilibreChargeJournaliere(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .groupBy(SeancePlan::getGroupe, s -> s.getCreneau().jour(),
                        ConstraintCollectors.toList())
                .filter((groupe, jour, seances) -> ecartChargeAuDelaTolerance(seances) > 0)
                .penalize(PEDAGOGIE, (groupe, jour, seances) -> ecartChargeAuDelaTolerance(seances))
                .asConstraint("F-03");
    }

    private static long ecartChargeAuDelaTolerance(List<SeancePlan> seances) {
        long ecart = Math.abs(CalculsPlanning.charge(seances) - seances.get(0).getChargeMoyenneUnites());
        return Math.max(0, ecart - 2);
    }

    /** F-04 : somme des poids cognitifs (pondérés par la durée) d'un groupe par demi-journée &lt;= seuil (12). */
    public Constraint poidsCognitifDemiJournee(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .groupBy(SeancePlan::getGroupe, s -> s.getCreneau().jour(),
                        s -> s.getCreneau().matin(), ConstraintCollectors.toList())
                .filter((groupe, jour, matin, seances) -> excesCognitif(seances) > 0)
                .penalize(PEDAGOGIE, (groupe, jour, matin, seances) -> excesCognitif(seances))
                .asConstraint("F-04");
    }

    private static long excesCognitif(List<SeancePlan> seances) {
        return Math.max(0,
                CalculsPlanning.chargeCognitive(seances) - seances.get(0).getSeuilCognitifDemiJournee());
    }

    /** F-05 : une matière à coefficient &gt;= 4 qui ne commence pas dans les 4 premières unités du jour. */
    public Constraint matiereForteEnDebutDeJournee(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .filter(s -> s.getMatiere().coefficient() >= 4 && s.getCreneau().indexDebut() >= 4)
                .penalize(PEDAGOGIE, SeancePlan::getDureeUnites)
                .asConstraint("F-05");
    }

    /** F-06 : en quinzaine, récompenser chaque paire (semaine A, semaine B) du même couple alignée jour + index. */
    public Constraint alignementQuinzaine(ConstraintFactory f) {
        return f.forEachUniquePair(SeancePlan.class,
                        Joiners.equal((SeancePlan s) -> s.getGroupe().id()),
                        Joiners.equal((SeancePlan s) -> s.getMatiere().id()),
                        Joiners.equal((SeancePlan s) -> s.getCreneau().jour()),
                        Joiners.equal((SeancePlan s) -> s.getCreneau().indexDebut()))
                .filter((a, b) -> (a.getSemaine() == SemainePlan.A && b.getSemaine() == SemainePlan.B)
                        || (a.getSemaine() == SemainePlan.B && b.getSemaine() == SemainePlan.A))
                .reward(PEDAGOGIE)
                .asConstraint("F-06");
    }

    // ------------------------------------------------------------------
    // soft2 — confort enseignants
    // ------------------------------------------------------------------

    /** D-08 : minimiser le nombre de jours de présence des vacataires. */
    public Constraint joursPresenceVacataires(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .filter(s -> s.getEnseignant().isVacataire())
                .groupBy(SeancePlan::getEnseignant,
                        ConstraintCollectors.countDistinct(s -> s.getCreneau().jour()))
                .filter((enseignant, nbJours) -> nbJours > 1)
                .penalize(CONFORT, (enseignant, nbJours) -> nbJours - 1)
                .asConstraint("D-08");
    }

    /** D-09 : minimiser les trous des enseignants. */
    public Constraint trousEnseignants(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .groupBy(SeancePlan::getEnseignant, s -> s.getCreneau().jour(),
                        ConstraintCollectors.toList())
                .filter((enseignant, jour, seances) -> CalculsPlanning.trous(seances) > 0)
                .penalize(CONFORT, (enseignant, jour, seances) -> CalculsPlanning.trous(seances))
                .asConstraint("D-09");
    }

    /**
     * D-10 : pénaliser chaque unité de séance sur une plage EVITER, récompenser chaque unité
     * sur une plage PREFERER (impact signé).
     */
    public Constraint preferencesEnseignant(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .filter(s -> CalculsPlanning.impactPreferences(s) != 0)
                .impact(CONFORT, CalculsPlanning::impactPreferences)
                .asConstraint("D-10");
    }

    /** E-04 : minimiser les changements de bâtiment d'un groupe dans la journée. */
    public Constraint changementsBatimentGroupe(ConstraintFactory f) {
        return f.forEach(SeancePlan.class)
                .groupBy(SeancePlan::getGroupe, s -> s.getCreneau().jour(),
                        ConstraintCollectors.toList())
                .filter((groupe, jour, seances) -> CalculsPlanning.changementsBatiment(seances) > 0)
                .penalize(CONFORT, (groupe, jour, seances) -> CalculsPlanning.changementsBatiment(seances))
                .asConstraint("E-04");
    }
}
