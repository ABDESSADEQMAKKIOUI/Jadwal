package ma.jadwal.api.scolarite;

import ma.jadwal.api.dto.AnalyseImportReponse;
import ma.jadwal.api.dto.EleveReponse;
import ma.jadwal.api.dto.LigneImportEleve;
import ma.jadwal.api.dto.ResumeImportEleves;
import ma.jadwal.api.dto.ValidationImportReponse;
import ma.jadwal.api.dto.ValidationImportRequete;
import ma.jadwal.referentiel.depot.GroupeRepository;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.scolarite.entite.Eleve;
import ma.jadwal.scolarite.entite.Sexe;
import ma.jadwal.scolarite.entite.StatutEleve;
import ma.jadwal.scolarite.service.DonneesEleve;
import ma.jadwal.scolarite.service.EleveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Import d'un fichier d'élèves exporté de Massar, en deux temps : une analyse
 * qui n'écrit RIEN et se relit à l'écran, puis une validation transactionnelle
 * des seules lignes retenues par l'utilisateur (même principe que la règle D-04).
 * <p>
 * Le fichier n'est jamais écrit sur disque et son contenu n'est jamais
 * journalisé : il ne contient que des données personnelles de mineurs. Les
 * traces se limitent à l'identifiant d'établissement et à des compteurs.
 * <p>
 * L'établissement est toujours celui du jeton, transmis par le contrôleur :
 * toutes les lectures (élèves existants, classes) et toutes les écritures
 * passent par lui, ce qui rend impossible l'import dans un autre locataire.
 */
@Service
public class ImportMassarService {

    private static final Logger journal = LoggerFactory.getLogger(ImportMassarService.class);

    /** Taille maximale acceptée pour le fichier téléversé. */
    public static final long TAILLE_MAXI_OCTETS = 5L * 1024 * 1024;

    /** Nombre maximal de lignes traitées en un lot. */
    public static final int LIGNES_MAXI = 5000;

    // Champs canoniques du contrat ELEVE_JSON reconnus dans l'en-tête.
    private static final String CODE_MASSAR = "codeMassar";
    private static final String NOM = "nom";
    private static final String PRENOM = "prenom";
    private static final String NOM_AR = "nomAr";
    private static final String PRENOM_AR = "prenomAr";
    private static final String DATE_NAISSANCE = "dateNaissance";
    private static final String LIEU_NAISSANCE = "lieuNaissance";
    private static final String SEXE = "sexe";
    private static final String STATUT = "statut";
    private static final String CLASSE = "classe";
    private static final String TUTEUR_NOM = "tuteurNom";
    private static final String TUTEUR_TELEPHONE = "tuteurTelephone";

    /** En-têtes Massar usuels, indexés par clé compacte (sans accents, casse ni ponctuation). */
    private static final Map<String, String> ENTETES = construireEntetes();

    /** Formats de date rencontrés dans les exports Massar et les retouches Excel. */
    private static final List<DateTimeFormatter> FORMATS_DATE = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE),
            DateTimeFormatter.ofPattern("d/M/yyyy", Locale.FRANCE),
            DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.FRANCE),
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.FRANCE));

    private final EleveService eleveService;
    private final GroupeRepository groupeRepository;

    public ImportMassarService(EleveService eleveService, GroupeRepository groupeRepository) {
        this.eleveService = eleveService;
        this.groupeRepository = groupeRepository;
    }

    // ------------------------------------------------------------------
    // Temps 1 : analyse (aucune écriture)
    // ------------------------------------------------------------------

    /**
     * Lit le fichier en mémoire et produit le rapport d'analyse.
     *
     * @throws IllegalArgumentException fichier absent, vide, trop gros, ou en-tête illisible
     */
    @Transactional(readOnly = true)
    public AnalyseImportReponse analyser(Long etablissementId, MultipartFile fichier) {
        String texte = decoder(lire(fichier));
        char separateur = detecterSeparateur(texte);
        List<List<String>> enregistrements = decouper(texte, separateur);
        if (enregistrements.isEmpty()) {
            throw new IllegalArgumentException("Le fichier ne contient aucune ligne.");
        }
        if (enregistrements.size() - 1 > LIGNES_MAXI) {
            throw new IllegalArgumentException(
                    "Le fichier dépasse " + LIGNES_MAXI + " lignes : découpez-le en plusieurs lots.");
        }

        List<String> detectees = new ArrayList<>();
        List<String> ignorees = new ArrayList<>();
        Map<String, Integer> colonnes = lireEntete(enregistrements.get(0), detectees, ignorees);
        if (!colonnes.containsKey(CODE_MASSAR)) {
            throw new IllegalArgumentException("En-tête illisible : aucune colonne de code Massar reconnue "
                    + "(attendu « Code Massar », « Massar » ou « CNE » en première ligne).");
        }

        Map<String, Eleve> existants = elevesParCodeMassar(etablissementId);
        Map<String, List<Groupe>> classes = classesParLibelle(etablissementId);

        List<LigneImportEleve> lignes = new ArrayList<>();
        Map<String, Integer> dejaVus = new HashMap<>();
        int nouveaux = 0;
        int existantsComptes = 0;
        int erreurs = 0;

        for (int index = 1; index < enregistrements.size(); index++) {
            List<String> cellules = enregistrements.get(index);
            if (estLigneVide(cellules)) {
                continue;
            }
            LigneImportEleve ligne = analyserLigne(index + 1, cellules, colonnes, existants, classes, dejaVus);
            lignes.add(ligne);
            switch (ligne.statut()) {
                case LigneImportEleve.NOUVEAU -> nouveaux++;
                case LigneImportEleve.EXISTANT -> existantsComptes++;
                default -> erreurs++;
            }
        }

        journal.info("Analyse d'import Massar : établissement={} lignes={} nouveaux={} existants={} erreurs={}",
                etablissementId, lignes.size(), nouveaux, existantsComptes, erreurs);

        return new AnalyseImportReponse(detectees, ignorees, String.valueOf(separateur), lignes,
                new ResumeImportEleves(lignes.size(), nouveaux, existantsComptes, erreurs));
    }

    private LigneImportEleve analyserLigne(int numero, List<String> cellules, Map<String, Integer> colonnes,
                                           Map<String, Eleve> existants, Map<String, List<Groupe>> classes,
                                           Map<String, Integer> dejaVus) {
        List<String> messages = new ArrayList<>();
        boolean erreur = false;

        String code = normaliserCode(valeur(cellules, colonnes, CODE_MASSAR));
        if (code == null) {
            messages.add("Code Massar absent : la ligne ne peut pas être importée.");
            erreur = true;
        } else if (code.length() > 30) {
            messages.add("Le code Massar dépasse 30 caractères.");
            erreur = true;
        } else {
            Integer premiere = dejaVus.putIfAbsent(code, numero);
            if (premiere != null) {
                messages.add("Code Massar déjà présent à la ligne " + premiere + " du fichier.");
                erreur = true;
            }
        }

        int messagesAvantChamps = messages.size();
        String nom = borner(valeur(cellules, colonnes, NOM), 100, "Le nom", messages);
        String prenom = borner(valeur(cellules, colonnes, PRENOM), 100, "Le prénom", messages);
        String nomAr = borner(valeur(cellules, colonnes, NOM_AR), 100, "Le nom arabe", messages);
        String prenomAr = borner(valeur(cellules, colonnes, PRENOM_AR), 100, "Le prénom arabe", messages);
        String lieuNaissance = borner(valeur(cellules, colonnes, LIEU_NAISSANCE), 120,
                "Le lieu de naissance", messages);
        String tuteurNom = borner(valeur(cellules, colonnes, TUTEUR_NOM), 150, "Le nom du tuteur", messages);
        String tuteurTelephone = borner(valeur(cellules, colonnes, TUTEUR_TELEPHONE), 30,
                "Le téléphone du tuteur", messages);
        // Tout dépassement de longueur rend la ligne inexploitable : la base refuserait l'écriture.
        if (messages.size() > messagesAvantChamps) {
            erreur = true;
        }

        LocalDate dateNaissance = null;
        String dateBrute = valeur(cellules, colonnes, DATE_NAISSANCE);
        if (dateBrute != null) {
            dateNaissance = lireDate(dateBrute);
            if (dateNaissance == null) {
                messages.add("Date de naissance illisible (formats acceptés : 31/12/2012 ou 2012-12-31) :"
                        + " elle ne sera pas importée.");
            }
        }

        Sexe sexe = null;
        String sexeBrut = valeur(cellules, colonnes, SEXE);
        if (sexeBrut != null) {
            sexe = lireSexe(sexeBrut);
            if (sexe == null) {
                messages.add("Sexe non reconnu : il ne sera pas importé.");
            }
        }

        StatutEleve statut = null;
        String statutBrut = valeur(cellules, colonnes, STATUT);
        if (statutBrut != null) {
            statut = lireStatut(statutBrut);
            if (statut == null) {
                messages.add("Statut non reconnu : l'élève sera considéré comme inscrit.");
            }
        }

        Groupe groupe = null;
        String classeBrute = valeur(cellules, colonnes, CLASSE);
        if (classeBrute != null) {
            List<Groupe> candidats = classes.getOrDefault(cleCompacte(classeBrute), List.of());
            if (candidats.isEmpty()) {
                messages.add("Classe « " + classeBrute + " » inconnue : l'élève sera importé sans classe.");
            } else if (candidats.size() > 1) {
                messages.add("Plusieurs classes portent le libellé « " + classeBrute
                        + " » : l'élève sera importé sans classe.");
            } else {
                groupe = candidats.get(0);
            }
        }

        Eleve existant = code == null ? null : existants.get(code);
        if (!erreur && existant == null) {
            if (nom == null) {
                messages.add("Nom absent : obligatoire pour créer un élève.");
                erreur = true;
            }
            if (prenom == null) {
                messages.add("Prénom absent : obligatoire pour créer un élève.");
                erreur = true;
            }
        }

        String statutLigne = erreur ? LigneImportEleve.ERREUR
                : existant != null ? LigneImportEleve.EXISTANT : LigneImportEleve.NOUVEAU;

        EleveReponse donnees = new EleveReponse(
                existant == null ? null : existant.getId(),
                code, nom, prenom, nomAr, prenomAr, dateNaissance, lieuNaissance,
                sexe == null ? null : sexe.name(),
                statut == null ? null : statut.name(),
                groupe == null ? null : groupe.getId(),
                groupe == null ? null : groupe.getLibelle(),
                groupe == null || groupe.getNiveau() == null ? null : groupe.getNiveau().getLibelle(),
                tuteurNom, tuteurTelephone);

        return new LigneImportEleve(numero, donnees, statutLigne, List.copyOf(messages));
    }

    // ------------------------------------------------------------------
    // Temps 2 : validation (transactionnelle)
    // ------------------------------------------------------------------

    /**
     * Écrit les lignes retenues. Rien n'est écrit si une seule opération échoue.
     * <p>
     * L'identifiant d'élève éventuellement transmis par le client est ignoré :
     * le rapprochement se refait sur le code Massar de l'établissement du jeton.
     * Une ligne marquée en erreur, dont le code Massar est absent ou déjà traité
     * dans le lot, est comptée dans {@code ignores} sans être écrite.
     */
    @Transactional
    public ValidationImportReponse valider(Long etablissementId, ValidationImportRequete requete) {
        List<LigneImportEleve> lignes = requete == null ? null : requete.lignes();
        if (lignes == null || lignes.isEmpty()) {
            throw new IllegalArgumentException("Aucune ligne à importer.");
        }
        if (lignes.size() > LIGNES_MAXI) {
            throw new IllegalArgumentException("Un import est limité à " + LIGNES_MAXI + " lignes par lot.");
        }

        Map<String, Eleve> existants = elevesParCodeMassar(etablissementId);
        Set<String> traites = new HashSet<>();
        int crees = 0;
        int misAJour = 0;
        int ignores = 0;

        for (LigneImportEleve ligne : lignes) {
            EleveReponse donnees = ligne == null ? null : ligne.donnees();
            String code = normaliserCode(donnees == null ? null : donnees.codeMassar());
            if (donnees == null || code == null || code.length() > 30
                    || LigneImportEleve.ERREUR.equalsIgnoreCase(ligne.statut())
                    || !longueursValides(donnees)
                    || !traites.add(code)) {
                ignores++;
                continue;
            }
            Eleve existant = existants.get(code);
            if (existant == null) {
                if (vide(donnees.nom()) || vide(donnees.prenom())) {
                    ignores++;
                    continue;
                }
                eleveService.creer(etablissementId, donneesCreation(code, donnees));
                crees++;
            } else if (requete.miseAJourDemandee()) {
                eleveService.mettreAJour(etablissementId, existant.getId(), donneesFusionnees(code, existant, donnees));
                misAJour++;
            } else {
                ignores++;
            }
        }

        journal.info("Import Massar validé : établissement={} créés={} misÀJour={} ignorés={}",
                etablissementId, crees, misAJour, ignores);
        return new ValidationImportReponse(crees, misAJour, ignores);
    }

    private DonneesEleve donneesCreation(String code, EleveReponse donnees) {
        return new DonneesEleve(code,
                texte(donnees.nom()),
                texte(donnees.prenom()),
                texte(donnees.nomAr()),
                texte(donnees.prenomAr()),
                donnees.dateNaissance(),
                texte(donnees.lieuNaissance()),
                lireSexe(donnees.sexe()),
                lireStatut(donnees.statut()),
                texte(donnees.tuteurNom()),
                texte(donnees.tuteurTelephone()),
                donnees.groupeId());
    }

    /**
     * Mise à jour d'un élève déjà inscrit : le service remplace l'intégralité de
     * la fiche, on repart donc de l'existant et on n'écrase que les champs
     * réellement renseignés par le fichier. Une cellule vide ne détruit jamais
     * une donnée déjà saisie.
     */
    private DonneesEleve donneesFusionnees(String code, Eleve existant, EleveReponse donnees) {
        Sexe sexe = lireSexe(donnees.sexe());
        StatutEleve statut = lireStatut(donnees.statut());
        Long groupeExistant = existant.getGroupe() == null ? null : existant.getGroupe().getId();
        return new DonneesEleve(code,
                premier(texte(donnees.nom()), existant.getNom()),
                premier(texte(donnees.prenom()), existant.getPrenom()),
                premier(texte(donnees.nomAr()), existant.getNomAr()),
                premier(texte(donnees.prenomAr()), existant.getPrenomAr()),
                donnees.dateNaissance() == null ? existant.getDateNaissance() : donnees.dateNaissance(),
                premier(texte(donnees.lieuNaissance()), existant.getLieuNaissance()),
                sexe == null ? existant.getSexe() : sexe,
                statut == null ? existant.getStatut() : statut,
                premier(texte(donnees.tuteurNom()), existant.getTuteurNom()),
                premier(texte(donnees.tuteurTelephone()), existant.getTuteurTelephone()),
                donnees.groupeId() == null ? groupeExistant : donnees.groupeId());
    }

    // ------------------------------------------------------------------
    // Lectures filtrées par établissement
    // ------------------------------------------------------------------

    /**
     * Élèves de l'établissement indexés par code Massar : une seule requête
     * plutôt qu'une par ligne du fichier.
     */
    private Map<String, Eleve> elevesParCodeMassar(Long etablissementId) {
        Map<String, Eleve> parCode = new HashMap<>();
        for (Eleve eleve : eleveService.rechercher(etablissementId, null, null, null, Pageable.unpaged())) {
            parCode.putIfAbsent(eleve.getCodeMassar(), eleve);
        }
        return parCode;
    }

    /** Classes de l'établissement indexées par libellé normalisé (rapprochement du champ « classe »). */
    private Map<String, List<Groupe>> classesParLibelle(Long etablissementId) {
        Map<String, List<Groupe>> parLibelle = new HashMap<>();
        for (Groupe groupe : groupeRepository.findByEtablissementIdOrderByLibelleAsc(etablissementId)) {
            parLibelle.computeIfAbsent(cleCompacte(groupe.getLibelle()), cle -> new ArrayList<>()).add(groupe);
        }
        return parLibelle;
    }

    // ------------------------------------------------------------------
    // Lecture du fichier
    // ------------------------------------------------------------------

    private static byte[] lire(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Aucun fichier reçu, ou fichier vide.");
        }
        if (fichier.getSize() > TAILLE_MAXI_OCTETS) {
            throw new IllegalArgumentException("Le fichier dépasse la taille maximale autorisée (5 Mo).");
        }
        byte[] octets;
        try {
            octets = fichier.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Lecture du fichier téléversé impossible.", exception);
        }
        if (octets.length > TAILLE_MAXI_OCTETS) {
            throw new IllegalArgumentException("Le fichier dépasse la taille maximale autorisée (5 Mo).");
        }
        verifierFormat(octets);
        return octets;
    }

    /** Refuse d'emblée les formats binaires les plus courants pour donner un message utile. */
    private static void verifierFormat(byte[] octets) {
        if (octets.length >= 2 && octets[0] == 'P' && octets[1] == 'K') {
            throw new IllegalArgumentException("Ce fichier est un classeur Excel : enregistrez-le au format "
                    + "CSV (UTF-8) depuis Excel avant de l'importer.");
        }
        if (octets.length >= 2 && (octets[0] == (byte) 0xFF && octets[1] == (byte) 0xFE
                || octets[0] == (byte) 0xFE && octets[1] == (byte) 0xFF)) {
            throw new IllegalArgumentException("Encodage UTF-16 non pris en charge : enregistrez le fichier "
                    + "en CSV UTF-8.");
        }
    }

    /**
     * Décodage UTF-8, avec repli sur Windows-1252 : un CSV retouché dans Excel
     * francophone sort souvent dans cette page de code, et un décodage UTF-8
     * strict le rendrait illisible.
     */
    private static String decoder(byte[] octets) {
        String texte;
        try {
            texte = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(octets))
                    .toString();
        } catch (CharacterCodingException exception) {
            texte = new String(octets, Charset.forName("windows-1252"));
        }
        if (!texte.isEmpty() && texte.charAt(0) == '\uFEFF') {
            texte = texte.substring(1);
        }
        return texte;
    }

    /** Séparateur le plus fréquent sur la ligne d'en-tête, hors guillemets ; « ; » en cas d'égalité. */
    static char detecterSeparateur(String texte) {
        int pointsVirgules = 0;
        int virgules = 0;
        boolean entreGuillemets = false;
        for (int i = 0; i < texte.length(); i++) {
            char caractere = texte.charAt(i);
            if (caractere == '"') {
                entreGuillemets = !entreGuillemets;
            } else if (entreGuillemets) {
                continue;
            } else if (caractere == '\n' || caractere == '\r') {
                break;
            } else if (caractere == ';') {
                pointsVirgules++;
            } else if (caractere == ',') {
                virgules++;
            }
        }
        return virgules > pointsVirgules ? ',' : ';';
    }

    /**
     * Découpage CSV (RFC 4180) : guillemets doublés, séparateurs et sauts de
     * ligne admis à l'intérieur des guillemets, fins de ligne CRLF, LF ou CR.
     */
    static List<List<String>> decouper(String texte, char separateur) {
        List<List<String>> enregistrements = new ArrayList<>();
        List<String> courant = new ArrayList<>();
        StringBuilder cellule = new StringBuilder();
        boolean entreGuillemets = false;
        int i = 0;
        while (i < texte.length()) {
            char caractere = texte.charAt(i);
            if (entreGuillemets) {
                if (caractere == '"') {
                    if (i + 1 < texte.length() && texte.charAt(i + 1) == '"') {
                        cellule.append('"');
                        i += 2;
                        continue;
                    }
                    entreGuillemets = false;
                } else {
                    cellule.append(caractere);
                }
                i++;
                continue;
            }
            if (caractere == '"') {
                entreGuillemets = true;
                i++;
            } else if (caractere == separateur) {
                courant.add(cellule.toString());
                cellule.setLength(0);
                i++;
            } else if (caractere == '\n' || caractere == '\r') {
                courant.add(cellule.toString());
                cellule.setLength(0);
                enregistrements.add(courant);
                courant = new ArrayList<>();
                i += caractere == '\r' && i + 1 < texte.length() && texte.charAt(i + 1) == '\n' ? 2 : 1;
            } else {
                cellule.append(caractere);
                i++;
            }
        }
        if (cellule.length() > 0 || !courant.isEmpty()) {
            courant.add(cellule.toString());
            enregistrements.add(courant);
        }
        return enregistrements;
    }

    /** Associe chaque champ canonique reconnu à son index de colonne. */
    private static Map<String, Integer> lireEntete(List<String> entete, List<String> detectees,
                                                   List<String> ignorees) {
        Map<String, Integer> colonnes = new LinkedHashMap<>();
        for (int i = 0; i < entete.size(); i++) {
            String brut = entete.get(i) == null ? "" : entete.get(i).trim();
            String champ = ENTETES.get(cleCompacte(brut));
            if (champ == null || colonnes.containsKey(champ)) {
                if (!brut.isBlank()) {
                    ignorees.add(brut);
                }
                continue;
            }
            colonnes.put(champ, i);
            detectees.add(champ);
        }
        return colonnes;
    }

    private static String valeur(List<String> cellules, Map<String, Integer> colonnes, String champ) {
        Integer index = colonnes.get(champ);
        if (index == null || index >= cellules.size()) {
            return null;
        }
        return texte(cellules.get(index));
    }

    private static boolean estLigneVide(List<String> cellules) {
        for (String cellule : cellules) {
            if (!vide(cellule)) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Conversions tolérantes
    // ------------------------------------------------------------------

    static LocalDate lireDate(String valeur) {
        String propre = texte(valeur);
        if (propre == null) {
            return null;
        }
        // Un export passé par Excel peut porter l'heure : « 31/12/2012 00:00 ».
        int espace = propre.indexOf(' ');
        if (espace > 0) {
            propre = propre.substring(0, espace);
        }
        for (DateTimeFormatter format : FORMATS_DATE) {
            try {
                return LocalDate.parse(propre, format);
            } catch (DateTimeParseException ignoree) {
                // Format suivant.
            }
        }
        return null;
    }

    /** M / F / H / Garçon / Fille / ذكر / أنثى, insensible à la casse et aux accents. */
    static Sexe lireSexe(String valeur) {
        String cle = cleCompacte(valeur);
        return switch (cle) {
            case "m", "h", "male", "masculin", "garcon", "g", "1", "ذكر" -> Sexe.M;
            case "f", "female", "feminin", "fille", "2", "انثى" -> Sexe.F;
            default -> null;
        };
    }

    static StatutEleve lireStatut(String valeur) {
        String cle = cleCompacte(valeur);
        return switch (cle) {
            case "inscrit", "inscrite", "actif", "present" -> StatutEleve.INSCRIT;
            case "parti", "partie", "sorti", "radie", "transfere" -> StatutEleve.PARTI;
            case "redoublant", "redoublante", "redouble" -> StatutEleve.REDOUBLANT;
            default -> null;
        };
    }

    /**
     * Clé de comparaison des libellés : accents et diacritiques retirés,
     * ponctuation et espaces supprimés, casse ignorée. Les lettres arabes sont
     * conservées, seules leurs diacritiques disparaissent.
     */
    static String cleCompacte(String valeur) {
        if (valeur == null) {
            return "";
        }
        String sansDiacritiques = Normalizer.normalize(valeur, Normalizer.Form.NFD)
                .replaceAll("\\p{Mn}+", "");
        StringBuilder compacte = new StringBuilder(sansDiacritiques.length());
        for (int i = 0; i < sansDiacritiques.length(); i++) {
            char caractere = sansDiacritiques.charAt(i);
            if (Character.isLetterOrDigit(caractere)) {
                compacte.append(Character.toLowerCase(caractere));
            }
        }
        return compacte.toString();
    }

    // ------------------------------------------------------------------
    // Utilitaires
    // ------------------------------------------------------------------

    private static String borner(String valeur, int longueurMax, String champ, List<String> messages) {
        if (valeur == null) {
            return null;
        }
        if (valeur.length() > longueurMax) {
            messages.add(champ + " dépasse " + longueurMax + " caractères.");
            return valeur;
        }
        return valeur;
    }

    /** Garde-fou du temps 2 : les longueurs sont revalidées avant toute écriture. */
    private static boolean longueursValides(EleveReponse donnees) {
        return longueurValide(donnees.nom(), 100)
                && longueurValide(donnees.prenom(), 100)
                && longueurValide(donnees.nomAr(), 100)
                && longueurValide(donnees.prenomAr(), 100)
                && longueurValide(donnees.lieuNaissance(), 120)
                && longueurValide(donnees.tuteurNom(), 150)
                && longueurValide(donnees.tuteurTelephone(), 30);
    }

    private static boolean longueurValide(String valeur, int longueurMax) {
        return valeur == null || valeur.trim().length() <= longueurMax;
    }

    private static String normaliserCode(String valeur) {
        String propre = texte(valeur);
        return propre == null ? null : propre.toUpperCase(Locale.ROOT);
    }

    private static String texte(String valeur) {
        if (valeur == null) {
            return null;
        }
        String propre = valeur.trim();
        return propre.isEmpty() ? null : propre;
    }

    private static boolean vide(String valeur) {
        return valeur == null || valeur.isBlank();
    }

    private static String premier(String valeur, String repli) {
        return valeur == null ? repli : valeur;
    }

    private static Map<String, String> construireEntetes() {
        Map<String, String> entetes = new HashMap<>();
        ajouter(entetes, CODE_MASSAR, "code massar", "codemassar", "massar", "cne", "code cne", "code national",
                "code eleve", "numero massar", "n massar", "رقم مسار", "مسار", "رمز مسار");
        ajouter(entetes, NOM, "nom", "nom de famille", "nom famille", "nom eleve", "nom latin", "nom fr");
        ajouter(entetes, PRENOM, "prenom", "prénom", "prenom eleve", "prenom latin", "prenom fr");
        ajouter(entetes, NOM_AR, "nom ar", "nom arabe", "nom en arabe", "الاسم العائلي", "النسب");
        ajouter(entetes, PRENOM_AR, "prenom ar", "prénom ar", "prenom arabe", "prenom en arabe",
                "الاسم الشخصي");
        ajouter(entetes, DATE_NAISSANCE, "date naissance", "date de naissance", "naissance", "ddn",
                "تاريخ الميلاد", "تاريخ الولادة");
        ajouter(entetes, LIEU_NAISSANCE, "lieu naissance", "lieu de naissance", "مكان الميلاد",
                "مكان الولادة");
        ajouter(entetes, SEXE, "sexe", "genre", "الجنس", "النوع");
        ajouter(entetes, STATUT, "statut", "situation", "statut eleve");
        ajouter(entetes, CLASSE, "classe", "groupe", "nom classe", "libelle classe", "القسم", "الفصل");
        ajouter(entetes, TUTEUR_NOM, "tuteur", "nom tuteur", "nom du tuteur", "tuteur nom", "responsable",
                "responsable legal", "pere");
        ajouter(entetes, TUTEUR_TELEPHONE, "telephone", "téléphone", "tel", "gsm", "portable", "mobile",
                "telephone tuteur", "tel tuteur", "telephone du tuteur", "هاتف");
        return entetes;
    }

    private static void ajouter(Map<String, String> entetes, String champ, String... libelles) {
        for (String libelle : libelles) {
            entetes.put(cleCompacte(libelle), champ);
        }
    }
}
