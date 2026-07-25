package ma.jadwal.abonnement.depot;

import ma.jadwal.abonnement.entite.Paiement;
import ma.jadwal.abonnement.entite.StatutPaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    List<Paiement> findAllByOrderByDatePaiementDescIdDesc();

    List<Paiement> findByAbonnement_Etablissement_IdOrderByDatePaiementDescIdDesc(Long etablissementId);

    long countByStatut(StatutPaiement statut);

    @Query("select sum(p.montant) from Paiement p where p.abonnement.id = :abonnementId and p.statut = :statut")
    BigDecimal sommeParAbonnementEtStatut(@Param("abonnementId") Long abonnementId,
                                          @Param("statut") StatutPaiement statut);

    @Query("select sum(p.montant) from Paiement p where p.statut = :statut and p.datePaiement between :debut and :fin")
    BigDecimal sommeParStatutEtPeriode(@Param("statut") StatutPaiement statut,
                                       @Param("debut") LocalDate debut,
                                       @Param("fin") LocalDate fin);
}
