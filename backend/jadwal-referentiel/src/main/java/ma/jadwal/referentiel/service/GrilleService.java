package ma.jadwal.referentiel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.referentiel.depot.CreneauRepository;
import ma.jadwal.referentiel.depot.EtablissementRepository;
import ma.jadwal.referentiel.entite.Creneau;
import ma.jadwal.referentiel.entite.Etablissement;
import ma.jadwal.referentiel.entite.Jour;
import ma.jadwal.referentiel.entite.TypeCreneau;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GrilleService {

    private final EtablissementRepository etablissementRepository;
    private final CreneauRepository creneauRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GrilleService(EtablissementRepository etablissementRepository, CreneauRepository creneauRepository) {
        this.etablissementRepository = etablissementRepository;
        this.creneauRepository = creneauRepository;
    }

    @Transactional(readOnly = true)
    public GrilleConfig lireGrille(Long etablissementId) {
        Etablissement etablissement = obtenirEtablissement(etablissementId);
        if (etablissement.getGrilleJson() == null || etablissement.getGrilleJson().isBlank()) {
            return null;
        }
        return parser(etablissement.getGrilleJson());
    }

    @Transactional
    public GrilleConfig enregistrerGrille(Long etablissementId, GrilleConfig config) {
        valider(config);
        Etablissement etablissement = obtenirEtablissement(etablissementId);
        etablissement.setGrilleJson(serialiser(config));
        etablissementRepository.save(etablissement);
        regenererCreneauxInterne(etablissement, config);
        return config;
    }

    @Transactional(readOnly = true)
    public List<Creneau> listerCreneaux(Long etablissementId) {
        return creneauRepository.findByEtablissementIdOrderByJourAscIndexDebutAsc(etablissementId);
    }

    /**
     * Recrée l'intégralité des créneaux de l'établissement à partir de sa grille JSON :
     * un créneau par unité COURS (unites_disponibles = nombre d'unités COURS contiguës
     * restantes jusqu'à la prochaine plage bloquée ou la fin de journée) et un créneau
     * par plage bloquée (unites_disponibles = 0).
     */
    @Transactional
    public void regenererCreneaux(Long etablissementId) {
        Etablissement etablissement = obtenirEtablissement(etablissementId);
        if (etablissement.getGrilleJson() == null || etablissement.getGrilleJson().isBlank()) {
            throw new ConflitException("La grille horaire de l'établissement n'est pas configurée.");
        }
        GrilleConfig config = parser(etablissement.getGrilleJson());
        valider(config);
        regenererCreneauxInterne(etablissement, config);
    }

    private void regenererCreneauxInterne(Etablissement etablissement, GrilleConfig config) {
        creneauRepository.deleteByEtablissementId(etablissement.getId());
        creneauRepository.flush();

        int unitesParJour = config.unitesParJour();
        List<Creneau> nouveaux = new ArrayList<>();

        for (String libelleJour : config.joursActifs()) {
            Jour jour = convertirJour(libelleJour);

            boolean[] bloquee = new boolean[unitesParJour];
            Map<Integer, GrilleConfig.PlageBloquee> plagesParDebut = new LinkedHashMap<>();
            if (config.plagesBloquees() != null) {
                for (GrilleConfig.PlageBloquee plage : config.plagesBloquees()) {
                    if (plage.indexDebut() < 0 || plage.indexDebut() >= unitesParJour) {
                        throw new ConflitException("Plage bloquée hors de la grille : index " + plage.indexDebut());
                    }
                    plagesParDebut.put(plage.indexDebut(), plage);
                    int fin = Math.min(plage.indexDebut() + Math.max(1, plage.dureeUnites()), unitesParJour);
                    for (int i = plage.indexDebut(); i < fin; i++) {
                        bloquee[i] = true;
                    }
                }
            }

            int index = 0;
            while (index < unitesParJour) {
                GrilleConfig.PlageBloquee plage = plagesParDebut.get(index);
                if (plage != null) {
                    nouveaux.add(nouveauCreneau(etablissement, jour, index, convertirTypePlage(plage.type()), 0));
                    index += Math.max(1, plage.dureeUnites());
                } else if (bloquee[index]) {
                    index++;
                } else {
                    int finContigue = index;
                    while (finContigue < unitesParJour && !bloquee[finContigue]) {
                        finContigue++;
                    }
                    for (int i = index; i < finContigue; i++) {
                        nouveaux.add(nouveauCreneau(etablissement, jour, i, TypeCreneau.COURS, finContigue - i));
                    }
                    index = finContigue;
                }
            }
        }

        creneauRepository.saveAll(nouveaux);
    }

    private Creneau nouveauCreneau(Etablissement etablissement, Jour jour, int indexDebut, TypeCreneau type,
                                   int unitesDisponibles) {
        Creneau creneau = new Creneau();
        creneau.setEtablissement(etablissement);
        creneau.setJour(jour);
        creneau.setIndexDebut(indexDebut);
        creneau.setType(type);
        creneau.setUnitesDisponibles(unitesDisponibles);
        return creneau;
    }

    private Jour convertirJour(String libelle) {
        try {
            return Jour.valueOf(libelle);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ConflitException("Jour inconnu dans la grille : " + libelle);
        }
    }

    private TypeCreneau convertirTypePlage(String type) {
        try {
            TypeCreneau typeCreneau = TypeCreneau.valueOf(type);
            if (typeCreneau == TypeCreneau.COURS) {
                throw new ConflitException("Une plage bloquée ne peut pas être de type COURS.");
            }
            return typeCreneau;
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ConflitException("Type de plage bloquée inconnu : " + type);
        }
    }

    private void valider(GrilleConfig config) {
        if (config == null) {
            throw new ConflitException("La configuration de la grille est obligatoire.");
        }
        if (config.joursActifs() == null || config.joursActifs().isEmpty()) {
            throw new ConflitException("La grille doit comporter au moins un jour actif.");
        }
        if (config.unitesParJour() <= 0) {
            throw new ConflitException("Le nombre d'unités par jour doit être strictement positif.");
        }
        if (config.dureeUniteMinutes() <= 0) {
            throw new ConflitException("La durée d'une unité doit être strictement positive.");
        }
    }

    private GrilleConfig parser(String json) {
        try {
            return objectMapper.readValue(json, GrilleConfig.class);
        } catch (JsonProcessingException e) {
            throw new ConflitException("Grille horaire illisible : " + e.getOriginalMessage());
        }
    }

    private String serialiser(GrilleConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new ConflitException("Impossible de sérialiser la grille horaire.");
        }
    }

    private Etablissement obtenirEtablissement(Long etablissementId) {
        return etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Établissement introuvable : " + etablissementId));
    }
}
