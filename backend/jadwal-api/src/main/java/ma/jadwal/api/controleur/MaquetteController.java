package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import ma.jadwal.api.solveur.MappeurSolveur;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.pedagogie.entite.Dedoublement;
import ma.jadwal.pedagogie.entite.Maquette;
import ma.jadwal.pedagogie.entite.VolumeOverride;
import ma.jadwal.pedagogie.service.MaquetteService;
import ma.jadwal.referentiel.depot.GroupeRepository;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.pedagogie.depot.VolumeOverrideRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Maquettes pédagogiques par niveau (C-01/C-02/C-05/C-07) et volumes spécifiques
 * par groupe (overrides A-03).
 */
@RestController
@RequestMapping("/api/ecole")
public class MaquetteController {

    public record MaquetteReponse(Long id, Long niveauId, Long matiereId, String matiereLibelle,
                                  int volumeUnites, Integer volumeUnitesB, int maxParJourUnites,
                                  String dedoublement, int nbSousGroupes, int coEnseignants,
                                  List<List<Integer>> patterns) {
    }

    public record LigneRequete(@NotNull Long matiereId, @PositiveOrZero int volumeUnites, Integer volumeUnitesB,
                               Integer maxParJourUnites, Dedoublement dedoublement, Integer nbSousGroupes,
                               Integer coEnseignants, List<List<Integer>> patterns) {
    }

    public record MaquetteNiveauRequete(@NotNull Long niveauId, @Valid List<LigneRequete> lignes) {
    }

    public record VolumeOverrideDto(@NotNull Long groupeId, @NotNull Long matiereId,
                                    @Positive int volumeUnites) {
    }

    private final MaquetteService maquetteService;
    private final MappeurSolveur mappeurSolveur;
    private final GroupeRepository groupeRepository;
    private final VolumeOverrideRepository volumeOverrideRepository;

    public MaquetteController(MaquetteService maquetteService,
                              MappeurSolveur mappeurSolveur,
                              GroupeRepository groupeRepository,
                              VolumeOverrideRepository volumeOverrideRepository) {
        this.maquetteService = maquetteService;
        this.mappeurSolveur = mappeurSolveur;
        this.groupeRepository = groupeRepository;
        this.volumeOverrideRepository = volumeOverrideRepository;
    }

    // ---------- Maquettes ----------

    @GetMapping("/maquettes")
    public List<MaquetteReponse> lister(@AuthenticationPrincipal UtilisateurCourant courant,
                                        @RequestParam(name = "niveauId", required = false) Long niveauId) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        List<Maquette> maquettes = niveauId == null
                ? maquetteService.lister(etablissementId)
                : maquetteService.listerParNiveau(etablissementId, niveauId);
        return maquettes.stream().map(this::versReponse).toList();
    }

    /** Remplace intégralement la maquette du niveau. */
    @PutMapping("/maquettes")
    public List<MaquetteReponse> remplacer(@AuthenticationPrincipal UtilisateurCourant courant,
                                           @Valid @RequestBody MaquetteNiveauRequete requete) {
        List<MaquetteService.LigneMaquette> lignes = requete.lignes() == null
                ? List.of()
                : requete.lignes().stream()
                        .map(ligne -> new MaquetteService.LigneMaquette(
                                ligne.matiereId(),
                                ligne.volumeUnites(),
                                ligne.volumeUnitesB(),
                                ligne.maxParJourUnites() == null ? 4 : ligne.maxParJourUnites(),
                                ligne.dedoublement() == null ? Dedoublement.AUCUN : ligne.dedoublement(),
                                ligne.nbSousGroupes() == null ? 2 : ligne.nbSousGroupes(),
                                ligne.coEnseignants() == null ? 1 : ligne.coEnseignants(),
                                mappeurSolveur.patternsVersJson(ligne.patterns())))
                        .toList();
        return maquetteService.remplacerMaquetteNiveau(ContexteEcole.etablissementId(courant),
                        requete.niveauId(), lignes).stream()
                .map(this::versReponse)
                .toList();
    }

    // ---------- Volumes spécifiques par groupe (A-03) ----------

    @GetMapping("/volume-overrides")
    public List<VolumeOverrideDto> listerOverrides(@AuthenticationPrincipal UtilisateurCourant courant) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        List<Long> groupeIds = groupeRepository.findByEtablissementIdOrderByLibelleAsc(etablissementId).stream()
                .map(Groupe::getId)
                .toList();
        if (groupeIds.isEmpty()) {
            return List.of();
        }
        return volumeOverrideRepository.findByGroupeIdIn(groupeIds).stream()
                .map(MaquetteController::versOverrideDto)
                .toList();
    }

    @PostMapping("/volume-overrides")
    public VolumeOverrideDto definirOverride(@AuthenticationPrincipal UtilisateurCourant courant,
                                             @Valid @RequestBody VolumeOverrideDto requete) {
        VolumeOverride override = maquetteService.definirVolumeGroupe(ContexteEcole.etablissementId(courant),
                requete.groupeId(), requete.matiereId(), requete.volumeUnites());
        return versOverrideDto(override);
    }

    @PutMapping("/volume-overrides")
    public VolumeOverrideDto remplacerOverride(@AuthenticationPrincipal UtilisateurCourant courant,
                                               @Valid @RequestBody VolumeOverrideDto requete) {
        return definirOverride(courant, requete);
    }

    @DeleteMapping("/volume-overrides")
    public void supprimerOverride(@AuthenticationPrincipal UtilisateurCourant courant,
                                  @RequestParam Long groupeId,
                                  @RequestParam Long matiereId) {
        maquetteService.supprimerVolumeGroupe(ContexteEcole.etablissementId(courant), groupeId, matiereId);
    }

    // ---------- Conversions ----------

    private MaquetteReponse versReponse(Maquette maquette) {
        return new MaquetteReponse(maquette.getId(), maquette.getNiveau().getId(),
                maquette.getMatiere().getId(), maquette.getMatiere().getLibelle(),
                maquette.getVolumeUnites(), maquette.getVolumeUnitesB(), maquette.getMaxParJourUnites(),
                maquette.getDedoublement().name(), maquette.getNbSousGroupes(), maquette.getCoEnseignants(),
                mappeurSolveur.parsePatterns(maquette.getPatternsJson()));
    }

    private static VolumeOverrideDto versOverrideDto(VolumeOverride override) {
        return new VolumeOverrideDto(override.getGroupe().getId(), override.getMatiere().getId(),
                override.getVolumeUnites());
    }
}
