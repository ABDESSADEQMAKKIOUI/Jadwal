package ma.jadwal.api.generation;

import ai.timefold.solver.core.api.score.BendableScore;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.jadwal.api.dto.AnalyseReponse;
import ma.jadwal.api.solveur.MappeurSolveur;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.enseignant.depot.EnseignantRepository;
import ma.jadwal.planning.depot.GenerationRepository;
import ma.jadwal.planning.depot.SeanceRepository;
import ma.jadwal.planning.entite.Generation;
import ma.jadwal.planning.entite.PlanningVersion;
import ma.jadwal.planning.entite.Seance;
import ma.jadwal.planning.entite.SemaineSeance;
import ma.jadwal.planning.entite.StatutGeneration;
import ma.jadwal.planning.service.PlanningVersionService;
import ma.jadwal.referentiel.depot.CreneauRepository;
import ma.jadwal.referentiel.depot.GroupeRepository;
import ma.jadwal.referentiel.depot.MatiereRepository;
import ma.jadwal.referentiel.depot.SalleRepository;
import ma.jadwal.solver.modele.EmploiDuTempsPlan;
import ma.jadwal.solver.modele.SeancePlan;
import ma.jadwal.solver.validation.DiagnosticPlanning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistance transactionnelle des résultats du solveur (appelée depuis les threads Timefold).
 */
@Service
public class FinalisationGeneration {

    private static final Logger journal = LoggerFactory.getLogger(FinalisationGeneration.class);
    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm").withZone(ZoneId.of("Africa/Casablanca"));

    private final GenerationRepository generationRepository;
    private final SeanceRepository seanceRepository;
    private final PlanningVersionService planningVersionService;
    private final GroupeRepository groupeRepository;
    private final MatiereRepository matiereRepository;
    private final SalleRepository salleRepository;
    private final CreneauRepository creneauRepository;
    private final EnseignantRepository enseignantRepository;
    private final DiagnosticPlanning diagnostic = new DiagnosticPlanning();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FinalisationGeneration(GenerationRepository generationRepository,
                                  SeanceRepository seanceRepository,
                                  PlanningVersionService planningVersionService,
                                  GroupeRepository groupeRepository,
                                  MatiereRepository matiereRepository,
                                  SalleRepository salleRepository,
                                  CreneauRepository creneauRepository,
                                  EnseignantRepository enseignantRepository) {
        this.generationRepository = generationRepository;
        this.seanceRepository = seanceRepository;
        this.planningVersionService = planningVersionService;
        this.groupeRepository = groupeRepository;
        this.matiereRepository = matiereRepository;
        this.salleRepository = salleRepository;
        this.creneauRepository = creneauRepository;
        this.enseignantRepository = enseignantRepository;
    }

    /** Crée la génération et sa version de planning « Génération du &lt;date&gt; ». */
    @Transactional
    public Generation creerGeneration(Long etablissementId, int dureeMaxSecondes, String rapportFaisabiliteJson) {
        PlanningVersion version = planningVersionService.creer(etablissementId,
                "Génération du " + FORMAT_DATE.format(Instant.now()));
        Generation generation = new Generation();
        generation.setEtablissementId(etablissementId);
        generation.setVersion(version);
        generation.setStatut(StatutGeneration.EN_COURS);
        generation.setDureeMaxSecondes(dureeMaxSecondes);
        generation.setRapportFaisabilite(rapportFaisabiliteJson);
        return generationRepository.save(generation);
    }

    /**
     * Persiste la meilleure solution : séances, score, analyse par contrainte (I-06),
     * statut TERMINEE (hard tous à 0) / INFAISABLE / ARRETEE, et active la version si TERMINEE.
     */
    @Transactional
    public StatutGeneration persisterResultat(Long generationId, Long versionId, EmploiDuTempsPlan solution,
                                              boolean arretee) {
        Generation generation = generationRepository.findById(generationId)
                .orElseThrow(() -> new RessourceIntrouvableException("Génération introuvable : " + generationId));
        BendableScore score = solution.getScore();
        boolean hardOk = score != null
                && score.hardScore(0) == 0 && score.hardScore(1) == 0 && score.hardScore(2) == 0;

        seanceRepository.deleteByVersionId(versionId);
        List<Seance> entites = new ArrayList<>();
        for (SeancePlan plan : solution.getSeances()) {
            entites.add(versEntite(generation.getEtablissementId(), versionId, plan));
        }
        seanceRepository.saveAll(entites);

        StatutGeneration statut = arretee
                ? StatutGeneration.ARRETEE
                : (hardOk ? StatutGeneration.TERMINEE : StatutGeneration.INFAISABLE);
        generation.setScore(score == null ? null : score.toString());
        generation.setStatut(statut);
        generation.setTermineeLe(Instant.now());
        generation.setAnalyse(analyserEnJson(solution, score));
        generationRepository.save(generation);

        if (statut == StatutGeneration.TERMINEE) {
            planningVersionService.activer(generation.getEtablissementId(), versionId);
        }
        return statut;
    }

    @Transactional
    public void marquerEchec(Long generationId, String message) {
        generationRepository.findById(generationId).ifPresent(generation -> {
            generation.setStatut(StatutGeneration.ECHEC);
            generation.setTermineeLe(Instant.now());
            if (message != null) {
                generation.setAnalyse(null);
            }
            generationRepository.save(generation);
            journal.error("Génération {} en échec : {}", generationId, message);
        });
    }

    @Transactional
    public void marquerArretee(Long generationId) {
        generationRepository.findById(generationId).ifPresent(generation -> {
            if (generation.getStatut() == StatutGeneration.EN_COURS) {
                generation.setStatut(StatutGeneration.ARRETEE);
                generation.setTermineeLe(Instant.now());
                generationRepository.save(generation);
            }
        });
    }

    private Seance versEntite(Long etablissementId, Long versionId, SeancePlan plan) {
        Seance seance = new Seance();
        seance.setEtablissementId(etablissementId);
        seance.setVersion(planningVersionService.obtenir(etablissementId, versionId));
        seance.setGroupe(groupeRepository.getReferenceById(plan.getGroupe().id()));
        seance.setMatiere(matiereRepository.getReferenceById(plan.getMatiere().id()));
        if (plan.getEnseignant() != null) {
            seance.setEnseignant(enseignantRepository.getReferenceById(plan.getEnseignant().getId()));
        }
        if (plan.getSalle() != null) {
            seance.setSalle(salleRepository.getReferenceById(plan.getSalle().id()));
        }
        if (plan.getCreneau() != null) {
            seance.setCreneau(creneauRepository.getReferenceById(plan.getCreneau().id()));
        }
        seance.setDureeUnites(plan.getDureeUnites());
        seance.setSemaine(SemaineSeance.valueOf(plan.getSemaine().name()));
        seance.setBlocAlignement(plan.getBlocAlignementId());
        seance.setBarretteId(MappeurSolveur.decoderBarrette(plan.getBarretteId()));
        seance.setVerrouillee(plan.isVerrouillee());
        return seance;
    }

    /**
     * I-06 : explication du résultat règle par règle.
     * <p>
     * L'analyse de score native de Timefold ({@code SolutionManager.analyze}) est une
     * fonctionnalité commerciale, indisponible en édition Community : le diagnostic est donc
     * recalculé par {@link DiagnosticPlanning}, à partir des mêmes prédicats que les contraintes.
     */
    private String analyserEnJson(EmploiDuTempsPlan solution, BendableScore score) {
        try {
            List<AnalyseReponse.ContrainteAnalyse> contraintes = new ArrayList<>();
            for (DiagnosticPlanning.ViolationRegle violation : diagnostic.analyser(solution.getSeances())) {
                contraintes.add(new AnalyseReponse.ContrainteAnalyse(
                        violation.regle(),
                        violation.libelle(),
                        violation.nombreViolations()));
            }
            AnalyseReponse reponse = new AnalyseReponse(score == null ? null : score.toString(), contraintes);
            return objectMapper.writeValueAsString(reponse);
        } catch (Exception e) {
            journal.warn("Analyse du planning impossible : {}", e.getMessage());
            return null;
        }
    }
}
