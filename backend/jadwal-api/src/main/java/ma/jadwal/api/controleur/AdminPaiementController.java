package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import ma.jadwal.abonnement.entite.Paiement;
import ma.jadwal.abonnement.entite.StatutPaiement;
import ma.jadwal.abonnement.service.PaiementService;
import ma.jadwal.api.dto.CreationPaiementRequete;
import ma.jadwal.api.dto.Mappeurs;
import ma.jadwal.api.dto.ModificationStatutPaiementRequete;
import ma.jadwal.api.dto.PaiementReponse;
import ma.jadwal.common.UtilisateurCourant;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/admin/paiements")
public class AdminPaiementController {

    private final PaiementService paiementService;

    public AdminPaiementController(PaiementService paiementService) {
        this.paiementService = paiementService;
    }

    @GetMapping
    public List<PaiementReponse> lister() {
        return paiementService.listerTout().stream()
                .map(Mappeurs::versPaiementReponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaiementReponse creer(@Valid @RequestBody CreationPaiementRequete requete,
                                 @AuthenticationPrincipal UtilisateurCourant courant) {
        Paiement paiement = paiementService.creer(
                requete.abonnementId(),
                requete.montant(),
                requete.mode(),
                requete.reference(),
                requete.datePaiement(),
                requete.note(),
                courant == null ? null : courant.id());
        return Mappeurs.versPaiementReponse(paiement);
    }

    @PatchMapping("/{id}")
    public PaiementReponse changerStatut(@PathVariable("id") Long id,
                                         @Valid @RequestBody ModificationStatutPaiementRequete requete) {
        if (requete.statut() != StatutPaiement.CONFIRME && requete.statut() != StatutPaiement.REJETE) {
            throw new IllegalArgumentException("Statut invalide : CONFIRME ou REJETE attendu");
        }
        Paiement paiement = paiementService.changerStatut(id, requete.statut());
        return Mappeurs.versPaiementReponse(paiement);
    }
}
