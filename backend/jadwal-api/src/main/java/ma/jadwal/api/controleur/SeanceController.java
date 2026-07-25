package ma.jadwal.api.controleur;

import ma.jadwal.api.dto.MappeursPlanning;
import ma.jadwal.api.dto.SeanceReponse;
import ma.jadwal.api.erreur.ConflitsSeanceException;
import ma.jadwal.api.solveur.MappeurSolveur;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.enseignant.entite.Enseignant;
import ma.jadwal.enseignant.service.EnseignantService;
import ma.jadwal.planning.depot.SeanceRepository;
import ma.jadwal.planning.entite.Seance;
import ma.jadwal.referentiel.depot.CreneauRepository;
import ma.jadwal.referentiel.entite.Creneau;
import ma.jadwal.referentiel.entite.Salle;
import ma.jadwal.referentiel.service.GrilleConfig;
import ma.jadwal.referentiel.service.SalleService;
import ma.jadwal.solver.modele.EnseignantPlan;
import ma.jadwal.solver.modele.SeancePlan;
import ma.jadwal.solver.validation.ConflitDur;
import ma.jadwal.solver.validation.ValidationConflits;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Modification manuelle d'une séance : validation synchrone des règles dures (I-04)
 * via le module solveur, et verrouillage (I-03).
 */
@RestController
@RequestMapping("/api/ecole/seances")
public class SeanceController {

    public record ModificationSeanceRequete(Long creneauId, Long salleId, Long enseignantId) {
    }

    private final SeanceRepository seanceRepository;
    private final CreneauRepository creneauRepository;
    private final SalleService salleService;
    private final EnseignantService enseignantService;
    private final MappeurSolveur mappeurSolveur;
    private final ValidationConflits validationConflits = new ValidationConflits();

    public SeanceController(SeanceRepository seanceRepository,
                            CreneauRepository creneauRepository,
                            SalleService salleService,
                            EnseignantService enseignantService,
                            MappeurSolveur mappeurSolveur) {
        this.seanceRepository = seanceRepository;
        this.creneauRepository = creneauRepository;
        this.salleService = salleService;
        this.enseignantService = enseignantService;
        this.mappeurSolveur = mappeurSolveur;
    }

    @PatchMapping("/{id}")
    @Transactional
    public SeanceReponse modifier(@AuthenticationPrincipal UtilisateurCourant courant,
                                  @PathVariable Long id,
                                  @RequestBody ModificationSeanceRequete requete) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        Seance seance = obtenir(etablissementId, id);

        Creneau creneau = seance.getCreneau();
        if (requete.creneauId() != null) {
            creneau = creneauRepository.findById(requete.creneauId())
                    .filter(c -> c.getEtablissement().getId().equals(etablissementId))
                    .orElseThrow(() -> new RessourceIntrouvableException(
                            "Créneau introuvable : " + requete.creneauId()));
        }
        Salle salle = seance.getSalle();
        if (requete.salleId() != null) {
            salle = salleService.obtenir(etablissementId, requete.salleId());
        }
        Enseignant enseignant = seance.getEnseignant();
        if (requete.enseignantId() != null) {
            enseignant = enseignantService.obtenir(etablissementId, requete.enseignantId());
        }

        // I-04 : validation synchrone contre les règles dures.
        GrilleConfig grille = mappeurSolveur.exigerGrille(etablissementId);
        Map<Long, EnseignantPlan> enseignants =
                mappeurSolveur.preparerEnseignants(etablissementId, grille.unitesParJour());
        Map<String, Integer> maxParJour = mappeurSolveur.maxParJourParNiveauMatiere(etablissementId);
        List<SeancePlan> existantes =
                mappeurSolveur.mapperSeancesVersion(etablissementId, seance.getVersion().getId());

        SeancePlan candidate = mappeurSolveur.versSeancePlan(seance, grille, enseignants, maxParJour);
        if (creneau != null) {
            candidate.setCreneau(mappeurSolveur.versCreneauPlan(creneau, grille));
        }
        candidate.setSalle(salle == null ? null : MappeurSolveur.versSallePlan(salle));
        candidate.setEnseignant(enseignant == null ? null : enseignants.get(enseignant.getId()));

        List<ConflitDur> conflits = validationConflits.verifier(candidate, existantes);
        if (!conflits.isEmpty()) {
            throw new ConflitsSeanceException(conflits);
        }

        seance.setCreneau(creneau);
        seance.setSalle(salle);
        seance.setEnseignant(enseignant);
        return MappeursPlanning.versSeanceReponse(seanceRepository.save(seance));
    }

    /** I-03 : le solveur ne déplacera plus cette séance (@PlanningPin). */
    @PostMapping("/{id}/verrouiller")
    public SeanceReponse verrouiller(@AuthenticationPrincipal UtilisateurCourant courant,
                                     @PathVariable Long id) {
        return changerVerrou(ContexteEcole.etablissementId(courant), id, true);
    }

    @PostMapping("/{id}/deverrouiller")
    public SeanceReponse deverrouiller(@AuthenticationPrincipal UtilisateurCourant courant,
                                       @PathVariable Long id) {
        return changerVerrou(ContexteEcole.etablissementId(courant), id, false);
    }

    private SeanceReponse changerVerrou(Long etablissementId, Long id, boolean verrouillee) {
        Seance seance = obtenir(etablissementId, id);
        seance.setVerrouillee(verrouillee);
        return MappeursPlanning.versSeanceReponse(seanceRepository.save(seance));
    }

    private Seance obtenir(Long etablissementId, Long id) {
        return seanceRepository.findByIdAndEtablissementId(id, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Séance introuvable : " + id));
    }
}
