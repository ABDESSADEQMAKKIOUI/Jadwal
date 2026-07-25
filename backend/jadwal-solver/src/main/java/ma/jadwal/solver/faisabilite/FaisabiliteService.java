package ma.jadwal.solver.faisabilite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ma.jadwal.solver.generation.BesoinSeances;
import ma.jadwal.solver.modele.EnseignantPlan;
import ma.jadwal.solver.modele.GroupePlan;
import ma.jadwal.solver.modele.SallePlan;

/**
 * Bilan de faisabilité avant calcul (H-01..H-06).
 * <p>
 * Chaque échec produit un message chiffré actionnable en français (H-06), en unités de 30 min
 * ET en heures, ex : « Il manque 12 unités (6h00) d'enseignant pour SVT ».
 * <p>
 * Interprétation des contrôles :
 * <ul>
 *   <li>H-01 (C-08) : Σ volumes de chaque groupe &lt;= 95% des unités COURS de la grille ;</li>
 *   <li>H-02 : besoin d'encadrement par matière (volume normal + volume dédoublé × nbSousGroupes) ;
 *       ÉCHEC si une matière a un besoin &gt; 0 sans aucun enseignant habilité ;</li>
 *   <li>H-03 : besoin(matière) &lt;= Σ quotas des enseignants habilités ;</li>
 *   <li>H-04 : Σ volumes exigeant un type de salle &lt;= nbSalles(type) × unités COURS de la grille ;</li>
 *   <li>H-05 : par enseignant MIXTE, unités COURS libres après indispos ETAT (buffer inclus)
 *       &gt;= quota école attendu (approximation).</li>
 * </ul>
 * Remarque C-07 : la multiplication par le nombre de co-enseignants doit être appliquée par le
 * mapper en amont (le contrat {@link BesoinSeances} ne porte pas ce champ).
 */
public class FaisabiliteService {

    public FaisabiliteRapport verifier(DonneesFaisabilite donnees) {
        List<BilanFaisabilite> bilans = new ArrayList<>();
        bilans.add(verifierGrilleParGroupe(donnees));
        bilans.add(verifierCouvertureMatieres(donnees));
        bilans.add(verifierQuotasParMatiere(donnees));
        bilans.add(verifierSallesParType(donnees));
        bilans.add(verifierEnseignantsMixtes(donnees));

        String global = BilanFaisabilite.STATUT_OK;
        for (BilanFaisabilite bilan : bilans) {
            if (BilanFaisabilite.STATUT_ECHEC.equals(bilan.statut())) {
                global = BilanFaisabilite.STATUT_ECHEC;
                break;
            }
            if (BilanFaisabilite.STATUT_AVERTISSEMENT.equals(bilan.statut())) {
                global = BilanFaisabilite.STATUT_AVERTISSEMENT;
            }
        }
        return new FaisabiliteRapport(global, bilans);
    }

    // ------------------------------------------------------------------
    // H-01 / C-08 : bilan grille par groupe
    // ------------------------------------------------------------------

    private BilanFaisabilite verifierGrilleParGroupe(DonneesFaisabilite donnees) {
        int plafond = donnees.unitesCoursParSemaine() * 95 / 100;
        Map<Long, Integer> chargeParGroupe = new LinkedHashMap<>();
        Map<Long, GroupePlan> groupes = new LinkedHashMap<>();
        for (BesoinSeances besoin : donnees.besoins()) {
            int volume = volumeHebdoPire(besoin);
            chargeParGroupe.merge(besoin.groupe().id(), volume, Integer::sum);
            groupes.putIfAbsent(besoin.groupe().id(), besoin.groupe());
        }

        List<String> echecs = new ArrayList<>();
        for (Map.Entry<Long, Integer> entree : chargeParGroupe.entrySet()) {
            int charge = entree.getValue();
            if (charge > plafond) {
                GroupePlan groupe = groupes.get(entree.getKey());
                int exces = charge - plafond;
                echecs.add("La classe " + groupe.libelle() + " totalise " + charge + " unités ("
                        + enHeures(charge) + ") alors que la grille n'en offre que " + plafond
                        + " (95% de " + donnees.unitesCoursParSemaine() + ") : retirez au moins "
                        + exces + " unités (" + enHeures(exces) + ") de sa maquette.");
            }
        }
        if (echecs.isEmpty()) {
            return new BilanFaisabilite("H-01", "Capacité de la grille par groupe", BilanFaisabilite.STATUT_OK,
                    "Tous les groupes tiennent dans " + plafond + " unités (" + enHeures(plafond)
                            + "), soit 95% de la grille.", "");
        }
        return new BilanFaisabilite("H-01", "Capacité de la grille par groupe", BilanFaisabilite.STATUT_ECHEC,
                echecs.get(0), String.join("\n", echecs));
    }

    // ------------------------------------------------------------------
    // H-02 : besoin par matière + couverture minimale
    // ------------------------------------------------------------------

    private BilanFaisabilite verifierCouvertureMatieres(DonneesFaisabilite donnees) {
        Map<Long, Integer> besoinParMatiere = besoinParMatiere(donnees);
        List<String> lignes = new ArrayList<>();
        List<String> echecs = new ArrayList<>();
        for (Map.Entry<Long, Integer> entree : besoinParMatiere.entrySet()) {
            long matiereId = entree.getKey();
            int besoin = entree.getValue();
            String nom = nomMatiere(donnees, matiereId);
            long nbHabilites = donnees.enseignants().stream()
                    .filter(e -> e.getHabilitations().containsKey(matiereId))
                    .count();
            lignes.add(nom + " : besoin de " + besoin + " unités (" + enHeures(besoin) + "), "
                    + nbHabilites + " enseignant(s) habilité(s).");
            if (besoin > 0 && nbHabilites == 0) {
                echecs.add("Aucun enseignant habilité pour " + nom + " alors que le besoin est de "
                        + besoin + " unités (" + enHeures(besoin)
                        + ") : habilitez ou recrutez au moins un enseignant pour cette matière.");
            }
        }
        if (echecs.isEmpty()) {
            return new BilanFaisabilite("H-02", "Besoin d'encadrement par matière", BilanFaisabilite.STATUT_OK,
                    "Chaque matière demandée a au moins un enseignant habilité.", String.join("\n", lignes));
        }
        return new BilanFaisabilite("H-02", "Besoin d'encadrement par matière", BilanFaisabilite.STATUT_ECHEC,
                echecs.get(0), String.join("\n", echecs));
    }

    // ------------------------------------------------------------------
    // H-03 : besoin(matière) <= somme des quotas des habilités
    // ------------------------------------------------------------------

    private BilanFaisabilite verifierQuotasParMatiere(DonneesFaisabilite donnees) {
        Map<Long, Integer> besoinParMatiere = besoinParMatiere(donnees);
        List<String> echecs = new ArrayList<>();
        for (Map.Entry<Long, Integer> entree : besoinParMatiere.entrySet()) {
            long matiereId = entree.getKey();
            int besoin = entree.getValue();
            int capacite = donnees.enseignants().stream()
                    .filter(e -> e.getHabilitations().containsKey(matiereId))
                    .mapToInt(EnseignantPlan::getQuotaHebdoUnites)
                    .sum();
            if (besoin > capacite) {
                int manque = besoin - capacite;
                echecs.add("Il manque " + manque + " unités (" + enHeures(manque) + ") d'enseignant pour "
                        + nomMatiere(donnees, matiereId) + " : besoin de " + besoin + " unités ("
                        + enHeures(besoin) + ") pour " + capacite + " unités (" + enHeures(capacite)
                        + ") de quota disponibles. Augmentez les quotas ou habilitez d'autres enseignants.");
            }
        }
        if (echecs.isEmpty()) {
            return new BilanFaisabilite("H-03", "Quotas des enseignants par matière", BilanFaisabilite.STATUT_OK,
                    "Les quotas des enseignants habilités couvrent le besoin de chaque matière.", "");
        }
        return new BilanFaisabilite("H-03", "Quotas des enseignants par matière", BilanFaisabilite.STATUT_ECHEC,
                echecs.get(0), String.join("\n", echecs));
    }

    // ------------------------------------------------------------------
    // H-04 : capacité des salles par type requis
    // ------------------------------------------------------------------

    private BilanFaisabilite verifierSallesParType(DonneesFaisabilite donnees) {
        Map<String, Integer> besoinParType = new LinkedHashMap<>();
        for (BesoinSeances besoin : donnees.besoins()) {
            String type = besoin.matiere().typeSalleRequis();
            if (type != null) {
                besoinParType.merge(type, occupationSalles(besoin), Integer::sum);
            }
        }
        List<String> echecs = new ArrayList<>();
        for (Map.Entry<String, Integer> entree : besoinParType.entrySet()) {
            String type = entree.getKey();
            int besoin = entree.getValue();
            long nbSalles = donnees.salles().stream().filter(s -> type.equals(s.type())).count();
            long capacite = nbSalles * donnees.unitesCoursParSemaine();
            if (besoin > capacite) {
                long manque = besoin - capacite;
                long sallesManquantes = donnees.unitesCoursParSemaine() > 0
                        ? (manque + donnees.unitesCoursParSemaine() - 1) / donnees.unitesCoursParSemaine()
                        : 1;
                echecs.add("Il manque " + manque + " unités (" + enHeures(manque) + ") de salle de type "
                        + type + " : besoin de " + besoin + " unités (" + enHeures(besoin) + ") pour "
                        + nbSalles + " salle(s) de ce type. Ajoutez au moins " + sallesManquantes
                        + " salle(s) de type " + type + " ou réduisez les volumes concernés.");
            }
        }
        if (echecs.isEmpty()) {
            return new BilanFaisabilite("H-04", "Capacité des salles par type", BilanFaisabilite.STATUT_OK,
                    "Les salles disponibles couvrent tous les volumes exigeant un type de salle.", "");
        }
        return new BilanFaisabilite("H-04", "Capacité des salles par type", BilanFaisabilite.STATUT_ECHEC,
                echecs.get(0), String.join("\n", echecs));
    }

    // ------------------------------------------------------------------
    // H-05 : enseignants MIXTES — unités libres après indispos ETAT
    // ------------------------------------------------------------------

    private BilanFaisabilite verifierEnseignantsMixtes(DonneesFaisabilite donnees) {
        List<String> echecs = new ArrayList<>();
        for (Map.Entry<Long, Integer> entree : donnees.unitesCoursParSemaineParEnseignantMixte().entrySet()) {
            long enseignantId = entree.getKey();
            int libres = entree.getValue();
            EnseignantPlan enseignant = donnees.enseignants().stream()
                    .filter(e -> e.getId() == enseignantId)
                    .findFirst()
                    .orElse(null);
            int attendu = donnees.quotasEcoleMixtes().getOrDefault(enseignantId,
                    enseignant != null ? enseignant.getQuotaHebdoUnites() : 0);
            if (attendu > libres) {
                int manque = attendu - libres;
                String nom = enseignant != null ? enseignant.getNom() : ("enseignant n°" + enseignantId);
                echecs.add("L'enseignant " + nom + " (MIXTE) ne dispose que de " + libres + " unités ("
                        + enHeures(libres) + ") libres après ses cours à l'école publique, pour "
                        + attendu + " unités (" + enHeures(attendu) + ") attendues : réduisez son quota école de "
                        + manque + " unités (" + enHeures(manque) + ") ou répartissez sur d'autres enseignants.");
            }
        }
        if (echecs.isEmpty()) {
            return new BilanFaisabilite("H-05", "Disponibilité des enseignants mixtes", BilanFaisabilite.STATUT_OK,
                    "Chaque enseignant mixte dispose d'assez d'unités libres après ses indisponibilités État.", "");
        }
        return new BilanFaisabilite("H-05", "Disponibilité des enseignants mixtes", BilanFaisabilite.STATUT_ECHEC,
                echecs.get(0), String.join("\n", echecs));
    }

    // ------------------------------------------------------------------
    // Calculs partagés
    // ------------------------------------------------------------------

    /** Volume hebdomadaire « pire semaine » d'un besoin (quinzaine C-05 : max des semaines A et B). */
    private static int volumeHebdoPire(BesoinSeances besoin) {
        return besoin.volumeUnitesB() == null
                ? besoin.volumeUnites()
                : Math.max(besoin.volumeUnites(), besoin.volumeUnitesB());
    }

    /**
     * H-02 : unités d'encadrement nécessaires pour un besoin :
     * AUCUN -&gt; volume ; TOTAL -&gt; volume × nbSousGroupes ;
     * PARTIEL -&gt; moitié classe entière + moitié × nbSousGroupes.
     */
    private static int occupationEncadrement(BesoinSeances besoin) {
        int volume = volumeHebdoPire(besoin);
        int nbSousGroupes = besoin.sousGroupes().size();
        return switch (besoin.dedoublement()) {
            case "TOTAL" -> nbSousGroupes > 0 ? volume * nbSousGroupes : volume;
            case "PARTIEL" -> nbSousGroupes > 0
                    ? (volume + 1) / 2 + (volume / 2) * nbSousGroupes
                    : volume;
            default -> volume;
        };
    }

    /** H-04 : occupation de salles = même formule que l'encadrement (les sous-groupes sont simultanés). */
    private static int occupationSalles(BesoinSeances besoin) {
        return occupationEncadrement(besoin);
    }

    private static Map<Long, Integer> besoinParMatiere(DonneesFaisabilite donnees) {
        Map<Long, Integer> besoinParMatiere = new LinkedHashMap<>();
        for (BesoinSeances besoin : donnees.besoins()) {
            besoinParMatiere.merge(besoin.matiere().id(), occupationEncadrement(besoin), Integer::sum);
        }
        return besoinParMatiere;
    }

    private String nomMatiere(DonneesFaisabilite donnees, long matiereId) {
        String nom = donnees.nomsMatieres().get(matiereId);
        if (nom != null) {
            return nom;
        }
        return donnees.besoins().stream()
                .filter(b -> b.matiere().id() == matiereId)
                .map(b -> b.matiere().libelle())
                .findFirst()
                .orElse("matière n°" + matiereId);
    }

    /** Convertit des unités de 30 min en heures lisibles : 12 -&gt; "6h00", 5 -&gt; "2h30". */
    static String enHeures(long unites) {
        long heures = unites / 2;
        return heures + "h" + (unites % 2 == 0 ? "00" : "30");
    }
}
