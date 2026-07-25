package ma.jadwal.abonnement.service;

import ma.jadwal.abonnement.depot.AbonnementRepository;
import ma.jadwal.abonnement.depot.PaiementRepository;
import ma.jadwal.abonnement.entite.Abonnement;
import ma.jadwal.abonnement.entite.ModePaiement;
import ma.jadwal.abonnement.entite.Paiement;
import ma.jadwal.abonnement.entite.PlanAbonnement;
import ma.jadwal.abonnement.entite.StatutAbonnement;
import ma.jadwal.abonnement.entite.StatutPaiement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaiementServiceTest {

    @Mock
    private PaiementRepository paiementRepository;

    @Mock
    private AbonnementRepository abonnementRepository;

    @InjectMocks
    private PaiementService paiementService;

    private Abonnement abonnement;

    @BeforeEach
    void initialiser() {
        PlanAbonnement plan = new PlanAbonnement();
        plan.setId(10L);
        plan.setCode("ESSENTIEL");
        plan.setNom("Essentiel");
        plan.setPrixAnnuel(new BigDecimal("9900.00"));

        abonnement = new Abonnement();
        abonnement.setId(1L);
        abonnement.setPlan(plan);
        abonnement.setStatut(StatutAbonnement.EN_ATTENTE_PAIEMENT);
    }

    @Test
    void laCreationDunPaiementActiveLAbonnementQuandLePrixEstAtteint() {
        when(abonnementRepository.findById(1L)).thenReturn(Optional.of(abonnement));
        when(paiementRepository.sommeParAbonnementEtStatut(1L, StatutPaiement.CONFIRME))
                .thenReturn(new BigDecimal("9900.00"));

        Paiement paiement = paiementService.creer(1L, new BigDecimal("9900.00"), ModePaiement.VIREMENT,
                "VIR-2026-001", LocalDate.of(2026, 1, 15), null, 1L);

        assertEquals(StatutPaiement.CONFIRME, paiement.getStatut());
        assertEquals(StatutAbonnement.ACTIF, abonnement.getStatut());
        verify(abonnementRepository).save(abonnement);
    }

    @Test
    void lAbonnementResteEnAttenteQuandLeTotalConfirmeEstInsuffisant() {
        when(paiementRepository.sommeParAbonnementEtStatut(1L, StatutPaiement.CONFIRME))
                .thenReturn(new BigDecimal("4000.00"));

        paiementService.recalculerStatut(abonnement);

        assertEquals(StatutAbonnement.EN_ATTENTE_PAIEMENT, abonnement.getStatut());
    }

    @Test
    void leRejetDunPaiementFaitRepasserLAbonnementEnAttente() {
        abonnement.setStatut(StatutAbonnement.ACTIF);
        Paiement paiement = new Paiement();
        paiement.setId(5L);
        paiement.setAbonnement(abonnement);
        paiement.setStatut(StatutPaiement.CONFIRME);

        when(paiementRepository.findById(5L)).thenReturn(Optional.of(paiement));
        when(paiementRepository.sommeParAbonnementEtStatut(1L, StatutPaiement.CONFIRME))
                .thenReturn(BigDecimal.ZERO);

        Paiement resultat = paiementService.changerStatut(5L, StatutPaiement.REJETE);

        assertEquals(StatutPaiement.REJETE, resultat.getStatut());
        assertEquals(StatutAbonnement.EN_ATTENTE_PAIEMENT, abonnement.getStatut());
    }

    @Test
    void unAbonnementExpireNestJamaisModifie() {
        abonnement.setStatut(StatutAbonnement.EXPIRE);

        paiementService.recalculerStatut(abonnement);

        assertEquals(StatutAbonnement.EXPIRE, abonnement.getStatut());
        verify(abonnementRepository, never()).save(any());
    }
}
