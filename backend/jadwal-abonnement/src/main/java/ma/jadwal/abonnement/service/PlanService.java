package ma.jadwal.abonnement.service;

import ma.jadwal.abonnement.depot.PlanAbonnementRepository;
import ma.jadwal.abonnement.entite.PlanAbonnement;
import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PlanService {

    private final PlanAbonnementRepository planRepository;

    public PlanService(PlanAbonnementRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanAbonnement> listerTout() {
        return planRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public PlanAbonnement obtenir(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Plan d'abonnement introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public long compter() {
        return planRepository.count();
    }

    @Transactional
    public PlanAbonnement creer(String code, String nom, BigDecimal prixAnnuel, String description) {
        if (planRepository.existsByCode(code)) {
            throw new ConflitException("Un plan existe déjà avec le code : " + code);
        }
        PlanAbonnement plan = new PlanAbonnement();
        plan.setCode(code);
        plan.setNom(nom);
        plan.setPrixAnnuel(prixAnnuel);
        plan.setDescription(description);
        plan.setActif(true);
        return planRepository.save(plan);
    }
}
