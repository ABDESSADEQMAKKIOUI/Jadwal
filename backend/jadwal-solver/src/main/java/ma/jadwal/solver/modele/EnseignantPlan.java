package ma.jadwal.solver.modele;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enseignant planifiable.
 * <p>
 * Les habilitations (D-02) sont une carte matiereId -&gt; niveaux autorisés ;
 * un ensemble de niveaux VIDE signifie « tous les niveaux ».
 * Les indisponibilités (D-03) sont déjà étendues du buffer trajet pour les indispos ETAT (D-05).
 */
public final class EnseignantPlan {

    private final long id;
    private final String nom;
    private final int quotaHebdoUnites;
    private final int maxConsecutifUnites;
    private final Integer amplitudeMaxUnites;
    private final boolean vacataire;
    private final Map<Long, Set<Long>> habilitations;
    private final List<IndispoPlan> indisponibilites;
    private final List<PreferencePlan> preferences;

    public EnseignantPlan(long id, String nom, int quotaHebdoUnites, int maxConsecutifUnites,
            Integer amplitudeMaxUnites, boolean vacataire, Map<Long, Set<Long>> habilitations,
            List<IndispoPlan> indisponibilites, List<PreferencePlan> preferences) {
        this.id = id;
        this.nom = nom;
        this.quotaHebdoUnites = quotaHebdoUnites;
        this.maxConsecutifUnites = maxConsecutifUnites;
        this.amplitudeMaxUnites = amplitudeMaxUnites;
        this.vacataire = vacataire;
        this.habilitations = habilitations == null ? Map.of() : Map.copyOf(habilitations);
        this.indisponibilites = indisponibilites == null ? List.of() : List.copyOf(indisponibilites);
        this.preferences = preferences == null ? List.of() : List.copyOf(preferences);
    }

    public long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public int getQuotaHebdoUnites() {
        return quotaHebdoUnites;
    }

    public int getMaxConsecutifUnites() {
        return maxConsecutifUnites;
    }

    public Integer getAmplitudeMaxUnites() {
        return amplitudeMaxUnites;
    }

    public boolean isVacataire() {
        return vacataire;
    }

    public Map<Long, Set<Long>> getHabilitations() {
        return habilitations;
    }

    public List<IndispoPlan> getIndisponibilites() {
        return indisponibilites;
    }

    public List<PreferencePlan> getPreferences() {
        return preferences;
    }

    /**
     * D-02 : l'enseignant est habilité pour la matière, et si la liste de niveaux de
     * l'habilitation est non vide, le niveau du groupe doit y figurer.
     */
    public boolean estHabilite(long matiereId, long niveauId) {
        Set<Long> niveaux = habilitations.get(matiereId);
        return niveaux != null && (niveaux.isEmpty() || niveaux.contains(niveauId));
    }

    @Override
    public String toString() {
        return "EnseignantPlan[" + id + ", " + nom + "]";
    }
}
