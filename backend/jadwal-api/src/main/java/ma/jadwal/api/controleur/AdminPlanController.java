package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import ma.jadwal.abonnement.entite.PlanAbonnement;
import ma.jadwal.abonnement.service.PlanService;
import ma.jadwal.api.dto.CreationPlanRequete;
import ma.jadwal.api.dto.Mappeurs;
import ma.jadwal.api.dto.PlanReponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/plans")
public class AdminPlanController {

    private final PlanService planService;

    public AdminPlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public List<PlanReponse> lister() {
        return planService.listerTout().stream()
                .map(Mappeurs::versPlanReponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanReponse creer(@Valid @RequestBody CreationPlanRequete requete) {
        PlanAbonnement plan = planService.creer(
                requete.code(), requete.nom(), requete.prixAnnuel(), requete.description());
        return Mappeurs.versPlanReponse(plan);
    }
}
