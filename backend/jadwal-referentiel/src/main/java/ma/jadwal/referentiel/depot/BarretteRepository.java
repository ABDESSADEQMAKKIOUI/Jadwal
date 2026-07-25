package ma.jadwal.referentiel.depot;

import ma.jadwal.referentiel.entite.Barrette;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BarretteRepository extends JpaRepository<Barrette, Long> {

    List<Barrette> findByEtablissementIdOrderByLibelleAsc(Long etablissementId);

    Optional<Barrette> findByIdAndEtablissementId(Long id, Long etablissementId);
}
