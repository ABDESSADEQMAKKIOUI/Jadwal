package ma.jadwal.api.controleur;

import jakarta.validation.Valid;
import ma.jadwal.abonnement.entite.ModuleJadwal;
import ma.jadwal.abonnement.service.ModuleService;
import ma.jadwal.api.dto.AnalyseImportReponse;
import ma.jadwal.api.dto.CreationEleveRequete;
import ma.jadwal.api.dto.EleveReponse;
import ma.jadwal.api.dto.MappeursEleve;
import ma.jadwal.api.dto.ModificationEleveRequete;
import ma.jadwal.api.dto.PageReponse;
import ma.jadwal.api.dto.ValidationImportReponse;
import ma.jadwal.api.dto.ValidationImportRequete;
import ma.jadwal.api.scolarite.ExportElevesService;
import ma.jadwal.api.scolarite.ImportMassarService;
import ma.jadwal.common.UtilisateurCourant;
import ma.jadwal.scolarite.entite.Eleve;
import ma.jadwal.scolarite.entite.Sexe;
import ma.jadwal.scolarite.entite.StatutEleve;
import ma.jadwal.scolarite.service.DonneesEleve;
import ma.jadwal.scolarite.service.EleveService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

/**
 * Élèves de l'établissement : fiches, import Massar en deux temps et exports
 * tableur (module VIE_SCOLAIRE, rôle DIRECTEUR).
 * <p>
 * Ces données sont des données personnelles de mineurs. Deux règles gouvernent
 * chaque méthode :
 * <ul>
 *   <li>l'établissement vient EXCLUSIVEMENT du jeton ({@link ContexteEcole}),
 *       jamais d'un paramètre ni d'un corps de requête ;</li>
 *   <li>l'accès est conditionné à l'abonnement, vérifié en tête de méthode par
 *       {@link ModuleService#exigerModule}.</li>
 * </ul>
 * Aucune entité JPA n'est sérialisée : tout passe par des records DTO, et rien
 * de nominatif n'est journalisé.
 */
@RestController
@RequestMapping("/api/ecole/eleves")
public class EleveController {

    /** Bornes de pagination : évite qu'une page démesurée serve de canal d'extraction. */
    private static final int TAILLE_PAGE_MAXI = 200;
    private static final int TAILLE_PAGE_DEFAUT = 50;

    private final EleveService eleveService;
    private final ModuleService moduleService;
    private final ImportMassarService importMassarService;
    private final ExportElevesService exportElevesService;

    public EleveController(EleveService eleveService, ModuleService moduleService,
                           ImportMassarService importMassarService, ExportElevesService exportElevesService) {
        this.eleveService = eleveService;
        this.moduleService = moduleService;
        this.importMassarService = importMassarService;
        this.exportElevesService = exportElevesService;
    }

    // ------------------------------------------------------------------
    // Fiches
    // ------------------------------------------------------------------

    @GetMapping
    public PageReponse<EleveReponse> lister(@AuthenticationPrincipal UtilisateurCourant courant,
                                            @RequestParam(required = false) String recherche,
                                            @RequestParam(required = false) Long groupeId,
                                            @RequestParam(required = false) Long niveauId,
                                            @RequestParam(required = false) String statut,
                                            @RequestParam(required = false, defaultValue = "0") int page,
                                            @RequestParam(required = false, defaultValue = "50") int taille) {
        Long etablissementId = etablissement(courant);
        PageRequest pagination = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(taille, 1), TAILLE_PAGE_MAXI));
        Page<Eleve> resultat = eleveService.rechercher(etablissementId, groupeId, niveauId,
                statut(statut), recherche, pagination);
        return PageReponse.depuis(resultat, MappeursEleve::versEleveReponse);
    }

    @GetMapping("/{id}")
    public EleveReponse obtenir(@AuthenticationPrincipal UtilisateurCourant courant, @PathVariable Long id) {
        Long etablissementId = etablissement(courant);
        return MappeursEleve.versEleveReponse(eleveService.obtenir(etablissementId, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EleveReponse creer(@AuthenticationPrincipal UtilisateurCourant courant,
                              @Valid @RequestBody CreationEleveRequete requete) {
        Long etablissementId = etablissement(courant);
        DonneesEleve donnees = new DonneesEleve(
                requete.codeMassar(),
                requete.nom(),
                requete.prenom(),
                requete.nomAr(),
                requete.prenomAr(),
                requete.dateNaissance(),
                requete.lieuNaissance(),
                sexe(requete.sexe()),
                statut(requete.statut()),
                requete.tuteurNom(),
                requete.tuteurTelephone(),
                requete.groupeId());
        return MappeursEleve.versEleveReponse(eleveService.creer(etablissementId, donnees));
    }

    /**
     * Modification partielle : tout champ nul reste inchangé. Le service
     * remplaçant la fiche entière, on repart de l'élève existant — lui-même
     * chargé filtré par établissement.
     */
    @PatchMapping("/{id}")
    public EleveReponse modifier(@AuthenticationPrincipal UtilisateurCourant courant,
                                 @PathVariable Long id,
                                 @Valid @RequestBody ModificationEleveRequete requete) {
        Long etablissementId = etablissement(courant);
        Eleve existant = eleveService.obtenir(etablissementId, id);
        DonneesEleve donnees = new DonneesEleve(
                valeur(requete.codeMassar(), existant.getCodeMassar()),
                valeur(requete.nom(), existant.getNom()),
                valeur(requete.prenom(), existant.getPrenom()),
                valeur(requete.nomAr(), existant.getNomAr()),
                valeur(requete.prenomAr(), existant.getPrenomAr()),
                requete.dateNaissance() == null ? existant.getDateNaissance() : requete.dateNaissance(),
                valeur(requete.lieuNaissance(), existant.getLieuNaissance()),
                requete.sexe() == null ? existant.getSexe() : sexe(requete.sexe()),
                requete.statut() == null ? existant.getStatut() : statut(requete.statut()),
                valeur(requete.tuteurNom(), existant.getTuteurNom()),
                valeur(requete.tuteurTelephone(), existant.getTuteurTelephone()),
                groupeCible(requete, existant));
        return MappeursEleve.versEleveReponse(eleveService.mettreAJour(etablissementId, id, donnees));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@AuthenticationPrincipal UtilisateurCourant courant, @PathVariable Long id) {
        Long etablissementId = etablissement(courant);
        eleveService.supprimer(etablissementId, id);
    }

    // ------------------------------------------------------------------
    // Import Massar (deux temps : analyse puis validation humaine)
    // ------------------------------------------------------------------

    /**
     * Analyse un export Massar au format CSV et renvoie le rapport SANS RIEN
     * ÉCRIRE. Le fichier reste en mémoire : il n'est ni stocké ni journalisé.
     */
    @PostMapping(value = "/import/analyser", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalyseImportReponse analyserImport(@AuthenticationPrincipal UtilisateurCourant courant,
                                               @RequestPart(value = "fichier", required = false)
                                               MultipartFile fichier) {
        Long etablissementId = etablissement(courant);
        return importMassarService.analyser(etablissementId, fichier);
    }

    /** Écrit les seules lignes retenues par l'utilisateur. Transactionnel. */
    @PostMapping("/import/valider")
    public ValidationImportReponse validerImport(@AuthenticationPrincipal UtilisateurCourant courant,
                                                 @Valid @RequestBody ValidationImportRequete requete) {
        Long etablissementId = etablissement(courant);
        return importMassarService.valider(etablissementId, requete);
    }

    // ------------------------------------------------------------------
    // Exports tableur
    // ------------------------------------------------------------------

    /** CSV avec BOM UTF-8 et séparateur « ; » : ouvrable directement dans Excel francophone. */
    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exporterCsv(@AuthenticationPrincipal UtilisateurCourant courant,
                                              @RequestParam(required = false) String champs,
                                              @RequestParam(required = false) String recherche,
                                              @RequestParam(required = false) Long groupeId,
                                              @RequestParam(required = false) Long niveauId,
                                              @RequestParam(required = false) String statut) {
        return servir(exporter(courant, champs, recherche, groupeId, niveauId, statut,
                ExportElevesService.Format.CSV));
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> exporterXlsx(@AuthenticationPrincipal UtilisateurCourant courant,
                                               @RequestParam(required = false) String champs,
                                               @RequestParam(required = false) String recherche,
                                               @RequestParam(required = false) Long groupeId,
                                               @RequestParam(required = false) Long niveauId,
                                               @RequestParam(required = false) String statut) {
        return servir(exporter(courant, champs, recherche, groupeId, niveauId, statut,
                ExportElevesService.Format.XLSX));
    }

    private ExportElevesService.FichierExport exporter(UtilisateurCourant courant, String champs, String recherche,
                                                       Long groupeId, Long niveauId, String statut,
                                                       ExportElevesService.Format format) {
        Long etablissementId = etablissement(courant);
        return exportElevesService.exporter(etablissementId, groupeId, niveauId, statut(statut), recherche,
                champs, format);
    }

    /**
     * Le {@code Content-Type} est posé explicitement : Spring s'en sert tel quel
     * et ne renégocie pas selon l'en-tête {@code Accept} du proxy.
     */
    private static ResponseEntity<byte[]> servir(ExportElevesService.FichierExport fichier) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, fichier.typeMime())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fichier.nom() + "\"")
                .body(fichier.contenu());
    }

    // ------------------------------------------------------------------
    // Garde-fous communs
    // ------------------------------------------------------------------

    /**
     * Établissement du jeton, puis vérification de l'abonnement : appelée en
     * tête de CHAQUE méthode du contrôleur.
     */
    private Long etablissement(UtilisateurCourant courant) {
        Long etablissementId = ContexteEcole.etablissementId(courant);
        moduleService.exigerModule(etablissementId, ModuleJadwal.VIE_SCOLAIRE);
        return etablissementId;
    }

    /** Groupe visé par une modification : retrait explicite, valeur fournie, ou classe actuelle. */
    private static Long groupeCible(ModificationEleveRequete requete, Eleve existant) {
        if (requete.retraitDemande()) {
            return null;
        }
        if (requete.groupeId() != null) {
            return requete.groupeId();
        }
        return existant.getGroupe() == null ? null : existant.getGroupe().getId();
    }

    /** Champ texte facultatif : nul = inchangé, chaîne vide = effacé. */
    private static String valeur(String fourni, String actuel) {
        return fourni == null ? actuel : fourni;
    }

    private static Sexe sexe(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return Sexe.valueOf(valeur.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Sexe invalide : « " + valeur + " ». Valeurs acceptées : M, F.");
        }
    }

    private static StatutEleve statut(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return StatutEleve.valueOf(valeur.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Statut invalide : « " + valeur
                    + " ». Valeurs acceptées : INSCRIT, PARTI, REDOUBLANT.");
        }
    }
}
