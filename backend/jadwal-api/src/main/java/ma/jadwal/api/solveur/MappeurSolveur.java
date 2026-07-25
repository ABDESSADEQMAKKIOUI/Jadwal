package ma.jadwal.api.solveur;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.BendableScore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.enseignant.depot.EnseignantRepository;
import ma.jadwal.enseignant.depot.HabilitationRepository;
import ma.jadwal.enseignant.depot.IndisponibiliteRepository;
import ma.jadwal.enseignant.depot.PreferenceHoraireRepository;
import ma.jadwal.enseignant.entite.Enseignant;
import ma.jadwal.enseignant.entite.Habilitation;
import ma.jadwal.enseignant.entite.Indisponibilite;
import ma.jadwal.enseignant.entite.PreferenceHoraire;
import ma.jadwal.enseignant.entite.SourceIndispo;
import ma.jadwal.enseignant.entite.StatutIndispo;
import ma.jadwal.enseignant.entite.TypeEnseignant;
import ma.jadwal.enseignant.entite.TypePreference;
import ma.jadwal.pedagogie.depot.AffectationRepository;
import ma.jadwal.pedagogie.depot.MaquetteRepository;
import ma.jadwal.pedagogie.depot.VolumeOverrideRepository;
import ma.jadwal.pedagogie.entite.Affectation;
import ma.jadwal.pedagogie.entite.Dedoublement;
import ma.jadwal.pedagogie.entite.Maquette;
import ma.jadwal.pedagogie.entite.VolumeOverride;
import ma.jadwal.pedagogie.service.PonderationService;
import ma.jadwal.planning.depot.PlanningVersionRepository;
import ma.jadwal.planning.depot.SeanceRepository;
import ma.jadwal.planning.entite.PlanningVersion;
import ma.jadwal.planning.entite.Seance;
import ma.jadwal.referentiel.depot.EtablissementRepository;
import ma.jadwal.referentiel.depot.MatiereRepository;
import ma.jadwal.referentiel.depot.SalleRepository;
import ma.jadwal.referentiel.entite.Barrette;
import ma.jadwal.referentiel.depot.BarretteRepository;
import ma.jadwal.referentiel.depot.GroupeRepository;
import ma.jadwal.referentiel.entite.Creneau;
import ma.jadwal.referentiel.entite.Etablissement;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.referentiel.entite.Jour;
import ma.jadwal.referentiel.entite.Matiere;
import ma.jadwal.referentiel.entite.Niveau;
import ma.jadwal.referentiel.entite.Salle;
import ma.jadwal.referentiel.entite.TypeCreneau;
import ma.jadwal.referentiel.entite.TypeGroupe;
import ma.jadwal.referentiel.service.GrilleConfig;
import ma.jadwal.referentiel.service.GrilleService;
import ma.jadwal.solver.faisabilite.DonneesFaisabilite;
import ma.jadwal.solver.generation.BesoinSeances;
import ma.jadwal.solver.generation.SeanceFactory;
import ma.jadwal.solver.modele.CreneauPlan;
import ma.jadwal.solver.modele.EmploiDuTempsPlan;
import ma.jadwal.solver.modele.EnseignantPlan;
import ma.jadwal.solver.modele.GroupePlan;
import ma.jadwal.solver.modele.IndispoPlan;
import ma.jadwal.solver.modele.JourPlan;
import ma.jadwal.solver.modele.MatierePlan;
import ma.jadwal.solver.modele.PreferencePlan;
import ma.jadwal.solver.modele.SallePlan;
import ma.jadwal.solver.modele.SeancePlan;
import ma.jadwal.solver.modele.SemainePlan;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Construit le modèle de planification du solveur (module pur jadwal-solver) à partir
 * de la base de données, pour un établissement donné.
 * <ul>
 *   <li>Créneaux COURS -&gt; {@link CreneauPlan} (matin = avant la plage DEJEUNER, A-07) ;</li>
 *   <li>Enseignants avec habilitations, indisponibilités VALIDÉES (les indispos source ETAT
 *       sont étendues du bufferTrajetUnites avant/après — D-05) et préférences ;</li>
 *   <li>Besoins depuis maquettes × groupes (+ overrides A-03, dédoublement A-04, barrettes B-05,
 *       affectations imposées C-06 — plusieurs affectations = répartition au prorata des volumes) ;</li>
 *   <li>Réinjection des séances VERROUILLÉES de la version active (I-03, épinglées) ;</li>
 *   <li>Pondérations souples de l'établissement (I-01) en {@link ConstraintWeightOverrides}.</li>
 * </ul>
 * <p>
 * Encodage des barrettes : {@code SeancePlan.barretteId} porte {@code idBarrette * 1000 + rang}
 * pour que seules les séances de même rang s'alignent entre groupes (B-05) sans forcer toutes
 * les séances d'un même groupe à se superposer. À la persistance, l'id réel est
 * {@code barretteId / 1000}. Limitation : barrette combinée à la quinzaine ou au dédoublement
 * garde un rang unique (alignement global).
 */
@Component
public class MappeurSolveur {

    /** Niveau soft de chaque règle souple (I-01) : S0 groupes, S1 pédagogie, S2 enseignants. */
    private static final Map<String, Integer> NIVEAU_SOFT = Map.ofEntries(
            Map.entry("G-04", 0), Map.entry("G-05", 0), Map.entry("G-06", 0),
            Map.entry("F-02", 1), Map.entry("F-03", 1), Map.entry("F-04", 1),
            Map.entry("F-05", 1), Map.entry("F-06", 1),
            Map.entry("D-08", 2), Map.entry("D-09", 2), Map.entry("D-10", 2), Map.entry("E-04", 2));

    /** Seuil de charge cognitive par demi-journée (F-04). */
    private static final int SEUIL_COGNITIF_DEMI_JOURNEE = 12;

    private static final long MULTIPLICATEUR_BARRETTE = 1000L;

    private final GrilleService grilleService;
    private final EtablissementRepository etablissementRepository;
    private final SalleRepository salleRepository;
    private final MatiereRepository matiereRepository;
    private final GroupeRepository groupeRepository;
    private final BarretteRepository barretteRepository;
    private final EnseignantRepository enseignantRepository;
    private final HabilitationRepository habilitationRepository;
    private final IndisponibiliteRepository indisponibiliteRepository;
    private final PreferenceHoraireRepository preferenceHoraireRepository;
    private final MaquetteRepository maquetteRepository;
    private final VolumeOverrideRepository volumeOverrideRepository;
    private final AffectationRepository affectationRepository;
    private final PonderationService ponderationService;
    private final SeanceRepository seanceRepository;
    private final PlanningVersionRepository planningVersionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MappeurSolveur(GrilleService grilleService,
                          EtablissementRepository etablissementRepository,
                          SalleRepository salleRepository,
                          MatiereRepository matiereRepository,
                          GroupeRepository groupeRepository,
                          BarretteRepository barretteRepository,
                          EnseignantRepository enseignantRepository,
                          HabilitationRepository habilitationRepository,
                          IndisponibiliteRepository indisponibiliteRepository,
                          PreferenceHoraireRepository preferenceHoraireRepository,
                          MaquetteRepository maquetteRepository,
                          VolumeOverrideRepository volumeOverrideRepository,
                          AffectationRepository affectationRepository,
                          PonderationService ponderationService,
                          SeanceRepository seanceRepository,
                          PlanningVersionRepository planningVersionRepository) {
        this.grilleService = grilleService;
        this.etablissementRepository = etablissementRepository;
        this.salleRepository = salleRepository;
        this.matiereRepository = matiereRepository;
        this.groupeRepository = groupeRepository;
        this.barretteRepository = barretteRepository;
        this.enseignantRepository = enseignantRepository;
        this.habilitationRepository = habilitationRepository;
        this.indisponibiliteRepository = indisponibiliteRepository;
        this.preferenceHoraireRepository = preferenceHoraireRepository;
        this.maquetteRepository = maquetteRepository;
        this.volumeOverrideRepository = volumeOverrideRepository;
        this.affectationRepository = affectationRepository;
        this.ponderationService = ponderationService;
        this.seanceRepository = seanceRepository;
        this.planningVersionRepository = planningVersionRepository;
    }

    // ------------------------------------------------------------------
    // Construction du problème complet
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public EmploiDuTempsPlan construirePlan(Long etablissementId) {
        Etablissement etablissement = obtenirEtablissement(etablissementId);
        GrilleConfig grille = exigerGrille(etablissementId);

        List<CreneauPlan> creneaux = creneauxCours(etablissementId, grille);
        if (creneaux.isEmpty()) {
            throw new ConflitException("Aucun créneau de cours : enregistrez la grille horaire pour les générer.");
        }
        List<SallePlan> salles = salleRepository.findByEtablissementIdOrderByNomAsc(etablissementId).stream()
                .map(MappeurSolveur::versSallePlan)
                .toList();
        if (salles.isEmpty()) {
            throw new ConflitException("Aucune salle : ajoutez au moins une salle avant de générer.");
        }
        Map<Long, EnseignantPlan> enseignants = preparerEnseignants(etablissementId, grille.unitesParJour());
        if (enseignants.isEmpty()) {
            throw new ConflitException("Aucun enseignant : ajoutez des enseignants avant de générer.");
        }

        List<BesoinSeances> besoins = construireBesoins(etablissementId, false);
        if (besoins.isEmpty()) {
            throw new ConflitException("Aucun besoin de séance : renseignez les maquettes pédagogiques.");
        }
        int nbJoursActifs = grille.joursActifs().size();
        List<SeancePlan> seances = SeanceFactory.genererSeances(besoins, nbJoursActifs,
                etablissement.getAmplitudeMaxUnites(), SEUIL_COGNITIF_DEMI_JOURNEE);

        restreindreDomaines(seances, salles, enseignants);

        Map<Long, CreneauPlan> creneauxParId = creneaux.stream()
                .collect(Collectors.toMap(CreneauPlan::id, Function.identity()));
        Map<Long, SallePlan> sallesParId = salles.stream()
                .collect(Collectors.toMap(SallePlan::id, Function.identity()));
        epinglerSeancesVerrouillees(etablissementId, seances, creneauxParId, sallesParId, enseignants);

        EmploiDuTempsPlan plan = new EmploiDuTempsPlan(new ArrayList<>(creneaux), new ArrayList<>(salles),
                new ArrayList<>(enseignants.values()), seances);
        plan.setNbJoursActifs(nbJoursActifs);
        plan.setAmplitudeMaxUnitesGroupe(etablissement.getAmplitudeMaxUnites());
        plan.setSeuilCognitifDemiJournee(SEUIL_COGNITIF_DEMI_JOURNEE);
        plan.setPonderations(construirePonderations(etablissementId));
        return plan;
    }

    /**
     * Restreint le domaine de chaque séance aux valeurs réellement admissibles.
     * <p>
     * Plutôt que de laisser le solveur proposer n'importe quelle salle ou n'importe quel enseignant
     * puis de pénaliser après coup, on lui interdit d'emblée les valeurs invalides :
     * <ul>
     *   <li>salles : type requis (E-02), équipements requis (E-03), capacité suffisante (E-01) ;</li>
     *   <li>enseignants : l'affectation imposée si elle existe (C-06), sinon les habilités pour la
     *       matière et le niveau du groupe (D-02).</li>
     * </ul>
     * Si un filtre ne laisse aucune valeur (données incohérentes), on retombe sur l'ensemble complet :
     * la contrainte dure correspondante signalera alors le problème au lieu de bloquer la résolution.
     */
    private void restreindreDomaines(List<SeancePlan> seances, List<SallePlan> salles,
                                     Map<Long, EnseignantPlan> enseignants) {
        List<EnseignantPlan> tousEnseignants = new ArrayList<>(enseignants.values());
        for (SeancePlan seance : seances) {
            MatierePlan matiere = seance.getMatiere();
            int effectif = seance.getGroupe().effectif();

            List<SallePlan> sallesOk = salles.stream()
                    .filter(s -> matiere.typeSalleRequis() == null
                            || matiere.typeSalleRequis().equals(s.type()))
                    .filter(s -> s.equipements().containsAll(matiere.equipementsRequis()))
                    .filter(s -> s.capacite() >= effectif)
                    .toList();
            seance.setSallesPossibles(new ArrayList<>(sallesOk.isEmpty() ? salles : sallesOk));

            List<EnseignantPlan> enseignantsOk;
            Long impose = seance.getAffectationEnseignantId();
            if (impose != null && enseignants.containsKey(impose)) {
                enseignantsOk = List.of(enseignants.get(impose));
            } else {
                long niveauId = seance.getGroupe().niveauId();
                enseignantsOk = tousEnseignants.stream()
                        .filter(e -> {
                            Set<Long> niveaux = e.getHabilitations().get(matiere.id());
                            return niveaux != null && (niveaux.isEmpty() || niveaux.contains(niveauId));
                        })
                        .toList();
            }
            seance.setEnseignantsPossibles(
                    new ArrayList<>(enseignantsOk.isEmpty() ? tousEnseignants : enseignantsOk));
        }
    }

    // ------------------------------------------------------------------
    // Données de faisabilité (H-01..H-06)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DonneesFaisabilite construireDonneesFaisabilite(Long etablissementId) {
        GrilleConfig grille = exigerGrille(etablissementId);
        List<Creneau> tousCreneaux = grilleService.listerCreneaux(etablissementId);
        Map<Jour, List<Integer>> coursParJour = new LinkedHashMap<>();
        int unitesCours = 0;
        for (Creneau creneau : tousCreneaux) {
            if (creneau.getType() == TypeCreneau.COURS) {
                coursParJour.computeIfAbsent(creneau.getJour(), j -> new ArrayList<>()).add(creneau.getIndexDebut());
                unitesCours++;
            }
        }

        Map<Long, EnseignantPlan> enseignants = preparerEnseignants(etablissementId, grille.unitesParJour());
        List<BesoinSeances> besoins = construireBesoins(etablissementId, true);

        Map<Long, Integer> libresMixtes = new LinkedHashMap<>();
        Map<Long, Integer> quotasMixtes = new LinkedHashMap<>();
        for (Enseignant enseignant : enseignantRepository.findByEtablissementIdOrderByNomCompletAsc(etablissementId)) {
            if (enseignant.getType() != TypeEnseignant.MIXTE) {
                continue;
            }
            int bloquees = unitesBloqueesEtat(enseignant, coursParJour, grille.unitesParJour());
            libresMixtes.put(enseignant.getId(), Math.max(0, unitesCours - bloquees));
            quotasMixtes.put(enseignant.getId(), enseignant.getQuotaHebdoUnites());
        }

        Map<Long, String> nomsMatieres = matiereRepository.findByEtablissementIdOrderByLibelleAsc(etablissementId)
                .stream()
                .collect(Collectors.toMap(Matiere::getId, Matiere::getLibelle));

        List<SallePlan> salles = salleRepository.findByEtablissementIdOrderByNomAsc(etablissementId).stream()
                .map(MappeurSolveur::versSallePlan)
                .toList();

        return new DonneesFaisabilite(unitesCours, libresMixtes, besoins,
                new ArrayList<>(enseignants.values()), salles, nomsMatieres, quotasMixtes);
    }

    /** Unités COURS bloquées par les indispos ETAT VALIDÉES (buffer inclus), max des vues semaine A/B. */
    private int unitesBloqueesEtat(Enseignant enseignant, Map<Jour, List<Integer>> coursParJour, int unitesParJour) {
        List<Indisponibilite> indispos = indisponibiliteRepository
                .findByEnseignantIdAndStatut(enseignant.getId(), StatutIndispo.VALIDE).stream()
                .filter(i -> i.getSource() == SourceIndispo.ETAT)
                .toList();
        int total = 0;
        for (Map.Entry<Jour, List<Integer>> entree : coursParJour.entrySet()) {
            Set<Integer> vueA = new HashSet<>();
            Set<Integer> vueB = new HashSet<>();
            for (Indisponibilite indispo : indispos) {
                if (indispo.getJour() != entree.getKey()) {
                    continue;
                }
                int debut = Math.max(0, indispo.getIndexDebut() - enseignant.getBufferTrajetUnites());
                int fin = Math.min(unitesParJour,
                        indispo.getIndexDebut() + indispo.getDureeUnites() + enseignant.getBufferTrajetUnites());
                for (int index : entree.getValue()) {
                    if (index >= debut && index < fin) {
                        if (indispo.getSemaine() != ma.jadwal.enseignant.entite.Semaine.B) {
                            vueA.add(index);
                        }
                        if (indispo.getSemaine() != ma.jadwal.enseignant.entite.Semaine.A) {
                            vueB.add(index);
                        }
                    }
                }
            }
            total += Math.max(vueA.size(), vueB.size());
        }
        return total;
    }

    // ------------------------------------------------------------------
    // Besoins (maquettes × groupes, overrides, barrettes, affectations)
    // ------------------------------------------------------------------

    /**
     * Construit les besoins de séances de l'établissement.
     *
     * @param avecCoEnseignants si vrai (faisabilité, C-07/H-02), chaque maquette avec k co-enseignants
     *                          ajoute (k-1) besoins fantômes (groupe d'id négatif, matière sans type de
     *                          salle) pour multiplier le besoin d'encadrement sans fausser H-01 ni H-04.
     */
    @Transactional(readOnly = true)
    public List<BesoinSeances> construireBesoins(Long etablissementId, boolean avecCoEnseignants) {
        List<Groupe> groupes = groupeRepository.findByEtablissementIdOrderByLibelleAsc(etablissementId);
        Map<Long, List<Groupe>> enfantsParParent = new HashMap<>();
        List<Groupe> classes = new ArrayList<>();
        for (Groupe groupe : groupes) {
            if (groupe.getParent() != null) {
                enfantsParParent.computeIfAbsent(groupe.getParent().getId(), id -> new ArrayList<>()).add(groupe);
            } else if (groupe.getType() != TypeGroupe.SOUS_GROUPE) {
                classes.add(groupe);
            }
        }

        Map<Long, List<Maquette>> maquettesParNiveau = maquetteRepository.findByEtablissementId(etablissementId)
                .stream()
                .collect(Collectors.groupingBy(m -> m.getNiveau().getId()));

        Map<String, Integer> overrides = new HashMap<>();
        List<Long> idsClasses = classes.stream().map(Groupe::getId).toList();
        if (!idsClasses.isEmpty()) {
            for (VolumeOverride override : volumeOverrideRepository.findByGroupeIdIn(idsClasses)) {
                overrides.put(override.getGroupe().getId() + "|" + override.getMatiere().getId(),
                        override.getVolumeUnites());
            }
        }

        Map<String, Long> barretteParCle = new HashMap<>();
        for (Barrette barrette : barretteRepository.findByEtablissementIdOrderByLibelleAsc(etablissementId)) {
            for (Groupe groupe : barrette.getGroupes()) {
                barretteParCle.put(barrette.getMatiere().getId() + "|" + groupe.getId(), barrette.getId());
            }
        }

        Map<String, List<Affectation>> affectationsParCle = affectationRepository
                .findByEtablissementId(etablissementId).stream()
                .collect(Collectors.groupingBy(a -> a.getGroupe().getId() + "|" + a.getMatiere().getId()));

        List<BesoinSeances> besoins = new ArrayList<>();
        for (Groupe classe : classes) {
            List<Maquette> maquettes = maquettesParNiveau.getOrDefault(classe.getNiveau().getId(), List.of());
            for (Maquette maquette : maquettes) {
                construireBesoinsLigne(classe, maquette, overrides, barretteParCle, affectationsParCle,
                        enfantsParParent, avecCoEnseignants, besoins);
            }
        }
        return besoins;
    }

    private void construireBesoinsLigne(Groupe classe, Maquette maquette, Map<String, Integer> overrides,
                                        Map<String, Long> barretteParCle,
                                        Map<String, List<Affectation>> affectationsParCle,
                                        Map<Long, List<Groupe>> enfantsParParent,
                                        boolean avecCoEnseignants, List<BesoinSeances> besoins) {
        Matiere matiere = maquette.getMatiere();
        String cle = classe.getId() + "|" + matiere.getId();
        int volume = overrides.getOrDefault(cle, maquette.getVolumeUnites());
        Integer volumeB = maquette.getVolumeUnitesB();
        if (volume <= 0 && (volumeB == null || volumeB <= 0)) {
            return;
        }
        List<List<Integer>> patterns = parsePatterns(maquette.getPatternsJson());
        GroupePlan groupePlan = versGroupePlan(classe);
        MatierePlan matierePlan = versMatierePlan(matiere);
        List<GroupePlan> sousGroupes = maquette.getDedoublement() == Dedoublement.AUCUN
                ? List.of()
                : enfantsParParent.getOrDefault(classe.getId(), List.of()).stream()
                        .map(this::versGroupePlan)
                        .toList();
        String dedoublement = sousGroupes.isEmpty() ? "AUCUN" : maquette.getDedoublement().name();
        Long barretteId = barretteParCle.get(matiere.getId() + "|" + classe.getId());
        List<Affectation> affectations = affectationsParCle.getOrDefault(cle, List.of());
        int maxParJour = maquette.getMaxParJourUnites();

        List<BesoinSeances> nouveaux = new ArrayList<>();
        if (affectations.size() > 1) {
            // C-06 volume partagé : répartition des séances au prorata des volumes déclarés.
            int[] parts = repartir(volume, affectations);
            int[] partsB = volumeB == null ? null : repartir(volumeB, affectations);
            for (int i = 0; i < affectations.size(); i++) {
                int part = parts[i];
                Integer partB = partsB == null ? null : partsB[i];
                if (part <= 0 && (partB == null || partB <= 0)) {
                    continue;
                }
                nouveaux.add(new BesoinSeances(groupePlan, matierePlan, part, partB, patterns, maxParJour,
                        dedoublement, sousGroupes, encoderBarrette(barretteId, 0),
                        affectations.get(i).getEnseignant().getId()));
            }
        } else {
            Long enseignantImpose = affectations.size() == 1 ? affectations.get(0).getEnseignant().getId() : null;
            if (barretteId != null && volumeB == null && "AUCUN".equals(dedoublement)) {
                // B-05 : un rang de barrette par séance pour n'aligner entre groupes que les séances
                // de même rang, sans superposer les séances d'un même groupe.
                List<Integer> durees = decouper(volume, patterns,
                        matierePlan.dureeMinUnites(), matierePlan.dureeMaxUnites());
                for (int rang = 0; rang < durees.size(); rang++) {
                    int duree = durees.get(rang);
                    nouveaux.add(new BesoinSeances(groupePlan, matierePlan, duree, null,
                            List.of(List.of(duree)), maxParJour, "AUCUN", List.of(),
                            encoderBarrette(barretteId, rang), enseignantImpose));
                }
            } else {
                nouveaux.add(new BesoinSeances(groupePlan, matierePlan, volume, volumeB, patterns, maxParJour,
                        dedoublement, sousGroupes, encoderBarrette(barretteId, 0), enseignantImpose));
            }
        }
        besoins.addAll(nouveaux);

        if (avecCoEnseignants && maquette.getCoEnseignants() > 1) {
            // C-07 : (k-1) besoins fantômes pour la faisabilité — groupe d'id négatif (H-01 isolé)
            // et matière sans exigence de salle (H-04 non gonflé).
            GroupePlan fantome = new GroupePlan(-classe.getId(), classe.getLibelle() + " (co-ens.)",
                    classe.getEffectif(), null, classe.getNiveau().getId(), classe.getNiveau().getOrdre(), null);
            MatierePlan sansSalle = new MatierePlan(matierePlan.id(), matierePlan.libelle(),
                    matierePlan.coefficient(), matierePlan.poidsCognitif(), null, Set.of(),
                    matierePlan.dureeMinUnites(), matierePlan.dureeMaxUnites(),
                    matierePlan.eviterAvantDejeuner(), matierePlan.eviterFinJournee());
            for (int i = 1; i < maquette.getCoEnseignants(); i++) {
                besoins.add(new BesoinSeances(fantome, sansSalle, volume, volumeB, patterns, maxParJour,
                        dedoublement, sousGroupes, null, null));
            }
        }
    }

    private static Long encoderBarrette(Long barretteId, int rang) {
        return barretteId == null ? null : barretteId * MULTIPLICATEUR_BARRETTE + rang;
    }

    /** Id de barrette réel depuis l'id encodé porté par une {@link SeancePlan}. */
    public static Long decoderBarrette(Long barretteEncodee) {
        return barretteEncodee == null ? null : barretteEncodee / MULTIPLICATEUR_BARRETTE;
    }

    /** Répartition d'un volume entre affectations au prorata des volumes déclarés (somme exacte). */
    static int[] repartir(int volume, List<Affectation> affectations) {
        int n = affectations.size();
        int[] parts = new int[n];
        if (volume <= 0 || n == 0) {
            return parts;
        }
        long totalDeclare = 0;
        for (Affectation affectation : affectations) {
            totalDeclare += affectation.getVolumeUnites() == null ? 0 : Math.max(0, affectation.getVolumeUnites());
        }
        double[] bruts = new double[n];
        for (int i = 0; i < n; i++) {
            double poids = totalDeclare > 0
                    ? Math.max(0, affectations.get(i).getVolumeUnites() == null ? 0 : affectations.get(i).getVolumeUnites())
                    : 1.0;
            double totalPoids = totalDeclare > 0 ? totalDeclare : n;
            bruts[i] = volume * poids / totalPoids;
            parts[i] = (int) Math.floor(bruts[i]);
        }
        int reste = volume;
        for (int part : parts) {
            reste -= part;
        }
        // Plus forts restes d'abord.
        Integer[] ordre = new Integer[n];
        for (int i = 0; i < n; i++) {
            ordre[i] = i;
        }
        java.util.Arrays.sort(ordre, (a, b) -> Double.compare(bruts[b] - parts[b], bruts[a] - parts[a]));
        for (int i = 0; i < reste; i++) {
            parts[ordre[i % n]]++;
        }
        return parts;
    }

    /** Réplique du découpage de la SeanceFactory (C-02/C-03) pour les besoins de barrette par rang. */
    static List<Integer> decouper(int volume, List<List<Integer>> patterns, int dureeMin, int dureeMax) {
        if (volume <= 0) {
            return List.of();
        }
        if (patterns != null) {
            for (List<Integer> pattern : patterns) {
                if (pattern != null && !pattern.isEmpty()
                        && pattern.stream().mapToInt(Integer::intValue).sum() == volume) {
                    return List.copyOf(pattern);
                }
            }
        }
        int max = dureeMax > 0 ? dureeMax : volume;
        int min = Math.max(1, dureeMin);
        List<Integer> blocs = new ArrayList<>();
        int reste = volume;
        while (reste > 0) {
            int bloc = Math.min(max, reste);
            int reliquat = reste - bloc;
            if (reliquat > 0 && reliquat < min) {
                bloc = reste - min;
            }
            if (bloc < min) {
                bloc = reste;
            }
            blocs.add(bloc);
            reste -= bloc;
        }
        return blocs;
    }

    // ------------------------------------------------------------------
    // Enseignants
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Map<Long, EnseignantPlan> preparerEnseignants(Long etablissementId, int unitesParJour) {
        Map<Long, EnseignantPlan> resultat = new LinkedHashMap<>();
        for (Enseignant enseignant : enseignantRepository.findByEtablissementIdOrderByNomCompletAsc(etablissementId)) {
            resultat.put(enseignant.getId(), versEnseignantPlan(enseignant, unitesParJour));
        }
        return resultat;
    }

    private EnseignantPlan versEnseignantPlan(Enseignant enseignant, int unitesParJour) {
        Map<Long, Set<Long>> habilitations = new LinkedHashMap<>();
        for (Habilitation habilitation : habilitationRepository.findByEnseignantId(enseignant.getId())) {
            Set<Long> niveaux = habilitation.getNiveaux().stream()
                    .map(Niveau::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            habilitations.put(habilitation.getMatiere().getId(), niveaux);
        }

        List<IndispoPlan> indispos = new ArrayList<>();
        for (Indisponibilite indispo : indisponibiliteRepository
                .findByEnseignantIdAndStatut(enseignant.getId(), StatutIndispo.VALIDE)) {
            int debut = indispo.getIndexDebut();
            int fin = debut + indispo.getDureeUnites();
            if (indispo.getSource() == SourceIndispo.ETAT) {
                // D-05 : extension du buffer trajet avant/après.
                debut = Math.max(0, debut - enseignant.getBufferTrajetUnites());
                fin = Math.min(unitesParJour, fin + enseignant.getBufferTrajetUnites());
            }
            if (fin <= debut) {
                continue;
            }
            indispos.add(new IndispoPlan(JourPlan.valueOf(indispo.getJour().name()), debut, fin - debut,
                    SemainePlan.valueOf(indispo.getSemaine().name())));
        }

        List<PreferencePlan> preferences = new ArrayList<>();
        for (PreferenceHoraire preference : preferenceHoraireRepository
                .findByEnseignantIdOrderByJourAscIndexDebutAsc(enseignant.getId())) {
            preferences.add(new PreferencePlan(JourPlan.valueOf(preference.getJour().name()),
                    preference.getIndexDebut(), preference.getDureeUnites(),
                    preference.getType() == TypePreference.EVITER));
        }

        return new EnseignantPlan(enseignant.getId(), enseignant.getNomComplet(),
                enseignant.getQuotaHebdoUnites(), enseignant.getMaxConsecutifUnites(),
                enseignant.getAmplitudeMaxUnites(), enseignant.isVacataire(),
                habilitations, indispos, preferences);
    }

    // ------------------------------------------------------------------
    // Séances verrouillées (I-03)
    // ------------------------------------------------------------------

    private void epinglerSeancesVerrouillees(Long etablissementId, List<SeancePlan> seances,
                                             Map<Long, CreneauPlan> creneauxParId,
                                             Map<Long, SallePlan> sallesParId,
                                             Map<Long, EnseignantPlan> enseignants) {
        PlanningVersion active = planningVersionRepository
                .findByEtablissementIdAndActiveTrue(etablissementId).stream()
                .findFirst()
                .orElse(null);
        if (active == null) {
            return;
        }
        for (Seance verrouillee : seanceRepository.findByVersionId(active.getId())) {
            if (!verrouillee.isVerrouillee() || verrouillee.getCreneau() == null) {
                continue;
            }
            CreneauPlan creneau = creneauxParId.get(verrouillee.getCreneau().getId());
            SallePlan salle = verrouillee.getSalle() == null ? null : sallesParId.get(verrouillee.getSalle().getId());
            EnseignantPlan enseignant = verrouillee.getEnseignant() == null
                    ? null : enseignants.get(verrouillee.getEnseignant().getId());
            if (creneau == null || salle == null || enseignant == null) {
                continue;
            }
            final Seance cible = verrouillee;
            SeancePlan correspondante = seances.stream()
                    .filter(s -> !s.isVerrouillee() && s.getIdPersistant() == null
                            && s.getGroupe().id() == cible.getGroupe().getId()
                            && s.getMatiere().id() == cible.getMatiere().getId()
                            && s.getDureeUnites() == cible.getDureeUnites()
                            && s.getSemaine().name().equals(cible.getSemaine().name()))
                    .findFirst()
                    .orElse(null);
            if (correspondante == null) {
                continue;
            }
            correspondante.setIdPersistant(verrouillee.getId());
            correspondante.setCreneau(creneau);
            correspondante.setSalle(salle);
            correspondante.setEnseignant(enseignant);
            correspondante.setVerrouillee(true);
        }
    }

    // ------------------------------------------------------------------
    // Pondérations (I-01)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ConstraintWeightOverrides<BendableScore> construirePonderations(Long etablissementId) {
        Map<String, BendableScore> poids = new LinkedHashMap<>();
        for (PonderationService.ReglePonderee regle : ponderationService.listerReglesEffectives(etablissementId)) {
            Integer niveau = NIVEAU_SOFT.get(regle.regle());
            if (niveau == null) {
                continue;
            }
            poids.put(regle.regle(), BendableScore.ofSoft(3, 3, niveau, regle.poids()));
        }
        return ConstraintWeightOverrides.of(poids);
    }

    /** Niveau soft (0..2) d'une règle souple, null si la règle n'est pas pilotée par le solveur. */
    public static Integer niveauSoft(String regle) {
        return NIVEAU_SOFT.get(regle);
    }

    // ------------------------------------------------------------------
    // Validation d'un déplacement manuel (I-04)
    // ------------------------------------------------------------------

    /**
     * Mappe toutes les séances persistées d'une version en {@link SeancePlan}, avec des
     * enseignants complets (habilitations, indispos) pour la validation synchrone.
     */
    @Transactional(readOnly = true)
    public List<SeancePlan> mapperSeancesVersion(Long etablissementId, Long versionId) {
        GrilleConfig grille = exigerGrille(etablissementId);
        Map<Long, EnseignantPlan> enseignants = preparerEnseignants(etablissementId, grille.unitesParJour());
        Map<String, Integer> maxParJour = maxParJourParNiveauMatiere(etablissementId);
        List<SeancePlan> resultat = new ArrayList<>();
        for (Seance seance : seanceRepository.findByVersionId(versionId)) {
            resultat.add(versSeancePlan(seance, grille, enseignants, maxParJour));
        }
        return resultat;
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> maxParJourParNiveauMatiere(Long etablissementId) {
        Map<String, Integer> resultat = new HashMap<>();
        for (Maquette maquette : maquetteRepository.findByEtablissementId(etablissementId)) {
            resultat.put(maquette.getNiveau().getId() + "|" + maquette.getMatiere().getId(),
                    maquette.getMaxParJourUnites());
        }
        return resultat;
    }

    public SeancePlan versSeancePlan(Seance seance, GrilleConfig grille,
                                     Map<Long, EnseignantPlan> enseignants, Map<String, Integer> maxParJour) {
        int plafond = maxParJour.getOrDefault(
                seance.getGroupe().getNiveau().getId() + "|" + seance.getMatiere().getId(), 0);
        SeancePlan plan = new SeancePlan(String.valueOf(seance.getId()), seance.getId(),
                versGroupePlan(seance.getGroupe()), versMatierePlan(seance.getMatiere()),
                seance.getDureeUnites(), SemainePlan.valueOf(seance.getSemaine().name()),
                seance.getBlocAlignement(), seance.getBarretteId(), null, plafond);
        plan.setVerrouillee(seance.isVerrouillee());
        if (seance.getCreneau() != null) {
            plan.setCreneau(versCreneauPlan(seance.getCreneau(), grille));
        }
        if (seance.getSalle() != null) {
            plan.setSalle(versSallePlan(seance.getSalle()));
        }
        if (seance.getEnseignant() != null) {
            plan.setEnseignant(enseignants.get(seance.getEnseignant().getId()));
        }
        return plan;
    }

    // ------------------------------------------------------------------
    // Conversions élémentaires
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public GrilleConfig exigerGrille(Long etablissementId) {
        GrilleConfig grille = grilleService.lireGrille(etablissementId);
        if (grille == null) {
            throw new ConflitException("La grille horaire n'est pas configurée : enregistrez-la d'abord.");
        }
        return grille;
    }

    private List<CreneauPlan> creneauxCours(Long etablissementId, GrilleConfig grille) {
        return grilleService.listerCreneaux(etablissementId).stream()
                .filter(c -> c.getType() == TypeCreneau.COURS)
                .map(c -> versCreneauPlan(c, grille))
                .toList();
    }

    public CreneauPlan versCreneauPlan(Creneau creneau, GrilleConfig grille) {
        int limiteMatin = indexDebutDejeuner(grille);
        return new CreneauPlan(creneau.getId(), JourPlan.valueOf(creneau.getJour().name()),
                creneau.getIndexDebut(), creneau.getUnitesDisponibles(),
                creneau.getIndexDebut() < limiteMatin);
    }

    /** Index de début de la plage DEJEUNER, sinon la moitié de la journée (A-07). */
    private static int indexDebutDejeuner(GrilleConfig grille) {
        if (grille.plagesBloquees() != null) {
            for (GrilleConfig.PlageBloquee plage : grille.plagesBloquees()) {
                if ("DEJEUNER".equals(plage.type())) {
                    return plage.indexDebut();
                }
            }
        }
        return grille.unitesParJour() / 2;
    }

    public GroupePlan versGroupePlan(Groupe groupe) {
        return new GroupePlan(groupe.getId(), groupe.getLibelle(), groupe.getEffectif(),
                groupe.getParent() == null ? null : groupe.getParent().getId(),
                groupe.getNiveau().getId(), groupe.getNiveau().getOrdre(),
                groupe.getNiveau().getChargeMaxUnitesJour());
    }

    public MatierePlan versMatierePlan(Matiere matiere) {
        return new MatierePlan(matiere.getId(), matiere.getLibelle(), matiere.getCoefficient(),
                matiere.getPoidsCognitif(), matiere.getTypeSalleRequis(),
                csvVersEnsemble(matiere.getEquipementsRequis()), matiere.getDureeMinUnites(),
                matiere.getDureeMaxUnites(), matiere.isEviterAvantDejeuner(), matiere.isEviterFinJournee());
    }

    public static SallePlan versSallePlan(Salle salle) {
        return new SallePlan(salle.getId(), salle.getNom(), salle.getCapacite(), salle.getType(),
                csvVersEnsemble(salle.getEquipements()), salle.getBatiment());
    }

    /** "vidéo, paillasses" -&gt; {"vidéo","paillasses"} ; null/vide -&gt; ensemble vide. */
    public static Set<String> csvVersEnsemble(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        Set<String> resultat = new LinkedHashSet<>();
        for (String element : csv.split(",")) {
            String propre = element.trim();
            if (!propre.isEmpty()) {
                resultat.add(propre);
            }
        }
        return resultat;
    }

    /** {"vidéo","paillasses"} -&gt; "vidéo,paillasses" ; vide -&gt; null (stockage CSV). */
    public static String listeVersCsv(List<String> elements) {
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        String csv = elements.stream()
                .map(String::trim)
                .filter(e -> !e.isEmpty())
                .collect(Collectors.joining(","));
        return csv.isEmpty() ? null : csv;
    }

    public List<List<Integer>> parsePatterns(String patternsJson) {
        if (patternsJson == null || patternsJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(patternsJson, new TypeReference<List<List<Integer>>>() {
            });
        } catch (Exception e) {
            return null;
        }
    }

    public String patternsVersJson(List<List<Integer>> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(patterns);
        } catch (Exception e) {
            return null;
        }
    }

    private Etablissement obtenirEtablissement(Long etablissementId) {
        return etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ConflitException("Établissement introuvable : " + etablissementId));
    }
}
