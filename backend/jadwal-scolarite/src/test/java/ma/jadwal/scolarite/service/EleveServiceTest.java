package ma.jadwal.scolarite.service;

import ma.jadwal.common.exception.ConflitException;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import ma.jadwal.referentiel.depot.GroupeRepository;
import ma.jadwal.referentiel.entite.Groupe;
import ma.jadwal.scolarite.depot.EleveRepository;
import ma.jadwal.scolarite.entite.Eleve;
import ma.jadwal.scolarite.entite.Sexe;
import ma.jadwal.scolarite.entite.StatutEleve;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EleveServiceTest {

    private static final Long ETABLISSEMENT = 1L;
    private static final Long AUTRE_ETABLISSEMENT = 2L;

    @Mock
    private EleveRepository eleveRepository;

    @Mock
    private GroupeRepository groupeRepository;

    @InjectMocks
    private EleveService eleveService;

    private static DonneesEleve donnees(String codeMassar) {
        return new DonneesEleve(codeMassar, "Alaoui", "Yasmine", null, null,
                LocalDate.of(2012, 5, 3), "Rabat", Sexe.F, StatutEleve.INSCRIT,
                "Alaoui Karim", "0600000000", null);
    }

    @Test
    void unCodeMassarDejaUtiliseDansLEtablissementEstRefuse() {
        when(eleveRepository.existsByEtablissementIdAndCodeMassar(ETABLISSEMENT, "R130012345")).thenReturn(true);

        ConflitException exception = assertThrows(ConflitException.class,
                () -> eleveService.creer(ETABLISSEMENT, donnees("R130012345")));

        assertEquals("Un élève est déjà inscrit avec ce code Massar dans cet établissement.",
                exception.getMessage());
        verify(eleveRepository, never()).save(any());
    }

    @Test
    void leCodeMassarEstNormaliseAvantLeControleDUnicite() {
        when(eleveRepository.existsByEtablissementIdAndCodeMassar(ETABLISSEMENT, "R130012345")).thenReturn(false);
        when(eleveRepository.save(any(Eleve.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Eleve eleve = eleveService.creer(ETABLISSEMENT, donnees("  r130012345 "));

        assertEquals("R130012345", eleve.getCodeMassar());
        assertEquals(ETABLISSEMENT, eleve.getEtablissementId());
        assertEquals(StatutEleve.INSCRIT, eleve.getStatut());
        assertNull(eleve.getGroupe());
    }

    @Test
    void leMemeCodeMassarEstAccepteDansUnAutreEtablissement() {
        when(eleveRepository.existsByEtablissementIdAndCodeMassar(AUTRE_ETABLISSEMENT, "R130012345"))
                .thenReturn(false);
        when(eleveRepository.save(any(Eleve.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Eleve eleve = eleveService.creer(AUTRE_ETABLISSEMENT, donnees("R130012345"));

        assertEquals(AUTRE_ETABLISSEMENT, eleve.getEtablissementId());
    }

    @Test
    void unEleveDunAutreEtablissementEstIntrouvable() {
        when(eleveRepository.findByIdAndEtablissementId(77L, ETABLISSEMENT)).thenReturn(Optional.empty());

        assertThrows(RessourceIntrouvableException.class, () -> eleveService.obtenir(ETABLISSEMENT, 77L));

        // L'invariant critique : aucun chargement par identifiant seul.
        verify(eleveRepository, never()).findById(anyLong());
    }

    @Test
    void laMiseAJourRefuseUnEleveDunAutreEtablissement() {
        when(eleveRepository.findByIdAndEtablissementId(77L, ETABLISSEMENT)).thenReturn(Optional.empty());

        assertThrows(RessourceIntrouvableException.class,
                () -> eleveService.mettreAJour(ETABLISSEMENT, 77L, donnees("R130012345")));

        verify(eleveRepository, never()).save(any());
    }

    @Test
    void laSuppressionRefuseUnEleveDunAutreEtablissement() {
        when(eleveRepository.findByIdAndEtablissementId(77L, ETABLISSEMENT)).thenReturn(Optional.empty());

        assertThrows(RessourceIntrouvableException.class, () -> eleveService.supprimer(ETABLISSEMENT, 77L));

        verify(eleveRepository, never()).delete(any());
    }

    @Test
    void lAffectationRefuseLeGroupeDunAutreEtablissement() {
        Eleve eleve = new Eleve();
        eleve.setId(5L);
        eleve.setEtablissementId(ETABLISSEMENT);
        when(eleveRepository.findByIdAndEtablissementId(5L, ETABLISSEMENT)).thenReturn(Optional.of(eleve));
        when(groupeRepository.findByIdAndEtablissementId(900L, ETABLISSEMENT)).thenReturn(Optional.empty());

        assertThrows(RessourceIntrouvableException.class,
                () -> eleveService.affecterAuGroupe(ETABLISSEMENT, 5L, 900L));

        verify(eleveRepository, never()).save(any());
    }

    @Test
    void lAffectationRattacheLeGroupeDeLEtablissement() {
        Eleve eleve = new Eleve();
        eleve.setId(5L);
        eleve.setEtablissementId(ETABLISSEMENT);
        Groupe groupe = new Groupe();
        groupe.setId(12L);
        groupe.setLibelle("1APIC-1");
        when(eleveRepository.findByIdAndEtablissementId(5L, ETABLISSEMENT)).thenReturn(Optional.of(eleve));
        when(groupeRepository.findByIdAndEtablissementId(12L, ETABLISSEMENT)).thenReturn(Optional.of(groupe));
        when(eleveRepository.save(any(Eleve.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Eleve resultat = eleveService.affecterAuGroupe(ETABLISSEMENT, 5L, 12L);

        assertEquals(12L, resultat.getGroupe().getId().longValue());
    }

    @Test
    void lesStatistiquesAgregentLesEffectifsDeLEtablissement() {
        when(eleveRepository.countByEtablissementId(ETABLISSEMENT)).thenReturn(120L);
        when(eleveRepository.countByEtablissementIdAndStatut(ETABLISSEMENT, StatutEleve.INSCRIT)).thenReturn(110L);
        when(eleveRepository.countByEtablissementIdAndStatut(ETABLISSEMENT, StatutEleve.PARTI)).thenReturn(4L);
        when(eleveRepository.countByEtablissementIdAndStatut(ETABLISSEMENT, StatutEleve.REDOUBLANT)).thenReturn(6L);
        when(eleveRepository.countByEtablissementIdAndGroupeIsNull(ETABLISSEMENT)).thenReturn(3L);

        StatistiquesEleves statistiques = eleveService.statistiques(ETABLISSEMENT);

        assertEquals(120L, statistiques.total());
        assertEquals(110L, statistiques.inscrits());
        assertEquals(4L, statistiques.partis());
        assertEquals(6L, statistiques.redoublants());
        assertEquals(3L, statistiques.sansGroupe());
    }
}
