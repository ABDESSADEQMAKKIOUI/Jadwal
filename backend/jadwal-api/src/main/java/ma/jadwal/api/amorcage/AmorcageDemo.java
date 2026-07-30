package ma.jadwal.api.amorcage;

import ma.jadwal.abonnement.entite.Abonnement;
import ma.jadwal.abonnement.entite.ModePaiement;
import ma.jadwal.abonnement.entite.PlanAbonnement;
import ma.jadwal.abonnement.service.AbonnementService;
import ma.jadwal.abonnement.service.PaiementService;
import ma.jadwal.abonnement.service.PlanService;
import ma.jadwal.enseignant.entite.Enseignant;
import ma.jadwal.enseignant.entite.Indisponibilite;
import ma.jadwal.enseignant.entite.Semaine;
import ma.jadwal.enseignant.entite.SourceIndispo;
import ma.jadwal.enseignant.entite.StatutIndispo;
import ma.jadwal.enseignant.entite.TypeEnseignant;
import ma.jadwal.enseignant.service.EnseignantService;
import ma.jadwal.pedagogie.entite.Dedoublement;
import ma.jadwal.pedagogie.service.AffectationService;
import ma.jadwal.pedagogie.service.MaquetteService;
import ma.jadwal.referentiel.depot.EtablissementRepository;
import ma.jadwal.referentiel.entite.Etablissement;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.referentiel.entite.Jour;
import ma.jadwal.referentiel.entite.Matiere;
import ma.jadwal.referentiel.entite.Niveau;
import ma.jadwal.referentiel.entite.TypeGroupe;
import ma.jadwal.referentiel.entite.Utilisateur;
import ma.jadwal.referentiel.service.BarretteService;
import ma.jadwal.referentiel.service.EtablissementService;
import ma.jadwal.referentiel.service.GrilleConfig;
import ma.jadwal.referentiel.service.GrilleService;
import ma.jadwal.referentiel.service.GroupeService;
import ma.jadwal.referentiel.service.MatiereService;
import ma.jadwal.referentiel.service.NiveauService;
import ma.jadwal.referentiel.service.SalleService;
import ma.jadwal.referentiel.service.UtilisateurService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Jeu de démonstration complet et FAISABLE (activé par jadwal.demo=true) :
 * « Collège Atlas » (code DEMO, Casablanca), compte directeur demo@jadwal.ma / demo123,
 * abonnement ACTIF, grille Lun-Ven 16 unités (déjeuner 8-11), 2 niveaux × 2 classes,
 * sous-groupes dédoublés (Informatique en TOTAL), 10 matières, 12 salles, 14 enseignants
 * couvrant tous les besoins (dont 2 MIXTES avec indisponibilités ETAT validées, D-05/H-05)
 * et une barrette d'anglais sur les 2AC (B-05).
 */
@Component
public class AmorcageDemo {

    private static final Logger journal = LoggerFactory.getLogger(AmorcageDemo.class);
    private static final String CODE_DEMO = "DEMO";

    private final EtablissementRepository etablissementRepository;
    private final EtablissementService etablissementService;
    private final UtilisateurService utilisateurService;
    private final PlanService planService;
    private final AbonnementService abonnementService;
    private final PaiementService paiementService;
    private final GrilleService grilleService;
    private final NiveauService niveauService;
    private final GroupeService groupeService;
    private final MatiereService matiereService;
    private final SalleService salleService;
    private final BarretteService barretteService;
    private final EnseignantService enseignantService;
    private final MaquetteService maquetteService;
    private final AffectationService affectationService;

    public AmorcageDemo(EtablissementRepository etablissementRepository,
                        EtablissementService etablissementService,
                        UtilisateurService utilisateurService,
                        PlanService planService,
                        AbonnementService abonnementService,
                        PaiementService paiementService,
                        GrilleService grilleService,
                        NiveauService niveauService,
                        GroupeService groupeService,
                        MatiereService matiereService,
                        SalleService salleService,
                        BarretteService barretteService,
                        EnseignantService enseignantService,
                        MaquetteService maquetteService,
                        AffectationService affectationService) {
        this.etablissementRepository = etablissementRepository;
        this.etablissementService = etablissementService;
        this.utilisateurService = utilisateurService;
        this.planService = planService;
        this.abonnementService = abonnementService;
        this.paiementService = paiementService;
        this.grilleService = grilleService;
        this.niveauService = niveauService;
        this.groupeService = groupeService;
        this.matiereService = matiereService;
        this.salleService = salleService;
        this.barretteService = barretteService;
        this.enseignantService = enseignantService;
        this.maquetteService = maquetteService;
        this.affectationService = affectationService;
    }

    @Transactional
    public void amorcer() {
        if (etablissementRepository.existsByCode(CODE_DEMO)) {
            return;
        }
        journal.info("Amorçage du jeu de démonstration (Collège Atlas)…");

        Etablissement etablissement = etablissementService.creer("Collège Atlas", CODE_DEMO, "Casablanca",
                "0522-45-45-45", "contact@college-atlas.ma");
        Long id = etablissement.getId();
        Utilisateur directeur = utilisateurService.creerDirecteur(etablissement, "demo@jadwal.ma",
                "Directrice Démo", "demo123");
        creerAbonnementActif(id, directeur.getId());

        // A-07 : Lun-Ven, 08:00-17:00, 18 unités de 30 min, déjeuner de l'unité 8 à 11 (12:00-14:00).
        // Restent 14 unités de cours par jour, soit 70 par semaine. Les maquettes en demandent 56 :
        // 80% d'occupation, la marge de 5 à 10% exigée par C-08/H-01 est donc largement respectée.
        grilleService.enregistrerGrille(id, new GrilleConfig(
                List.of("LUNDI", "MARDI", "MERCREDI", "JEUDI", "VENDREDI"), "08:00", 30, 18,
                List.of(new GrilleConfig.PlageBloquee("DEJEUNER", 8, 4))));

        Niveau ac1 = niveauService.creer(id, "1AC", "Collège", 1, null);
        Niveau ac2 = niveauService.creer(id, "2AC", "Collège", 2, null);

        Groupe ac1a = creerClasseDedoublee(id, ac1, "1AC-A", 32);
        Groupe ac1b = creerClasseDedoublee(id, ac1, "1AC-B", 32);
        Groupe ac2a = creerClasseDedoublee(id, ac2, "2AC-A", 34);
        Groupe ac2b = creerClasseDedoublee(id, ac2, "2AC-B", 34);

        Map<String, Matiere> matieres = creerMatieres(id);
        creerSalles(id);
        creerMaquettes(id, List.of(ac1.getId(), ac2.getId()), matieres);
        Map<String, Enseignant> profs = creerEnseignants(id, matieres);

        barretteService.creer(id, "Barrette Anglais 2AC", matieres.get("EN").getId(),
                List.of(ac2a.getId(), ac2b.getId()));

        creerAffectations(id, matieres, profs, List.of(ac1a, ac1b, ac2a, ac2b));

        journal.info("Jeu de démonstration créé : connectez-vous avec demo@jadwal.ma / demo123");
    }

    // ------------------------------------------------------------------
    // Abonnement
    // ------------------------------------------------------------------

    /**
     * PREMIUM, et non ESSENTIEL : c'est le seul plan qui inclut le module
     * VIE_SCOLAIRE. Le jeu de démonstration doit permettre d'explorer les élèves
     * et les absences, sinon ces écrans répondent 403.
     */
    private void creerAbonnementActif(Long etablissementId, Long directeurId) {
        PlanAbonnement plan = planService.listerTout().stream()
                .filter(p -> "PREMIUM".equals(p.getCode()))
                .findFirst()
                .orElse(null);
        if (plan == null) {
            return;
        }
        Abonnement abonnement = abonnementService.creer(etablissementId, plan.getId(),
                LocalDate.now(), LocalDate.now().plusYears(1));
        // Paiement intégral confirmé : l'abonnement passe ACTIF.
        paiementService.creer(abonnement.getId(), plan.getPrixAnnuel(), ModePaiement.VIREMENT,
                "DEMO-0001", LocalDate.now(), "Paiement de démonstration", directeurId);
    }

    // ------------------------------------------------------------------
    // Groupes (A-04 : 2 sous-groupes par classe, effectif réparti)
    // ------------------------------------------------------------------

    private Groupe creerClasseDedoublee(Long etablissementId, Niveau niveau, String libelle, int effectif) {
        Groupe classe = groupeService.creer(etablissementId, niveau.getId(), libelle, effectif, null,
                TypeGroupe.CLASSE);
        int premier = (effectif + 1) / 2;
        groupeService.creer(etablissementId, niveau.getId(), libelle + " (G1)", premier, classe.getId(),
                TypeGroupe.SOUS_GROUPE);
        groupeService.creer(etablissementId, niveau.getId(), libelle + " (G2)", effectif - premier,
                classe.getId(), TypeGroupe.SOUS_GROUPE);
        return classe;
    }

    // ------------------------------------------------------------------
    // Matières
    // ------------------------------------------------------------------

    /**
     * Couleurs de matières : palette catégorielle accordée au design system Ynexis
     * (teintes mates de même clarté que le teal de marque #47a398), au lieu des
     * couleurs vives par défaut. Chaque établissement reste libre de les changer.
     */
    private Map<String, Matiere> creerMatieres(Long etablissementId) {
        Map<String, Matiere> matieres = new LinkedHashMap<>();
        matieres.put("MATH", matiere(etablissementId, "MATH", "Mathématiques", 5, 5, "#3a8a80",
                null, 1, 4, false, false));
        matieres.put("FR", matiere(etablissementId, "FR", "Français", 4, 4, "#c14e44",
                null, 1, 4, false, false));
        matieres.put("AR", matiere(etablissementId, "AR", "Arabe", 4, 4, "#2a6fdb",
                null, 1, 4, false, false));
        matieres.put("EN", matiere(etablissementId, "EN", "Anglais", 3, 3, "#d98a16",
                null, 1, 4, false, false));
        matieres.put("SVT", matiere(etablissementId, "SVT", "Sciences de la Vie et de la Terre", 3, 4,
                "#1f9d57", "LABO_SVT", 1, 4, false, false));
        matieres.put("PC", matiere(etablissementId, "PC", "Physique-Chimie", 3, 4, "#0f7d8c",
                "LABO_PC", 1, 4, false, false));
        matieres.put("HG", matiere(etablissementId, "HG", "Histoire-Géographie", 2, 3, "#7a5bb3",
                null, 1, 4, false, false));
        matieres.put("INFO", matiere(etablissementId, "INFO", "Informatique", 1, 2, "#6b7877",
                "INFO", 1, 4, false, false));
        matieres.put("EPS", matiere(etablissementId, "EPS", "Éducation Physique et Sportive", 1, 1,
                "#b8502f", "GYMNASE", 4, 4, true, false));
        matieres.put("EI", matiere(etablissementId, "EI", "Éducation Islamique", 2, 2, "#8a7a1f",
                null, 1, 4, false, false));
        return matieres;
    }

    private Matiere matiere(Long etablissementId, String code, String libelle, int coefficient,
                            int poidsCognitif, String couleur, String typeSalleRequis, int dureeMin,
                            int dureeMax, boolean eviterAvantDejeuner, boolean eviterFinJournee) {
        Matiere donnees = new Matiere();
        donnees.setCode(code);
        donnees.setLibelle(libelle);
        donnees.setCoefficient(coefficient);
        donnees.setPoidsCognitif(poidsCognitif);
        donnees.setCouleur(couleur);
        donnees.setTypeSalleRequis(typeSalleRequis);
        donnees.setDureeMinUnites(dureeMin);
        donnees.setDureeMaxUnites(dureeMax);
        donnees.setEviterAvantDejeuner(eviterAvantDejeuner);
        donnees.setEviterFinJournee(eviterFinJournee);
        return matiereService.creer(etablissementId, donnees);
    }

    // ------------------------------------------------------------------
    // Salles
    // ------------------------------------------------------------------

    private void creerSalles(Long etablissementId) {
        for (int i = 1; i <= 6; i++) {
            salleService.creer(etablissementId, "A-" + i, 36, "STANDARD", null, "A");
        }
        salleService.creer(etablissementId, "LABO-SVT", 36, "LABO_SVT", "paillasses", "A");
        salleService.creer(etablissementId, "LABO-PC", 36, "LABO_PC", "paillasses", "A");
        // Deux salles INFO : le dédoublement TOTAL aligne les deux sous-groupes en simultané (B-04).
        salleService.creer(etablissementId, "SALLE-INFO-1", 20, "INFO", "postes", "A");
        salleService.creer(etablissementId, "SALLE-INFO-2", 20, "INFO", "postes", "A");
        salleService.creer(etablissementId, "GYMNASE", 40, "GYMNASE", null, "B");
    }

    // ------------------------------------------------------------------
    // Maquettes (56 unités par classe <= 57 = 95% de 60, C-08/H-01)
    // ------------------------------------------------------------------

    private void creerMaquettes(Long etablissementId, List<Long> niveauIds, Map<String, Matiere> matieres) {
        for (Long niveauId : niveauIds) {
            List<MaquetteService.LigneMaquette> lignes = new ArrayList<>();
            lignes.add(ligne(matieres, "MATH", 10, "[[2,2,2,2,2]]", Dedoublement.AUCUN));
            lignes.add(ligne(matieres, "FR", 10, "[[2,2,2,2,2]]", Dedoublement.AUCUN));
            lignes.add(ligne(matieres, "AR", 10, "[[2,2,2,2,2]]", Dedoublement.AUCUN));
            lignes.add(ligne(matieres, "EN", 6, "[[2,2,2]]", Dedoublement.AUCUN));
            lignes.add(ligne(matieres, "SVT", 4, "[[2,2]]", Dedoublement.AUCUN));
            lignes.add(ligne(matieres, "PC", 4, "[[2,2]]", Dedoublement.AUCUN));
            lignes.add(ligne(matieres, "HG", 4, "[[2,2]]", Dedoublement.AUCUN));
            lignes.add(ligne(matieres, "INFO", 2, "[[2]]", Dedoublement.TOTAL));
            lignes.add(ligne(matieres, "EPS", 4, "[[4]]", Dedoublement.AUCUN));
            lignes.add(ligne(matieres, "EI", 2, "[[2]]", Dedoublement.AUCUN));
            maquetteService.remplacerMaquetteNiveau(etablissementId, niveauId, lignes);
        }
    }

    private MaquetteService.LigneMaquette ligne(Map<String, Matiere> matieres, String code, int volume,
                                                String patternsJson, Dedoublement dedoublement) {
        return new MaquetteService.LigneMaquette(matieres.get(code).getId(), volume, null, 4,
                dedoublement, 2, 1, patternsJson);
    }

    // ------------------------------------------------------------------
    // Enseignants (quotas couvrant les besoins, H-02/H-03)
    // ------------------------------------------------------------------

    private Map<String, Enseignant> creerEnseignants(Long etablissementId, Map<String, Matiere> matieres) {
        Map<String, Enseignant> profs = new LinkedHashMap<>();
        // MATH : besoin 40u -> 2 × 24 (dont 1 MIXTE).
        Enseignant ahmed = enseignant(etablissementId, "Ahmed Bennis", TypeEnseignant.MIXTE, 24, false,
                matieres, "MATH");
        bloquerMatinsEtat(etablissementId, ahmed);
        profs.put("MATH1", ahmed);
        profs.put("MATH2", enseignant(etablissementId, "Salma Idrissi", TypeEnseignant.PROPRE, 24, false,
                matieres, "MATH"));
        // FR : besoin 40u -> 2 × 24 (dont 1 MIXTE).
        Enseignant karim = enseignant(etablissementId, "Karim Alaoui", TypeEnseignant.MIXTE, 24, false,
                matieres, "FR");
        bloquerMatinsEtat(etablissementId, karim);
        profs.put("FR1", karim);
        profs.put("FR2", enseignant(etablissementId, "Nadia Berrada", TypeEnseignant.PROPRE, 24, false,
                matieres, "FR"));
        // AR : besoin 40u -> 2 × 24.
        profs.put("AR1", enseignant(etablissementId, "Yassine Tazi", TypeEnseignant.PROPRE, 24, false,
                matieres, "AR"));
        profs.put("AR2", enseignant(etablissementId, "Fatima El Fassi", TypeEnseignant.PROPRE, 24, false,
                matieres, "AR"));
        // EN : besoin 24u -> 1 × 28.
        profs.put("EN1", enseignant(etablissementId, "Omar Chraibi", TypeEnseignant.PROPRE, 28, false,
                matieres, "EN"));
        // SVT 16u + PC 16u -> 2 bivalents × 20.
        profs.put("SP1", enseignant(etablissementId, "Laila Bensouda", TypeEnseignant.PROPRE, 20, false,
                matieres, "SVT", "PC"));
        profs.put("SP2", enseignant(etablissementId, "Hicham Amrani", TypeEnseignant.PROPRE, 20, false,
                matieres, "SVT", "PC"));
        // HG : 16u -> 1 × 20.
        profs.put("HG1", enseignant(etablissementId, "Khadija Lahlou", TypeEnseignant.PROPRE, 20, false,
                matieres, "HG"));
        // INFO : 16u alignées par paires de sous-groupes -> 1 × 20.
        profs.put("INFO1", enseignant(etablissementId, "Mehdi Berrougui", TypeEnseignant.PROPRE, 20, false,
                matieres, "INFO"));
        // EPS : 16u -> 1 × 20.
        profs.put("EPS1", enseignant(etablissementId, "Rachid Ouazzani", TypeEnseignant.PROPRE, 20, false,
                matieres, "EPS"));
        // EI : 8u -> 1 × 12.
        profs.put("EI1", enseignant(etablissementId, "Zineb Kettani", TypeEnseignant.PROPRE, 12, false,
                matieres, "EI"));
        // Renfort vacataire EN + INFO : indispensable pour la barrette 2AC (2 groupes simultanés,
        // B-05/B-01) et les paires INFO alignées (B-04/B-01).
        profs.put("EN2", enseignant(etablissementId, "Imane Saidi", TypeEnseignant.PROPRE, 16, true,
                matieres, "EN", "INFO"));
        return profs;
    }

    /**
     * Répartition de service (C-06) : dans un établissement réel, la direction fixe QUI enseigne
     * QUOI à CHAQUE classe avant de construire l'emploi du temps. Ces affectations imposent la
     * variable « enseignant » et réduisent massivement l'espace de recherche du solveur.
     * <p>
     * L'Informatique est volontairement laissée libre : chaque classe dédoublée exige deux
     * enseignants simultanés (B-04), le solveur répartit lui-même Mehdi et Imane.
     */
    private void creerAffectations(Long etablissementId, Map<String, Matiere> matieres,
                                   Map<String, Enseignant> profs, List<Groupe> classes) {
        // Chaque matière : le code de l'enseignant retenu pour chacune des 4 classes, dans l'ordre.
        Map<String, List<String>> repartition = new LinkedHashMap<>();
        repartition.put("MATH", List.of("MATH1", "MATH1", "MATH2", "MATH2"));
        repartition.put("FR", List.of("FR1", "FR1", "FR2", "FR2"));
        repartition.put("AR", List.of("AR1", "AR1", "AR2", "AR2"));
        // Barrette 2AC : 2AC-A et 2AC-B ont cours en même temps -> deux professeurs distincts.
        repartition.put("EN", List.of("EN1", "EN1", "EN1", "EN2"));
        repartition.put("SVT", List.of("SP1", "SP1", "SP2", "SP2"));
        repartition.put("PC", List.of("SP2", "SP2", "SP1", "SP1"));
        repartition.put("HG", List.of("HG1", "HG1", "HG1", "HG1"));
        repartition.put("EPS", List.of("EPS1", "EPS1", "EPS1", "EPS1"));
        repartition.put("EI", List.of("EI1", "EI1", "EI1", "EI1"));

        for (Map.Entry<String, List<String>> entree : repartition.entrySet()) {
            Long matiereId = matieres.get(entree.getKey()).getId();
            for (int i = 0; i < classes.size(); i++) {
                affectationService.creer(etablissementId, classes.get(i).getId(), matiereId,
                        profs.get(entree.getValue().get(i)).getId(), null);
            }
        }
    }

    private Enseignant enseignant(Long etablissementId, String nom, TypeEnseignant type, int quota,
                                  boolean vacataire, Map<String, Matiere> matieres, String... codes) {
        Enseignant enseignant = enseignantService.creer(etablissementId, nom,
                nom.toLowerCase().replace(' ', '.') + "@college-atlas.ma", type, quota, 10, null,
                vacataire, 1);
        List<EnseignantService.HabilitationDemande> habilitations = new ArrayList<>();
        for (String code : codes) {
            habilitations.add(new EnseignantService.HabilitationDemande(matieres.get(code).getId(), List.of()));
        }
        enseignantService.remplacerHabilitations(etablissementId, enseignant.getId(), habilitations);
        return enseignant;
    }

    /** Indisponibilités ETAT validées : lundi et mardi matin (unités 0-7) — D-05/D-06/H-05. */
    private void bloquerMatinsEtat(Long etablissementId, Enseignant enseignant) {
        for (Jour jour : List.of(Jour.LUNDI, Jour.MARDI)) {
            Indisponibilite indispo = enseignantService.creerIndisponibilite(etablissementId,
                    enseignant.getId(), SourceIndispo.ETAT, jour, 0, 8, Semaine.TOUTES,
                    "Cours à l'école publique");
            enseignantService.changerStatutIndisponibilite(etablissementId, enseignant.getId(),
                    indispo.getId(), StatutIndispo.VALIDE);
        }
    }
}
