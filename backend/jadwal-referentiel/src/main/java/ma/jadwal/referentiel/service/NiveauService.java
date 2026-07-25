package ma.jadwal.referentiel.service;

import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.referentiel.depot.EtablissementRepository;
import ma.jadwal.referentiel.depot.GroupeRepository;
import ma.jadwal.referentiel.depot.NiveauRepository;
import ma.jadwal.referentiel.entite.Etablissement;
import ma.jadwal.referentiel.entite.Niveau;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NiveauService {

    private final NiveauRepository niveauRepository;
    private final GroupeRepository groupeRepository;
    private final EtablissementRepository etablissementRepository;

    public NiveauService(NiveauRepository niveauRepository,
                         GroupeRepository groupeRepository,
                         EtablissementRepository etablissementRepository) {
        this.niveauRepository = niveauRepository;
        this.groupeRepository = groupeRepository;
        this.etablissementRepository = etablissementRepository;
    }

    @Transactional(readOnly = true)
    public List<Niveau> lister(Long etablissementId) {
        return niveauRepository.findByEtablissementIdOrderByOrdreAscIdAsc(etablissementId);
    }

    @Transactional(readOnly = true)
    public Niveau obtenir(Long etablissementId, Long id) {
        return niveauRepository.findByIdAndEtablissementId(id, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Niveau introuvable : " + id));
    }

    @Transactional
    public Niveau creer(Long etablissementId, String libelle, String cycle, int ordre, Integer chargeMaxUnitesJour) {
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Établissement introuvable : " + etablissementId));
        Niveau niveau = new Niveau();
        niveau.setEtablissement(etablissement);
        niveau.setLibelle(libelle);
        niveau.setCycle(cycle);
        niveau.setOrdre(ordre);
        niveau.setChargeMaxUnitesJour(chargeMaxUnitesJour);
        return niveauRepository.save(niveau);
    }

    @Transactional
    public Niveau mettreAJour(Long etablissementId, Long id, String libelle, String cycle, Integer ordre,
                              Integer chargeMaxUnitesJour) {
        Niveau niveau = obtenir(etablissementId, id);
        if (libelle != null) {
            niveau.setLibelle(libelle);
        }
        niveau.setCycle(cycle);
        if (ordre != null) {
            niveau.setOrdre(ordre);
        }
        niveau.setChargeMaxUnitesJour(chargeMaxUnitesJour);
        return niveauRepository.save(niveau);
    }

    @Transactional
    public void supprimer(Long etablissementId, Long id) {
        Niveau niveau = obtenir(etablissementId, id);
        if (groupeRepository.countByNiveauId(niveau.getId()) > 0) {
            throw new ConflitException("Impossible de supprimer le niveau : des groupes y sont rattachés.");
        }
        niveauRepository.delete(niveau);
    }
}
