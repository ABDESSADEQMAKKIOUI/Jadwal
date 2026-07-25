package ma.jadwal.api.controleur;

import ma.jadwal.api.dto.Mappeurs;
import ma.jadwal.api.dto.UtilisateurReponse;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.referentiel.entite.Utilisateur;
import ma.jadwal.referentiel.service.UtilisateurService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    private final UtilisateurService utilisateurService;

    public MeController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping("/api/me")
    public UtilisateurReponse moi(@AuthenticationPrincipal UtilisateurCourant courant) {
        Utilisateur utilisateur = utilisateurService.obtenir(courant.id());
        return Mappeurs.versUtilisateurReponse(utilisateur);
    }
}
