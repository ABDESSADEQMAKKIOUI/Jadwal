package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import ma.jadwal.api.solveur.MappeurSolveur;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.referentiel.entite.Salle;
import ma.jadwal.referentiel.service.SalleService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Salles de l'établissement. equipements est stocké en CSV et exposé en tableau JSON.
 */
@RestController
@RequestMapping("/api/ecole/salles")
public class SalleController {

    public record SalleReponse(Long id, String nom, int capacite, String type, List<String> equipements,
                               String batiment) {
    }

    public record CreationSalleRequete(@NotBlank String nom, @Positive int capacite, String type,
                                       List<String> equipements, String batiment) {
    }

    public record ModificationSalleRequete(String nom, Integer capacite, String type,
                                           List<String> equipements, String batiment) {
    }

    private final SalleService salleService;

    public SalleController(SalleService salleService) {
        this.salleService = salleService;
    }

    @GetMapping
    public List<SalleReponse> lister(@AuthenticationPrincipal UtilisateurCourant courant) {
        return salleService.lister(ContexteEcole.etablissementId(courant)).stream()
                .map(SalleController::versReponse)
                .toList();
    }

    @PostMapping
    public SalleReponse creer(@AuthenticationPrincipal UtilisateurCourant courant,
                              @Valid @RequestBody CreationSalleRequete requete) {
        Salle salle = salleService.creer(ContexteEcole.etablissementId(courant), requete.nom(),
                requete.capacite(), requete.type(), MappeurSolveur.listeVersCsv(requete.equipements()),
                requete.batiment());
        return versReponse(salle);
    }

    @PatchMapping("/{id}")
    public SalleReponse modifier(@AuthenticationPrincipal UtilisateurCourant courant,
                                 @PathVariable Long id,
                                 @RequestBody ModificationSalleRequete requete) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        Salle existante = salleService.obtenir(etablissementId, id);
        String equipements = requete.equipements() == null
                ? existante.getEquipements() : MappeurSolveur.listeVersCsv(requete.equipements());
        String batiment = requete.batiment() == null ? existante.getBatiment() : requete.batiment();
        Salle salle = salleService.mettreAJour(etablissementId, id, requete.nom(), requete.capacite(),
                requete.type(), equipements, batiment);
        return versReponse(salle);
    }

    @DeleteMapping("/{id}")
    public void supprimer(@AuthenticationPrincipal UtilisateurCourant courant, @PathVariable Long id) {
        salleService.supprimer(ContexteEcole.etablissementId(courant), id);
    }

    static SalleReponse versReponse(Salle salle) {
        return new SalleReponse(salle.getId(), salle.getNom(), salle.getCapacite(), salle.getType(),
                new ArrayList<>(MappeurSolveur.csvVersEnsemble(salle.getEquipements())), salle.getBatiment());
    }
}
