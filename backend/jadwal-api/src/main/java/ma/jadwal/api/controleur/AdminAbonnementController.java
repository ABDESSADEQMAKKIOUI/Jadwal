package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import ma.jadwal.abonnement.entite.Abonnement;
import ma.jadwal.abonnement.service.AbonnementService;
import ma.jadwal.abonnement.service.PaiementService;
import ma.jadwal.api.dto.AbonnementReponse;
import ma.jadwal.api.dto.CreationAbonnementRequete;
import ma.jadwal.api.dto.Mappeurs;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/abonnements")
public class AdminAbonnementController {

    private final AbonnementService abonnementService;
    private final PaiementService paiementService;

    public AdminAbonnementController(AbonnementService abonnementService, PaiementService paiementService) {
        this.abonnementService = abonnementService;
        this.paiementService = paiementService;
    }

    @GetMapping
    public List<AbonnementReponse> lister(
            @RequestParam(name = "etablissementId", required = false) Long etablissementId) {
        List<Abonnement> abonnements = etablissementId == null
                ? abonnementService.listerTout()
                : abonnementService.listerParEtablissement(etablissementId);
        return abonnements.stream()
                .map(abonnement -> Mappeurs.versAbonnementReponse(
                        abonnement, paiementService.totalPaye(abonnement.getId())))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AbonnementReponse creer(@Valid @RequestBody CreationAbonnementRequete requete) {
        Abonnement abonnement = abonnementService.creer(
                requete.etablissementId(), requete.planId(), requete.dateDebut(), requete.dateFin());
        return Mappeurs.versAbonnementReponse(abonnement, BigDecimal.ZERO);
    }
}
