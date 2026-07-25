package ma.jadwal.api.generation;

import ai.timefold.solver.core.api.solver.SolverConfigOverride;
import ai.timefold.solver.core.api.solver.SolverManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.jadwal.api.dto.AnalyseReponse;
import ma.jadwal.api.erreur.FaisabiliteEchecException;
import ma.jadwal.api.solveur.MappeurSolveur;
import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.planning.depot.GenerationRepository;
import ma.jadwal.planning.entite.Generation;
import ma.jadwal.planning.entite.StatutGeneration;
import ma.jadwal.solver.faisabilite.BilanFaisabilite;
import ma.jadwal.solver.faisabilite.FaisabiliteRapport;
import ma.jadwal.solver.faisabilite.FaisabiliteService;
import ma.jadwal.solver.modele.EmploiDuTempsPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestration des générations d'emploi du temps : faisabilité préalable (H-01..H-06),
 * lancement asynchrone via {@link SolverManager}, progression en mémoire + SSE,
 * arrêt anticipé (I-05) et restitution de l'analyse (I-06).
 */
@Service
public class GenerationService {

    private static final Logger journal = LoggerFactory.getLogger(GenerationService.class);
    private static final long DELAI_SSE_MS = 15L * 60 * 1000;

    private final SolverManager<EmploiDuTempsPlan> solverManager;
    private final MappeurSolveur mappeur;
    private final FinalisationGeneration finalisation;
    private final GenerationRepository generationRepository;
    private final FaisabiliteService faisabiliteService = new FaisabiliteService();
    private final Map<Long, ProgressionGeneration> progressions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GenerationService(SolverManager<EmploiDuTempsPlan> solverManager,
                             MappeurSolveur mappeur,
                             FinalisationGeneration finalisation,
                             GenerationRepository generationRepository) {
        this.solverManager = solverManager;
        this.mappeur = mappeur;
        this.finalisation = finalisation;
        this.generationRepository = generationRepository;
    }

    // ------------------------------------------------------------------
    // Faisabilité
    // ------------------------------------------------------------------

    public FaisabiliteRapport verifierFaisabilite(Long etablissementId) {
        return faisabiliteService.verifier(mappeur.construireDonneesFaisabilite(etablissementId));
    }

    // ------------------------------------------------------------------
    // Lancement
    // ------------------------------------------------------------------

    public Generation lancer(Long etablissementId, int dureeMaxSecondes) {
        FaisabiliteRapport rapport = verifierFaisabilite(etablissementId);
        if (BilanFaisabilite.STATUT_ECHEC.equals(rapport.global())) {
            throw new FaisabiliteEchecException(rapport);
        }
        EmploiDuTempsPlan plan = mappeur.construirePlan(etablissementId);
        Generation generation = finalisation.creerGeneration(etablissementId, dureeMaxSecondes,
                serialiser(rapport));
        Long generationId = generation.getId();
        Long versionId = generation.getVersion().getId();

        ProgressionGeneration progression = new ProgressionGeneration(generationId);
        progressions.put(generationId, progression);

        solverManager.solveBuilder()
                .withProblemId(generationId)
                .withProblem(plan)
                .withConfigOverride(new SolverConfigOverride()
                        .withTerminationSpentLimit(Duration.ofSeconds(dureeMaxSecondes)))
                .withBestSolutionEventConsumer(evenement -> surAmelioration(generationId, evenement.solution()))
                .withFinalBestSolutionEventConsumer(evenement ->
                        surResultatFinal(generationId, versionId, evenement.solution()))
                .withExceptionHandler((problemeId, exception) -> surErreur(generationId, exception))
                .run();
        return generation;
    }

    private void surAmelioration(Long generationId, EmploiDuTempsPlan solution) {
        ProgressionGeneration progression = progressions.get(generationId);
        if (progression == null) {
            return;
        }
        progression.setScore(solution.getScore() == null ? null : solution.getScore().toString());
        diffuser(progression);
    }

    private void surResultatFinal(Long generationId, Long versionId, EmploiDuTempsPlan solution) {
        ProgressionGeneration progression = progressions.get(generationId);
        boolean arretee = progression != null && progression.isArretDemande();
        StatutGeneration statut;
        try {
            statut = finalisation.persisterResultat(generationId, versionId, solution, arretee);
        } catch (Exception e) {
            journal.error("Persistance du résultat de la génération {} impossible", generationId, e);
            finalisation.marquerEchec(generationId, e.getMessage());
            statut = StatutGeneration.ECHEC;
        }
        if (progression != null) {
            progression.setScore(solution.getScore() == null ? null : solution.getScore().toString());
            progression.setStatut(statut.name());
            diffuser(progression);
            terminerEmetteurs(progression);
        }
        progressions.remove(generationId);
    }

    private void surErreur(Long generationId, Throwable exception) {
        journal.error("Erreur du solveur pour la génération {}", generationId, exception);
        finalisation.marquerEchec(generationId, exception == null ? null : exception.getMessage());
        ProgressionGeneration progression = progressions.get(generationId);
        if (progression != null) {
            progression.setStatut(StatutGeneration.ECHEC.name());
            diffuser(progression);
            terminerEmetteurs(progression);
        }
        progressions.remove(generationId);
    }

    // ------------------------------------------------------------------
    // Arrêt anticipé (I-05)
    // ------------------------------------------------------------------

    public Generation arreter(Long etablissementId, Long generationId) {
        Generation generation = obtenir(etablissementId, generationId);
        if (generation.getStatut() != StatutGeneration.EN_COURS) {
            throw new ConflitException("La génération n'est plus en cours (statut " + generation.getStatut() + ").");
        }
        ProgressionGeneration progression = progressions.get(generationId);
        if (progression != null) {
            progression.demanderArret();
        }
        try {
            solverManager.terminateEarly(generationId);
        } catch (Exception e) {
            // Le job n'est plus connu du solveur (redémarrage) : on clôture directement.
            journal.warn("Arrêt anticipé sans job actif pour la génération {} : {}", generationId, e.getMessage());
            finalisation.marquerArretee(generationId);
            if (progression != null) {
                progression.setStatut(StatutGeneration.ARRETEE.name());
                diffuser(progression);
                terminerEmetteurs(progression);
                progressions.remove(generationId);
            }
        }
        return obtenir(etablissementId, generationId);
    }

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    public List<Generation> lister(Long etablissementId) {
        return generationRepository.findByEtablissementIdOrderByLanceeLeDesc(etablissementId);
    }

    public Generation obtenir(Long etablissementId, Long generationId) {
        return generationRepository.findByIdAndEtablissementId(generationId, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Génération introuvable : " + generationId));
    }

    public AnalyseReponse analyser(Long etablissementId, Long generationId) {
        Generation generation = obtenir(etablissementId, generationId);
        if (generation.getAnalyse() != null && !generation.getAnalyse().isBlank()) {
            try {
                return objectMapper.readValue(generation.getAnalyse(), AnalyseReponse.class);
            } catch (Exception e) {
                journal.warn("Analyse illisible pour la génération {}", generationId);
            }
        }
        return new AnalyseReponse(generation.getScore(), List.of());
    }

    // ------------------------------------------------------------------
    // SSE
    // ------------------------------------------------------------------

    public SseEmitter suivre(Long etablissementId, Long generationId) {
        Generation generation = obtenir(etablissementId, generationId);
        SseEmitter emetteur = new SseEmitter(DELAI_SSE_MS);
        ProgressionGeneration progression = progressions.get(generationId);
        if (progression == null || generation.getStatut() != StatutGeneration.EN_COURS) {
            envoyer(emetteur, donneesDe(generation));
            emetteur.complete();
            return emetteur;
        }
        progression.ajouterEmetteur(emetteur);
        envoyer(emetteur, progression.donneesCourantes());
        return emetteur;
    }

    private Map<String, Object> donneesDe(Generation generation) {
        long secondes = generation.getTermineeLe() == null
                ? 0
                : Duration.between(generation.getLanceeLe(), generation.getTermineeLe()).toSeconds();
        Map<String, Object> donnees = new java.util.LinkedHashMap<>();
        donnees.put("score", generation.getScore());
        donnees.put("tempsEcouleSecondes", secondes);
        donnees.put("statut", generation.getStatut().name());
        return donnees;
    }

    private void diffuser(ProgressionGeneration progression) {
        Map<String, Object> donnees = progression.donneesCourantes();
        for (SseEmitter emetteur : progression.emetteurs()) {
            envoyer(emetteur, donnees);
        }
    }

    private void terminerEmetteurs(ProgressionGeneration progression) {
        for (SseEmitter emetteur : progression.emetteurs()) {
            try {
                emetteur.complete();
            } catch (Exception ignoree) {
                // Émetteur déjà clos côté client.
            }
        }
    }

    private void envoyer(SseEmitter emetteur, Map<String, Object> donnees) {
        try {
            emetteur.send(SseEmitter.event().name("progression").data(donnees));
        } catch (Exception e) {
            try {
                emetteur.completeWithError(e);
            } catch (Exception ignoree) {
                // Déjà clos.
            }
        }
    }

    private String serialiser(FaisabiliteRapport rapport) {
        try {
            return objectMapper.writeValueAsString(rapport);
        } catch (Exception e) {
            return null;
        }
    }
}
