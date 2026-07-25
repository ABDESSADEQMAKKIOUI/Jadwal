package ma.jadwal.referentiel.service;

import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.referentiel.depot.EtablissementRepository;
import ma.jadwal.referentiel.depot.SalleRepository;
import ma.jadwal.referentiel.entite.Etablissement;
import ma.jadwal.referentiel.entite.Salle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SalleService {

    private final SalleRepository salleRepository;
    private final EtablissementRepository etablissementRepository;

    public SalleService(SalleRepository salleRepository, EtablissementRepository etablissementRepository) {
        this.salleRepository = salleRepository;
        this.etablissementRepository = etablissementRepository;
    }

    @Transactional(readOnly = true)
    public List<Salle> lister(Long etablissementId) {
        return salleRepository.findByEtablissementIdOrderByNomAsc(etablissementId);
    }

    @Transactional(readOnly = true)
    public Salle obtenir(Long etablissementId, Long id) {
        return salleRepository.findByIdAndEtablissementId(id, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Salle introuvable : " + id));
    }

    @Transactional
    public Salle creer(Long etablissementId, String nom, int capacite, String type, String equipements,
                       String batiment) {
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Établissement introuvable : " + etablissementId));
        Salle salle = new Salle();
        salle.setEtablissement(etablissement);
        salle.setNom(nom);
        salle.setCapacite(capacite);
        salle.setType(type != null ? type : "STANDARD");
        salle.setEquipements(equipements);
        salle.setBatiment(batiment);
        return salleRepository.save(salle);
    }

    @Transactional
    public Salle mettreAJour(Long etablissementId, Long id, String nom, Integer capacite, String type,
                             String equipements, String batiment) {
        Salle salle = obtenir(etablissementId, id);
        if (nom != null) {
            salle.setNom(nom);
        }
        if (capacite != null) {
            salle.setCapacite(capacite);
        }
        if (type != null) {
            salle.setType(type);
        }
        salle.setEquipements(equipements);
        salle.setBatiment(batiment);
        return salleRepository.save(salle);
    }

    @Transactional
    public void supprimer(Long etablissementId, Long id) {
        Salle salle = obtenir(etablissementId, id);
        salleRepository.delete(salle);
    }
}
