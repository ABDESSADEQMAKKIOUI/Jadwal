package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import ma.jadwal.api.solveur.MappeurSolveur;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.referentiel.entite.Matiere;
import ma.jadwal.referentiel.service.MatiereService;
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
 * Matières de l'établissement. equipementsRequis est stocké en CSV et exposé en tableau JSON.
 */
@RestController
@RequestMapping("/api/ecole/matieres")
public class MatiereController {

    public record MatiereReponse(Long id, String libelle, String code, int coefficient, int poidsCognitif,
                                 String couleur, String typeSalleRequis, List<String> equipementsRequis,
                                 int dureeMinUnites, int dureeMaxUnites, boolean eviterAvantDejeuner,
                                 boolean eviterFinJournee) {
    }

    public record CreationMatiereRequete(@NotBlank String libelle, @NotBlank String code,
                                         Integer coefficient, Integer poidsCognitif, String couleur,
                                         String typeSalleRequis, List<String> equipementsRequis,
                                         Integer dureeMinUnites, Integer dureeMaxUnites,
                                         Boolean eviterAvantDejeuner, Boolean eviterFinJournee) {
    }

    public record ModificationMatiereRequete(String libelle, String code, Integer coefficient,
                                             Integer poidsCognitif, String couleur, String typeSalleRequis,
                                             List<String> equipementsRequis, Integer dureeMinUnites,
                                             Integer dureeMaxUnites, Boolean eviterAvantDejeuner,
                                             Boolean eviterFinJournee) {
    }

    private final MatiereService matiereService;

    public MatiereController(MatiereService matiereService) {
        this.matiereService = matiereService;
    }

    @GetMapping
    public List<MatiereReponse> lister(@AuthenticationPrincipal UtilisateurCourant courant) {
        return matiereService.lister(ContexteEcole.etablissementId(courant)).stream()
                .map(MatiereController::versReponse)
                .toList();
    }

    @PostMapping
    public MatiereReponse creer(@AuthenticationPrincipal UtilisateurCourant courant,
                                @Valid @RequestBody CreationMatiereRequete requete) {
        Matiere donnees = new Matiere();
        donnees.setLibelle(requete.libelle());
        donnees.setCode(requete.code());
        donnees.setCoefficient(requete.coefficient() == null ? 1 : requete.coefficient());
        donnees.setPoidsCognitif(requete.poidsCognitif() == null ? 3 : requete.poidsCognitif());
        if (requete.couleur() != null) {
            donnees.setCouleur(requete.couleur());
        }
        donnees.setTypeSalleRequis(vide(requete.typeSalleRequis()));
        donnees.setEquipementsRequis(MappeurSolveur.listeVersCsv(requete.equipementsRequis()));
        donnees.setDureeMinUnites(requete.dureeMinUnites() == null ? 1 : requete.dureeMinUnites());
        donnees.setDureeMaxUnites(requete.dureeMaxUnites() == null ? 4 : requete.dureeMaxUnites());
        donnees.setEviterAvantDejeuner(Boolean.TRUE.equals(requete.eviterAvantDejeuner()));
        donnees.setEviterFinJournee(Boolean.TRUE.equals(requete.eviterFinJournee()));
        return versReponse(matiereService.creer(ContexteEcole.etablissementId(courant), donnees));
    }

    @PatchMapping("/{id}")
    public MatiereReponse modifier(@AuthenticationPrincipal UtilisateurCourant courant,
                                   @PathVariable Long id,
                                   @RequestBody ModificationMatiereRequete requete) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        Matiere existante = matiereService.obtenir(etablissementId, id);
        Matiere donnees = new Matiere();
        donnees.setLibelle(requete.libelle());
        donnees.setCode(requete.code());
        donnees.setCoefficient(requete.coefficient() == null ? existante.getCoefficient() : requete.coefficient());
        donnees.setPoidsCognitif(requete.poidsCognitif() == null
                ? existante.getPoidsCognitif() : requete.poidsCognitif());
        donnees.setCouleur(requete.couleur());
        donnees.setTypeSalleRequis(requete.typeSalleRequis() == null
                ? existante.getTypeSalleRequis() : vide(requete.typeSalleRequis()));
        donnees.setEquipementsRequis(requete.equipementsRequis() == null
                ? existante.getEquipementsRequis() : MappeurSolveur.listeVersCsv(requete.equipementsRequis()));
        donnees.setDureeMinUnites(requete.dureeMinUnites() == null
                ? existante.getDureeMinUnites() : requete.dureeMinUnites());
        donnees.setDureeMaxUnites(requete.dureeMaxUnites() == null
                ? existante.getDureeMaxUnites() : requete.dureeMaxUnites());
        donnees.setEviterAvantDejeuner(requete.eviterAvantDejeuner() == null
                ? existante.isEviterAvantDejeuner() : requete.eviterAvantDejeuner());
        donnees.setEviterFinJournee(requete.eviterFinJournee() == null
                ? existante.isEviterFinJournee() : requete.eviterFinJournee());
        return versReponse(matiereService.mettreAJour(etablissementId, id, donnees));
    }

    @DeleteMapping("/{id}")
    public void supprimer(@AuthenticationPrincipal UtilisateurCourant courant, @PathVariable Long id) {
        matiereService.supprimer(ContexteEcole.etablissementId(courant), id);
    }

    private static String vide(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur;
    }

    static MatiereReponse versReponse(Matiere matiere) {
        return new MatiereReponse(matiere.getId(), matiere.getLibelle(), matiere.getCode(),
                matiere.getCoefficient(), matiere.getPoidsCognitif(), matiere.getCouleur(),
                matiere.getTypeSalleRequis(),
                new ArrayList<>(MappeurSolveur.csvVersEnsemble(matiere.getEquipementsRequis())),
                matiere.getDureeMinUnites(), matiere.getDureeMaxUnites(),
                matiere.isEviterAvantDejeuner(), matiere.isEviterFinJournee());
    }
}
