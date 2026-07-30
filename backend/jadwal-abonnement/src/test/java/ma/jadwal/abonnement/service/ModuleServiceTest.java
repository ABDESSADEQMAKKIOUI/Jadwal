package ma.jadwal.abonnement.service;

import ma.jadwal.abonnement.depot.AbonnementRepository;
import ma.jadwal.abonnement.entite.Abonnement;
import ma.jadwal.abonnement.entite.ModuleJadwal;
import ma.jadwal.abonnement.entite.PlanAbonnement;
import ma.jadwal.abonnement.entite.StatutAbonnement;
import ma.jadwal.common.exception.ModuleNonSouscritException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModuleServiceTest {

    private static final Long ETABLISSEMENT = 1L;

    @Mock
    private AbonnementRepository abonnementRepository;

    @InjectMocks
    private ModuleService moduleService;

    private void abonnement(StatutAbonnement statut, String modules) {
        PlanAbonnement plan = new PlanAbonnement();
        plan.setId(3L);
        plan.setCode("PREMIUM");
        plan.setModules(modules);

        Abonnement abonnement = new Abonnement();
        abonnement.setId(7L);
        abonnement.setPlan(plan);
        abonnement.setStatut(statut);

        when(abonnementRepository.findFirstByEtablissement_IdAndStatutNotOrderByDateDebutDescIdDesc(
                ETABLISSEMENT, StatutAbonnement.EXPIRE)).thenReturn(Optional.of(abonnement));
    }

    @Test
    void unPlanIncluantLaVieScolaireOuvreLeModule() {
        abonnement(StatutAbonnement.ACTIF, "PLANNING,VIE_SCOLAIRE");

        assertDoesNotThrow(() -> moduleService.exigerModule(ETABLISSEMENT, ModuleJadwal.VIE_SCOLAIRE));
        assertEquals(Set.of(ModuleJadwal.PLANNING, ModuleJadwal.VIE_SCOLAIRE),
                moduleService.modulesActifs(ETABLISSEMENT));
    }

    @Test
    void unPlanSansVieScolaireRefuseLeModuleAvecUnMessageExplicite() {
        abonnement(StatutAbonnement.ACTIF, "PLANNING");

        ModuleNonSouscritException exception = assertThrows(ModuleNonSouscritException.class,
                () -> moduleService.exigerModule(ETABLISSEMENT, ModuleJadwal.VIE_SCOLAIRE));

        assertEquals("Le module Vie scolaire n'est pas inclus dans l'abonnement de votre établissement.",
                exception.getMessage());
        assertEquals("VIE_SCOLAIRE", exception.getModule());
    }

    @Test
    void unAbonnementNonActifNouvreAucunModule() {
        abonnement(StatutAbonnement.EN_ATTENTE_PAIEMENT, "PLANNING,VIE_SCOLAIRE");

        assertThrows(ModuleNonSouscritException.class,
                () -> moduleService.exigerModule(ETABLISSEMENT, ModuleJadwal.VIE_SCOLAIRE));
        assertTrue(moduleService.modulesActifs(ETABLISSEMENT).isEmpty());
    }

    @Test
    void sansAucunAbonnementLeModuleEstRefuse() {
        when(abonnementRepository.findFirstByEtablissement_IdAndStatutNotOrderByDateDebutDescIdDesc(
                ETABLISSEMENT, StatutAbonnement.EXPIRE)).thenReturn(Optional.empty());

        ModuleNonSouscritException exception = assertThrows(ModuleNonSouscritException.class,
                () -> moduleService.exigerModule(ETABLISSEMENT, ModuleJadwal.VIE_SCOLAIRE));

        assertEquals("Aucun abonnement actif pour votre établissement : le module Vie scolaire "
                + "n'est pas accessible.", exception.getMessage());
        assertFalse(moduleService.moduleDisponible(ETABLISSEMENT, ModuleJadwal.PLANNING));
    }

    @Test
    void lesCodesInconnusOuMalFormesSontIgnores() {
        abonnement(StatutAbonnement.ACTIF, " planning , NOTES , ");

        assertEquals(Set.of(ModuleJadwal.PLANNING), moduleService.modulesActifs(ETABLISSEMENT));
        assertFalse(moduleService.moduleDisponible(ETABLISSEMENT, ModuleJadwal.VIE_SCOLAIRE));
    }

    @Test
    void lEncodageDesModulesInclutToujoursLeSoclePlanning() {
        assertEquals("PLANNING", PlanService.encoderModules(Set.of()));
        assertEquals("PLANNING,VIE_SCOLAIRE",
                PlanService.encoderModules(Set.of(ModuleJadwal.VIE_SCOLAIRE)));
    }
}
