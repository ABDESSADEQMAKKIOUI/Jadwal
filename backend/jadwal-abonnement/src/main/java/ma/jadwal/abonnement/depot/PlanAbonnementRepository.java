package ma.jadwal.abonnement.depot;

import ma.jadwal.abonnement.entite.PlanAbonnement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanAbonnementRepository extends JpaRepository<PlanAbonnement, Long> {

    boolean existsByCode(String code);

    List<PlanAbonnement> findAllByOrderByIdAsc();
}
