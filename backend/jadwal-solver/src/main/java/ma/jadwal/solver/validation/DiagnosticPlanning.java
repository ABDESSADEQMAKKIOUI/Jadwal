package ma.jadwal.solver.validation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import ma.jadwal.solver.contrainte.CalculsPlanning;
import ma.jadwal.solver.modele.EnseignantPlan;
import ma.jadwal.solver.modele.IndispoPlan;
import ma.jadwal.solver.modele.SeancePlan;

/**
 * Diagnostic d'un emploi du temps complet : compte les violations de CHAQUE règle dure du cahier
 * et produit un message actionnable en français (I-06).
 * <p>
 * Ce service est volontairement indépendant du solveur : l'analyse de score native de Timefold
 * ({@code SolutionManager.analyze}) est une fonctionnalité commerciale indisponible en édition
 * Community. Le diagnostic est ici recalculé en Java pur, à partir des mêmes prédicats que les
 * contraintes ({@link CalculsPlanning}), ce qui garantit la cohérence avec le score du solveur.
 */
public class DiagnosticPlanning {

    /** Une règle dure et le nombre de fois où le planning la viole. */
    public record ViolationRegle(String regle, String libelle, long nombreViolations) {
    }

    private static final Map<String, String> LIBELLES = new LinkedHashMap<>();

    static {
        LIBELLES.put("B-01", "Un enseignant assure deux séances simultanées");
        LIBELLES.put("B-02", "Un groupe suit deux séances simultanées");
        LIBELLES.put("B-03", "Une salle accueille deux groupes simultanément");
        LIBELLES.put("B-04", "Les sous-groupes dédoublés ne sont pas alignés");
        LIBELLES.put("B-05", "Les groupes d'une barrette ne sont pas alignés");
        LIBELLES.put("C-06", "L'enseignant affecté n'est pas celui prévu par la répartition de service");
        LIBELLES.put("F-01", "Plafond journalier dépassé pour une matière");
        LIBELLES.put("F-07", "Deux séances de la même matière sont accolées sans bloc autorisé");
        LIBELLES.put("D-01", "Quota horaire d'un enseignant dépassé");
        LIBELLES.put("D-02", "Enseignant non habilité pour la matière ou le niveau");
        LIBELLES.put("D-03", "Séance posée sur une indisponibilité de l'enseignant");
        LIBELLES.put("D-07", "Amplitude ou heures consécutives d'un enseignant dépassées");
        LIBELLES.put("E-01", "Capacité de la salle insuffisante pour l'effectif");
        LIBELLES.put("E-02", "Type de salle inadapté à la matière");
        LIBELLES.put("E-03", "Équipements requis absents de la salle");
        LIBELLES.put("G-01", "Séance débordant de sa plage de cours (pause ou fin de journée)");
        LIBELLES.put("G-02", "Amplitude journalière d'un groupe dépassée");
        LIBELLES.put("G-03", "Trop d'heures consécutives sans pause pour un groupe");
    }

    /**
     * Analyse le planning et renvoie une entrée par règle dure violée, la plus violée d'abord.
     * Un planning valide renvoie une liste vide.
     */
    public List<ViolationRegle> analyser(List<SeancePlan> seances) {
        Map<String, Long> compteurs = new LinkedHashMap<>();
        for (String regle : LIBELLES.keySet()) {
            compteurs.put(regle, 0L);
        }

        comptabiliserPaires(seances, compteurs);
        comptabiliserUnitaires(seances, compteurs);
        comptabiliserAgregats(seances, compteurs);

        List<ViolationRegle> violations = new ArrayList<>();
        for (Map.Entry<String, Long> entree : compteurs.entrySet()) {
            if (entree.getValue() > 0) {
                violations.add(new ViolationRegle(entree.getKey(), LIBELLES.get(entree.getKey()),
                        entree.getValue()));
            }
        }
        violations.sort((a, b) -> Long.compare(b.nombreViolations(), a.nombreViolations()));
        return violations;
    }

    // ------------------------------------------------------------------
    // Règles portant sur des paires de séances (B-01..B-05, F-07)
    // ------------------------------------------------------------------

    private void comptabiliserPaires(List<SeancePlan> seances, Map<String, Long> compteurs) {
        for (int i = 0; i < seances.size(); i++) {
            SeancePlan a = seances.get(i);
            for (int j = i + 1; j < seances.size(); j++) {
                SeancePlan b = seances.get(j);

                if (CalculsPlanning.seChevauchent(a, b)) {
                    if (memeEnseignant(a, b)) {
                        incrementer(compteurs, "B-01");
                    }
                    if (memeSalle(a, b)) {
                        incrementer(compteurs, "B-03");
                    }
                    if (memeGroupeOuHierarchie(a, b)) {
                        incrementer(compteurs, "B-02");
                    }
                }
                if (memeCle(a.getBlocAlignementId(), b.getBlocAlignementId()) && !simultanees(a, b)) {
                    incrementer(compteurs, "B-04");
                }
                if (a.getBarretteId() != null && a.getBarretteId().equals(b.getBarretteId())
                        && !simultanees(a, b)) {
                    incrementer(compteurs, "B-05");
                }
                if (F07Applicable(a, b)) {
                    incrementer(compteurs, "F-07");
                }
            }
        }
    }

    private boolean F07Applicable(SeancePlan a, SeancePlan b) {
        if (a.getCreneau() == null || b.getCreneau() == null) {
            return false;
        }
        return a.getGroupe().id() == b.getGroupe().id()
                && a.getMatiere().id() == b.getMatiere().id()
                && a.getCreneau().jour() == b.getCreneau().jour()
                && a.getSemaine().chevauche(b.getSemaine())
                && a.getMatiere().dureeMaxUnites() < 4
                && CalculsPlanning.adjacentesOuChevauchantes(a, b);
    }

    // ------------------------------------------------------------------
    // Règles portant sur une séance isolée (C-06, D-02, D-03, E-01..E-03, G-01)
    // ------------------------------------------------------------------

    private void comptabiliserUnitaires(List<SeancePlan> seances, Map<String, Long> compteurs) {
        for (SeancePlan s : seances) {
            EnseignantPlan prof = s.getEnseignant();
            if (prof != null) {
                if (s.getAffectationEnseignantId() != null
                        && prof.getId() != s.getAffectationEnseignantId()) {
                    incrementer(compteurs, "C-06");
                }
                Set<Long> niveaux = prof.getHabilitations().get(s.getMatiere().id());
                if (niveaux == null || (!niveaux.isEmpty() && !niveaux.contains(s.getGroupe().niveauId()))) {
                    incrementer(compteurs, "D-02");
                }
                for (IndispoPlan indispo : prof.getIndisponibilites()) {
                    if (CalculsPlanning.conflitIndispo(s, indispo)) {
                        incrementer(compteurs, "D-03");
                        break;
                    }
                }
            }
            if (s.getSalle() != null) {
                if (s.getSalle().capacite() < s.getGroupe().effectif()) {
                    incrementer(compteurs, "E-01");
                }
                String typeRequis = s.getMatiere().typeSalleRequis();
                if (typeRequis != null && !typeRequis.equals(s.getSalle().type())) {
                    incrementer(compteurs, "E-02");
                }
                if (!s.getSalle().equipements().containsAll(s.getMatiere().equipementsRequis())) {
                    incrementer(compteurs, "E-03");
                }
            }
            if (s.getCreneau() != null && s.getDureeUnites() > s.getCreneau().unitesDisponibles()) {
                incrementer(compteurs, "G-01");
            }
        }
    }

    // ------------------------------------------------------------------
    // Règles d'agrégat (D-01, D-07, F-01, G-02, G-03)
    // ------------------------------------------------------------------

    private void comptabiliserAgregats(List<SeancePlan> seances, Map<String, Long> compteurs) {
        List<SeancePlan> placees = seances.stream().filter(s -> s.getCreneau() != null).toList();

        // D-01 : quota hebdomadaire par enseignant.
        Map<Long, List<SeancePlan>> parEnseignant = placees.stream()
                .filter(s -> s.getEnseignant() != null)
                .collect(Collectors.groupingBy(s -> s.getEnseignant().getId()));
        for (List<SeancePlan> lot : parEnseignant.values()) {
            EnseignantPlan prof = lot.get(0).getEnseignant();
            if (CalculsPlanning.charge(lot) > prof.getQuotaHebdoUnites()) {
                incrementer(compteurs, "D-01");
            }
        }

        // D-07 : heures consécutives et amplitude, par enseignant et par jour.
        for (List<SeancePlan> lot : parEnseignant.values()) {
            EnseignantPlan prof = lot.get(0).getEnseignant();
            for (List<SeancePlan> journee : parJour(lot).values()) {
                if (CalculsPlanning.excesConsecutif(journee, prof.getMaxConsecutifUnites()) > 0
                        || CalculsPlanning.excesAmplitude(journee, prof.getAmplitudeMaxUnites()) > 0) {
                    incrementer(compteurs, "D-07");
                }
            }
        }

        // F-01 : plafond journalier par couple (groupe, MATIÈRE) et par jour.
        Map<String, List<SeancePlan>> parGroupeMatiereJour = placees.stream()
                .collect(Collectors.groupingBy(s -> s.getGroupe().id() + "|" + s.getMatiere().id()
                        + "|" + s.getCreneau().jour()));
        for (List<SeancePlan> lot : parGroupeMatiereJour.values()) {
            if (CalculsPlanning.excesMaxParJour(lot) > 0) {
                incrementer(compteurs, "F-01");
            }
        }

        // G-02, G-03 : agrégats journaliers par groupe (toutes matières confondues).
        Map<Long, List<SeancePlan>> parGroupe = placees.stream()
                .collect(Collectors.groupingBy(s -> s.getGroupe().id()));
        for (List<SeancePlan> lot : parGroupe.values()) {
            SeancePlan reference = lot.get(0);
            for (List<SeancePlan> journee : parJour(lot).values()) {
                if (CalculsPlanning.excesAmplitude(journee, reference.getAmplitudeMaxUnitesGroupe()) > 0) {
                    incrementer(compteurs, "G-02");
                }
                if (CalculsPlanning.excesConsecutif(journee, 8) > 0) {
                    incrementer(compteurs, "G-03");
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Utilitaires
    // ------------------------------------------------------------------

    private Map<Object, List<SeancePlan>> parJour(List<SeancePlan> seances) {
        return seances.stream().collect(Collectors.groupingBy(
                (Function<SeancePlan, Object>) s -> s.getCreneau().jour()));
    }

    private boolean memeEnseignant(SeancePlan a, SeancePlan b) {
        return a.getEnseignant() != null && b.getEnseignant() != null
                && a.getEnseignant().getId() == b.getEnseignant().getId();
    }

    private boolean memeSalle(SeancePlan a, SeancePlan b) {
        return a.getSalle() != null && b.getSalle() != null && a.getSalle().id() == b.getSalle().id();
    }

    private boolean memeGroupeOuHierarchie(SeancePlan a, SeancePlan b) {
        long ga = a.getGroupe().id();
        long gb = b.getGroupe().id();
        Long pa = a.getGroupe().parentId();
        Long pb = b.getGroupe().parentId();
        return ga == gb || (pa != null && pa == gb) || (pb != null && pb == ga);
    }

    private boolean memeCle(String a, String b) {
        return a != null && a.equals(b);
    }

    private boolean simultanees(SeancePlan a, SeancePlan b) {
        if (a.getCreneau() == null || b.getCreneau() == null) {
            return false;
        }
        return a.getCreneau().jour() == b.getCreneau().jour()
                && a.getCreneau().indexDebut() == b.getCreneau().indexDebut();
    }

    private void incrementer(Map<String, Long> compteurs, String regle) {
        compteurs.merge(regle, 1L, Long::sum);
    }
}
