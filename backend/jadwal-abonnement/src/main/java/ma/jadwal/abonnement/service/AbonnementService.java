package ma.jadwal.abonnement.service;

import ma.jadwal.abonnement.depot.AbonnementRepository;
import ma.jadwal.abonnement.depot.PlanAbonnementRepository;
import ma.jadwal.abonnement.entite.Abonnement;
import ma.jadwal.abonnement.entite.PlanAbonnement;
import ma.jadwal.abonnement.entite.StatutAbonnement;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.referentiel.depot.EtablissementRepository;
import ma.jadwal.referentiel.entite.Etablissement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class AbonnementService {

    private final AbonnementRepository abonnementRepository;
    private final EtablissementRepository etablissementRepository;
    private final PlanAbonnementRepository planRepository;

    public AbonnementService(AbonnementRepository abonnementRepository,
                             EtablissementRepository etablissementRepository,
                             PlanAbonnementRepository planRepository) {
        this.abonnementRepository = abonnementRepository;
        this.etablissementRepository = etablissementRepository;
        this.planRepository = planRepository;
    }

    @Transactional(readOnly = true)
    public List<Abonnement> listerTout() {
        return abonnementRepository.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public List<Abonnement> listerParEtablissement(Long etablissementId) {
        return abonnementRepository.findByEtablissement_IdOrderByDateDebutDescIdDesc(etablissementId);
    }

    @Transactional(readOnly = true)
    public Abonnement obtenir(Long id) {
        return abonnementRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Abonnement introuvable : " + id));
    }

    /**
     * L'abonnement le plus récent non expiré de l'établissement, s'il existe.
     */
    @Transactional(readOnly = true)
    public Optional<Abonnement> abonnementActif(Long etablissementId) {
        return abonnementRepository.findFirstByEtablissement_IdAndStatutNotOrderByDateDebutDescIdDesc(
                etablissementId, StatutAbonnement.EXPIRE);
    }

    @Transactional(readOnly = true)
    public long compterParStatut(StatutAbonnement statut) {
        return abonnementRepository.countByStatut(statut);
    }

    @Transactional
    public Abonnement creer(Long etablissementId, Long planId, LocalDate dateDebut, LocalDate dateFin) {
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Établissement introuvable : " + etablissementId));
        PlanAbonnement plan = planRepository.findById(planId)
                .orElseThrow(() -> new RessourceIntrouvableException("Plan d'abonnement introuvable : " + planId));
        if (dateDebut != null && dateFin != null && !dateFin.isAfter(dateDebut)) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début");
        }
        Abonnement abonnement = new Abonnement();
        abonnement.setEtablissement(etablissement);
        abonnement.setPlan(plan);
        abonnement.setDateDebut(dateDebut);
        abonnement.setDateFin(dateFin);
        abonnement.setStatut(StatutAbonnement.EN_ATTENTE_PAIEMENT);
        return abonnementRepository.save(abonnement);
    }
}
