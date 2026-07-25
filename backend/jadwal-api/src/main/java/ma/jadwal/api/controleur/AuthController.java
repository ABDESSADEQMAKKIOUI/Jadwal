package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import ma.jadwal.api.dto.ConnexionReponse;
import ma.jadwal.api.dto.ConnexionRequete;
import ma.jadwal.api.dto.Mappeurs;
import ma.jadwal.api.securite.IdentifiantsInvalidesException;
import ma.jadwal.api.securite.JwtService;
import ma.jadwal.common.exception.OperationNonAutoriseeException;
import ma.jadwal.referentiel.entite.Role;
import ma.jadwal.referentiel.entite.StatutEtablissement;
import ma.jadwal.referentiel.entite.Utilisateur;
import ma.jadwal.referentiel.service.UtilisateurService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UtilisateurService utilisateurService;
    private final JwtService jwtService;

    public AuthController(UtilisateurService utilisateurService, JwtService jwtService) {
        this.utilisateurService = utilisateurService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ConnexionReponse connexion(@Valid @RequestBody ConnexionRequete requete) {
        Utilisateur utilisateur = utilisateurService.chercherParEmail(requete.email())
                .orElseThrow(IdentifiantsInvalidesException::new);
        if (!utilisateur.isActif() || !utilisateurService.verifierMotDePasse(utilisateur, requete.motDePasse())) {
            throw new IdentifiantsInvalidesException();
        }
        if (utilisateur.getRole() == Role.DIRECTEUR
                && utilisateur.getEtablissement() != null
                && utilisateur.getEtablissement().getStatut() == StatutEtablissement.SUSPENDU) {
            throw new OperationNonAutoriseeException("Établissement suspendu");
        }
        String token = jwtService.genererToken(
                utilisateur.getId(),
                utilisateur.getEmail(),
                utilisateur.getRole().name(),
                utilisateur.getEtablissement() == null ? null : utilisateur.getEtablissement().getId(),
                utilisateur.getNomComplet());
        return new ConnexionReponse(token, Mappeurs.versUtilisateurReponse(utilisateur));
    }
}
