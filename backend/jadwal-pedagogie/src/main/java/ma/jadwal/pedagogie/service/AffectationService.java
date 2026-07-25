package ma.jadwal.pedagogie.service;

import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.enseignant.depot.EnseignantRepository;
import ma.jadwal.enseignant.entite.Enseignant;
import ma.jadwal.pedagogie.depot.AffectationRepository;
import ma.jadwal.pedagogie.entite.Affectation;
import ma.jadwal.referentiel.depot.GroupeRepository;
import ma.jadwal.referentiel.depot.MatiereRepository;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.referentiel.entite.Matiere;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AffectationService {

    private final AffectationRepository affectationRepository;
    private final GroupeRepository groupeRepository;
    private final MatiereRepository matiereRepository;
    private final EnseignantRepository enseignantRepository;

    public AffectationService(AffectationRepository affectationRepository,
                              GroupeRepository groupeRepository,
                              MatiereRepository matiereRepository,
                              EnseignantRepository enseignantRepository) {
        this.affectationRepository = affectationRepository;
        this.groupeRepository = groupeRepository;
        this.matiereRepository = matiereRepository;
        this.enseignantRepository = enseignantRepository;
    }

    @Transactional(readOnly = true)
    public List<Affectation> lister(Long etablissementId) {
        return affectationRepository.findByEtablissementId(etablissementId);
    }

    @Transactional(readOnly = true)
    public List<Affectation> listerParGroupe(Long etablissementId, Long groupeId) {
        return affectationRepository.findByEtablissementIdAndGroupeId(etablissementId, groupeId);
    }

    @Transactional(readOnly = true)
    public List<Affectation> listerParEnseignant(Long etablissementId, Long enseignantId) {
        return affectationRepository.findByEtablissementIdAndEnseignantId(etablissementId, enseignantId);
    }

    @Transactional(readOnly = true)
    public Affectation obtenir(Long etablissementId, Long id) {
        return affectationRepository.findByIdAndEtablissementId(id, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Affectation introuvable : " + id));
    }

    @Transactional
    public Affectation creer(Long etablissementId, Long groupeId, Long matiereId, Long enseignantId,
                             Integer volumeUnites) {
        Groupe groupe = groupeRepository.findByIdAndEtablissementId(groupeId, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Groupe introuvable : " + groupeId));
        Matiere matiere = matiereRepository.findByIdAndEtablissementId(matiereId, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Matière introuvable : " + matiereId));
        Enseignant enseignant = enseignantRepository.findByIdAndEtablissementId(enseignantId, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Enseignant introuvable : " + enseignantId));
        if (affectationRepository.existsByGroupeIdAndMatiereIdAndEnseignantId(
                groupe.getId(), matiere.getId(), enseignant.getId())) {
            throw new ConflitException("Cette affectation existe déjà pour ce groupe, cette matière et cet enseignant.");
        }
        Affectation affectation = new Affectation();
        affectation.setEtablissementId(etablissementId);
        affectation.setGroupe(groupe);
        affectation.setMatiere(matiere);
        affectation.setEnseignant(enseignant);
        affectation.setVolumeUnites(volumeUnites);
        return affectationRepository.save(affectation);
    }

    @Transactional
    public Affectation mettreAJourVolume(Long etablissementId, Long id, Integer volumeUnites) {
        Affectation affectation = obtenir(etablissementId, id);
        affectation.setVolumeUnites(volumeUnites);
        return affectationRepository.save(affectation);
    }

    @Transactional
    public void supprimer(Long etablissementId, Long id) {
        Affectation affectation = obtenir(etablissementId, id);
        affectationRepository.delete(affectation);
    }
}
