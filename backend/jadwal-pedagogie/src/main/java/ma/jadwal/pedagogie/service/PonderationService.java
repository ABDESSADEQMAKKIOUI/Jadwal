package ma.jadwal.pedagogie.service;

import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.pedagogie.depot.PonderationRepository;
import ma.jadwal.pedagogie.entite.Ponderation;
import ma.jadwal.referentiel.depot.EtablissementRepository;
import ma.jadwal.referentiel.entite.Etablissement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PonderationService {

    /** Règle souple pondérée, avec son libellé français et son poids effectif pour l'établissement. */
    public record ReglePonderee(String regle, String libelle, int poids) {
    }

    private static final int POIDS_PAR_DEFAUT = 1;

    /** Règles souples du moteur : libellés français, dans l'ordre d'affichage. */
    private static final Map<String, String> REGLES_SOUPLES = creerCatalogue();

    /** Poids par défaut spécifiques (toutes les autres règles valent 1). */
    private static final Map<String, Integer> POIDS_SPECIFIQUES = Map.of("G-04", 10);

    private static Map<String, String> creerCatalogue() {
        Map<String, String> regles = new LinkedHashMap<>();
        regles.put("G-04", "Limiter les trous dans l'emploi du temps des groupes");
        regles.put("G-05", "Équilibrer la charge quotidienne des groupes");
        regles.put("G-06", "Alterner les matières à fort poids cognitif");
        regles.put("F-02", "Limiter les trous dans l'emploi du temps des enseignants");
        regles.put("F-03", "Regrouper les cours des enseignants sur peu de journées");
        regles.put("F-04", "Respecter les préférences horaires des enseignants");
        regles.put("F-05", "Limiter l'amplitude journalière des enseignants");
        regles.put("F-06", "Équilibrer la charge hebdomadaire des enseignants");
        regles.put("D-08", "Éviter les matières lourdes en fin de journée");
        regles.put("D-09", "Éviter certaines matières juste avant le déjeuner");
        regles.put("D-10", "Répartir les séances d'une même matière sur la semaine");
        regles.put("E-04", "Stabiliser la salle d'un même groupe");
        regles.put("I-02", "Minimiser les déplacements entre bâtiments");
        return Collections.unmodifiableMap(regles);
    }

    private final PonderationRepository ponderationRepository;
    private final EtablissementRepository etablissementRepository;

    public PonderationService(PonderationRepository ponderationRepository,
                              EtablissementRepository etablissementRepository) {
        this.ponderationRepository = ponderationRepository;
        this.etablissementRepository = etablissementRepository;
    }

    /**
     * Liste des règles souples avec le poids effectif de l'établissement :
     * le poids enregistré s'il existe, sinon le poids par défaut (G-04 = 10, autres = 1).
     */
    @Transactional(readOnly = true)
    public List<ReglePonderee> listerReglesEffectives(Long etablissementId) {
        Map<String, Integer> enregistres = new HashMap<>();
        for (Ponderation ponderation : ponderationRepository.findByEtablissementId(etablissementId)) {
            enregistres.put(ponderation.getRegle(), ponderation.getPoids());
        }
        List<ReglePonderee> resultat = new ArrayList<>();
        for (Map.Entry<String, String> entree : REGLES_SOUPLES.entrySet()) {
            int poids = enregistres.getOrDefault(entree.getKey(), poidsParDefaut(entree.getKey()));
            resultat.add(new ReglePonderee(entree.getKey(), entree.getValue(), poids));
        }
        return resultat;
    }

    @Transactional
    public Ponderation definirPoids(Long etablissementId, String regle, int poids) {
        if (!REGLES_SOUPLES.containsKey(regle)) {
            throw new ConflitException("Règle souple inconnue : " + regle);
        }
        if (poids < 0) {
            throw new ConflitException("Le poids d'une règle doit être positif ou nul.");
        }
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Établissement introuvable : " + etablissementId));
        Ponderation ponderation = ponderationRepository.findByEtablissementIdAndRegle(etablissementId, regle)
                .orElseGet(Ponderation::new);
        ponderation.setEtablissement(etablissement);
        ponderation.setRegle(regle);
        ponderation.setPoids(poids);
        return ponderationRepository.save(ponderation);
    }

    @Transactional
    public void reinitialiser(Long etablissementId) {
        ponderationRepository.deleteAll(ponderationRepository.findByEtablissementId(etablissementId));
    }

    public static int poidsParDefaut(String regle) {
        return POIDS_SPECIFIQUES.getOrDefault(regle, POIDS_PAR_DEFAUT);
    }

    public static Map<String, String> reglesSouples() {
        return REGLES_SOUPLES;
    }
}
