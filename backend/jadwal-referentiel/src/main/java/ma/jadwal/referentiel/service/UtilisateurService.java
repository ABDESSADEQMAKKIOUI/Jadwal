package ma.jadwal.referentiel.service;

import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.referentiel.depot.UtilisateurRepository;
import ma.jadwal.referentiel.entite.Etablissement;
import ma.jadwal.referentiel.entite.Role;
import ma.jadwal.referentiel.entite.Utilisateur;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder encodeurMotDePasse;

    public UtilisateurService(UtilisateurRepository utilisateurRepository, PasswordEncoder encodeurMotDePasse) {
        this.utilisateurRepository = utilisateurRepository;
        this.encodeurMotDePasse = encodeurMotDePasse;
    }

    @Transactional(readOnly = true)
    public Optional<Utilisateur> chercherParEmail(String email) {
        return utilisateurRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Utilisateur obtenir(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public List<Utilisateur> listerParEtablissement(Long etablissementId) {
        return utilisateurRepository.findByEtablissement_IdOrderById(etablissementId);
    }

    @Transactional
    public Utilisateur creerDirecteur(Etablissement etablissement, String email, String nomComplet, String motDePasse) {
        if (utilisateurRepository.existsByEmail(email)) {
            throw new ConflitException("Un compte existe déjà avec l'email : " + email);
        }
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setNomComplet(nomComplet);
        utilisateur.setMotDePasseHash(encodeurMotDePasse.encode(motDePasse));
        utilisateur.setRole(Role.DIRECTEUR);
        utilisateur.setEtablissement(etablissement);
        utilisateur.setActif(true);
        return utilisateurRepository.save(utilisateur);
    }

    @Transactional
    public Utilisateur creerSuperAdmin(String email, String nomComplet, String motDePasse) {
        if (utilisateurRepository.existsByEmail(email)) {
            throw new ConflitException("Un compte existe déjà avec l'email : " + email);
        }
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setNomComplet(nomComplet);
        utilisateur.setMotDePasseHash(encodeurMotDePasse.encode(motDePasse));
        utilisateur.setRole(Role.SUPER_ADMIN);
        utilisateur.setEtablissement(null);
        utilisateur.setActif(true);
        return utilisateurRepository.save(utilisateur);
    }

    public boolean verifierMotDePasse(Utilisateur utilisateur, String motDePasse) {
        return encodeurMotDePasse.matches(motDePasse, utilisateur.getMotDePasseHash());
    }
}
