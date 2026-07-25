package ma.jadwal.referentiel.service;

import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.referentiel.depot.EtablissementRepository;
import ma.jadwal.referentiel.entite.Etablissement;
import ma.jadwal.referentiel.entite.StatutEtablissement;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EtablissementService {

    private final EtablissementRepository etablissementRepository;

    public EtablissementService(EtablissementRepository etablissementRepository) {
        this.etablissementRepository = etablissementRepository;
    }

    @Transactional(readOnly = true)
    public List<Etablissement> listerTout() {
        return etablissementRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public Etablissement obtenir(Long id) {
        return etablissementRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Établissement introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public long compter() {
        return etablissementRepository.count();
    }

    @Transactional
    public Etablissement creer(String nom, String code, String ville, String telephone, String email) {
        if (etablissementRepository.existsByCode(code)) {
            throw new ConflitException("Un établissement existe déjà avec le code : " + code);
        }
        Etablissement etablissement = new Etablissement();
        etablissement.setNom(nom);
        etablissement.setCode(code);
        etablissement.setVille(ville);
        etablissement.setTelephone(telephone);
        etablissement.setEmail(email);
        etablissement.setStatut(StatutEtablissement.ACTIF);
        return etablissementRepository.save(etablissement);
    }

    @Transactional
    public Etablissement mettreAJour(Long id, String nom, String ville, String telephone, String email,
                                     StatutEtablissement statut) {
        Etablissement etablissement = etablissementRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Établissement introuvable : " + id));
        if (nom != null) {
            etablissement.setNom(nom);
        }
        if (ville != null) {
            etablissement.setVille(ville);
        }
        if (telephone != null) {
            etablissement.setTelephone(telephone);
        }
        if (email != null) {
            etablissement.setEmail(email);
        }
        if (statut != null) {
            etablissement.setStatut(statut);
        }
        return etablissementRepository.save(etablissement);
    }
}
