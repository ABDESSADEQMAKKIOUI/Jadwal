package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.planning.depot.GenerationRepository;
import ma.jadwal.planning.depot.PlanningVersionRepository;
import ma.jadwal.planning.depot.SeanceRepository;
import ma.jadwal.planning.entite.PlanningVersion;
import ma.jadwal.referentiel.depot.EtablissementRepository;
import ma.jadwal.referentiel.entite.Etablissement;
import ma.jadwal.referentiel.service.GrilleConfig;
import ma.jadwal.referentiel.service.GrilleService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Grille horaire de l'établissement (A-07) : lecture et remplacement.
 * Le remplacement est refusé (409) si des séances existent, sauf forcer=true qui
 * supprime versions + séances + générations puis régénère les créneaux.
 */
@RestController
@RequestMapping("/api/ecole/grille")
public class GrilleController {

    public record PlageBloqueeDto(String type, int indexDebut, int dureeUnites) {
    }

    public record GrilleReponse(List<String> joursActifs, String heureDebut, Integer dureeUniteMinutes,
                                Integer unitesParJour, List<PlageBloqueeDto> plagesBloquees,
                                int amplitudeMaxUnites) {
    }

    public record GrilleRequete(@NotEmpty List<String> joursActifs,
                                @NotBlank String heureDebut,
                                @Positive int dureeUniteMinutes,
                                @Positive int unitesParJour,
                                List<PlageBloqueeDto> plagesBloquees,
                                Integer amplitudeMaxUnites) {
    }

    private final GrilleService grilleService;
    private final EtablissementRepository etablissementRepository;
    private final SeanceRepository seanceRepository;
    private final PlanningVersionRepository planningVersionRepository;
    private final GenerationRepository generationRepository;

    public GrilleController(GrilleService grilleService,
                            EtablissementRepository etablissementRepository,
                            SeanceRepository seanceRepository,
                            PlanningVersionRepository planningVersionRepository,
                            GenerationRepository generationRepository) {
        this.grilleService = grilleService;
        this.etablissementRepository = etablissementRepository;
        this.seanceRepository = seanceRepository;
        this.planningVersionRepository = planningVersionRepository;
        this.generationRepository = generationRepository;
    }

    @GetMapping
    public GrilleReponse lire(@AuthenticationPrincipal UtilisateurCourant courant) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ConflitException("Établissement introuvable"));
        GrilleConfig config = grilleService.lireGrille(etablissementId);
        if (config == null) {
            return new GrilleReponse(List.of(), null, null, null, List.of(),
                    etablissement.getAmplitudeMaxUnites());
        }
        return versReponse(config, etablissement.getAmplitudeMaxUnites());
    }

    @PutMapping
    @Transactional
    public GrilleReponse remplacer(@AuthenticationPrincipal UtilisateurCourant courant,
                                   @RequestParam(name = "forcer", defaultValue = "false") boolean forcer,
                                   @Valid @RequestBody GrilleRequete requete) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        boolean seancesExistantes = seanceRepository.existsByEtablissementId(etablissementId);
        if (seancesExistantes && !forcer) {
            throw new ConflitException("Des séances planifiées existent déjà : modifier la grille les invaliderait. "
                    + "Relancez avec forcer=true pour supprimer les plannings et régénérer les créneaux.");
        }
        if (forcer) {
            purgerPlannings(etablissementId);
        }

        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ConflitException("Établissement introuvable"));
        if (requete.amplitudeMaxUnites() != null && requete.amplitudeMaxUnites() > 0) {
            etablissement.setAmplitudeMaxUnites(requete.amplitudeMaxUnites());
            etablissementRepository.save(etablissement);
        }

        List<GrilleConfig.PlageBloquee> plages = requete.plagesBloquees() == null
                ? List.of()
                : requete.plagesBloquees().stream()
                        .map(p -> new GrilleConfig.PlageBloquee(p.type(), p.indexDebut(), p.dureeUnites()))
                        .toList();
        GrilleConfig config = new GrilleConfig(requete.joursActifs(), requete.heureDebut(),
                requete.dureeUniteMinutes(), requete.unitesParJour(), plages);
        GrilleConfig enregistree = grilleService.enregistrerGrille(etablissementId, config);
        return versReponse(enregistree, etablissement.getAmplitudeMaxUnites());
    }

    private void purgerPlannings(Long etablissementId) {
        List<PlanningVersion> versions = planningVersionRepository
                .findByEtablissementIdOrderByCreeeLeDesc(etablissementId);
        for (PlanningVersion version : versions) {
            seanceRepository.deleteByVersionId(version.getId());
        }
        generationRepository.deleteAll(generationRepository.findByEtablissementIdOrderByLanceeLeDesc(etablissementId));
        planningVersionRepository.deleteAll(versions);
    }

    private static GrilleReponse versReponse(GrilleConfig config, int amplitudeMaxUnites) {
        List<PlageBloqueeDto> plages = config.plagesBloquees() == null
                ? List.of()
                : config.plagesBloquees().stream()
                        .map(p -> new PlageBloqueeDto(p.type(), p.indexDebut(), p.dureeUnites()))
                        .toList();
        return new GrilleReponse(config.joursActifs(), config.heureDebut(), config.dureeUniteMinutes(),
                config.unitesParJour(), plages, amplitudeMaxUnites);
    }
}
