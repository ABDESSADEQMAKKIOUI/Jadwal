package ma.jadwal.abonnement.service;

import ma.jadwal.abonnement.depot.AbonnementRepository;
import ma.jadwal.abonnement.depot.PaiementRepository;
import ma.jadwal.abonnement.entite.Abonnement;
import ma.jadwal.abonnement.entite.ModePaiement;
import ma.jadwal.abonnement.entite.Paiement;
import ma.jadwal.abonnement.entite.StatutAbonnement;
import ma.jadwal.abonnement.entite.StatutPaiement;
import ma.jadwal.common.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final AbonnementRepository abonnementRepository;

    public PaiementService(PaiementRepository paiementRepository, AbonnementRepository abonnementRepository) {
        this.paiementRepository = paiementRepository;
        this.abonnementRepository = abonnementRepository;
    }

    @Transactional(readOnly = true)
    public List<Paiement> listerTout() {
        return paiementRepository.findAllByOrderByDatePaiementDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<Paiement> listerParEtablissement(Long etablissementId) {
        return paiementRepository.findByAbonnement_Etablissement_IdOrderByDatePaiementDescIdDesc(etablissementId);
    }

    @Transactional
    public Paiement creer(Long abonnementId, BigDecimal montant, ModePaiement mode, String reference,
                          LocalDate datePaiement, String note, Long enregistrePar) {
        Abonnement abonnement = abonnementRepository.findById(abonnementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Abonnement introuvable : " + abonnementId));
        Paiement paiement = new Paiement();
        paiement.setAbonnement(abonnement);
        paiement.setMontant(montant);
        paiement.setMode(mode);
        paiement.setReference(reference);
        paiement.setDatePaiement(datePaiement);
        paiement.setStatut(StatutPaiement.CONFIRME);
        paiement.setNote(note);
        paiement.setEnregistrePar(enregistrePar);
        paiementRepository.saveAndFlush(paiement);
        recalculerStatut(abonnement);
        return paiement;
    }

    @Transactional
    public Paiement changerStatut(Long paiementId, StatutPaiement statut) {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Paiement introuvable : " + paiementId));
        paiement.setStatut(statut);
        paiementRepository.saveAndFlush(paiement);
        recalculerStatut(paiement.getAbonnement());
        return paiement;
    }

    /**
     * Règle métier : si la somme des paiements CONFIRME atteint le prix annuel du plan,
     * l'abonnement passe ACTIF ; sinon il revient EN_ATTENTE_PAIEMENT.
     * Un abonnement EXPIRE n'est jamais modifié.
     */
    public void recalculerStatut(Abonnement abonnement) {
        if (abonnement.getStatut() == StatutAbonnement.EXPIRE) {
            return;
        }
        BigDecimal total = totalPaye(abonnement.getId());
        if (total.compareTo(abonnement.getPlan().getPrixAnnuel()) >= 0) {
            abonnement.setStatut(StatutAbonnement.ACTIF);
        } else {
            abonnement.setStatut(StatutAbonnement.EN_ATTENTE_PAIEMENT);
        }
        abonnementRepository.save(abonnement);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalPaye(Long abonnementId) {
        BigDecimal total = paiementRepository.sommeParAbonnementEtStatut(abonnementId, StatutPaiement.CONFIRME);
        return total == null ? BigDecimal.ZERO : total;
    }

    @Transactional(readOnly = true)
    public long compterParStatut(StatutPaiement statut) {
        return paiementRepository.countByStatut(statut);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalConfirmeAnnee(int annee) {
        BigDecimal total = paiementRepository.sommeParStatutEtPeriode(StatutPaiement.CONFIRME,
                LocalDate.of(annee, 1, 1), LocalDate.of(annee, 12, 31));
        return total == null ? BigDecimal.ZERO : total;
    }
}
