package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.referentiel.entite.Niveau;
import ma.jadwal.referentiel.service.NiveauService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Niveaux scolaires de l'établissement.
 */
@RestController
@RequestMapping("/api/ecole/niveaux")
public class NiveauController {

    public record NiveauReponse(Long id, String libelle, String cycle, int ordre, Integer chargeMaxUnitesJour) {
    }

    public record CreationNiveauRequete(@NotBlank String libelle, String cycle, Integer ordre,
                                        Integer chargeMaxUnitesJour) {
    }

    public record ModificationNiveauRequete(String libelle, String cycle, Integer ordre,
                                            Integer chargeMaxUnitesJour) {
    }

    private final NiveauService niveauService;

    public NiveauController(NiveauService niveauService) {
        this.niveauService = niveauService;
    }

    @GetMapping
    public List<NiveauReponse> lister(@AuthenticationPrincipal UtilisateurCourant courant) {
        return niveauService.lister(ContexteEcole.etablissementId(courant)).stream()
                .map(NiveauController::versReponse)
                .toList();
    }

    @PostMapping
    public NiveauReponse creer(@AuthenticationPrincipal UtilisateurCourant courant,
                               @Valid @RequestBody CreationNiveauRequete requete) {
        Niveau niveau = niveauService.creer(ContexteEcole.etablissementId(courant), requete.libelle(),
                requete.cycle(), requete.ordre() == null ? 0 : requete.ordre(), requete.chargeMaxUnitesJour());
        return versReponse(niveau);
    }

    @PatchMapping("/{id}")
    public NiveauReponse modifier(@AuthenticationPrincipal UtilisateurCourant courant,
                                  @PathVariable Long id,
                                  @RequestBody ModificationNiveauRequete requete) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        Niveau existant = niveauService.obtenir(etablissementId, id);
        Niveau niveau = niveauService.mettreAJour(etablissementId, id,
                requete.libelle(),
                requete.cycle() != null ? requete.cycle() : existant.getCycle(),
                requete.ordre(),
                requete.chargeMaxUnitesJour() != null ? requete.chargeMaxUnitesJour()
                        : existant.getChargeMaxUnitesJour());
        return versReponse(niveau);
    }

    @DeleteMapping("/{id}")
    public void supprimer(@AuthenticationPrincipal UtilisateurCourant courant, @PathVariable Long id) {
        niveauService.supprimer(ContexteEcole.etablissementId(courant), id);
    }

    private static NiveauReponse versReponse(Niveau niveau) {
        return new NiveauReponse(niveau.getId(), niveau.getLibelle(), niveau.getCycle(), niveau.getOrdre(),
                niveau.getChargeMaxUnitesJour());
    }
}
