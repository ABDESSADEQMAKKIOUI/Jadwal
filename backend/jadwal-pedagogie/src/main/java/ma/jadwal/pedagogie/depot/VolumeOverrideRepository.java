package ma.jadwal.pedagogie.depot;

import ma.jadwal.pedagogie.entite.VolumeOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VolumeOverrideRepository extends JpaRepository<VolumeOverride, Long> {

    List<VolumeOverride> findByGroupeId(Long groupeId);

    List<VolumeOverride> findByGroupeIdIn(Collection<Long> groupeIds);

    Optional<VolumeOverride> findByGroupeIdAndMatiereId(Long groupeId, Long matiereId);
}
