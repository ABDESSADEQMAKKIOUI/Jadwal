package ma.jadwal.api.controleur;

import ma.jadwal.api.generation.GenerationService;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.solver.faisabilite.FaisabiliteRapport;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bilan de faisabilité avant calcul (H-01..H-06).
 */
@RestController
@RequestMapping("/api/ecole/faisabilite")
public class FaisabiliteController {

    private final GenerationService generationService;

    public FaisabiliteController(GenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping
    public FaisabiliteRapport verifier(@AuthenticationPrincipal UtilisateurCourant courant) {
        return generationService.verifierFaisabilite(ContexteEcole.etablissementId(courant));
    }
}
