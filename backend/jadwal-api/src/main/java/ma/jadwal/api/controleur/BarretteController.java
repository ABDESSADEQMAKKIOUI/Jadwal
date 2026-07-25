package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.referentiel.entite.Barrette;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.referentiel.service.BarretteService;
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
 * Barrettes : séances alignées entre groupes sur une matière (B-05).
 */
@RestController
@RequestMapping("/api/ecole/barrettes")
public class BarretteController {

    public record BarretteReponse(Long id, String libelle, Long matiereId, String matiereLibelle,
                                  List<Long> groupeIds) {
    }

    public record CreationBarretteRequete(@NotBlank String libelle, @NotNull Long matiereId,
                                          @NotEmpty List<Long> groupeIds) {
    }

    private final BarretteService barretteService;

    public BarretteController(BarretteService barretteService) {
        this.barretteService = barretteService;
    }

    @GetMapping
    public List<BarretteReponse> lister(@AuthenticationPrincipal UtilisateurCourant courant) {
        return barretteService.lister(ContexteEcole.etablissementId(courant)).stream()
                .map(BarretteController::versReponse)
                .toList();
    }

    @PostMapping
    public BarretteReponse creer(@AuthenticationPrincipal UtilisateurCourant courant,
                                 @Valid @RequestBody CreationBarretteRequete requete) {
        Barrette barrette = barretteService.creer(ContexteEcole.etablissementId(courant), requete.libelle(),
                requete.matiereId(), requete.groupeIds());
        return versReponse(barrette);
    }

    @DeleteMapping("/{id}")
    public void supprimer(@AuthenticationPrincipal UtilisateurCourant courant, @PathVariable Long id) {
        barretteService.supprimer(ContexteEcole.etablissementId(courant), id);
    }

    private static BarretteReponse versReponse(Barrette barrette) {
        return new BarretteReponse(barrette.getId(), barrette.getLibelle(), barrette.getMatiere().getId(),
                barrette.getMatiere().getLibelle(),
                barrette.getGroupes().stream().map(Groupe::getId).toList());
    }
}
