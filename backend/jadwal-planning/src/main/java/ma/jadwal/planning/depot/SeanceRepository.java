package ma.jadwal.planning.depot;

import ma.jadwal.planning.entite.Seance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SeanceRepository extends JpaRepository<Seance, Long> {

    List<Seance> findByVersionId(Long versionId);

    List<Seance> findByVersionIdAndGroupeIdIn(Long versionId, Collection<Long> groupeIds);

    List<Seance> findByVersionIdAndEnseignantId(Long versionId, Long enseignantId);

    List<Seance> findByVersionIdAndSalleId(Long versionId, Long salleId);

    Optional<Seance> findByIdAndEtablissementId(Long id, Long etablissementId);

    boolean existsByEtablissementId(Long etablissementId);

    long countByVersionId(Long versionId);

    void deleteByVersionId(Long versionId);
}
