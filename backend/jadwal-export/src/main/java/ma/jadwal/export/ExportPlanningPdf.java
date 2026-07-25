package ma.jadwal.export;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import ma.jadwal.export.modele.CellulePlanning;
import ma.jadwal.export.modele.DocumentPlanning;
import ma.jadwal.export.modele.GrillePlanning;
import ma.jadwal.export.modele.LignePlanning;

/**
 * Rend un {@link DocumentPlanning} en PDF, une page par groupe.
 * <p>
 * La mise en page reprend le modèle d'emploi du temps utilisé par les établissements marocains :
 * bandeau d'en-tête (monogramme, nom de l'établissement, sous-titre), titre « EMPLOI DU TEMPS »,
 * ligne « Classe … / Année scolaire … », puis la grille jours × horaires avec une case d'angle
 * barrée en diagonale (« Jours » / « Horaires »), fusion verticale des séances de plus d'une
 * tranche et bandeaux pleine largeur pour les pauses.
 */
public class ExportPlanningPdf {

    private static final Color BLEU_MARINE = new Color(0x1E, 0x3A, 0x8A);
    private static final Color BLEU_ENTETE = new Color(0xC7, 0xD7, 0xF0);
    private static final Color BLEU_HORAIRE = new Color(0xDC, 0xE6, 0xF6);
    private static final Color GRIS_BORDURE = new Color(0x33, 0x33, 0x33);

    private static final Font POLICE_ETABLISSEMENT =
            FontFactory.getFont(FontFactory.TIMES_BOLD, 26, BLEU_MARINE);
    private static final Font POLICE_SOUS_TITRE =
            FontFactory.getFont(FontFactory.TIMES_BOLD, 10, BLEU_MARINE);
    private static final Font POLICE_TITRE =
            FontFactory.getFont(FontFactory.TIMES_BOLD, 20, BLEU_MARINE);
    private static final Font POLICE_INTITULE =
            FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, Color.BLACK);
    private static final Font POLICE_ENTETE_JOUR =
            FontFactory.getFont(FontFactory.TIMES_BOLD, 11, Color.BLACK);
    private static final Font POLICE_ANGLE =
            FontFactory.getFont(FontFactory.TIMES_BOLD, 8, Color.BLACK);
    private static final Font POLICE_HORAIRE =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.BLACK);
    private static final Font POLICE_MATIERE =
            FontFactory.getFont(FontFactory.TIMES_ROMAN, 9.5f, Color.BLACK);
    private static final Font POLICE_DETAIL =
            FontFactory.getFont(FontFactory.HELVETICA, 6.5f, new Color(0x55, 0x55, 0x55));
    private static final Font POLICE_BANDEAU =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.BLACK);
    private static final Font POLICE_MONOGRAMME =
            FontFactory.getFont(FontFactory.TIMES_BOLD, 20, Color.WHITE);
    private static final Font POLICE_PIED =
            FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(0x88, 0x88, 0x88));

    /** Génère le PDF complet. Ne ferme pas le flux appelant : renvoie les octets. */
    public byte[] generer(DocumentPlanning donnees) {
        Document document = new Document(PageSize.A4, 32, 32, 28, 28);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, sortie);
            document.addTitle("Emploi du temps — " + donnees.etablissementNom());
            document.open();

            boolean premiere = true;
            for (GrillePlanning grille : donnees.grilles()) {
                if (!premiere) {
                    document.newPage();
                }
                premiere = false;
                document.add(entete(donnees));
                document.add(titre());
                document.add(ligneIntitule(grille.intitule(), donnees.anneeScolaire()));
                document.add(grille(grille));
                document.add(pied());
            }

            if (donnees.grilles().isEmpty()) {
                document.add(new Paragraph("Aucun emploi du temps à exporter.", POLICE_INTITULE));
            }
            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Génération du PDF impossible : " + e.getMessage(), e);
        }
        return sortie.toByteArray();
    }

    // ------------------------------------------------------------------
    // En-tête
    // ------------------------------------------------------------------

    /** Monogramme à gauche, nom et sous-titre de l'établissement centrés. */
    private PdfPTable entete(DocumentPlanning donnees) {
        PdfPTable table = new PdfPTable(new float[] { 1f, 5.6f });
        table.setWidthPercentage(100);
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell logo = new PdfPCell(new Phrase(monogramme(donnees.etablissementNom()), POLICE_MONOGRAMME));
        logo.setBorder(Rectangle.NO_BORDER);
        logo.setBackgroundColor(BLEU_MARINE);
        logo.setHorizontalAlignment(Element.ALIGN_CENTER);
        logo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logo.setFixedHeight(52);
        logo.setPaddingBottom(10);
        table.addCell(logo);

        PdfPCell texte = new PdfPCell();
        texte.setBorder(Rectangle.NO_BORDER);
        texte.setVerticalAlignment(Element.ALIGN_MIDDLE);
        texte.setPaddingLeft(10);
        Paragraph nom = new Paragraph(donnees.etablissementNom(), POLICE_ETABLISSEMENT);
        nom.setAlignment(Element.ALIGN_CENTER);
        texte.addElement(nom);
        if (donnees.sousTitre() != null && !donnees.sousTitre().isBlank()) {
            Paragraph sous = new Paragraph(donnees.sousTitre(), POLICE_SOUS_TITRE);
            sous.setAlignment(Element.ALIGN_CENTER);
            texte.addElement(sous);
        }
        table.addCell(texte);
        table.setSpacingAfter(10);
        return table;
    }

    /** Initiales de l'établissement (2 lettres au plus), à défaut de logo téléversé. */
    static String monogramme(String nom) {
        if (nom == null || nom.isBlank()) {
            return "ET";
        }
        StringBuilder initiales = new StringBuilder();
        for (String mot : nom.trim().split("\\s+")) {
            if (!mot.isEmpty() && Character.isLetter(mot.charAt(0))) {
                initiales.append(Character.toUpperCase(mot.charAt(0)));
            }
            if (initiales.length() == 2) {
                break;
            }
        }
        return initiales.length() == 0 ? "ET" : initiales.toString();
    }

    private Paragraph titre() {
        Paragraph titre = new Paragraph("EMPLOI DU TEMPS", POLICE_TITRE);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(12);
        return titre;
    }

    /** « Classe : … » à gauche, « Année scolaire : … » à droite, chacun souligné. */
    private PdfPTable ligneIntitule(String intitule, String anneeScolaire) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        PdfPCell gauche = new PdfPCell(new Phrase(intitule, POLICE_INTITULE));
        gauche.setBorder(Rectangle.BOTTOM);
        gauche.setBorderColor(GRIS_BORDURE);
        gauche.setHorizontalAlignment(Element.ALIGN_LEFT);
        gauche.setPaddingBottom(3);

        PdfPCell droite = new PdfPCell(new Phrase("Année scolaire : " + anneeScolaire, POLICE_INTITULE));
        droite.setBorder(Rectangle.BOTTOM);
        droite.setBorderColor(GRIS_BORDURE);
        droite.setHorizontalAlignment(Element.ALIGN_RIGHT);
        droite.setPaddingBottom(3);

        table.addCell(gauche);
        table.addCell(droite);
        table.setSpacingAfter(8);
        return table;
    }

    // ------------------------------------------------------------------
    // Grille
    // ------------------------------------------------------------------

    private PdfPTable grille(GrillePlanning grille) {
        int nbJours = grille.jours().size();
        float[] largeurs = new float[nbJours + 1];
        largeurs[0] = 1.25f;
        for (int i = 1; i <= nbJours; i++) {
            largeurs[i] = 1.6f;
        }

        PdfPTable table = new PdfPTable(largeurs);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        // Case d'angle barrée en diagonale.
        PdfPCell angle = new PdfPCell();
        angle.setBackgroundColor(BLEU_ENTETE);
        angle.setBorderColor(GRIS_BORDURE);
        angle.setFixedHeight(34);
        angle.setCellEvent(new AngleDiagonal());
        table.addCell(angle);

        for (String jour : grille.jours()) {
            PdfPCell cellule = new PdfPCell(new Phrase(jour, POLICE_ENTETE_JOUR));
            cellule.setBackgroundColor(BLEU_ENTETE);
            cellule.setBorderColor(GRIS_BORDURE);
            cellule.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellule.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cellule.setFixedHeight(34);
            table.addCell(cellule);
        }

        for (LignePlanning ligne : grille.lignes()) {
            if (ligne.estBandeau()) {
                PdfPCell bandeau = new PdfPCell(new Phrase(ligne.bandeau(), POLICE_BANDEAU));
                bandeau.setColspan(nbJours + 1);
                bandeau.setBackgroundColor(BLEU_HORAIRE);
                bandeau.setBorderColor(GRIS_BORDURE);
                bandeau.setHorizontalAlignment(Element.ALIGN_CENTER);
                bandeau.setVerticalAlignment(Element.ALIGN_MIDDLE);
                bandeau.setFixedHeight(13);
                table.addCell(bandeau);
                continue;
            }

            PdfPCell horaire = new PdfPCell(new Phrase(ligne.horaire(), POLICE_HORAIRE));
            horaire.setBackgroundColor(BLEU_HORAIRE);
            horaire.setBorderColor(GRIS_BORDURE);
            horaire.setHorizontalAlignment(Element.ALIGN_CENTER);
            horaire.setVerticalAlignment(Element.ALIGN_MIDDLE);
            horaire.setFixedHeight(22);
            table.addCell(horaire);

            for (CellulePlanning cellule : ligne.cellules()) {
                if (cellule == null) {
                    // Case couverte par la fusion verticale d'une séance située au-dessus.
                    continue;
                }
                table.addCell(celluleSeance(cellule));
            }
        }
        return table;
    }

    private PdfPCell celluleSeance(CellulePlanning cellule) {
        PdfPCell pdfCellule = new PdfPCell();
        pdfCellule.setBorderColor(GRIS_BORDURE);
        pdfCellule.setHorizontalAlignment(Element.ALIGN_CENTER);
        pdfCellule.setVerticalAlignment(Element.ALIGN_MIDDLE);
        pdfCellule.setRowspan(Math.max(1, cellule.nbLignes()));
        pdfCellule.setPaddingTop(3);
        pdfCellule.setPaddingBottom(3);

        if (cellule.estVide()) {
            pdfCellule.setPhrase(new Phrase(" "));
            return pdfCellule;
        }

        Color fond = couleurTresClaire(cellule.couleurHexa());
        if (fond != null) {
            pdfCellule.setBackgroundColor(fond);
        }

        Paragraph matiere = new Paragraph(cellule.matiere(), POLICE_MATIERE);
        matiere.setAlignment(Element.ALIGN_CENTER);
        pdfCellule.addElement(matiere);
        if (cellule.details() != null && !cellule.details().isBlank()) {
            // Une ligne par demi-groupe le cas échéant.
            for (String ligne : cellule.details().split("\n")) {
                if (ligne.isBlank()) {
                    continue;
                }
                Paragraph details = new Paragraph(ligne, POLICE_DETAIL);
                details.setAlignment(Element.ALIGN_CENTER);
                pdfCellule.addElement(details);
            }
        }
        return pdfCellule;
    }

    /** Teinte très pâle de la couleur de la matière, pour rester lisible en noir et blanc. */
    static Color couleurTresClaire(String hexa) {
        if (hexa == null || !hexa.matches("#[0-9a-fA-F]{6}")) {
            return null;
        }
        int r = Integer.parseInt(hexa.substring(1, 3), 16);
        int v = Integer.parseInt(hexa.substring(3, 5), 16);
        int b = Integer.parseInt(hexa.substring(5, 7), 16);
        // Mélange à 88 % de blanc.
        return new Color(eclaircir(r), eclaircir(v), eclaircir(b));
    }

    private static int eclaircir(int composante) {
        return (int) Math.round(255 - (255 - composante) * 0.12);
    }

    private Paragraph pied() {
        Paragraph pied = new Paragraph("Généré par JADWAL", POLICE_PIED);
        pied.setAlignment(Element.ALIGN_RIGHT);
        pied.setSpacingBefore(6);
        return pied;
    }

    /** Trace la diagonale de la case d'angle et place « Jours » en haut-droite, « Horaires » en bas-gauche. */
    private static final class AngleDiagonal implements PdfPCellEvent {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte toile = canvases[PdfPTable.LINECANVAS];
            toile.saveState();
            toile.setColorStroke(GRIS_BORDURE);
            toile.setLineWidth(0.6f);
            toile.moveTo(position.getLeft(), position.getTop());
            toile.lineTo(position.getRight(), position.getBottom());
            toile.stroke();
            toile.restoreState();

            PdfContentByte texte = canvases[PdfPTable.TEXTCANVAS];
            texte.saveState();
            texte.beginText();
            texte.setFontAndSize(POLICE_ANGLE.getBaseFont(), 8);
            texte.showTextAligned(Element.ALIGN_RIGHT, "Jours",
                    position.getRight() - 4, position.getTop() - 11, 0);
            texte.showTextAligned(Element.ALIGN_LEFT, "Horaires",
                    position.getLeft() + 4, position.getBottom() + 5, 0);
            texte.endText();
            texte.restoreState();
        }
    }

    /** Exposé pour les tests : nombre de pages attendu. */
    public static int nombreDePages(DocumentPlanning donnees) {
        List<GrillePlanning> grilles = donnees.grilles();
        return grilles.isEmpty() ? 1 : grilles.size();
    }
}
