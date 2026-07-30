package ma.jadwal.scolarite.service;

import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.planning.depot.SeanceRepository;
import ma.jadwal.referentiel.depot.GroupeRepository;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.scolarite.depot.AbsenceRepository;
import ma.jadwal.scolarite.depot.CompteurAbsencesEleve;
import ma.jadwal.scolarite.depot.EleveRepository;
import ma.jadwal.scolarite.entite.Absence;
import ma.jadwal.scolarite.entite.DemiJournee;
import ma.jadwal.scolarite.entite.Eleve;
import ma.jadwal.scolarite.entite.StatutEleve;
import ma.jadwal.scolarite.entite.TypeAbsence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbsenceServiceTest {

    private static final Long ETABLISSEMENT = 1L;
    private static final Long GROUPE = 10L;
    private static final LocalDate LUNDI = LocalDate.of(2026, 2, 2);

    @Mock
    private AbsenceRepository absenceRepository;

    @Mock
    private EleveRepository eleveRepository;

    @Mock
    private GroupeRepository groupeRepository;

    @Mock
    private SeanceRepository seanceRepository;

    @InjectMocks
    private AbsenceService absenceService;

    private Eleve amine;
    private Eleve sara;
    private Groupe groupe;

    @BeforeEach
    void initialiser() {
        groupe = new Groupe();
        groupe.setId(GROUPE);
        groupe.setLibelle("1APIC-1");

        amine = eleve(101L, "R130000101", "Bennani", "Amine");
        sara = eleve(102L, "R130000102", "Cherkaoui", "Sara");
    }

    private Eleve eleve(Long id, String codeMassar, String nom, String prenom) {
        Eleve eleve = new Eleve();
        eleve.setId(id);
        eleve.setEtablissementId(ETABLISSEMENT);
        eleve.setCodeMassar(codeMassar);
        eleve.setNom(nom);
        eleve.setPrenom(prenom);
        eleve.setStatut(StatutEleve.INSCRIT);
        eleve.setGroupe(groupe);
        return eleve;
    }

    private Absence absenceExistante(Long id, Eleve eleve, DemiJournee demiJournee) {
        Absence absence = new Absence();
        absence.setId(id);
        absence.setEtablissementId(ETABLISSEMENT);
        absence.setEleve(eleve);
        absence.setDateAbsence(LUNDI);
        absence.setDemiJournee(demiJournee);
        absence.setType(TypeAbsence.ABSENCE);
        return absence;
    }

    private void groupeEtElevesConnus() {
        when(groupeRepository.findByIdAndEtablissementId(GROUPE, ETABLISSEMENT)).thenReturn(Optional.of(groupe));
        when(eleveRepository.findByEtablissementIdAndGroupeIdOrderByNomAscPrenomAsc(ETABLISSEMENT, GROUPE))
                .thenReturn(List.of(amine, sara));
    }

    // ------------------------------------------------------------------
    // Idempotence de la feuille d'appel
    // ------------------------------------------------------------------

    @Test
    void unePremiereFeuilleCreeUneSaisieParEleveSignale() {
        groupeEtElevesConnus();
        when(absenceRepository.findByEtablissementIdAndDateAbsenceAndSeanceIsNullAndEleveIdIn(
                eq(ETABLISSEMENT), eq(LUNDI), anyCollection())).thenReturn(List.of());
        when(absenceRepository.save(any(Absence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Absence> resultat = absenceService.enregistrerFeuille(ETABLISSEMENT, GROUPE, LUNDI,
                DemiJournee.MATIN, null, List.of(new LigneAppel(101L)), 9L);

        assertEquals(1, resultat.size());
        Absence creee = resultat.get(0);
        assertEquals(101L, creee.getEleve().getId().longValue());
        assertEquals(DemiJournee.MATIN, creee.getDemiJournee());
        assertEquals(TypeAbsence.ABSENCE, creee.getType());
        assertFalse(creee.isJustifiee());
        assertEquals(9L, creee.getSaisiePar().longValue());
        verify(absenceRepository, never()).deleteAll(anyCollection());
    }

    @Test
    void laMemeFeuilleRenvoyeeDeuxFoisNeCreeAucunDoublon() {
        Absence existante = absenceExistante(500L, amine, DemiJournee.MATIN);
        groupeEtElevesConnus();
        when(absenceRepository.findByEtablissementIdAndDateAbsenceAndSeanceIsNullAndEleveIdIn(
                eq(ETABLISSEMENT), eq(LUNDI), anyCollection())).thenReturn(List.of(existante));
        when(absenceRepository.save(any(Absence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Absence> resultat = absenceService.enregistrerFeuille(ETABLISSEMENT, GROUPE, LUNDI,
                DemiJournee.MATIN, null, List.of(new LigneAppel(101L)), 9L);

        assertEquals(1, resultat.size());
        assertSame(existante, resultat.get(0), "la saisie existante doit être réutilisée, pas dupliquée");
        assertEquals(500L, resultat.get(0).getId().longValue());
        verify(absenceRepository, never()).deleteAll(anyCollection());
    }

    @Test
    void unEleveRetireDeLaFeuilleVoitSaSaisieSupprimee() {
        Absence existante = absenceExistante(500L, amine, DemiJournee.MATIN);
        groupeEtElevesConnus();
        when(absenceRepository.findByEtablissementIdAndDateAbsenceAndSeanceIsNullAndEleveIdIn(
                eq(ETABLISSEMENT), eq(LUNDI), anyCollection())).thenReturn(List.of(existante));

        List<Absence> resultat = absenceService.enregistrerFeuille(ETABLISSEMENT, GROUPE, LUNDI,
                DemiJournee.MATIN, null, List.of(), 9L);

        assertTrue(resultat.isEmpty());
        verify(absenceRepository).deleteAll(List.of(existante));
        verify(absenceRepository, never()).save(any());
    }

    @Test
    void unEleveCiteDeuxFoisDansLaMemeFeuilleNeProduitQuUneSaisie() {
        groupeEtElevesConnus();
        when(absenceRepository.findByEtablissementIdAndDateAbsenceAndSeanceIsNullAndEleveIdIn(
                eq(ETABLISSEMENT), eq(LUNDI), anyCollection())).thenReturn(List.of());
        when(absenceRepository.save(any(Absence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Absence> resultat = absenceService.enregistrerFeuille(ETABLISSEMENT, GROUPE, LUNDI,
                DemiJournee.MATIN, null,
                List.of(new LigneAppel(101L), new LigneAppel(101L, TypeAbsence.RETARD, true, "Transport")),
                9L);

        assertEquals(1, resultat.size());
        assertEquals(TypeAbsence.RETARD, resultat.get(0).getType());
        assertTrue(resultat.get(0).isJustifiee());
        assertEquals("Transport", resultat.get(0).getMotif());
    }

    @Test
    void uneAbsenceSurLaJourneeRemplaceLaSaisieDeDemiJournee() {
        Absence existante = absenceExistante(500L, amine, DemiJournee.MATIN);
        groupeEtElevesConnus();
        when(absenceRepository.findByEtablissementIdAndDateAbsenceAndSeanceIsNullAndEleveIdIn(
                eq(ETABLISSEMENT), eq(LUNDI), anyCollection())).thenReturn(List.of(existante));
        when(absenceRepository.save(any(Absence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Absence> resultat = absenceService.enregistrerFeuille(ETABLISSEMENT, GROUPE, LUNDI,
                DemiJournee.JOURNEE, null, List.of(new LigneAppel(101L)), 9L);

        verify(absenceRepository).deleteAll(List.of(existante));
        assertEquals(1, resultat.size());
        assertEquals(DemiJournee.JOURNEE, resultat.get(0).getDemiJournee());
    }

    // ------------------------------------------------------------------
    // Étanchéité entre établissements
    // ------------------------------------------------------------------

    @Test
    void laFeuilleRefuseUnEleveEtrangerAuGroupe() {
        groupeEtElevesConnus();

        assertThrows(RessourceIntrouvableException.class, () -> absenceService.enregistrerFeuille(
                ETABLISSEMENT, GROUPE, LUNDI, DemiJournee.MATIN, null, List.of(new LigneAppel(999L)), 9L));

        verify(absenceRepository, never()).save(any());
    }

    @Test
    void laFeuilleRefuseUnGroupeDunAutreEtablissement() {
        when(groupeRepository.findByIdAndEtablissementId(GROUPE, ETABLISSEMENT)).thenReturn(Optional.empty());

        assertThrows(RessourceIntrouvableException.class, () -> absenceService.enregistrerFeuille(
                ETABLISSEMENT, GROUPE, LUNDI, DemiJournee.MATIN, null, List.of(new LigneAppel(101L)), 9L));

        verify(absenceRepository, never()).save(any());
    }

    @Test
    void laSaisieUnitaireRefuseUnEleveDunAutreEtablissement() {
        when(eleveRepository.findByIdAndEtablissementId(101L, ETABLISSEMENT)).thenReturn(Optional.empty());

        assertThrows(RessourceIntrouvableException.class, () -> absenceService.enregistrer(
                ETABLISSEMENT, LUNDI, DemiJournee.MATIN, null, new LigneAppel(101L), 9L));

        verify(eleveRepository, never()).findById(anyLong());
        verify(absenceRepository, never()).save(any());
    }

    @Test
    void uneAbsenceDunAutreEtablissementEstIntrouvable() {
        when(absenceRepository.findByIdAndEtablissementId(500L, ETABLISSEMENT)).thenReturn(Optional.empty());

        assertThrows(RessourceIntrouvableException.class, () -> absenceService.obtenir(ETABLISSEMENT, 500L));

        verify(absenceRepository, never()).findById(anyLong());
    }

    @Test
    void laSaisieUnitaireEstIdempotenteSurLeMemeContexte() {
        Absence existante = absenceExistante(500L, amine, DemiJournee.MATIN);
        when(eleveRepository.findByIdAndEtablissementId(101L, ETABLISSEMENT)).thenReturn(Optional.of(amine));
        when(absenceRepository.findByEtablissementIdAndEleveIdAndDateAbsenceAndSeanceIsNull(
                ETABLISSEMENT, 101L, LUNDI)).thenReturn(List.of(existante));
        when(absenceRepository.save(any(Absence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Absence resultat = absenceService.enregistrer(ETABLISSEMENT, LUNDI, DemiJournee.MATIN, null,
                new LigneAppel(101L, TypeAbsence.ABSENCE, true, "Certificat médical"), 9L);

        assertSame(existante, resultat);
        assertTrue(resultat.isJustifiee());
        assertEquals("Certificat médical", resultat.getMotif());
        verify(absenceRepository, never()).deleteAll(anyCollection());
    }

    // ------------------------------------------------------------------
    // Taux d'absentéisme
    // ------------------------------------------------------------------

    @Test
    void leTauxDAbsenteismeRapporteLesDemiJourneesAuVolumeDu() {
        LocalDate fin = LocalDate.of(2026, 2, 7); // lundi -> samedi : 6 jours ouvrables
        when(eleveRepository.countByEtablissementIdAndStatut(ETABLISSEMENT, StatutEleve.INSCRIT)).thenReturn(10L);
        when(absenceRepository.countByEtablissementIdAndDateAbsenceBetweenAndType(
                ETABLISSEMENT, LUNDI, fin, TypeAbsence.ABSENCE)).thenReturn(10L);
        when(absenceRepository.countByEtablissementIdAndDateAbsenceBetweenAndTypeAndDemiJournee(
                ETABLISSEMENT, LUNDI, fin, TypeAbsence.ABSENCE, DemiJournee.JOURNEE)).thenReturn(2L);
        when(absenceRepository.countByEtablissementIdAndDateAbsenceBetweenAndTypeAndJustifiee(
                ETABLISSEMENT, LUNDI, fin, TypeAbsence.ABSENCE, true)).thenReturn(4L);
        when(absenceRepository.countByEtablissementIdAndDateAbsenceBetweenAndType(
                ETABLISSEMENT, LUNDI, fin, TypeAbsence.RETARD)).thenReturn(3L);
        when(absenceRepository.countByEtablissementIdAndDateAbsenceBetweenAndType(
                ETABLISSEMENT, LUNDI, fin, TypeAbsence.EXCLUSION)).thenReturn(1L);

        StatistiquesAbsences statistiques = absenceService.statistiques(ETABLISSEMENT, LUNDI, fin);

        assertEquals(6L, statistiques.joursOuvrables());
        assertEquals(120L, statistiques.demiJourneesDues());   // 10 élèves x 6 jours x 2
        assertEquals(12L, statistiques.demiJourneesAbsence()); // 10 saisies dont 2 journées entières
        assertEquals(10.0, statistiques.tauxAbsenteisme());
        assertEquals(4L, statistiques.absencesJustifiees());
        assertEquals(6L, statistiques.absencesNonJustifiees());
        assertEquals(3L, statistiques.retards());
        assertEquals(1L, statistiques.exclusions());
    }

    @Test
    void leTauxEstNulQuandAucunEleveNestInscrit() {
        LocalDate fin = LocalDate.of(2026, 2, 7);
        when(eleveRepository.countByEtablissementIdAndStatut(ETABLISSEMENT, StatutEleve.INSCRIT)).thenReturn(0L);
        when(absenceRepository.countByEtablissementIdAndDateAbsenceBetweenAndType(
                ETABLISSEMENT, LUNDI, fin, TypeAbsence.ABSENCE)).thenReturn(0L);
        when(absenceRepository.countByEtablissementIdAndDateAbsenceBetweenAndTypeAndDemiJournee(
                ETABLISSEMENT, LUNDI, fin, TypeAbsence.ABSENCE, DemiJournee.JOURNEE)).thenReturn(0L);
        when(absenceRepository.countByEtablissementIdAndDateAbsenceBetweenAndTypeAndJustifiee(
                ETABLISSEMENT, LUNDI, fin, TypeAbsence.ABSENCE, true)).thenReturn(0L);
        when(absenceRepository.countByEtablissementIdAndDateAbsenceBetweenAndType(
                ETABLISSEMENT, LUNDI, fin, TypeAbsence.RETARD)).thenReturn(0L);
        when(absenceRepository.countByEtablissementIdAndDateAbsenceBetweenAndType(
                ETABLISSEMENT, LUNDI, fin, TypeAbsence.EXCLUSION)).thenReturn(0L);

        StatistiquesAbsences statistiques = absenceService.statistiques(ETABLISSEMENT, LUNDI, fin);

        assertEquals(0.0, statistiques.tauxAbsenteisme());
        assertEquals(0L, statistiques.demiJourneesDues());
    }

    @Test
    void lesDimanchesNeSontPasDesJoursOuvrables() {
        // du lundi 2 au dimanche 8 février 2026 : 6 jours de classe
        assertEquals(6L, AbsenceService.joursOuvrables(LUNDI, LocalDate.of(2026, 2, 8)));
    }

    @Test
    void unePeriodeInverseeEstRefusee() {
        assertThrows(IllegalArgumentException.class,
                () -> absenceService.statistiques(ETABLISSEMENT, LUNDI, LUNDI.minusDays(1)));
    }

    // ------------------------------------------------------------------
    // Seuil d'alerte
    // ------------------------------------------------------------------

    @Test
    void leSeuilParDefautSappliqueQuandAucunSeuilNestFourni() {
        LocalDate fin = LocalDate.of(2026, 2, 28);
        when(absenceRepository.compterNonJustifieesParEleve(
                ETABLISSEMENT, TypeAbsence.ABSENCE, LUNDI, fin, AbsenceService.SEUIL_ALERTE_PAR_DEFAUT))
                .thenReturn(List.of());

        assertTrue(absenceService.alertes(ETABLISSEMENT, LUNDI, fin, 0L).isEmpty());
    }

    @Test
    void lAlerteRemonteLesElevesAtteignantLeSeuil() {
        LocalDate fin = LocalDate.of(2026, 2, 28);
        when(absenceRepository.compterNonJustifieesParEleve(
                ETABLISSEMENT, TypeAbsence.ABSENCE, LUNDI, fin, 3L))
                .thenReturn(List.of(new CompteurAbsencesEleve(101L, 7L),
                        new CompteurAbsencesEleve(102L, 3L)));
        when(eleveRepository.findByEtablissementIdAndIdInOrderByNomAscPrenomAsc(
                eq(ETABLISSEMENT), anyCollection())).thenReturn(List.of(amine, sara));

        List<AlerteAbsenteisme> alertes = absenceService.alertes(ETABLISSEMENT, LUNDI, fin, 3L);

        assertEquals(2, alertes.size());
        AlerteAbsenteisme premiere = alertes.get(0);
        assertEquals(101L, premiere.eleveId().longValue());
        assertEquals("R130000101", premiere.codeMassar());
        assertEquals("Bennani", premiere.nom());
        assertEquals(GROUPE, premiere.groupeId());
        assertEquals("1APIC-1", premiere.groupeLibelle());
        assertEquals(7L, premiere.absencesNonJustifiees());
        assertEquals(3L, alertes.get(1).absencesNonJustifiees());
    }

    @Test
    void uneAlerteSansEleveDeLEtablissementEstEcartee() {
        LocalDate fin = LocalDate.of(2026, 2, 28);
        when(absenceRepository.compterNonJustifieesParEleve(
                ETABLISSEMENT, TypeAbsence.ABSENCE, LUNDI, fin, 4L))
                .thenReturn(List.of(new CompteurAbsencesEleve(999L, 9L)));
        when(eleveRepository.findByEtablissementIdAndIdInOrderByNomAscPrenomAsc(
                eq(ETABLISSEMENT), anyCollection())).thenReturn(List.of());

        assertTrue(absenceService.alertes(ETABLISSEMENT, LUNDI, fin, 4L).isEmpty());
    }
}
