package ma.jadwal.scolarite.service;

import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.referentiel.depot.GroupeRepository;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.scolarite.depot.EleveRepository;
import ma.jadwal.scolarite.entite.Eleve;
import ma.jadwal.scolarite.entite.StatutEleve;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Règles métier des élèves. Deux invariants gouvernent toutes les méthodes :
 * <ul>
 *   <li>l'{@code etablissementId} est fourni par l'appelant depuis le JWT et
 *       participe à CHAQUE lecture ou écriture ;</li>
 *   <li>le code Massar est unique au sein d'un établissement.</li>
 * </ul>
 * Aucun message d'erreur ni aucune trace ne contient de donnée personnelle.
 */
@Service
public class EleveService {

    private static final Sort TRI_NOM = Sort.by(Sort.Order.asc("nom"), Sort.Order.asc("prenom"));

    private final EleveRepository eleveRepository;
    private final GroupeRepository groupeRepository;

    public EleveService(EleveRepository eleveRepository, GroupeRepository groupeRepository) {
        this.eleveRepository = eleveRepository;
        this.groupeRepository = groupeRepository;
    }

    /**
     * Recherche paginée. {@code groupeId} et {@code statut} nuls = pas de filtre ;
     * {@code recherche} porte sur le nom, le prénom et le code Massar.
     */
    @Transactional(readOnly = true)
    public Page<Eleve> rechercher(Long etablissementId, Long groupeId, StatutEleve statut, String recherche,
                                  Pageable pageable) {
        return rechercher(etablissementId, groupeId, null, statut, recherche, pageable);
    }

    /**
     * Recherche paginée avec filtre de niveau : {@code niveauId} retient les
     * élèves dont la classe appartient à ce niveau. Tous les critères sont
     * optionnels sauf l'établissement.
     */
    @Transactional(readOnly = true)
    public Page<Eleve> rechercher(Long etablissementId, Long groupeId, Long niveauId, StatutEleve statut,
                                  String recherche, Pageable pageable) {
        return eleveRepository.rechercher(etablissementId, groupeId, niveauId, statut, motifRecherche(recherche),
                avecTriParDefaut(pageable));
    }

    @Transactional(readOnly = true)
    public List<Eleve> listerParGroupe(Long etablissementId, Long groupeId) {
        exigerGroupe(etablissementId, groupeId);
        return eleveRepository.findByEtablissementIdAndGroupeIdOrderByNomAscPrenomAsc(etablissementId, groupeId);
    }

    /**
     * Chargement filtré par locataire : un directeur ne peut jamais atteindre
     * l'élève d'un autre établissement, même en connaissant son identifiant.
     */
    @Transactional(readOnly = true)
    public Eleve obtenir(Long etablissementId, Long id) {
        return eleveRepository.findByIdAndEtablissementId(id, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Élève introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public Optional<Eleve> chercherParCodeMassar(Long etablissementId, String codeMassar) {
        return eleveRepository.findByEtablissementIdAndCodeMassar(etablissementId, normaliserCodeMassar(codeMassar));
    }

    @Transactional
    public Eleve creer(Long etablissementId, DonneesEleve donnees) {
        String codeMassar = normaliserCodeMassar(donnees.codeMassar());
        if (eleveRepository.existsByEtablissementIdAndCodeMassar(etablissementId, codeMassar)) {
            throw new ConflitException("Un élève est déjà inscrit avec ce code Massar dans cet établissement.");
        }
        Eleve eleve = new Eleve();
        eleve.setEtablissementId(etablissementId);
        eleve.setCodeMassar(codeMassar);
        appliquer(etablissementId, eleve, donnees);
        return eleveRepository.save(eleve);
    }

    @Transactional
    public Eleve mettreAJour(Long etablissementId, Long id, DonneesEleve donnees) {
        Eleve eleve = obtenir(etablissementId, id);
        String codeMassar = normaliserCodeMassar(donnees.codeMassar());
        if (eleveRepository.existsByEtablissementIdAndCodeMassarAndIdNot(etablissementId, codeMassar, id)) {
            throw new ConflitException("Un élève est déjà inscrit avec ce code Massar dans cet établissement.");
        }
        eleve.setCodeMassar(codeMassar);
        appliquer(etablissementId, eleve, donnees);
        return eleveRepository.save(eleve);
    }

    /**
     * Affectation ou retrait de classe. {@code groupeId} nul retire l'élève de sa classe.
     */
    @Transactional
    public Eleve affecterAuGroupe(Long etablissementId, Long id, Long groupeId) {
        Eleve eleve = obtenir(etablissementId, id);
        eleve.setGroupe(groupeId == null ? null : exigerGroupe(etablissementId, groupeId));
        return eleveRepository.save(eleve);
    }

    @Transactional
    public Eleve changerStatut(Long etablissementId, Long id, StatutEleve statut) {
        if (statut == null) {
            throw new IllegalArgumentException("Le statut est obligatoire");
        }
        Eleve eleve = obtenir(etablissementId, id);
        eleve.setStatut(statut);
        return eleveRepository.save(eleve);
    }

    @Transactional
    public void supprimer(Long etablissementId, Long id) {
        eleveRepository.delete(obtenir(etablissementId, id));
    }

    @Transactional(readOnly = true)
    public StatistiquesEleves statistiques(Long etablissementId) {
        return new StatistiquesEleves(
                eleveRepository.countByEtablissementId(etablissementId),
                eleveRepository.countByEtablissementIdAndStatut(etablissementId, StatutEleve.INSCRIT),
                eleveRepository.countByEtablissementIdAndStatut(etablissementId, StatutEleve.PARTI),
                eleveRepository.countByEtablissementIdAndStatut(etablissementId, StatutEleve.REDOUBLANT),
                eleveRepository.countByEtablissementIdAndGroupeIsNull(etablissementId));
    }

    @Transactional(readOnly = true)
    public long compterParGroupe(Long etablissementId, Long groupeId) {
        return eleveRepository.countByEtablissementIdAndGroupeId(etablissementId, groupeId);
    }

    /**
     * Le groupe est chargé filtré par établissement : impossible de rattacher un
     * élève à la classe d'un autre locataire.
     */
    private Groupe exigerGroupe(Long etablissementId, Long groupeId) {
        return groupeRepository.findByIdAndEtablissementId(groupeId, etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Groupe introuvable : " + groupeId));
    }

    private void appliquer(Long etablissementId, Eleve eleve, DonneesEleve donnees) {
        eleve.setNom(texteObligatoire(donnees.nom(), "nom", 100));
        eleve.setPrenom(texteObligatoire(donnees.prenom(), "prénom", 100));
        eleve.setNomAr(texteOptionnel(donnees.nomAr(), "nom arabe", 100));
        eleve.setPrenomAr(texteOptionnel(donnees.prenomAr(), "prénom arabe", 100));
        eleve.setDateNaissance(donnees.dateNaissance());
        eleve.setLieuNaissance(texteOptionnel(donnees.lieuNaissance(), "lieu de naissance", 120));
        eleve.setSexe(donnees.sexe());
        eleve.setStatut(donnees.statut() == null ? StatutEleve.INSCRIT : donnees.statut());
        eleve.setTuteurNom(texteOptionnel(donnees.tuteurNom(), "nom du tuteur", 150));
        eleve.setTuteurTelephone(texteOptionnel(donnees.tuteurTelephone(), "téléphone du tuteur", 30));
        eleve.setGroupe(donnees.groupeId() == null ? null : exigerGroupe(etablissementId, donnees.groupeId()));
    }

    /**
     * Les codes Massar sont comparés en majuscules sans espaces : la même
     * identité ne doit pas passer deux fois par une simple différence de casse.
     */
    static String normaliserCodeMassar(String codeMassar) {
        String valeur = texteObligatoire(codeMassar, "code Massar", 30);
        return valeur.toUpperCase(Locale.ROOT);
    }

    private static String texteObligatoire(String valeur, String champ, int longueurMax) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("Le champ « " + champ + " » est obligatoire");
        }
        String propre = valeur.trim();
        if (propre.length() > longueurMax) {
            throw new IllegalArgumentException(
                    "Le champ « " + champ + " » dépasse " + longueurMax + " caractères");
        }
        return propre;
    }

    private static String texteOptionnel(String valeur, String champ, int longueurMax) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return texteObligatoire(valeur, champ, longueurMax);
    }

    /**
     * Tri par nom puis prénom quand l'appelant n'en impose pas d'autre.
     */
    private static Pageable avecTriParDefaut(Pageable pageable) {
        if (pageable == null || !pageable.isPaged()) {
            return Pageable.unpaged(TRI_NOM);
        }
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), TRI_NOM);
    }

    private static String motifRecherche(String recherche) {
        if (recherche == null || recherche.isBlank()) {
            return null;
        }
        return "%" + recherche.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
