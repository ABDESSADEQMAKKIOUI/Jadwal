package ma.jadwal.referentiel.service;

import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.referentiel.depot.EtablissementRepository;
import ma.jadwal.referentiel.depot.MatiereRepository;
import ma.jadwal.referentiel.entite.Etablissement;
import ma.jadwal.referentiel.entite.Matiere;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MatiereService {

    private final MatiereRepository matiereRepository;
    private final EtablissementRepository etablissementRepository;

    public MatiereService(MatiereRepository matiereRepository, EtablissementRepository etablissementRepository) {
        this.matiereRepository = matiereRepository;
        this.etablissementRepository = etablissementRepository;
    }

    @Transactional(readOnly = true)
    public List<Matiere> lister(Long etablissementId) {
        return matiereRepository.findByEtablissementIdOrderByLibelleAsc(etablissementId);
    }

    @Transactional(readOnly = true)
    public Matiere obtenir(Long etablissementId, Long id) {
        return matiereRepository.findByIdAndEtablissementId(id, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Matière introuvable : " + id));
    }

    @Transactional
    public Matiere creer(Long etablissementId, Matiere donnees) {
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Établissement introuvable : " + etablissementId));
        if (matiereRepository.existsByEtablissementIdAndCode(etablissementId, donnees.getCode())) {
            throw new ConflitException("Une matière existe déjà avec le code : " + donnees.getCode());
        }
        donnees.setId(null);
        donnees.setEtablissement(etablissement);
        return matiereRepository.save(donnees);
    }

    @Transactional
    public Matiere mettreAJour(Long etablissementId, Long id, Matiere donnees) {
        Matiere matiere = obtenir(etablissementId, id);
        if (donnees.getCode() != null && !donnees.getCode().equals(matiere.getCode())
                && matiereRepository.existsByEtablissementIdAndCode(etablissementId, donnees.getCode())) {
            throw new ConflitException("Une matière existe déjà avec le code : " + donnees.getCode());
        }
        if (donnees.getLibelle() != null) {
            matiere.setLibelle(donnees.getLibelle());
        }
        if (donnees.getCode() != null) {
            matiere.setCode(donnees.getCode());
        }
        matiere.setCoefficient(donnees.getCoefficient());
        matiere.setPoidsCognitif(donnees.getPoidsCognitif());
        if (donnees.getCouleur() != null) {
            matiere.setCouleur(donnees.getCouleur());
        }
        matiere.setTypeSalleRequis(donnees.getTypeSalleRequis());
        matiere.setEquipementsRequis(donnees.getEquipementsRequis());
        matiere.setDureeMinUnites(donnees.getDureeMinUnites());
        matiere.setDureeMaxUnites(donnees.getDureeMaxUnites());
        matiere.setEviterAvantDejeuner(donnees.isEviterAvantDejeuner());
        matiere.setEviterFinJournee(donnees.isEviterFinJournee());
        return matiereRepository.save(matiere);
    }

    @Transactional
    public void supprimer(Long etablissementId, Long id) {
        Matiere matiere = obtenir(etablissementId, id);
        matiereRepository.delete(matiere);
    }
}
