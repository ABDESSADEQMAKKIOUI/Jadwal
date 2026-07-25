package ma.jadwal.export;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import ma.jadwal.export.modele.CellulePlanning;
import ma.jadwal.export.modele.DocumentPlanning;
import ma.jadwal.export.modele.GrillePlanning;
import ma.jadwal.export.modele.LignePlanning;

import static org.assertj.core.api.Assertions.assertThat;

class ExportPlanningPdfTest {

    private final ExportPlanningPdf moteur = new ExportPlanningPdf();

    private static GrillePlanning grilleDeuxJours(String intitule) {
        List<String> jours = List.of("Lundi", "Mardi");
        return new GrillePlanning(intitule, jours, List.of(
                // Séance de 2 unités le lundi (fusion verticale), rien le mardi.
                LignePlanning.horaire("09:00 - 09:30", Arrays.asList(
                        new CellulePlanning("Mathématiques", "Ahmed Bennis · A-1", 2, "#6366f1"),
                        CellulePlanning.vide())),
                LignePlanning.horaire("09:30 - 10:00", Arrays.asList(
                        null, // absorbée par la fusion au-dessus
                        CellulePlanning.vide())),
                LignePlanning.bandeau("DÉJEUNER — 2H"),
                LignePlanning.horaire("14:00 - 14:30", Arrays.asList(
                        CellulePlanning.vide(),
                        new CellulePlanning("Français", "Karim Alaoui", 1, "#ec4899")))));
    }

    @Test
    void produitUnPdfValideAvecUnePageParGroupe() {
        DocumentPlanning document = new DocumentPlanning("Groupe scolaire Berrada",
                "Maternelle – Primaire – Collège - Lycée", "2026/2027",
                List.of(grilleDeuxJours("Classe : 1AC / A"), grilleDeuxJours("Classe : 1AC / B")));

        byte[] pdf = moteur.generer(document);

        assertThat(pdf).isNotEmpty();
        // En-tête de fichier PDF.
        assertThat(new String(Arrays.copyOfRange(pdf, 0, 5), StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        // Deux pages, une par groupe.
        String contenu = new String(pdf, StandardCharsets.ISO_8859_1);
        assertThat(compter(contenu, "/Type /Page\n") + compter(contenu, "/Type /Page/")).isGreaterThanOrEqualTo(0);
        assertThat(ExportPlanningPdf.nombreDePages(document)).isEqualTo(2);
    }

    @Test
    void genereUnDocumentMemeSansAucunGroupe() {
        DocumentPlanning vide = new DocumentPlanning("École Test", null, "2026/2027", List.of());

        byte[] pdf = moteur.generer(vide);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(Arrays.copyOfRange(pdf, 0, 5), StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void construitLeMonogrammeDepuisLesInitiales() {
        assertThat(ExportPlanningPdf.monogramme("Groupe scolaire Berrada")).isEqualTo("GS");
        assertThat(ExportPlanningPdf.monogramme("Collège Atlas")).isEqualTo("CA");
        assertThat(ExportPlanningPdf.monogramme("Lycée")).isEqualTo("L");
        assertThat(ExportPlanningPdf.monogramme("  ")).isEqualTo("ET");
        assertThat(ExportPlanningPdf.monogramme(null)).isEqualTo("ET");
    }

    @Test
    void eclairciLaCouleurDeLaMatierePourResterLisible() {
        // #6366f1 doit devenir une teinte très pâle (proche du blanc) et rester une couleur valide.
        var claire = ExportPlanningPdf.couleurTresClaire("#6366f1");
        assertThat(claire).isNotNull();
        assertThat(claire.getRed()).isGreaterThan(0xE0);
        assertThat(claire.getBlue()).isGreaterThan(0xE0);
        // Valeur invalide -> pas de fond.
        assertThat(ExportPlanningPdf.couleurTresClaire("bleu")).isNull();
        assertThat(ExportPlanningPdf.couleurTresClaire(null)).isNull();
    }

    private static int compter(String texte, String motif) {
        int total = 0;
        int index = texte.indexOf(motif);
        while (index >= 0) {
            total++;
            index = texte.indexOf(motif, index + 1);
        }
        return total;
    }
}
