package ma.jadwal.api.scolarite;

import ma.jadwal.api.dto.AnalyseImportReponse;
import ma.jadwal.api.dto.EleveReponse;
import ma.jadwal.api.dto.LigneImportEleve;
import ma.jadwal.api.dto.ValidationImportReponse;
import ma.jadwal.api.dto.ValidationImportRequete;
import ma.jadwal.referentiel.depot.GroupeRepository;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.referentiel.entite.Niveau;
import ma.jadwal.scolarite.entite.Eleve;
import ma.jadwal.scolarite.entite.Sexe;
import ma.jadwal.scolarite.entite.StatutEleve;
import ma.jadwal.scolarite.service.DonneesEleve;
import ma.jadwal.scolarite.service.EleveService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImportMassarServiceTest {

    private static final Long ETABLISSEMENT = 7L;

    @Mock
    private EleveService eleveService;

    @Mock
    private GroupeRepository groupeRepository;

    @InjectMocks
    private ImportMassarService importMassarService;

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private void aucunEleveExistant() {
        elevesExistants();
    }

    private void elevesExistants(Eleve... eleves) {
        Page<Eleve> page = new PageImpl<>(List.of(eleves));
        when(eleveService.rechercher(eq(ETABLISSEMENT), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);
    }

    private void classes(Groupe... groupes) {
        when(groupeRepository.findByEtablissementIdOrderByLibelleAsc(ETABLISSEMENT)).thenReturn(List.of(groupes));
    }

    private static Groupe groupe(Long id, String libelle, String niveauLibelle) {
        Niveau niveau = new Niveau();
        niveau.setId(100L);
        niveau.setLibelle(niveauLibelle);
        Groupe groupe = new Groupe();
        groupe.setId(id);
        groupe.setLibelle(libelle);
        groupe.setNiveau(niveau);
        return groupe;
    }

    private static Eleve eleve(Long id, String codeMassar, String nom, String prenom) {
        Eleve eleve = new Eleve();
        eleve.setId(id);
        eleve.setEtablissementId(ETABLISSEMENT);
        eleve.setCodeMassar(codeMassar);
        eleve.setNom(nom);
        eleve.setPrenom(prenom);
        eleve.setStatut(StatutEleve.INSCRIT);
        return eleve;
    }

    private static MockMultipartFile fichier(String contenu) {
        return fichier(contenu, StandardCharsets.UTF_8);
    }

    private static MockMultipartFile fichier(String contenu, Charset charset) {
        return new MockMultipartFile("fichier", "eleves.csv", "text/csv", contenu.getBytes(charset));
    }

    private static LigneImportEleve ligne(AnalyseImportReponse analyse, int index) {
        return analyse.lignes().get(index);
    }

    // ------------------------------------------------------------------
    // Analyse
    // ------------------------------------------------------------------

    @Test
    void lEnteteMassarEstReconnueMalgreLaCasseEtLesAccents() {
        aucunEleveExistant();
        classes();

        AnalyseImportReponse analyse = importMassarService.analyser(ETABLISSEMENT, fichier("""
                CODE MASSAR;Nom de famille;Prénom;Date de naissance;Sexe;Lieu naissance
                r130012345;Alaoui;Yasmine;03/05/2012;F;Rabat
                """));

        assertEquals(List.of("codeMassar", "nom", "prenom", "dateNaissance", "sexe", "lieuNaissance"),
                analyse.colonnesDetectees());
        assertEquals(";", analyse.separateur());
        assertEquals(1, analyse.resume().total());
        assertEquals(1, analyse.resume().nouveaux());

        EleveReponse donnees = ligne(analyse, 0).donnees();
        assertEquals(2, ligne(analyse, 0).numero());
        assertEquals("R130012345", donnees.codeMassar(), "le code Massar est normalisé en majuscules");
        assertEquals("Alaoui", donnees.nom());
        assertEquals(LocalDate.of(2012, 5, 3), donnees.dateNaissance());
        assertEquals("F", donnees.sexe());
        assertEquals(LigneImportEleve.NOUVEAU, ligne(analyse, 0).statut());
    }

    @Test
    void lesEntetesArabesSontReconnues() {
        aucunEleveExistant();
        classes();

        AnalyseImportReponse analyse = importMassarService.analyser(ETABLISSEMENT, fichier("""
                Massar;الاسم العائلي;الاسم الشخصي
                R130000001;العلوي;ياسمين
                """));

        assertEquals(List.of("codeMassar", "nomAr", "prenomAr"), analyse.colonnesDetectees());
        assertEquals("العلوي", ligne(analyse, 0).donnees().nomAr());
    }

    @Test
    void leSeparateurVirguleEstDetecte() {
        aucunEleveExistant();
        classes();

        AnalyseImportReponse analyse = importMassarService.analyser(ETABLISSEMENT, fichier("""
                Massar,Nom,Prenom
                R130012345,"Alaoui, dit Idrissi",Yasmine
                """));

        assertEquals(",", analyse.separateur());
        assertEquals("Alaoui, dit Idrissi", ligne(analyse, 0).donnees().nom(),
                "le séparateur entre guillemets ne coupe pas la cellule");
    }

    @Test
    void unEleveDejaInscritEstMarqueExistantAvecSonIdentifiant() {
        elevesExistants(eleve(42L, "R130012345", "Alaoui", "Yasmine"));
        classes();

        AnalyseImportReponse analyse = importMassarService.analyser(ETABLISSEMENT, fichier("""
                Massar;Nom;Prenom
                r130012345;Alaoui;Yasmine
                """));

        assertEquals(LigneImportEleve.EXISTANT, ligne(analyse, 0).statut());
        assertEquals(42L, ligne(analyse, 0).donnees().id());
        assertEquals(1, analyse.resume().existants());
    }

    @Test
    void unCodeMassarRepeteDansLeFichierEstUneErreur() {
        aucunEleveExistant();
        classes();

        AnalyseImportReponse analyse = importMassarService.analyser(ETABLISSEMENT, fichier("""
                Massar;Nom;Prenom
                R130012345;Alaoui;Yasmine
                R130012345;Alaoui;Yassine
                """));

        assertEquals(LigneImportEleve.NOUVEAU, ligne(analyse, 0).statut());
        assertEquals(LigneImportEleve.ERREUR, ligne(analyse, 1).statut());
        assertTrue(ligne(analyse, 1).messages().get(0).contains("ligne 2"));
        assertEquals(1, analyse.resume().erreurs());
    }

    @Test
    void unNouvelEleveSansNomEstUneErreurMaisUnExistantSansNomResteValide() {
        elevesExistants(eleve(42L, "R130000002", "Alaoui", "Yasmine"));
        classes();

        AnalyseImportReponse analyse = importMassarService.analyser(ETABLISSEMENT, fichier("""
                Massar;Nom;Prenom
                R130000001;;
                R130000002;;
                """));

        assertEquals(LigneImportEleve.ERREUR, ligne(analyse, 0).statut());
        assertEquals(LigneImportEleve.EXISTANT, ligne(analyse, 1).statut());
    }

    @Test
    void uneDateIllisibleEstSignaleeSansBloquerLaLigne() {
        aucunEleveExistant();
        classes();

        AnalyseImportReponse analyse = importMassarService.analyser(ETABLISSEMENT, fichier("""
                Massar;Nom;Prenom;Date de naissance
                R130012345;Alaoui;Yasmine;05-2012
                """));

        assertEquals(LigneImportEleve.NOUVEAU, ligne(analyse, 0).statut());
        assertNull(ligne(analyse, 0).donnees().dateNaissance());
        assertTrue(ligne(analyse, 0).messages().get(0).contains("Date de naissance illisible"));
    }

    @Test
    void laClasseEstRapprocheeDuLibelleDuGroupe() {
        aucunEleveExistant();
        classes(groupe(5L, "1ère APIC-A", "1AC"));

        AnalyseImportReponse analyse = importMassarService.analyser(ETABLISSEMENT, fichier("""
                Massar;Nom;Prenom;Classe
                R130012345;Alaoui;Yasmine;1ere apic a
                R130012346;Alaoui;Yassine;2APIC-B
                """));

        assertEquals(5L, ligne(analyse, 0).donnees().groupeId());
        assertEquals("1ère APIC-A", ligne(analyse, 0).donnees().groupeLibelle());
        assertEquals("1AC", ligne(analyse, 0).donnees().niveauLibelle());

        assertNull(ligne(analyse, 1).donnees().groupeId());
        assertEquals(LigneImportEleve.NOUVEAU, ligne(analyse, 1).statut(), "une classe inconnue ne bloque pas");
        assertTrue(ligne(analyse, 1).messages().get(0).contains("inconnue"));
    }

    @Test
    void lesEntetesNonReconnuesSontRestituees() {
        aucunEleveExistant();
        classes();

        AnalyseImportReponse analyse = importMassarService.analyser(ETABLISSEMENT, fichier("""
                Massar;Nom;Prenom;Moyenne annuelle
                R130012345;Alaoui;Yasmine;14,5
                """));

        assertEquals(List.of("Moyenne annuelle"), analyse.colonnesIgnorees());
    }

    @Test
    void lesLignesVidesSontIgnorees() {
        aucunEleveExistant();
        classes();

        AnalyseImportReponse analyse = importMassarService.analyser(ETABLISSEMENT, fichier("""
                Massar;Nom;Prenom
                R130012345;Alaoui;Yasmine
                ;;

                """));

        assertEquals(1, analyse.resume().total());
    }

    @Test
    void unFichierWindows1252ResteLisible() {
        aucunEleveExistant();
        classes();

        AnalyseImportReponse analyse = importMassarService.analyser(ETABLISSEMENT,
                fichier("""
                        Massar;Nom;Prénom
                        R130012345;Benchekroun;Zoé
                        """, Charset.forName("windows-1252")));

        assertEquals("Zoé", ligne(analyse, 0).donnees().prenom());
    }

    @Test
    void lAnalyseNEcritJamaisRien() {
        aucunEleveExistant();
        classes();

        importMassarService.analyser(ETABLISSEMENT, fichier("""
                Massar;Nom;Prenom
                R130012345;Alaoui;Yasmine
                """));

        verify(eleveService, never()).creer(anyLong(), any());
        verify(eleveService, never()).mettreAJour(anyLong(), anyLong(), any());
    }

    @Test
    void unFichierVideOuBinaireEstRefuse() {
        assertEquals("Aucun fichier reçu, ou fichier vide.",
                assertThrows(IllegalArgumentException.class,
                        () -> importMassarService.analyser(ETABLISSEMENT, fichier(""))).getMessage());

        MockMultipartFile classeur = new MockMultipartFile("fichier", "eleves.xlsx",
                "application/octet-stream", new byte[] { 'P', 'K', 3, 4, 0, 0 });
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> importMassarService.analyser(ETABLISSEMENT, classeur))
                .getMessage().contains("classeur Excel"));
    }

    @Test
    void unEnteteSansCodeMassarEstRefuse() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> importMassarService.analyser(ETABLISSEMENT, fichier("""
                        Nom;Prenom
                        Alaoui;Yasmine
                        """)))
                .getMessage().contains("code Massar"));
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    private static LigneImportEleve ligneRetenue(String codeMassar, String nom, String prenom, String statut) {
        return new LigneImportEleve(2, new EleveReponse(null, codeMassar, nom, prenom, null, null, null, null,
                null, null, null, null, null, null, null), statut, List.of());
    }

    @Test
    void laValidationCreeLesNouveauxEleves() {
        aucunEleveExistant();
        when(eleveService.creer(anyLong(), any())).thenReturn(eleve(1L, "R130012345", "Alaoui", "Yasmine"));

        ValidationImportReponse resultat = importMassarService.valider(ETABLISSEMENT,
                new ValidationImportRequete(
                        List.of(ligneRetenue("r130012345", "Alaoui", "Yasmine", LigneImportEleve.NOUVEAU)), false));

        assertEquals(new ValidationImportReponse(1, 0, 0), resultat);
        ArgumentCaptor<DonneesEleve> capture = ArgumentCaptor.forClass(DonneesEleve.class);
        verify(eleveService).creer(eq(ETABLISSEMENT), capture.capture());
        assertEquals("R130012345", capture.getValue().codeMassar());
    }

    @Test
    void unEleveExistantNEstToucheQueSiLaMiseAJourEstDemandee() {
        elevesExistants(eleve(42L, "R130012345", "Alaoui", "Yasmine"));

        ValidationImportReponse sansMiseAJour = importMassarService.valider(ETABLISSEMENT,
                new ValidationImportRequete(
                        List.of(ligneRetenue("R130012345", "Alaoui", "Yasmina", LigneImportEleve.EXISTANT)), false));

        assertEquals(new ValidationImportReponse(0, 0, 1), sansMiseAJour);
        verify(eleveService, never()).mettreAJour(anyLong(), anyLong(), any());

        ValidationImportReponse avecMiseAJour = importMassarService.valider(ETABLISSEMENT,
                new ValidationImportRequete(
                        List.of(ligneRetenue("R130012345", "Alaoui", "Yasmina", LigneImportEleve.EXISTANT)), true));

        assertEquals(new ValidationImportReponse(0, 1, 0), avecMiseAJour);
        verify(eleveService).mettreAJour(eq(ETABLISSEMENT), eq(42L), any());
    }

    /** L'identifiant transmis par le client ne doit jamais servir à retrouver l'élève. */
    @Test
    void lIdentifiantFourniParLeClientEstIgnore() {
        elevesExistants(eleve(42L, "R130012345", "Alaoui", "Yasmine"));

        LigneImportEleve falsifiee = new LigneImportEleve(2,
                new EleveReponse(999L, "R130012345", "Alaoui", "Yasmine", null, null, null, null, null, null,
                        null, null, null, null, null),
                LigneImportEleve.EXISTANT, List.of());

        importMassarService.valider(ETABLISSEMENT, new ValidationImportRequete(List.of(falsifiee), true));

        verify(eleveService).mettreAJour(eq(ETABLISSEMENT), eq(42L), any());
        verify(eleveService, never()).mettreAJour(anyLong(), eq(999L), any());
    }

    @Test
    void laMiseAJourNEcrasePasLesChampsAbsentsDuFichier() {
        Eleve existant = eleve(42L, "R130012345", "Alaoui", "Yasmine");
        existant.setLieuNaissance("Rabat");
        existant.setSexe(Sexe.F);
        existant.setTuteurTelephone("0600000000");
        existant.setGroupe(groupe(5L, "1ère APIC-A", "1AC"));
        elevesExistants(existant);

        importMassarService.valider(ETABLISSEMENT, new ValidationImportRequete(
                List.of(ligneRetenue("R130012345", "Alaoui", "Yasmina", LigneImportEleve.EXISTANT)), true));

        ArgumentCaptor<DonneesEleve> capture = ArgumentCaptor.forClass(DonneesEleve.class);
        verify(eleveService).mettreAJour(eq(ETABLISSEMENT), eq(42L), capture.capture());
        DonneesEleve fusion = capture.getValue();
        assertEquals("Yasmina", fusion.prenom(), "la valeur du fichier gagne");
        assertEquals("Rabat", fusion.lieuNaissance(), "les champs absents du fichier sont préservés");
        assertEquals(Sexe.F, fusion.sexe());
        assertEquals("0600000000", fusion.tuteurTelephone());
        assertEquals(5L, fusion.groupeId(), "l'élève n'est pas sorti de sa classe");
    }

    @Test
    void lesLignesEnErreurOuEnDoublonSontIgnorees() {
        aucunEleveExistant();
        when(eleveService.creer(anyLong(), any())).thenReturn(eleve(1L, "R130012345", "Alaoui", "Yasmine"));

        ValidationImportReponse resultat = importMassarService.valider(ETABLISSEMENT, new ValidationImportRequete(
                List.of(ligneRetenue("R130012345", "Alaoui", "Yasmine", LigneImportEleve.NOUVEAU),
                        ligneRetenue("R130012345", "Alaoui", "Yassine", LigneImportEleve.NOUVEAU),
                        ligneRetenue("R130099999", "Trop long", "X", LigneImportEleve.ERREUR),
                        ligneRetenue(null, "Sans", "Code", LigneImportEleve.NOUVEAU),
                        ligneRetenue("R130088888", null, "Sans nom", LigneImportEleve.NOUVEAU)),
                false));

        assertEquals(1, resultat.crees());
        assertEquals(4, resultat.ignores());
        verify(eleveService, never()).mettreAJour(anyLong(), anyLong(), any());
    }

    @Test
    void unLotVideEstRefuse() {
        assertEquals("Aucune ligne à importer.",
                assertThrows(IllegalArgumentException.class,
                        () -> importMassarService.valider(ETABLISSEMENT,
                                new ValidationImportRequete(List.of(), false))).getMessage());
    }

    @Test
    void lesFormatsDeDateEtDeSexeSontTolerants() {
        assertEquals(LocalDate.of(2012, 12, 31), ImportMassarService.lireDate("31/12/2012"));
        assertEquals(LocalDate.of(2012, 12, 31), ImportMassarService.lireDate("2012-12-31"));
        assertEquals(LocalDate.of(2012, 12, 31), ImportMassarService.lireDate("31/12/2012 00:00"));
        assertEquals(LocalDate.of(2012, 1, 3), ImportMassarService.lireDate("3/1/2012"));
        assertNull(ImportMassarService.lireDate("31 décembre"));

        assertEquals(Sexe.M, ImportMassarService.lireSexe("Garçon"));
        assertEquals(Sexe.M, ImportMassarService.lireSexe("h"));
        assertEquals(Sexe.F, ImportMassarService.lireSexe("FILLE"));
        assertNull(ImportMassarService.lireSexe("autre"));

        assertNotNull(ImportMassarService.lireStatut("Redoublant"));
        assertNull(ImportMassarService.lireStatut(""));
    }
}
