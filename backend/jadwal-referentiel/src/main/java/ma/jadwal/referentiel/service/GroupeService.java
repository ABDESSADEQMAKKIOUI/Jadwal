package ma.jadwal.referentiel.service;

import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.referentiel.depot.GroupeRepository;
import ma.jadwal.referentiel.depot.NiveauRepository;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.referentiel.entite.Niveau;
import ma.jadwal.referentiel.entite.TypeGroupe;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupeService {

    private final GroupeRepository groupeRepository;
    private final NiveauRepository niveauRepository;

    public GroupeService(GroupeRepository groupeRepository, NiveauRepository niveauRepository) {
        this.groupeRepository = groupeRepository;
        this.niveauRepository = niveauRepository;
    }

    @Transactional(readOnly = true)
    public List<Groupe> lister(Long etablissementId) {
        return groupeRepository.findByEtablissementIdOrderByLibelleAsc(etablissementId);
    }

    @Transactional(readOnly = true)
    public List<Groupe> listerParNiveau(Long etablissementId, Long niveauId) {
        return groupeRepository.findByEtablissementIdAndNiveauIdOrderByLibelleAsc(etablissementId, niveauId);
    }

    @Transactional(readOnly = true)
    public Groupe obtenir(Long etablissementId, Long id) {
        return groupeRepository.findByIdAndEtablissementId(id, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Groupe introuvable : " + id));
    }

    @Transactional
    public Groupe creer(Long etablissementId, Long niveauId, String libelle, int effectif, Long parentId,
                        TypeGroupe type) {
        Niveau niveau = niveauRepository.findByIdAndEtablissementId(niveauId, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Niveau introuvable : " + niveauId));
        Groupe groupe = new Groupe();
        groupe.setEtablissement(niveau.getEtablissement());
        groupe.setNiveau(niveau);
        groupe.setLibelle(libelle);
        groupe.setEffectif(effectif);
        if (parentId != null) {
            groupe.setParent(obtenir(etablissementId, parentId));
        }
        groupe.setType(type != null ? type : TypeGroupe.CLASSE);
        return groupeRepository.save(groupe);
    }

    @Transactional
    public Groupe mettreAJour(Long etablissementId, Long id, Long niveauId, String libelle, Integer effectif,
                              Long parentId, TypeGroupe type) {
        Groupe groupe = obtenir(etablissementId, id);
        if (niveauId != null) {
            Niveau niveau = niveauRepository.findByIdAndEtablissementId(niveauId, etablissementId)
                    .orElseThrow(() -> new RessourceIntrouvableException("Niveau introuvable : " + niveauId));
            groupe.setNiveau(niveau);
        }
        if (libelle != null) {
            groupe.setLibelle(libelle);
        }
        if (effectif != null) {
            groupe.setEffectif(effectif);
        }
        if (parentId != null) {
            groupe.setParent(obtenir(etablissementId, parentId));
        } else {
            groupe.setParent(null);
        }
        if (type != null) {
            groupe.setType(type);
        }
        return groupeRepository.save(groupe);
    }

    @Transactional
    public void supprimer(Long etablissementId, Long id) {
        Groupe groupe = obtenir(etablissementId, id);
        groupeRepository.delete(groupe);
    }
}
