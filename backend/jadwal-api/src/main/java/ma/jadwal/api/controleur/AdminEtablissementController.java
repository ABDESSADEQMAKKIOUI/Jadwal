package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import ma.jadwal.abonnement.service.AbonnementService;
import ma.jadwal.abonnement.service.PaiementService;
import ma.jadwal.api.dto.AbonnementEtablissementReponse;
import ma.jadwal.api.dto.CompteReponse;
import ma.jadwal.api.dto.CreationCompteRequete;
import ma.jadwal.api.dto.CreationEtablissementRequete;
import ma.jadwal.api.dto.EtablissementDetailReponse;
import ma.jadwal.api.dto.EtablissementResumeReponse;
import ma.jadwal.api.dto.Mappeurs;
import ma.jadwal.api.dto.ModificationEtablissementRequete;
import ma.jadwal.api.dto.PaiementReponse;
import ma.jadwal.referentiel.entite.Etablissement;
import ma.jadwal.referentiel.entite.Utilisateur;
import ma.jadwal.referentiel.service.EtablissementService;
import ma.jadwal.referentiel.service.UtilisateurService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/etablissements")
public class AdminEtablissementController {

    private final EtablissementService etablissementService;
    private final UtilisateurService utilisateurService;
    private final AbonnementService abonnementService;
    private final PaiementService paiementService;

    public AdminEtablissementController(EtablissementService etablissementService,
                                        UtilisateurService utilisateurService,
                                        AbonnementService abonnementService,
                                        PaiementService paiementService) {
        this.etablissementService = etablissementService;
        this.utilisateurService = utilisateurService;
        this.abonnementService = abonnementService;
        this.paiementService = paiementService;
    }

    @GetMapping
    public List<EtablissementResumeReponse> lister() {
        return etablissementService.listerTout().stream()
                .map(etablissement -> Mappeurs.versEtablissementResumeReponse(
                        etablissement,
                        abonnementService.abonnementActif(etablissement.getId()).orElse(null)))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EtablissementResumeReponse creer(@Valid @RequestBody CreationEtablissementRequete requete) {
        Etablissement etablissement = etablissementService.creer(
                requete.nom(), requete.code(), requete.ville(), requete.telephone(), requete.email());
        return Mappeurs.versEtablissementResumeReponse(etablissement, null);
    }

    @GetMapping("/{id}")
    public EtablissementDetailReponse detail(@PathVariable("id") Long id) {
        return construireDetail(etablissementService.obtenir(id));
    }

    @PatchMapping("/{id}")
    public EtablissementDetailReponse modifier(@PathVariable("id") Long id,
                                               @RequestBody ModificationEtablissementRequete requete) {
        Etablissement etablissement = etablissementService.mettreAJour(
                id, requete.nom(), requete.ville(), requete.telephone(), requete.email(), requete.statut());
        return construireDetail(etablissement);
    }

    @PostMapping("/{id}/comptes")
    @ResponseStatus(HttpStatus.CREATED)
    public CompteReponse creerCompte(@PathVariable("id") Long id,
                                     @Valid @RequestBody CreationCompteRequete requete) {
        Etablissement etablissement = etablissementService.obtenir(id);
        Utilisateur utilisateur = utilisateurService.creerDirecteur(
                etablissement, requete.email(), requete.nomComplet(), requete.motDePasse());
        return Mappeurs.versCompteReponse(utilisateur);
    }

    private EtablissementDetailReponse construireDetail(Etablissement etablissement) {
        List<CompteReponse> comptes = utilisateurService.listerParEtablissement(etablissement.getId()).stream()
                .map(Mappeurs::versCompteReponse)
                .toList();
        List<AbonnementEtablissementReponse> abonnements =
                abonnementService.listerParEtablissement(etablissement.getId()).stream()
                        .map(abonnement -> Mappeurs.versAbonnementEtablissementReponse(
                                abonnement, paiementService.totalPaye(abonnement.getId())))
                        .toList();
        List<PaiementReponse> paiements = paiementService.listerParEtablissement(etablissement.getId()).stream()
                .map(Mappeurs::versPaiementReponse)
                .toList();
        return new EtablissementDetailReponse(
                etablissement.getId(),
                etablissement.getNom(),
                etablissement.getCode(),
                etablissement.getVille(),
                etablissement.getTelephone(),
                etablissement.getEmail(),
                etablissement.getStatut().name(),
                etablissement.getDateCreation(),
                comptes,
                abonnements,
                paiements);
    }
}
