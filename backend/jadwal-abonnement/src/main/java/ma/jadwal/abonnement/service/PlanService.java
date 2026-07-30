package ma.jadwal.abonnement.service;

import ma.jadwal.abonnement.depot.PlanAbonnementRepository;
import ma.jadwal.abonnement.entite.ModuleJadwal;
import ma.jadwal.abonnement.entite.PlanAbonnement;
import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        return creer(code, nom, prixAnnuel, description, Set.of(ModuleJadwal.PLANNING));
    }

    @Transactional
    public PlanAbonnement creer(String code, String nom, BigDecimal prixAnnuel, String description,
                                Set<ModuleJadwal> modules) {
        if (planRepository.existsByCode(code)) {
            throw new ConflitException("Un plan existe déjà avec le code : " + code);
        }
        PlanAbonnement plan = new PlanAbonnement();
        plan.setCode(code);
        plan.setNom(nom);
        plan.setPrixAnnuel(prixAnnuel);
        plan.setDescription(description);
        plan.setModules(encoderModules(modules));
        plan.setActif(true);
        return planRepository.save(plan);
    }

    /**
     * Les modules sont stockés en clair, séparés par des virgules, dans l'ordre
     * de l'énumération. PLANNING est toujours inclus : c'est le socle du produit.
     */
    static String encoderModules(Set<ModuleJadwal> modules) {
        EnumSet<ModuleJadwal> retenus = EnumSet.of(ModuleJadwal.PLANNING);
        if (modules != null) {
            retenus.addAll(modules);
        }
        return retenus.stream().map(ModuleJadwal::name).collect(Collectors.joining(","));
    }
}
