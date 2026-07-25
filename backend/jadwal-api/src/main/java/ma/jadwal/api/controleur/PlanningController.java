package ma.jadwal.api.controleur;

import ma.jadwal.api.dto.CreneauReponse;
import ma.jadwal.api.dto.MappeursPlanning;
import ma.jadwal.api.dto.PlanningReponse;
import ma.jadwal.api.dto.SeanceReponse;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.enseignant.service.EnseignantService;
import ma.jadwal.planning.depot.PlanningVersionRepository;
import ma.jadwal.planning.depot.SeanceRepository;
import ma.jadwal.planning.entite.PlanningVersion;
import ma.jadwal.planning.entite.Seance;
import ma.jadwal.planning.entite.SemaineSeance;
import ma.jadwal.planning.service.PlanningVersionService;
import ma.jadwal.referentiel.depot.GroupeRepository;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.referentiel.service.GrilleService;
import ma.jadwal.referentiel.service.GroupeService;
import ma.jadwal.referentiel.service.SalleService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Versions de planning (I-08 rollback) et vues planning par groupe / enseignant / salle.
 */
@RestController
@RequestMapping("/api/ecole/plannings")
public class PlanningController {

    public record VersionReponse(Long id, String libelle, boolean active, Instant creeeLe, long nbSeances) {
    }

    private final PlanningVersionService planningVersionService;
    private final PlanningVersionRepository planningVersionRepository;
    private final SeanceRepository seanceRepository;
    private final GrilleService grilleService;
    private final GroupeService groupeService;
    private final GroupeRepository groupeRepository;
    private final EnseignantService enseignantService;
    private final SalleService salleService;

    public PlanningController(PlanningVersionService planningVersionService,
                              PlanningVersionRepository planningVersionRepository,
                              SeanceRepository seanceRepository,
                              GrilleService grilleService,
                              GroupeService groupeService,
                              GroupeRepository groupeRepository,
                              EnseignantService enseignantService,
                              SalleService salleService) {
        this.planningVersionService = planningVersionService;
        this.planningVersionRepository = planningVersionRepository;
        this.seanceRepository = seanceRepository;
        this.grilleService = grilleService;
        this.groupeService = groupeService;
        this.groupeRepository = groupeRepository;
        this.enseignantService = enseignantService;
        this.salleService = salleService;
    }

    // ---------- Versions ----------

    @GetMapping("/versions")
    public List<VersionReponse> listerVersions(@AuthenticationPrincipal UtilisateurCourant courant) {
        return planningVersionService.lister(ContexteEcole.etablissementId(courant)).stream()
                .map(version -> new VersionReponse(version.getId(), version.getLibelle(), version.isActive(),
                        version.getCreeeLe(), seanceRepository.countByVersionId(version.getId())))
                .toList();
    }

    @PostMapping("/versions/{id}/activer")
    public VersionReponse activer(@AuthenticationPrincipal UtilisateurCourant courant, @PathVariable Long id) {
        PlanningVersion version = planningVersionService.activer(ContexteEcole.etablissementId(courant), id);
        return new VersionReponse(version.getId(), version.getLibelle(), version.isActive(),
                version.getCreeeLe(), seanceRepository.countByVersionId(version.getId()));
    }

    // ---------- Vues planning ----------

    /** Planning d'un groupe, séances de ses sous-groupes incluses (A-04/B-02). */
    @GetMapping("/groupe/{groupeId}")
    public PlanningReponse planningGroupe(@AuthenticationPrincipal UtilisateurCourant courant,
                                          @PathVariable Long groupeId,
                                          @RequestParam(name = "versionId", required = false) Long versionId,
                                          @RequestParam(name = "semaine", defaultValue = "TOUTES")
                                          String semaine) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        Groupe groupe = groupeService.obtenir(etablissementId, groupeId);
        List<Long> groupeIds = new ArrayList<>();
        groupeIds.add(groupe.getId());
        for (Groupe enfant : groupeRepository.findByParentId(groupe.getId())) {
            groupeIds.add(enfant.getId());
        }
        PlanningVersion version = resoudreVersion(etablissementId, versionId);
        List<Seance> seances = version == null
                ? List.of()
                : seanceRepository.findByVersionIdAndGroupeIdIn(version.getId(), groupeIds);
        return versPlanning(etablissementId, seances, semaine);
    }

    @GetMapping("/enseignant/{enseignantId}")
    public PlanningReponse planningEnseignant(@AuthenticationPrincipal UtilisateurCourant courant,
                                              @PathVariable Long enseignantId,
                                              @RequestParam(name = "versionId", required = false) Long versionId,
                                              @RequestParam(name = "semaine", defaultValue = "TOUTES")
                                              String semaine) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        enseignantService.obtenir(etablissementId, enseignantId);
        PlanningVersion version = resoudreVersion(etablissementId, versionId);
        List<Seance> seances = version == null
                ? List.of()
                : seanceRepository.findByVersionIdAndEnseignantId(version.getId(), enseignantId);
        return versPlanning(etablissementId, seances, semaine);
    }

    @GetMapping("/salle/{salleId}")
    public PlanningReponse planningSalle(@AuthenticationPrincipal UtilisateurCourant courant,
                                         @PathVariable Long salleId,
                                         @RequestParam(name = "versionId", required = false) Long versionId,
                                         @RequestParam(name = "semaine", defaultValue = "TOUTES")
                                         String semaine) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        salleService.obtenir(etablissementId, salleId);
        PlanningVersion version = resoudreVersion(etablissementId, versionId);
        List<Seance> seances = version == null
                ? List.of()
                : seanceRepository.findByVersionIdAndSalleId(version.getId(), salleId);
        return versPlanning(etablissementId, seances, semaine);
    }

    // ---------- Interne ----------

    private PlanningVersion resoudreVersion(Long etablissementId, Long versionId) {
        if (versionId != null) {
            return planningVersionService.obtenir(etablissementId, versionId);
        }
        return planningVersionRepository.findByEtablissementIdAndActiveTrue(etablissementId).stream()
                .findFirst()
                .orElse(null);
    }

    private PlanningReponse versPlanning(Long etablissementId, List<Seance> seances, String semaine) {
        List<CreneauReponse> creneaux = grilleService.listerCreneaux(etablissementId).stream()
                .map(MappeursPlanning::versCreneauReponse)
                .toList();
        List<SeanceReponse> reponses = seances.stream()
                .filter(seance -> filtreSemaine(seance, semaine))
                .map(MappeursPlanning::versSeanceReponse)
                .toList();
        return new PlanningReponse(creneaux, reponses);
    }

    /** Filtre C-05 : la vue A affiche TOUTES + A, la vue B affiche TOUTES + B. */
    private static boolean filtreSemaine(Seance seance, String semaine) {
        if (semaine == null || "TOUTES".equalsIgnoreCase(semaine)) {
            return true;
        }
        if ("A".equalsIgnoreCase(semaine)) {
            return seance.getSemaine() != SemaineSeance.B;
        }
        if ("B".equalsIgnoreCase(semaine)) {
            return seance.getSemaine() != SemaineSeance.A;
        }
        return true;
    }
}
