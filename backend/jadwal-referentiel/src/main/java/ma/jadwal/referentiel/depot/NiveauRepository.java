package ma.jadwal.referentiel.depot;

import ma.jadwal.referentiel.entite.Niveau;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NiveauRepository extends JpaRepository<Niveau, Long> {

    List<Niveau> findByEtablissementIdOrderByOrdreAscIdAsc(Long etablissementId);

    Optional<Niveau> findByIdAndEtablissementId(Long id, Long etablissementId);
}
