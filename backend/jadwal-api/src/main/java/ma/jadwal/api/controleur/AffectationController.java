package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.pedagogie.entite.Affectation;
import ma.jadwal.pedagogie.service.AffectationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Affectations imposées (groupe, matière, enseignant) — C-06.
 */
@RestController
@RequestMapping("/api/ecole/affectations")
public class AffectationController {

    public record AffectationReponse(Long id, Long groupeId, String groupeLibelle, Long matiereId,
                                     String matiereLibelle, Long enseignantId, String enseignantNom,
                                     Integer volumeUnites) {
    }

    public record CreationAffectationRequete(@NotNull Long groupeId, @NotNull Long matiereId,
                                             @NotNull Long enseignantId, Integer volumeUnites) {
    }

    private final AffectationService affectationService;

    public AffectationController(AffectationService affectationService) {
        this.affectationService = affectationService;
    }

    @GetMapping
    public List<AffectationReponse> lister(@AuthenticationPrincipal UtilisateurCourant courant) {
        return affectationService.lister(ContexteEcole.etablissementId(courant)).stream()
                .map(AffectationController::versReponse)
                .toList();
    }

    @PostMapping
    public AffectationReponse creer(@AuthenticationPrincipal UtilisateurCourant courant,
                                    @Valid @RequestBody CreationAffectationRequete requete) {
        Affectation affectation = affectationService.creer(ContexteEcole.etablissementId(courant),
                requete.groupeId(), requete.matiereId(), requete.enseignantId(), requete.volumeUnites());
        return versReponse(affectation);
    }

    @DeleteMapping("/{id}")
    public void supprimer(@AuthenticationPrincipal UtilisateurCourant courant, @PathVariable Long id) {
        affectationService.supprimer(ContexteEcole.etablissementId(courant), id);
    }

    private static AffectationReponse versReponse(Affectation affectation) {
        return new AffectationReponse(affectation.getId(), affectation.getGroupe().getId(),
                affectation.getGroupe().getLibelle(), affectation.getMatiere().getId(),
                affectation.getMatiere().getLibelle(), affectation.getEnseignant().getId(),
                affectation.getEnseignant().getNomComplet(), affectation.getVolumeUnites());
    }
}
