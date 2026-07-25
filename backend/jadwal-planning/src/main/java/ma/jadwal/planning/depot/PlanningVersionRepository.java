package ma.jadwal.planning.depot;

import ma.jadwal.planning.entite.PlanningVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanningVersionRepository extends JpaRepository<PlanningVersion, Long> {

    List<PlanningVersion> findByEtablissementIdOrderByCreeeLeDesc(Long etablissementId);

    Optional<PlanningVersion> findByIdAndEtablissementId(Long id, Long etablissementId);

    List<PlanningVersion> findByEtablissementIdAndActiveTrue(Long etablissementId);
}
