package ma.jadwal.pedagogie.service;

import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.pedagogie.depot.MaquetteRepository;
import ma.jadwal.pedagogie.depot.VolumeOverrideRepository;
import ma.jadwal.pedagogie.entite.Dedoublement;
import ma.jadwal.pedagogie.entite.Maquette;
import ma.jadwal.pedagogie.entite.VolumeOverride;
import ma.jadwal.referentiel.depot.GroupeRepository;
import ma.jadwal.referentiel.depot.MatiereRepository;
import ma.jadwal.referentiel.depot.NiveauRepository;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.referentiel.entite.Matiere;
import ma.jadwal.referentiel.entite.Niveau;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class MaquetteService {

    public record LigneMaquette(Long matiereId, int volumeUnites, Integer volumeUnitesB, int maxParJourUnites,
                                Dedoublement dedoublement, int nbSousGroupes, int coEnseignants,
                                String patternsJson) {
    }

    private final MaquetteRepository maquetteRepository;
    private final VolumeOverrideRepository volumeOverrideRepository;
    private final NiveauRepository niveauRepository;
    private final MatiereRepository matiereRepository;
    private final GroupeRepository groupeRepository;

    public MaquetteService(MaquetteRepository maquetteRepository,
                           VolumeOverrideRepository volumeOverrideRepository,
                           NiveauRepository niveauRepository,
                           MatiereRepository matiereRepository,
                           GroupeRepository groupeRepository) {
        this.maquetteRepository = maquetteRepository;
        this.volumeOverrideRepository = volumeOverrideRepository;
        this.niveauRepository = niveauRepository;
        this.matiereRepository = matiereRepository;
        this.groupeRepository = groupeRepository;
    }

    @Transactional(readOnly = true)
    public List<Maquette> lister(Long etablissementId) {
        return maquetteRepository.findByEtablissementId(etablissementId);
    }

    @Transactional(readOnly = true)
    public List<Maquette> listerParNiveau(Long etablissementId, Long niveauId) {
        return maquetteRepository.findByNiveauIdAndEtablissementId(niveauId, etablissementId);
    }

    /**
     * Remplace intégralement la maquette pédagogique d'un niveau (opération transactionnelle) :
     * les lignes existantes sont supprimées puis recréées à partir des lignes fournies.
     */
    @Transactional
    public List<Maquette> remplacerMaquetteNiveau(Long etablissementId, Long niveauId, List<LigneMaquette> lignes) {
        Niveau niveau = niveauRepository.findByIdAndEtablissementId(niveauId, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Niveau introuvable : " + niveauId));
        maquetteRepository.deleteByNiveauId(niveau.getId());
        maquetteRepository.flush();

        List<Maquette> resultat = new ArrayList<>();
        if (lignes == null) {
            return resultat;
        }
        for (LigneMaquette ligne : lignes) {
            Matiere matiere = matiereRepository.findByIdAndEtablissementId(ligne.matiereId(), etablissementId)
                    .orElseThrow(() -> new RessourceIntrouvableException("Matière introuvable : " + ligne.matiereId()));
            Maquette maquette = new Maquette();
            maquette.setEtablissement(niveau.getEtablissement());
            maquette.setNiveau(niveau);
            maquette.setMatiere(matiere);
            maquette.setVolumeUnites(ligne.volumeUnites());
            maquette.setVolumeUnitesB(ligne.volumeUnitesB());
            maquette.setMaxParJourUnites(ligne.maxParJourUnites());
            maquette.setDedoublement(ligne.dedoublement() != null ? ligne.dedoublement() : Dedoublement.AUCUN);
            maquette.setNbSousGroupes(ligne.nbSousGroupes());
            maquette.setCoEnseignants(ligne.coEnseignants());
            maquette.setPatternsJson(ligne.patternsJson());
            resultat.add(maquetteRepository.save(maquette));
        }
        return resultat;
    }

    // ---------- Volumes spécifiques par groupe ----------

    @Transactional(readOnly = true)
    public List<VolumeOverride> listerVolumesGroupe(Long etablissementId, Long groupeId) {
        Groupe groupe = obtenirGroupe(etablissementId, groupeId);
        return volumeOverrideRepository.findByGroupeId(groupe.getId());
    }

    @Transactional
    public VolumeOverride definirVolumeGroupe(Long etablissementId, Long groupeId, Long matiereId, int volumeUnites) {
        Groupe groupe = obtenirGroupe(etablissementId, groupeId);
        Matiere matiere = matiereRepository.findByIdAndEtablissementId(matiereId, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Matière introuvable : " + matiereId));
        VolumeOverride override = volumeOverrideRepository
                .findByGroupeIdAndMatiereId(groupe.getId(), matiere.getId())
                .orElseGet(VolumeOverride::new);
        override.setGroupe(groupe);
        override.setMatiere(matiere);
        override.setVolumeUnites(volumeUnites);
        return volumeOverrideRepository.save(override);
    }

    @Transactional
    public void supprimerVolumeGroupe(Long etablissementId, Long groupeId, Long matiereId) {
        Groupe groupe = obtenirGroupe(etablissementId, groupeId);
        VolumeOverride override = volumeOverrideRepository
                .findByGroupeIdAndMatiereId(groupe.getId(), matiereId)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Volume spécifique introuvable pour le groupe " + groupeId + " et la matière " + matiereId));
        volumeOverrideRepository.delete(override);
    }

    private Groupe obtenirGroupe(Long etablissementId, Long groupeId) {
        return groupeRepository.findByIdAndEtablissementId(groupeId, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Groupe introuvable : " + groupeId));
    }
}
