package ma.jadwal.api.controleur;

import ma.jadwal.abonnement.entite.StatutAbonnement;
import ma.jadwal.abonnement.entite.StatutPaiement;
import ma.jadwal.abonnement.service.AbonnementService;
import ma.jadwal.abonnement.service.PaiementService;
import ma.jadwal.api.dto.TableauBordAdminReponse;
import ma.jadwal.referentiel.service.EtablissementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final EtablissementService etablissementService;
    private final AbonnementService abonnementService;
    private final PaiementService paiementService;

    public AdminDashboardController(EtablissementService etablissementService,
                                    AbonnementService abonnementService,
                                    PaiementService paiementService) {
        this.etablissementService = etablissementService;
        this.abonnementService = abonnementService;
        this.paiementService = paiementService;
    }

    @GetMapping("/dashboard")
    public TableauBordAdminReponse tableauDeBord() {
        int anneeCourante = LocalDate.now().getYear();
        return new TableauBordAdminReponse(
                etablissementService.compter(),
                abonnementService.compterParStatut(StatutAbonnement.ACTIF),
                paiementService.compterParStatut(StatutPaiement.EN_ATTENTE),
                paiementService.totalConfirmeAnnee(anneeCourante));
    }
}
