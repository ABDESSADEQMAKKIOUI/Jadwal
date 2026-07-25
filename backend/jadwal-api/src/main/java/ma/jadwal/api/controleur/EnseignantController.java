package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.enseignant.depot.HabilitationRepository;
import ma.jadwal.enseignant.entite.Enseignant;
import ma.jadwal.enseignant.entite.Habilitation;
import ma.jadwal.enseignant.entite.Indisponibilite;
import ma.jadwal.enseignant.entite.PreferenceHoraire;
import ma.jadwal.enseignant.entite.Semaine;
import ma.jadwal.enseignant.entite.SourceIndispo;
import ma.jadwal.enseignant.entite.StatutIndispo;
import ma.jadwal.enseignant.entite.TypeEnseignant;
import ma.jadwal.enseignant.entite.TypePreference;
import ma.jadwal.enseignant.service.EnseignantService;
import ma.jadwal.planning.depot.PlanningVersionRepository;
import ma.jadwal.planning.depot.SeanceRepository;
import ma.jadwal.planning.entite.PlanningVersion;
import ma.jadwal.planning.entite.Seance;
import ma.jadwal.referentiel.entite.Jour;
import ma.jadwal.referentiel.entite.Niveau;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Enseignants : CRUD, habilitations (D-02), indisponibilités (D-03/D-05/D-06)
 * et préférences horaires (D-10).
 */
@RestController
@RequestMapping("/api/ecole/enseignants")
public class EnseignantController {

    public record EnseignantReponse(Long id, String nomComplet, String email, String type,
                                    int quotaHebdoUnites, int maxConsecutifUnites, Integer amplitudeMaxUnites,
                                    boolean vacataire, int bufferTrajetUnites, List<String> matieres,
                                    int chargeAffectee) {
    }

    public record CreationEnseignantRequete(@NotBlank String nomComplet, String email, TypeEnseignant type,
                                            @Positive int quotaHebdoUnites, Integer maxConsecutifUnites,
                                            Integer amplitudeMaxUnites, Boolean vacataire,
                                            Integer bufferTrajetUnites) {
    }

    public record ModificationEnseignantRequete(String nomComplet, String email, TypeEnseignant type,
                                                Integer quotaHebdoUnites, Integer maxConsecutifUnites,
                                                Integer amplitudeMaxUnites, Boolean vacataire,
                                                Integer bufferTrajetUnites) {
    }

    public record HabilitationDto(@NotNull Long matiereId, List<Long> niveauIds) {
    }

    public record IndispoReponse(Long id, String source, String jour, int indexDebut, int dureeUnites,
                                 String semaine, String statut, String motif) {
    }

    public record CreationIndispoRequete(SourceIndispo source, @NotNull Jour jour, int indexDebut,
                                         @Positive int dureeUnites, Semaine semaine, String motif) {
    }

    public record ModificationIndispoRequete(SourceIndispo source, Jour jour, Integer indexDebut,
                                             Integer dureeUnites, Semaine semaine, String motif,
                                             StatutIndispo statut) {
    }

    public record PreferenceDto(@NotNull Jour jour, int indexDebut, @Positive int dureeUnites,
                                @NotNull TypePreference type) {
    }

    private final EnseignantService enseignantService;
    private final HabilitationRepository habilitationRepository;
    private final PlanningVersionRepository planningVersionRepository;
    private final SeanceRepository seanceRepository;

    public EnseignantController(EnseignantService enseignantService,
                                HabilitationRepository habilitationRepository,
                                PlanningVersionRepository planningVersionRepository,
                                SeanceRepository seanceRepository) {
        this.enseignantService = enseignantService;
        this.habilitationRepository = habilitationRepository;
        this.planningVersionRepository = planningVersionRepository;
        this.seanceRepository = seanceRepository;
    }

    // ---------- Enseignants ----------

    @GetMapping
    public List<EnseignantReponse> lister(@AuthenticationPrincipal UtilisateurCourant courant) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        PlanningVersion active = planningVersionRepository
                .findByEtablissementIdAndActiveTrue(etablissementId).stream()
                .findFirst()
                .orElse(null);
        return enseignantService.lister(etablissementId).stream()
                .map(enseignant -> versReponse(enseignant, active))
                .toList();
    }

    @PostMapping
    public EnseignantReponse creer(@AuthenticationPrincipal UtilisateurCourant courant,
                                   @Valid @RequestBody CreationEnseignantRequete requete) {
        Enseignant enseignant = enseignantService.creer(ContexteEcole.etablissementId(courant),
                requete.nomComplet(), requete.email(),
                requete.type() == null ? TypeEnseignant.PROPRE : requete.type(),
                requete.quotaHebdoUnites(),
                requete.maxConsecutifUnites() == null ? 10 : requete.maxConsecutifUnites(),
                requete.amplitudeMaxUnites(),
                Boolean.TRUE.equals(requete.vacataire()),
                requete.bufferTrajetUnites() == null ? 1 : requete.bufferTrajetUnites());
        return versReponse(enseignant, null);
    }

    @PatchMapping("/{id}")
    public EnseignantReponse modifier(@AuthenticationPrincipal UtilisateurCourant courant,
                                      @PathVariable Long id,
                                      @RequestBody ModificationEnseignantRequete requete) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        Enseignant existant = enseignantService.obtenir(etablissementId, id);
        String email = requete.email() == null ? existant.getEmail() : requete.email();
        Integer amplitude = requete.amplitudeMaxUnites() == null
                ? existant.getAmplitudeMaxUnites() : requete.amplitudeMaxUnites();
        Enseignant enseignant = enseignantService.mettreAJour(etablissementId, id, requete.nomComplet(),
                email, requete.type(), requete.quotaHebdoUnites(), requete.maxConsecutifUnites(),
                amplitude, requete.vacataire(), requete.bufferTrajetUnites());
        return versReponse(enseignant, null);
    }

    @DeleteMapping("/{id}")
    public void supprimer(@AuthenticationPrincipal UtilisateurCourant courant, @PathVariable Long id) {
        enseignantService.supprimer(ContexteEcole.etablissementId(courant), id);
    }

    // ---------- Habilitations (D-02) ----------

    @GetMapping("/{id}/habilitations")
    public List<HabilitationDto> listerHabilitations(@AuthenticationPrincipal UtilisateurCourant courant,
                                                     @PathVariable Long id) {
        return enseignantService.listerHabilitations(ContexteEcole.etablissementId(courant), id).stream()
                .map(EnseignantController::versHabilitationDto)
                .toList();
    }

    @PutMapping("/{id}/habilitations")
    public List<HabilitationDto> remplacerHabilitations(@AuthenticationPrincipal UtilisateurCourant courant,
                                                        @PathVariable Long id,
                                                        @Valid @RequestBody List<HabilitationDto> demandes) {
        List<EnseignantService.HabilitationDemande> conversions = demandes.stream()
                .map(d -> new EnseignantService.HabilitationDemande(d.matiereId(), d.niveauIds()))
                .toList();
        return enseignantService.remplacerHabilitations(ContexteEcole.etablissementId(courant), id, conversions)
                .stream()
                .map(EnseignantController::versHabilitationDto)
                .toList();
    }

    // ---------- Indisponibilités (D-03/D-06) ----------

    @GetMapping("/{id}/indisponibilites")
    public List<IndispoReponse> listerIndisponibilites(@AuthenticationPrincipal UtilisateurCourant courant,
                                                       @PathVariable Long id) {
        return enseignantService.listerIndisponibilites(ContexteEcole.etablissementId(courant), id).stream()
                .map(EnseignantController::versIndispoReponse)
                .toList();
    }

    @PostMapping("/{id}/indisponibilites")
    public IndispoReponse creerIndisponibilite(@AuthenticationPrincipal UtilisateurCourant courant,
                                               @PathVariable Long id,
                                               @Valid @RequestBody CreationIndispoRequete requete) {
        Indisponibilite indisponibilite = enseignantService.creerIndisponibilite(
                ContexteEcole.etablissementId(courant), id, requete.source(), requete.jour(),
                requete.indexDebut(), requete.dureeUnites(), requete.semaine(), requete.motif());
        return versIndispoReponse(indisponibilite);
    }

    @PatchMapping("/{id}/indisponibilites/{indispoId}")
    public IndispoReponse modifierIndisponibilite(@AuthenticationPrincipal UtilisateurCourant courant,
                                                  @PathVariable Long id,
                                                  @PathVariable Long indispoId,
                                                  @RequestBody ModificationIndispoRequete requete) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        if (requete.statut() != null) {
            return versIndispoReponse(enseignantService.changerStatutIndisponibilite(
                    etablissementId, id, indispoId, requete.statut()));
        }
        Indisponibilite indisponibilite = enseignantService.mettreAJourIndisponibilite(etablissementId, id,
                indispoId, requete.source(), requete.jour(), requete.indexDebut(), requete.dureeUnites(),
                requete.semaine(), requete.motif());
        return versIndispoReponse(indisponibilite);
    }

    @DeleteMapping("/{id}/indisponibilites/{indispoId}")
    public void supprimerIndisponibilite(@AuthenticationPrincipal UtilisateurCourant courant,
                                         @PathVariable Long id, @PathVariable Long indispoId) {
        enseignantService.supprimerIndisponibilite(ContexteEcole.etablissementId(courant), id, indispoId);
    }

    // ---------- Préférences (D-10) ----------

    @GetMapping("/{id}/preferences")
    public List<PreferenceDto> listerPreferences(@AuthenticationPrincipal UtilisateurCourant courant,
                                                 @PathVariable Long id) {
        return enseignantService.listerPreferences(ContexteEcole.etablissementId(courant), id).stream()
                .map(EnseignantController::versPreferenceDto)
                .toList();
    }

    @PutMapping("/{id}/preferences")
    public List<PreferenceDto> remplacerPreferences(@AuthenticationPrincipal UtilisateurCourant courant,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody List<PreferenceDto> demandes) {
        List<EnseignantService.PreferenceDemande> conversions = demandes.stream()
                .map(d -> new EnseignantService.PreferenceDemande(d.jour(), d.indexDebut(), d.dureeUnites(),
                        d.type()))
                .toList();
        return enseignantService.remplacerPreferences(ContexteEcole.etablissementId(courant), id, conversions)
                .stream()
                .map(EnseignantController::versPreferenceDto)
                .toList();
    }

    // ---------- Conversions ----------

    private EnseignantReponse versReponse(Enseignant enseignant, PlanningVersion versionActive) {
        List<String> matieres = habilitationRepository.findByEnseignantId(enseignant.getId()).stream()
                .map(habilitation -> habilitation.getMatiere().getLibelle())
                .toList();
        int chargeAffectee = 0;
        if (versionActive != null) {
            chargeAffectee = seanceRepository
                    .findByVersionIdAndEnseignantId(versionActive.getId(), enseignant.getId()).stream()
                    .mapToInt(Seance::getDureeUnites)
                    .sum();
        }
        return new EnseignantReponse(enseignant.getId(), enseignant.getNomComplet(), enseignant.getEmail(),
                enseignant.getType().name(), enseignant.getQuotaHebdoUnites(),
                enseignant.getMaxConsecutifUnites(), enseignant.getAmplitudeMaxUnites(),
                enseignant.isVacataire(), enseignant.getBufferTrajetUnites(), matieres, chargeAffectee);
    }

    private static HabilitationDto versHabilitationDto(Habilitation habilitation) {
        List<Long> niveaux = new ArrayList<>();
        for (Niveau niveau : habilitation.getNiveaux()) {
            niveaux.add(niveau.getId());
        }
        return new HabilitationDto(habilitation.getMatiere().getId(), niveaux);
    }

    private static IndispoReponse versIndispoReponse(Indisponibilite indisponibilite) {
        return new IndispoReponse(indisponibilite.getId(), indisponibilite.getSource().name(),
                indisponibilite.getJour().name(), indisponibilite.getIndexDebut(),
                indisponibilite.getDureeUnites(), indisponibilite.getSemaine().name(),
                indisponibilite.getStatut().name(), indisponibilite.getMotif());
    }

    private static PreferenceDto versPreferenceDto(PreferenceHoraire preference) {
        return new PreferenceDto(preference.getJour(), preference.getIndexDebut(),
                preference.getDureeUnites(), preference.getType());
    }
}
