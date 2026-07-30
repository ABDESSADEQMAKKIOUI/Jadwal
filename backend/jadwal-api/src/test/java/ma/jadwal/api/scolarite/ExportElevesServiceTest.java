package ma.jadwal.api.scolarite;

import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.referentiel.entite.Niveau;
import ma.jadwal.scolarite.entite.Eleve;
import ma.jadwal.scolarite.entite.Sexe;
import ma.jadwal.scolarite.entite.StatutEleve;
import ma.jadwal.scolarite.service.EleveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExportElevesServiceTest {

    private static final Long ETABLISSEMENT = 7L;

    /** Marque d'ordre des octets : Excel francophone en a besoin pour lire l'UTF-8. */
    private static final String BOM = "\uFEFF";

    private static final String NOM_ARABE = "العلوي";

    @Mock
    private EleveService eleveService;

    @InjectMocks
    private ExportElevesService exportElevesService;

    @BeforeEach
    void preparerUnEleve() {
        Niveau niveau = new Niveau();
        niveau.setId(100L);
        niveau.setLibelle("1AC");
        Groupe groupe = new Groupe();
        groupe.setId(5L);
        groupe.setLibelle("1ère APIC-A");
        groupe.setNiveau(niveau);

        Eleve eleve = new Eleve();
        eleve.setId(42L);
        eleve.setEtablissementId(ETABLISSEMENT);
        eleve.setCodeMassar("R130012345");
        eleve.setNom("Alaoui");
        eleve.setPrenom("Yasmine");
        eleve.setNomAr(NOM_ARABE);
        eleve.setDateNaissance(LocalDate.of(2012, 5, 3));
        eleve.setSexe(Sexe.F);
        eleve.setStatut(StatutEleve.INSCRIT);
        eleve.setGroupe(groupe);
        eleve.setTuteurTelephone("0600000000");

        when(eleveService.rechercher(eq(ETABLISSEMENT), isNull(), isNull(), isNull(), isNull(),
                any(Pageable.class))).thenReturn(new PageImpl<>(List.of(eleve)));
    }

    private String[] exporterCsv(String champs) {
        ExportElevesService.FichierExport fichier = exportElevesService.exporter(ETABLISSEMENT, null, null, null,
                null, champs, ExportElevesService.Format.CSV);
        return new String(fichier.contenu(), StandardCharsets.UTF_8).split("\r\n");
    }

    @Test
    void sansChampsToutesLesColonnesSontExportees() {
        String[] lignes = exporterCsv(null);

        assertEquals(BOM + "Code Massar;Nom;Prénom;Nom arabe;Prénom arabe;Date de naissance;"
                + "Lieu de naissance;Sexe;Statut;Classe;Niveau;Tuteur;Téléphone du tuteur", lignes[0]);

        String[] cellules = lignes[1].split(";", -1);
        assertEquals(13, cellules.length);
        assertEquals("R130012345", cellules[0]);
        assertEquals("Alaoui", cellules[1]);
        assertEquals("Yasmine", cellules[2]);
        assertEquals(NOM_ARABE, cellules[3]);
        assertEquals("", cellules[4], "un champ absent sort en cellule vide");
        assertEquals("03/05/2012", cellules[5], "date au format francophone, relisible par l'import");
        assertEquals("F", cellules[7]);
        assertEquals("INSCRIT", cellules[8]);
        assertEquals("1ère APIC-A", cellules[9]);
        assertEquals("1AC", cellules[10]);
        assertEquals("0600000000", cellules[12]);
    }

    @Test
    void lOrdreDesChampsDemandeEstRespecte() {
        String[] lignes = exporterCsv("nom,codeMassar,classe");

        assertEquals(BOM + "Nom;Code Massar;Classe", lignes[0]);
        assertEquals("Alaoui;R130012345;1ère APIC-A", lignes[1]);
    }

    @Test
    void lesDoublonsEtLesEspacesDeChampsSontToleres() {
        assertEquals(BOM + "Nom;Code Massar", exporterCsv(" nom , NOM ,massar ")[0]);
    }

    @Test
    void unChampInconnuEstRefuse() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> exporterCsv("nom,notes"));
        assertTrue(exception.getMessage().startsWith("Champ d'export inconnu : « notes »"),
                exception.getMessage());
    }

    @Test
    void lExportXlsxEstUnVraiClasseur() {
        ExportElevesService.FichierExport fichier = exportElevesService.exporter(ETABLISSEMENT, null, null, null,
                null, "nom", ExportElevesService.Format.XLSX);

        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", fichier.typeMime());
        assertTrue(fichier.nom().endsWith(".xlsx"));
        assertTrue(fichier.contenu().length > 4 && fichier.contenu()[0] == 'P' && fichier.contenu()[1] == 'K',
                "un classeur XLSX est une archive ZIP");
    }

    @Test
    void leNomDuFichierPorteLaDateDuJour() {
        ExportElevesService.FichierExport fichier = exportElevesService.exporter(ETABLISSEMENT, null, null, null,
                null, null, ExportElevesService.Format.CSV);

        assertEquals("eleves-" + LocalDate.now() + ".csv", fichier.nom());
        assertEquals("text/csv; charset=utf-8", fichier.typeMime());
    }
}
