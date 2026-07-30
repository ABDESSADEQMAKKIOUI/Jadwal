package ma.jadwal.api.scolarite;

import ma.jadwal.api.dto.EleveReponse;
import ma.jadwal.api.dto.MappeursEleve;
import ma.jadwal.export.tableur.EcrivainTableur;
import ma.jadwal.scolarite.entite.Eleve;
import ma.jadwal.scolarite.entite.StatutEleve;
import ma.jadwal.scolarite.service.EleveService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Export de la liste des élèves en tableur. Les colonnes sont choisies par
 * l'appelant (paramètre {@code champs}) et restituées dans l'ordre demandé ;
 * en son absence, toutes les colonnes de la fiche sont exportées.
 * <p>
 * Les dates sortent au format {@code dd/MM/yyyy} et les énumérations sous leur
 * code : un fichier exporté est donc relisible par l'import Massar.
 * <p>
 * Le contenu n'est jamais journalisé et la sélection est toujours filtrée par
 * l'établissement du jeton.
 */
@Service
public class ExportElevesService {

    /** Formats de sortie proposés par le contrat REST. */
    public enum Format {
        CSV("csv", "text/csv; charset=utf-8"),
        XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        private final String extension;
        private final String typeMime;

        Format(String extension, String typeMime) {
            this.extension = extension;
            this.typeMime = typeMime;
        }
    }

    /**
     * Fichier prêt à être servi.
     *
     * @param nom      nom de fichier proposé au navigateur
     * @param typeMime valeur de l'en-tête {@code Content-Type}
     * @param contenu  octets du fichier
     */
    public record FichierExport(String nom, String typeMime, byte[] contenu) {
    }

    private static final DateTimeFormatter DATE_FRANCAISE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String NOM_FEUILLE = "Élèves";

    /**
     * Colonnes exportables. Le code est celui du contrat ELEVE_JSON, le libellé
     * est l'en-tête écrit dans le fichier.
     */
    private enum Colonne {

        CODE_MASSAR("codeMassar", "Code Massar", EleveReponse::codeMassar),
        NOM("nom", "Nom", EleveReponse::nom),
        PRENOM("prenom", "Prénom", EleveReponse::prenom),
        NOM_AR("nomAr", "Nom arabe", EleveReponse::nomAr),
        PRENOM_AR("prenomAr", "Prénom arabe", EleveReponse::prenomAr),
        DATE_NAISSANCE("dateNaissance", "Date de naissance",
                eleve -> eleve.dateNaissance() == null ? null : DATE_FRANCAISE.format(eleve.dateNaissance())),
        LIEU_NAISSANCE("lieuNaissance", "Lieu de naissance", EleveReponse::lieuNaissance),
        SEXE("sexe", "Sexe", EleveReponse::sexe),
        STATUT("statut", "Statut", EleveReponse::statut),
        GROUPE_LIBELLE("groupeLibelle", "Classe", EleveReponse::groupeLibelle),
        NIVEAU_LIBELLE("niveauLibelle", "Niveau", EleveReponse::niveauLibelle),
        TUTEUR_NOM("tuteurNom", "Tuteur", EleveReponse::tuteurNom),
        TUTEUR_TELEPHONE("tuteurTelephone", "Téléphone du tuteur", EleveReponse::tuteurTelephone),
        ID("id", "Identifiant", eleve -> eleve.id() == null ? null : String.valueOf(eleve.id())),
        GROUPE_ID("groupeId", "Identifiant de classe",
                eleve -> eleve.groupeId() == null ? null : String.valueOf(eleve.groupeId()));

        private final String code;
        private final String libelle;
        private final Function<EleveReponse, String> extracteur;

        Colonne(String code, String libelle, Function<EleveReponse, String> extracteur) {
            this.code = code;
            this.libelle = libelle;
            this.extracteur = extracteur;
        }
    }

    /** Colonnes retenues quand {@code champs} est absent : la fiche complète, sans les identifiants techniques. */
    private static final List<Colonne> COLONNES_PAR_DEFAUT = List.of(
            Colonne.CODE_MASSAR, Colonne.NOM, Colonne.PRENOM, Colonne.NOM_AR, Colonne.PRENOM_AR,
            Colonne.DATE_NAISSANCE, Colonne.LIEU_NAISSANCE, Colonne.SEXE, Colonne.STATUT,
            Colonne.GROUPE_LIBELLE, Colonne.NIVEAU_LIBELLE, Colonne.TUTEUR_NOM, Colonne.TUTEUR_TELEPHONE);

    /** Codes de colonnes acceptés, y compris quelques synonymes usuels. */
    private static final Map<String, Colonne> COLONNES_PAR_CODE = construireIndex();

    private final EleveService eleveService;

    public ExportElevesService(EleveService eleveService) {
        this.eleveService = eleveService;
    }

    /**
     * Construit le fichier d'export des élèves de l'établissement.
     *
     * @param champs liste de codes séparés par des virgules ; nul ou vide = toutes les colonnes
     * @throws IllegalArgumentException si un code de colonne est inconnu
     */
    @Transactional(readOnly = true)
    public FichierExport exporter(Long etablissementId, Long groupeId, Long niveauId, StatutEleve statut,
                                  String recherche, String champs, Format format) {
        List<Colonne> colonnes = colonnes(champs);
        List<String> entetes = colonnes.stream().map(colonne -> colonne.libelle).toList();

        List<Eleve> eleves = eleveService
                .rechercher(etablissementId, groupeId, niveauId, statut, recherche, Pageable.unpaged())
                .getContent();

        List<List<String>> lignes = new ArrayList<>(eleves.size());
        for (Eleve eleve : eleves) {
            EleveReponse fiche = MappeursEleve.versEleveReponse(eleve);
            List<String> ligne = new ArrayList<>(colonnes.size());
            for (Colonne colonne : colonnes) {
                ligne.add(colonne.extracteur.apply(fiche));
            }
            lignes.add(ligne);
        }

        String nom = "eleves-" + LocalDate.now() + "." + format.extension;
        byte[] contenu = format == Format.XLSX
                ? EcrivainTableur.xlsx(entetes, lignes, NOM_FEUILLE)
                : EcrivainTableur.csv(entetes, lignes);
        return new FichierExport(nom, format.typeMime, contenu);
    }

    /** Résolution de {@code champs} : ordre demandé conservé, doublons écartés. */
    private static List<Colonne> colonnes(String champs) {
        if (champs == null || champs.isBlank()) {
            return COLONNES_PAR_DEFAUT;
        }
        Set<Colonne> retenues = new LinkedHashSet<>();
        for (String demande : champs.split(",")) {
            String code = demande.trim();
            if (code.isEmpty()) {
                continue;
            }
            Colonne colonne = COLONNES_PAR_CODE.get(code.toLowerCase(Locale.ROOT));
            if (colonne == null) {
                throw new IllegalArgumentException("Champ d'export inconnu : « " + code
                        + " ». Champs acceptés : " + codesAcceptes() + ".");
            }
            retenues.add(colonne);
        }
        if (retenues.isEmpty()) {
            return COLONNES_PAR_DEFAUT;
        }
        return List.copyOf(retenues);
    }

    private static String codesAcceptes() {
        List<String> codes = new ArrayList<>();
        for (Colonne colonne : Colonne.values()) {
            codes.add(colonne.code);
        }
        return String.join(", ", codes);
    }

    private static Map<String, Colonne> construireIndex() {
        Map<String, Colonne> index = new LinkedHashMap<>();
        for (Colonne colonne : Colonne.values()) {
            index.put(colonne.code.toLowerCase(Locale.ROOT), colonne);
        }
        index.put("massar", Colonne.CODE_MASSAR);
        index.put("classe", Colonne.GROUPE_LIBELLE);
        index.put("groupe", Colonne.GROUPE_LIBELLE);
        index.put("niveau", Colonne.NIVEAU_LIBELLE);
        index.put("tuteur", Colonne.TUTEUR_NOM);
        index.put("telephone", Colonne.TUTEUR_TELEPHONE);
        return index;
    }
}
