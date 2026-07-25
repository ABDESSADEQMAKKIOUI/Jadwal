package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import ma.jadwal.api.solveur.MappeurSolveur;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.pedagogie.service.PonderationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Pondérations des contraintes souples par établissement (I-01).
 */
@RestController
@RequestMapping("/api/ecole/ponderations")
public class PonderationController {

    public record PonderationReponse(String regle, String libelle, String famille, int poids) {
    }

    public record PoidsRequete(@NotBlank String regle, @Min(0) @Max(100) int poids) {
    }

    private final PonderationService ponderationService;

    public PonderationController(PonderationService ponderationService) {
        this.ponderationService = ponderationService;
    }

    @GetMapping
    public List<PonderationReponse> lister(@AuthenticationPrincipal UtilisateurCourant courant) {
        return ponderationService.listerReglesEffectives(ContexteEcole.etablissementId(courant)).stream()
                .map(PonderationController::versReponse)
                .toList();
    }

    @PutMapping
    public List<PonderationReponse> remplacer(@AuthenticationPrincipal UtilisateurCourant courant,
                                              @Valid @RequestBody List<PoidsRequete> requetes) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        for (PoidsRequete requete : requetes) {
            ponderationService.definirPoids(etablissementId, requete.regle(), requete.poids());
        }
        return lister(courant);
    }

    private static PonderationReponse versReponse(PonderationService.ReglePonderee regle) {
        return new PonderationReponse(regle.regle(), regle.libelle(), familleDe(regle.regle()), regle.poids());
    }

    /** Famille d'affichage dérivée du niveau soft de la règle (S0 groupes, S1 pédagogie, S2 enseignants). */
    private static String familleDe(String regle) {
        Integer niveau = MappeurSolveur.niveauSoft(regle);
        if (niveau == null) {
            return "Autres";
        }
        return switch (niveau) {
            case 0 -> "Confort des groupes";
            case 1 -> "Pédagogie";
            default -> "Confort des enseignants";
        };
    }
}
