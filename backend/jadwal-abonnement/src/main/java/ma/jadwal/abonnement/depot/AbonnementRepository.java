package ma.jadwal.abonnement.depot;

import ma.jadwal.abonnement.entite.Abonnement;
import ma.jadwal.abonnement.entite.StatutAbonnement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AbonnementRepository extends JpaRepository<Abonnement, Long> {

    List<Abonnement> findAllByOrderByIdDesc();

    List<Abonnement> findByEtablissement_IdOrderByDateDebutDescIdDesc(Long etablissementId);

    Optional<Abonnement> findFirstByEtablissement_IdAndStatutNotOrderByDateDebutDescIdDesc(
            Long etablissementId, StatutAbonnement statut);

    long countByStatut(StatutAbonnement statut);
}
