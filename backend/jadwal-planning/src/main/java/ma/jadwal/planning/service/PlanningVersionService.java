package ma.jadwal.planning.service;

import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.planning.depot.PlanningVersionRepository;
import ma.jadwal.planning.entite.PlanningVersion;
import ma.jadwal.referentiel.depot.EtablissementRepository;
import ma.jadwal.referentiel.entite.Etablissement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanningVersionService {

    private final PlanningVersionRepository planningVersionRepository;
    private final EtablissementRepository etablissementRepository;

    public PlanningVersionService(PlanningVersionRepository planningVersionRepository,
                                  EtablissementRepository etablissementRepository) {
        this.planningVersionRepository = planningVersionRepository;
        this.etablissementRepository = etablissementRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanningVersion> lister(Long etablissementId) {
        return planningVersionRepository.findByEtablissementIdOrderByCreeeLeDesc(etablissementId);
    }

    @Transactional(readOnly = true)
    public PlanningVersion obtenir(Long etablissementId, Long id) {
        return planningVersionRepository.findByIdAndEtablissementId(id, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Version de planning introuvable : " + id));
    }

    @Transactional
    public PlanningVersion creer(Long etablissementId, String libelle) {
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Établissement introuvable : " + etablissementId));
        PlanningVersion version = new PlanningVersion();
        version.setEtablissement(etablissement);
        version.setLibelle(libelle);
        version.setActive(false);
        return planningVersionRepository.save(version);
    }

    @Transactional
    public PlanningVersion renommer(Long etablissementId, Long id, String libelle) {
        PlanningVersion version = obtenir(etablissementId, id);
        version.setLibelle(libelle);
        return planningVersionRepository.save(version);
    }

    /**
     * Active la version indiquée : toutes les autres versions de l'établissement sont désactivées.
     */
    @Transactional
    public PlanningVersion activer(Long etablissementId, Long id) {
        PlanningVersion cible = obtenir(etablissementId, id);
        for (PlanningVersion active : planningVersionRepository.findByEtablissementIdAndActiveTrue(etablissementId)) {
            if (!active.getId().equals(cible.getId())) {
                active.setActive(false);
                planningVersionRepository.save(active);
            }
        }
        cible.setActive(true);
        return planningVersionRepository.save(cible);
    }

    @Transactional
    public void supprimer(Long etablissementId, Long id) {
        PlanningVersion version = obtenir(etablissementId, id);
        planningVersionRepository.delete(version);
    }
}
