package ma.jadwal.solver.modele;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

/**
 * Séance planifiable : l'entité de planification du solveur.
 * <p>
 * Trois variables de planification : le créneau de départ, la salle et l'enseignant.
 * <p>
 * Champs DÉNORMALISÉS posés par la {@code SeanceFactory} (ou le mapper d'intégration), car les
 * contraintes ne peuvent pas lire la solution :
 * <ul>
 *   <li>{@code maxParJourUnites} (F-01)</li>
 *   <li>{@code amplitudeMaxUnitesGroupe} (G-02, défaut 16)</li>
 *   <li>{@code nbJoursActifs} (défaut 5)</li>
 *   <li>{@code seuilCognitifDemiJournee} (F-04, défaut 12)</li>
 *   <li>{@code gapMinJours} (F-02, défaut 1)</li>
 *   <li>{@code chargeMoyenneUnites} (F-03, défaut 0)</li>
 * </ul>
 */
@PlanningEntity
public class SeancePlan {

    @PlanningId
    private String id;
    /** Id de la séance en base, null tant qu'elle n'est pas persistée. */
    private Long idPersistant;
    private GroupePlan groupe;
    private MatierePlan matiere;
    private int dureeUnites;
    private SemainePlan semaine = SemainePlan.TOUTES;
    /** B-04 : les séances d'un même bloc de dédoublement doivent être simultanées. */
    private String blocAlignementId;
    /** B-05 : les séances d'une même barrette doivent être simultanées. */
    private Long barretteId;
    /** C-06 : enseignant imposé par une affectation (groupe, matière, enseignant). */
    private Long affectationEnseignantId;
    /** F-01 : plafond journalier du couple (groupe, matière) ; 0 = illimité. */
    private int maxParJourUnites;

    // --- Champs dénormalisés (paramètres établissement / pré-calculs factory) ---
    private int amplitudeMaxUnitesGroupe = 16;
    private int nbJoursActifs = 5;
    private int seuilCognitifDemiJournee = 12;
    private int gapMinJours = 1;
    private int chargeMoyenneUnites = 0;

    /** I-03 : séance verrouillée par le directeur, le solveur ne la déplace pas. */
    @PlanningPin
    private boolean verrouillee;

    /**
     * Salles admissibles pour CETTE séance (type de salle E-02, équipements E-03, capacité E-01).
     * Restreindre le domaine par entité au lieu de pénaliser après coup réduit massivement
     * l'espace de recherche : le solveur ne propose jamais une salle inadaptée.
     * Vide = toutes les salles de l'établissement (repli si aucune ne convient : la contrainte
     * dure correspondante signalera alors le problème plutôt que de bloquer la résolution).
     */
    @ValueRangeProvider(id = "sallesSeance")
    private List<SallePlan> sallesPossibles = new ArrayList<>();

    /**
     * Enseignants admissibles pour CETTE séance : l'enseignant imposé par l'affectation (C-06)
     * s'il existe, sinon les enseignants habilités pour la matière et le niveau (D-02).
     */
    @ValueRangeProvider(id = "enseignantsSeance")
    private List<EnseignantPlan> enseignantsPossibles = new ArrayList<>();

    @PlanningVariable
    private CreneauPlan creneau;
    @PlanningVariable(valueRangeProviderRefs = "sallesSeance")
    private SallePlan salle;
    @PlanningVariable(valueRangeProviderRefs = "enseignantsSeance")
    private EnseignantPlan enseignant;

    public List<SallePlan> getSallesPossibles() {
        return sallesPossibles;
    }

    public void setSallesPossibles(List<SallePlan> sallesPossibles) {
        this.sallesPossibles = sallesPossibles;
    }

    public List<EnseignantPlan> getEnseignantsPossibles() {
        return enseignantsPossibles;
    }

    public void setEnseignantsPossibles(List<EnseignantPlan> enseignantsPossibles) {
        this.enseignantsPossibles = enseignantsPossibles;
    }

    public SeancePlan() {
        // Requis par Timefold.
    }

    public SeancePlan(String id, Long idPersistant, GroupePlan groupe, MatierePlan matiere, int dureeUnites,
            SemainePlan semaine, String blocAlignementId, Long barretteId, Long affectationEnseignantId,
            int maxParJourUnites) {
        this.id = id;
        this.idPersistant = idPersistant;
        this.groupe = groupe;
        this.matiere = matiere;
        this.dureeUnites = dureeUnites;
        this.semaine = semaine == null ? SemainePlan.TOUTES : semaine;
        this.blocAlignementId = blocAlignementId;
        this.barretteId = barretteId;
        this.affectationEnseignantId = affectationEnseignantId;
        this.maxParJourUnites = maxParJourUnites;
    }

    // --- Méthodes utilitaires temporelles ---

    /** Fin exclusive de la séance (indexDebut + durée), 0 si le créneau n'est pas encore affecté. */
    public int finExclu() {
        return creneau == null ? 0 : creneau.indexDebut() + dureeUnites;
    }

    /** Début de la séance sur un axe temps absolu de la semaine (jour * 1000 + index). */
    public int axeDebut() {
        return creneau == null ? 0 : creneau.jour().ordreSemaine() * 1000 + creneau.indexDebut();
    }

    /** Fin exclusive de la séance sur l'axe temps absolu de la semaine. */
    public int axeFin() {
        return creneau == null ? 0 : axeDebut() + dureeUnites;
    }

    // --- Getters / setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getIdPersistant() {
        return idPersistant;
    }

    public void setIdPersistant(Long idPersistant) {
        this.idPersistant = idPersistant;
    }

    public GroupePlan getGroupe() {
        return groupe;
    }

    public void setGroupe(GroupePlan groupe) {
        this.groupe = groupe;
    }

    public MatierePlan getMatiere() {
        return matiere;
    }

    public void setMatiere(MatierePlan matiere) {
        this.matiere = matiere;
    }

    public int getDureeUnites() {
        return dureeUnites;
    }

    public void setDureeUnites(int dureeUnites) {
        this.dureeUnites = dureeUnites;
    }

    public SemainePlan getSemaine() {
        return semaine;
    }

    public void setSemaine(SemainePlan semaine) {
        this.semaine = semaine;
    }

    public String getBlocAlignementId() {
        return blocAlignementId;
    }

    public void setBlocAlignementId(String blocAlignementId) {
        this.blocAlignementId = blocAlignementId;
    }

    public Long getBarretteId() {
        return barretteId;
    }

    public void setBarretteId(Long barretteId) {
        this.barretteId = barretteId;
    }

    public Long getAffectationEnseignantId() {
        return affectationEnseignantId;
    }

    public void setAffectationEnseignantId(Long affectationEnseignantId) {
        this.affectationEnseignantId = affectationEnseignantId;
    }

    public int getMaxParJourUnites() {
        return maxParJourUnites;
    }

    public void setMaxParJourUnites(int maxParJourUnites) {
        this.maxParJourUnites = maxParJourUnites;
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

    public int getGapMinJours() {
        return gapMinJours;
    }

    public void setGapMinJours(int gapMinJours) {
        this.gapMinJours = gapMinJours;
    }

    public int getChargeMoyenneUnites() {
        return chargeMoyenneUnites;
    }

    public void setChargeMoyenneUnites(int chargeMoyenneUnites) {
        this.chargeMoyenneUnites = chargeMoyenneUnites;
    }

    public boolean isVerrouillee() {
        return verrouillee;
    }

    public void setVerrouillee(boolean verrouillee) {
        this.verrouillee = verrouillee;
    }

    public CreneauPlan getCreneau() {
        return creneau;
    }

    public void setCreneau(CreneauPlan creneau) {
        this.creneau = creneau;
    }

    public SallePlan getSalle() {
        return salle;
    }

    public void setSalle(SallePlan salle) {
        this.salle = salle;
    }

    public EnseignantPlan getEnseignant() {
        return enseignant;
    }

    public void setEnseignant(EnseignantPlan enseignant) {
        this.enseignant = enseignant;
    }

    @Override
    public String toString() {
        return "SeancePlan[" + id + "]";
    }
}
