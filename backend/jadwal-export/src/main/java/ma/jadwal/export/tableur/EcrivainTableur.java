package ma.jadwal.export.tableur;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Écrit un tableau de chaînes (une ligne d'en-têtes, puis les lignes de données) au format
 * tableur : CSV lisible par Excel francophone, ou vrai classeur OOXML {@code .xlsx}.
 * <p>
 * Classe utilitaire pure : ni Spring, ni JPA, ni accès disque. Les deux méthodes renvoient les
 * octets du fichier, à charge de l'appelant de les servir ou de les stocker.
 * <p>
 * <strong>Aucune transformation des valeurs</strong> n'est appliquée en dehors de l'échappement
 * strictement nécessaire au format : les valeurs sont restituées telles quelles (les accents,
 * l'arabe et les caractères spéciaux passent en UTF-8). C'est donc à l'appelant de ne pas
 * placer de données personnelles inutiles dans un export.
 */
public final class EcrivainTableur {

    /**
     * Marque d'ordre des octets UTF-8. Sans elle, Excel en configuration francophone lit un CSV
     * dans la page de code Windows-1252 : les accents et l'arabe deviennent illisibles.
     */
    private static final byte[] BOM_UTF8 = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    /** Séparateur attendu par Excel francophone (la virgule est le séparateur décimal). */
    static final char SEPARATEUR = ';';

    /** Fin de ligne CSV normalisée (RFC 4180). */
    static final String FIN_DE_LIGNE = "\r\n";

    /** Nom de feuille utilisé par la surcharge courte de {@link #xlsx(List, List)}. */
    public static final String FEUILLE_PAR_DEFAUT = "Export";

    /** Largeur maximale d'une colonne acceptée par le format (255 caractères). */
    private static final int LARGEUR_MAXI = 255 * 256;

    private EcrivainTableur() {
        // Utilitaire statique.
    }

    // ------------------------------------------------------------------
    // CSV
    // ------------------------------------------------------------------

    /**
     * Sérialise les données en CSV : UTF-8 <em>avec BOM</em>, séparateur {@code ;},
     * guillemets doublés, fins de ligne CRLF.
     *
     * @param colonnes en-têtes de colonnes (obligatoire, non vide)
     * @param lignes   lignes de données ; une ligne plus courte que les en-têtes est complétée par
     *                 des cellules vides, une valeur {@code null} devient une cellule vide
     * @return les octets du fichier CSV
     */
    public static byte[] csv(List<String> colonnes, List<List<String>> lignes) {
        verifierEntrees(colonnes, lignes);

        StringBuilder tampon = new StringBuilder();
        ajouterLigneCsv(tampon, colonnes, colonnes.size());
        for (List<String> ligne : lignes) {
            ajouterLigneCsv(tampon, ligne == null ? List.of() : ligne, colonnes.size());
        }

        byte[] corps = tampon.toString().getBytes(StandardCharsets.UTF_8);
        byte[] fichier = new byte[BOM_UTF8.length + corps.length];
        System.arraycopy(BOM_UTF8, 0, fichier, 0, BOM_UTF8.length);
        System.arraycopy(corps, 0, fichier, BOM_UTF8.length, corps.length);
        return fichier;
    }

    private static void ajouterLigneCsv(StringBuilder tampon, List<String> valeurs, int nbColonnes) {
        int total = Math.max(nbColonnes, valeurs.size());
        for (int i = 0; i < total; i++) {
            if (i > 0) {
                tampon.append(SEPARATEUR);
            }
            tampon.append(echapper(i < valeurs.size() ? valeurs.get(i) : null));
        }
        tampon.append(FIN_DE_LIGNE);
    }

    /**
     * Échappe une valeur CSV : entourée de guillemets dès qu'elle contient le séparateur, un
     * guillemet, un saut de ligne, ou des espaces en bordure ; les guillemets internes sont doublés.
     */
    static String echapper(String valeur) {
        if (valeur == null || valeur.isEmpty()) {
            return "";
        }
        boolean besoinDeGuillemets = valeur.indexOf(SEPARATEUR) >= 0
                || valeur.indexOf('"') >= 0
                || valeur.indexOf('\n') >= 0
                || valeur.indexOf('\r') >= 0
                || valeur.charAt(0) == ' '
                || valeur.charAt(valeur.length() - 1) == ' ';
        if (!besoinDeGuillemets) {
            return valeur;
        }
        return '"' + valeur.replace("\"", "\"\"") + '"';
    }

    // ------------------------------------------------------------------
    // XLSX
    // ------------------------------------------------------------------

    /**
     * Sérialise les données en classeur OOXML {@code .xlsx} sur la feuille
     * {@value #FEUILLE_PAR_DEFAUT}.
     *
     * @see #xlsx(List, List, String)
     */
    public static byte[] xlsx(List<String> colonnes, List<List<String>> lignes) {
        return xlsx(colonnes, lignes, FEUILLE_PAR_DEFAUT);
    }

    /**
     * Sérialise les données en classeur OOXML {@code .xlsx} : première ligne en gras et figée,
     * colonnes auto-dimensionnées.
     *
     * @param colonnes   en-têtes de colonnes (obligatoire, non vide)
     * @param lignes     lignes de données ; une valeur {@code null} devient une cellule vide
     * @param nomFeuille nom de la feuille ; assaini si nécessaire (caractères interdits, longueur)
     * @return les octets du classeur
     */
    public static byte[] xlsx(List<String> colonnes, List<List<String>> lignes, String nomFeuille) {
        verifierEntrees(colonnes, lignes);

        try (XSSFWorkbook classeur = new XSSFWorkbook();
             ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {

            Sheet feuille = classeur.createSheet(nomDeFeuilleSur(nomFeuille));

            Font police = classeur.createFont();
            police.setBold(true);
            CellStyle styleEntete = classeur.createCellStyle();
            styleEntete.setFont(police);

            Row entete = feuille.createRow(0);
            for (int colonne = 0; colonne < colonnes.size(); colonne++) {
                Cell cellule = entete.createCell(colonne);
                cellule.setCellValue(valeurOuVide(colonnes.get(colonne)));
                cellule.setCellStyle(styleEntete);
            }

            int numeroLigne = 1;
            for (List<String> ligne : lignes) {
                Row rangee = feuille.createRow(numeroLigne++);
                List<String> valeurs = ligne == null ? List.of() : ligne;
                for (int colonne = 0; colonne < valeurs.size(); colonne++) {
                    rangee.createCell(colonne).setCellValue(valeurOuVide(valeurs.get(colonne)));
                }
            }

            // En-têtes toujours visibles au défilement.
            feuille.createFreezePane(0, 1);
            dimensionnerColonnes(feuille, colonnes, lignes);

            classeur.write(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Génération du classeur XLSX impossible : " + e.getMessage(), e);
        }
    }

    /**
     * Auto-dimensionne les colonnes. L'auto-dimensionnement de POI mesure le texte via AWT ; si
     * l'environnement n'expose aucune police (conteneur minimal), on retombe sur une largeur
     * calculée à partir du nombre de caractères pour ne jamais faire échouer l'export.
     */
    private static void dimensionnerColonnes(Sheet feuille, List<String> colonnes, List<List<String>> lignes) {
        for (int colonne = 0; colonne < colonnes.size(); colonne++) {
            try {
                feuille.autoSizeColumn(colonne);
            } catch (RuntimeException | LinkageError e) {
                feuille.setColumnWidth(colonne, largeurApprochee(colonne, colonnes, lignes));
            }
            if (feuille.getColumnWidth(colonne) > LARGEUR_MAXI) {
                feuille.setColumnWidth(colonne, LARGEUR_MAXI);
            }
        }
    }

    private static int largeurApprochee(int colonne, List<String> colonnes, List<List<String>> lignes) {
        int maxi = valeurOuVide(colonnes.get(colonne)).length();
        for (List<String> ligne : lignes) {
            if (ligne != null && colonne < ligne.size()) {
                maxi = Math.max(maxi, valeurOuVide(ligne.get(colonne)).length());
            }
        }
        return Math.min(LARGEUR_MAXI, (maxi + 2) * 256);
    }

    static String nomDeFeuilleSur(String nomFeuille) {
        String propose = nomFeuille == null || nomFeuille.isBlank() ? FEUILLE_PAR_DEFAUT : nomFeuille.trim();
        return WorkbookUtil.createSafeSheetName(propose);
    }

    // ------------------------------------------------------------------
    // Communs
    // ------------------------------------------------------------------

    private static void verifierEntrees(List<String> colonnes, List<List<String>> lignes) {
        Objects.requireNonNull(colonnes, "Les en-têtes de colonnes sont obligatoires.");
        Objects.requireNonNull(lignes, "La liste des lignes est obligatoire (éventuellement vide).");
        if (colonnes.isEmpty()) {
            throw new IllegalArgumentException("Un export doit comporter au moins une colonne.");
        }
    }

    private static String valeurOuVide(String valeur) {
        return valeur == null ? "" : valeur;
    }
}
