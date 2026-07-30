package ma.jadwal.export.tableur;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EcrivainTableurTest {

    private static final String NOM_ARABE = "محمد الأمين";

    private static final List<String> COLONNES = List.of("Code", "Nom", "Observation");

    /** Valeurs pièges : séparateur, guillemets, accents, saut de ligne, arabe, cellule nulle. */
    private static List<List<String>> lignesPieges() {
        List<String> avecNul = new ArrayList<>(Arrays.asList("A-3", NOM_ARABE, null));
        return List.of(
                List.of("A-1", "Zineb Alaoui", "Élève sérieuse; ponctuelle"),
                List.of("A-2", "Karim Bennis", "Mention \"très bien\" ; à confirmer"),
                avecNul,
                List.of("A-4", "Salma El Fassi", "Absence justifiée\nmotif médical"));
    }

    // ------------------------------------------------------------------
    // CSV
    // ------------------------------------------------------------------

    @Test
    void csvCommencePartLeBomUtf8() {
        byte[] csv = EcrivainTableur.csv(COLONNES, List.of());

        assertThat(csv).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
    }

    @Test
    void csvEchappeSeparateurGuillemetsAccentsEtArabe() {
        byte[] csv = EcrivainTableur.csv(COLONNES, lignesPieges());

        String texte = sansBom(csv);
        List<String> lignes = List.of(texte.split("\r\n", -1));

        // En-têtes puis 4 lignes de données, chacune terminée par un CRLF (d'où le dernier élément vide).
        assertThat(lignes).hasSize(6);
        assertThat(lignes.get(5)).isEmpty();
        assertThat(lignes.get(0)).isEqualTo("Code;Nom;Observation");

        // Le point-virgule force les guillemets ; l'accent reste tel quel.
        assertThat(lignes.get(1)).isEqualTo("A-1;Zineb Alaoui;\"Élève sérieuse; ponctuelle\"");
        // Les guillemets internes sont doublés.
        assertThat(lignes.get(2)).isEqualTo("A-2;Karim Bennis;\"Mention \"\"très bien\"\" ; à confirmer\"");
        // Nom arabe restitué à l'identique, cellule nulle rendue vide.
        assertThat(lignes.get(3)).isEqualTo("A-3;" + NOM_ARABE + ";");

        // Une valeur multi-ligne reste dans une seule cellule entourée de guillemets.
        assertThat(texte).contains("\"Absence justifiée\nmotif médical\"");

        // Fins de ligne strictement CRLF : aucun LF isolé en dehors de la valeur multi-ligne.
        assertThat(compterFinsDeLigne(texte)).isEqualTo(5);
    }

    @Test
    void csvEstRelisibleEnUtf8SansPerteDeCaracteres() {
        byte[] csv = EcrivainTableur.csv(List.of("Nom"), List.of(List.of(NOM_ARABE)));

        // Les octets décodés en UTF-8 redonnent exactement le nom arabe (aucune translittération).
        assertThat(sansBom(csv)).isEqualTo("Nom\r\n" + NOM_ARABE + "\r\n");
    }

    @Test
    void csvCompleteLesLignesTropCourtes() {
        byte[] csv = EcrivainTableur.csv(COLONNES, List.of(List.of("A-1")));

        assertThat(sansBom(csv)).isEqualTo("Code;Nom;Observation\r\nA-1;;\r\n");
    }

    @Test
    void csvProtegeLesEspacesEnBordure() {
        assertThat(EcrivainTableur.echapper(" 10 ")).isEqualTo("\" 10 \"");
        assertThat(EcrivainTableur.echapper("10")).isEqualTo("10");
        assertThat(EcrivainTableur.echapper(null)).isEmpty();
    }

    @Test
    void refuseUnExportSansColonne() {
        assertThatThrownBy(() -> EcrivainTableur.csv(List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("au moins une colonne");
        assertThatThrownBy(() -> EcrivainTableur.xlsx(List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------------
    // XLSX
    // ------------------------------------------------------------------

    @Test
    void xlsxEstUnVraiClasseurRelisibleParPoi() throws Exception {
        byte[] classeur = EcrivainTableur.xlsx(COLONNES, lignesPieges());

        // Signature d'un conteneur ZIP/OOXML.
        assertThat(classeur).startsWith((byte) 'P', (byte) 'K');

        try (XSSFWorkbook relu = new XSSFWorkbook(new ByteArrayInputStream(classeur))) {
            assertThat(relu.getNumberOfSheets()).isEqualTo(1);
            Sheet feuille = relu.getSheetAt(0);
            assertThat(feuille.getSheetName()).isEqualTo("Export");

            // En-têtes.
            Row entete = feuille.getRow(0);
            assertThat(entete.getCell(0).getStringCellValue()).isEqualTo("Code");
            assertThat(entete.getCell(1).getStringCellValue()).isEqualTo("Nom");
            assertThat(entete.getCell(2).getStringCellValue()).isEqualTo("Observation");

            // Contenu : accents, point-virgule, guillemets et arabe intacts (aucun échappement en XLSX).
            assertThat(feuille.getLastRowNum()).isEqualTo(4);
            assertThat(feuille.getRow(1).getCell(2).getStringCellValue())
                    .isEqualTo("Élève sérieuse; ponctuelle");
            assertThat(feuille.getRow(2).getCell(2).getStringCellValue())
                    .isEqualTo("Mention \"très bien\" ; à confirmer");
            assertThat(feuille.getRow(3).getCell(1).getStringCellValue()).isEqualTo(NOM_ARABE);
            assertThat(feuille.getRow(3).getCell(2).getStringCellValue()).isEmpty();
            assertThat(feuille.getRow(4).getCell(2).getStringCellValue())
                    .isEqualTo("Absence justifiée\nmotif médical");
        }
    }

    @Test
    void xlsxMetLaPremiereLigneEnGrasEtLaFige() throws Exception {
        byte[] classeur = EcrivainTableur.xlsx(COLONNES, lignesPieges());

        try (XSSFWorkbook relu = new XSSFWorkbook(new ByteArrayInputStream(classeur))) {
            Sheet feuille = relu.getSheetAt(0);

            XSSFCellStyle styleEntete = (XSSFCellStyle) feuille.getRow(0).getCell(0).getCellStyle();
            assertThat(styleEntete.getFont().getBold()).isTrue();
            XSSFCellStyle styleDonnee = (XSSFCellStyle) feuille.getRow(1).getCell(0).getCellStyle();
            assertThat(styleDonnee.getFont().getBold()).isFalse();

            PaneInformation volet = feuille.getPaneInformation();
            assertThat(volet).isNotNull();
            assertThat(volet.isFreezePane()).isTrue();
            assertThat(volet.getHorizontalSplitPosition()).isEqualTo((short) 1);
        }
    }

    @Test
    void xlsxDimensionneLesColonnesSelonLeContenu() throws Exception {
        byte[] classeur = EcrivainTableur.xlsx(COLONNES, lignesPieges());

        try (XSSFWorkbook relu = new XSSFWorkbook(new ByteArrayInputStream(classeur))) {
            Sheet feuille = relu.getSheetAt(0);
            // La colonne « Observation » (valeurs longues) est plus large que « Code » (3 caractères).
            assertThat(feuille.getColumnWidth(2)).isGreaterThan(feuille.getColumnWidth(0));
        }
    }

    @Test
    void xlsxAccepteUnNomDeFeuilleParametre() throws Exception {
        byte[] classeur = EcrivainTableur.xlsx(COLONNES, List.of(), "Absences juin");

        try (XSSFWorkbook relu = new XSSFWorkbook(new ByteArrayInputStream(classeur))) {
            assertThat(relu.getSheetAt(0).getSheetName()).isEqualTo("Absences juin");
        }
    }

    @Test
    void xlsxAssainitUnNomDeFeuilleInvalide() throws Exception {
        // « / » est interdit dans un nom de feuille Excel ; un nom vide retombe sur le nom par défaut.
        byte[] classeur = EcrivainTableur.xlsx(COLONNES, List.of(), "Absences 2026/2027");

        try (XSSFWorkbook relu = new XSSFWorkbook(new ByteArrayInputStream(classeur))) {
            assertThat(relu.getSheetAt(0).getSheetName()).isEqualTo("Absences 2026 2027");
        }
        assertThat(EcrivainTableur.nomDeFeuilleSur("  ")).isEqualTo("Export");
        assertThat(EcrivainTableur.nomDeFeuilleSur(null)).isEqualTo("Export");
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    private static String sansBom(byte[] fichier) {
        return new String(fichier, 3, fichier.length - 3, StandardCharsets.UTF_8);
    }

    /** Compte les CRLF ; échoue implicitement si un CR ou un LF traîne seul hors valeur échappée. */
    private static int compterFinsDeLigne(String texte) {
        int total = 0;
        int index = texte.indexOf("\r\n");
        while (index >= 0) {
            total++;
            index = texte.indexOf("\r\n", index + 2);
        }
        assertThat(texte.replace("\r\n", "")).doesNotContain("\r");
        return total;
    }
}
