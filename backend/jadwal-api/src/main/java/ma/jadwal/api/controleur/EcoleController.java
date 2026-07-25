package ma.jadwal.api.controleur;

import ma.jadwal.abonnement.service.AbonnementService;
import ma.jadwal.abonnement.service.PaiementService;
import ma.jadwal.api.dto.AbonnementEcoleReponse;
import ma.jadwal.api.dto.EtablissementEcoleReponse;
import ma.jadwal.api.dto.Mappeurs;
import ma.jadwal.api.dto.PaiementReponse;
import ma.jadwal.api.dto.TableauBordEcoleReponse;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.common.exception.OperationNonAutoriseeException;
import ma.jadwal.referentiel.entite.Etablissement;
import ma.jadwal.referentiel.service.EtablissementService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/ecole")
public class EcoleController {

    private final EtablissementService etablissementService;
    private final AbonnementService abonnementService;
    private final PaiementService paiementService;

    public EcoleController(EtablissementService etablissementService,
                           AbonnementService abonnementService,
                           PaiementService paiementService) {
        this.etablissementService = etablissementService;
        this.abonnementService = abonnementService;
        this.paiementService = paiementService;
    }

    @GetMapping("/dashboard")
    public TableauBordEcoleReponse tableauDeBord(@AuthenticationPrincipal UtilisateurCourant courant) {
        // L'etablissementId provient exclusivement du JWT, jamais de la requête.
        Long etablissementId = courant == null ? null : courant.etablissementId();
        if (etablissementId == null) {
            throw new OperationNonAutoriseeException("Aucun établissement associé à ce compte");
        }
        Etablissement etablissement = etablissementService.obtenir(etablissementId);
        EtablissementEcoleReponse etablissementReponse = new EtablissementEcoleReponse(
                etablissement.getId(),
                etablissement.getNom(),
                etablissement.getCode(),
                etablissement.getVille(),
                etablissement.getTelephone(),
                etablissement.getEmail(),
                etablissement.getStatut().name());
        AbonnementEcoleReponse abonnementReponse = abonnementService.abonnementActif(etablissementId)
                .map(abonnement -> {
                    BigDecimal prixAnnuel = abonnement.getPlan().getPrixAnnuel();
                    BigDecimal totalPaye = paiementService.totalPaye(abonnement.getId());
                    BigDecimal resteAPayer = prixAnnuel.subtract(totalPaye).max(BigDecimal.ZERO);
                    return new AbonnementEcoleReponse(
                            abonnement.getId(),
                            abonnement.getPlan().getNom(),
                            prixAnnuel,
                            abonnement.getDateDebut(),
                            abonnement.getDateFin(),
                            abonnement.getStatut().name(),
                            totalPaye,
                            resteAPayer);
                })
                .orElse(null);
        List<PaiementReponse> paiements = paiementService.listerParEtablissement(etablissementId).stream()
                .map(Mappeurs::versPaiementReponse)
                .toList();
        return new TableauBordEcoleReponse(etablissementReponse, abonnementReponse, paiements);
    }
}
