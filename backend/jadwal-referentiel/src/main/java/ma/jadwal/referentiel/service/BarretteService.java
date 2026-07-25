package ma.jadwal.referentiel.service;

import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.referentiel.depot.BarretteRepository;
import ma.jadwal.referentiel.depot.GroupeRepository;
import ma.jadwal.referentiel.depot.MatiereRepository;
import ma.jadwal.referentiel.entite.Barrette;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.referentiel.entite.Matiere;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class BarretteService {

    private final BarretteRepository barretteRepository;
    private final MatiereRepository matiereRepository;
    private final GroupeRepository groupeRepository;

    public BarretteService(BarretteRepository barretteRepository,
                           MatiereRepository matiereRepository,
                           GroupeRepository groupeRepository) {
        this.barretteRepository = barretteRepository;
        this.matiereRepository = matiereRepository;
        this.groupeRepository = groupeRepository;
    }

    @Transactional(readOnly = true)
    public List<Barrette> lister(Long etablissementId) {
        return barretteRepository.findByEtablissementIdOrderByLibelleAsc(etablissementId);
    }

    @Transactional(readOnly = true)
    public Barrette obtenir(Long etablissementId, Long id) {
        return barretteRepository.findByIdAndEtablissementId(id, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Barrette introuvable : " + id));
    }

    @Transactional
    public Barrette creer(Long etablissementId, String libelle, Long matiereId, List<Long> groupeIds) {
        Matiere matiere = matiereRepository.findByIdAndEtablissementId(matiereId, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Matière introuvable : " + matiereId));
        Barrette barrette = new Barrette();
        barrette.setEtablissement(matiere.getEtablissement());
        barrette.setLibelle(libelle);
        barrette.setMatiere(matiere);
        barrette.setGroupes(chargerGroupes(etablissementId, groupeIds));
        return barretteRepository.save(barrette);
    }

    @Transactional
    public Barrette mettreAJour(Long etablissementId, Long id, String libelle, Long matiereId, List<Long> groupeIds) {
        Barrette barrette = obtenir(etablissementId, id);
        if (libelle != null) {
            barrette.setLibelle(libelle);
        }
        if (matiereId != null) {
            Matiere matiere = matiereRepository.findByIdAndEtablissementId(matiereId, etablissementId)
                    .orElseThrow(() -> new RessourceIntrouvableException("Matière introuvable : " + matiereId));
            barrette.setMatiere(matiere);
        }
        if (groupeIds != null) {
            barrette.setGroupes(chargerGroupes(etablissementId, groupeIds));
        }
        return barretteRepository.save(barrette);
    }

    @Transactional
    public void supprimer(Long etablissementId, Long id) {
        Barrette barrette = obtenir(etablissementId, id);
        barretteRepository.delete(barrette);
    }

    private Set<Groupe> chargerGroupes(Long etablissementId, List<Long> groupeIds) {
        Set<Groupe> groupes = new LinkedHashSet<>();
        if (groupeIds == null) {
            return groupes;
        }
        for (Long groupeId : groupeIds) {
            Groupe groupe = groupeRepository.findByIdAndEtablissementId(groupeId, etablissementId)
                    .orElseThrow(() -> new RessourceIntrouvableException("Groupe introuvable : " + groupeId));
            groupes.add(groupe);
        }
        return groupes;
    }
}
