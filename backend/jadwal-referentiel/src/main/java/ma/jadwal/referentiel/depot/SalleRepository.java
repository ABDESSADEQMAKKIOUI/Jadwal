package ma.jadwal.referentiel.depot;

import ma.jadwal.referentiel.entite.Salle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalleRepository extends JpaRepository<Salle, Long> {

    List<Salle> findByEtablissementIdOrderByNomAsc(Long etablissementId);

    Optional<Salle> findByIdAndEtablissementId(Long id, Long etablissementId);
}
