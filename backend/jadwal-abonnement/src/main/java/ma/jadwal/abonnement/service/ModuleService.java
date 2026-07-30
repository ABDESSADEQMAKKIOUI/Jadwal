package ma.jadwal.abonnement.service;

import ma.jadwal.abonnement.depot.AbonnementRepository;
import ma.jadwal.abonnement.entite.Abonnement;
import ma.jadwal.abonnement.entite.ModuleJadwal;
import ma.jadwal.abonnement.entite.PlanAbonnement;
import ma.jadwal.abonnement.entite.StatutAbonnement;
import ma.jadwal.common.exception.ModuleNonSouscritException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Conditionne l'accès aux fonctionnalités à l'abonnement de l'établissement.
 * L'établissement vient toujours du JWT côté contrôleur : ce service ne fait
 * que lire le plan rattaché à l'abonnement actif.
 */
@Service
public class ModuleService {

    private final AbonnementRepository abonnementRepository;

    public ModuleService(AbonnementRepository abonnementRepository) {
        this.abonnementRepository = abonnementRepository;
    }

    /**
     * Modules réellement ouverts à l'établissement : ceux du plan de son
     * abonnement le plus récent non expiré, à condition qu'il soit ACTIF.
     * Ensemble vide si aucun abonnement exploitable.
     */
    @Transactional(readOnly = true)
    public Set<ModuleJadwal> modulesActifs(Long etablissementId) {
        return abonnementActif(etablissementId)
                .map(Abonnement::getPlan)
                .map(ModuleService::modulesDuPlan)
                .orElseGet(Collections::emptySet);
    }

    @Transactional(readOnly = true)
    public boolean moduleDisponible(Long etablissementId, ModuleJadwal module) {
        return modulesActifs(etablissementId).contains(module);
    }

    /**
     * Barrière à placer en tête de toute opération d'un module payant.
     *
     * @throws ModuleNonSouscritException si l'établissement n'a pas d'abonnement
     *                                    actif ou si son plan n'inclut pas le module
     */
    @Transactional(readOnly = true)
    public void exigerModule(Long etablissementId, ModuleJadwal module) {
        Optional<Abonnement> abonnement = abonnementActif(etablissementId);
        if (abonnement.isEmpty()) {
            throw new ModuleNonSouscritException(module.name(),
                    "Aucun abonnement actif pour votre établissement : le module "
                            + module.libelle() + " n'est pas accessible.");
        }
        if (!modulesDuPlan(abonnement.get().getPlan()).contains(module)) {
            throw new ModuleNonSouscritException(module.name(),
                    "Le module " + module.libelle()
                            + " n'est pas inclus dans l'abonnement de votre établissement.");
        }
    }

    /**
     * Décodage de {@code plan_abonnement.modules} : codes séparés par des
     * virgules, valeurs inconnues ignorées.
     */
    public static Set<ModuleJadwal> modulesDuPlan(PlanAbonnement plan) {
        Set<ModuleJadwal> modules = EnumSet.noneOf(ModuleJadwal.class);
        if (plan == null || plan.getModules() == null) {
            return modules;
        }
        for (String code : plan.getModules().split(",")) {
            ModuleJadwal.depuisCode(code).ifPresent(modules::add);
        }
        return modules;
    }

    private Optional<Abonnement> abonnementActif(Long etablissementId) {
        return abonnementRepository
                .findFirstByEtablissement_IdAndStatutNotOrderByDateDebutDescIdDesc(
                        etablissementId, StatutAbonnement.EXPIRE)
                .filter(abonnement -> abonnement.getStatut() == StatutAbonnement.ACTIF);
    }
}
