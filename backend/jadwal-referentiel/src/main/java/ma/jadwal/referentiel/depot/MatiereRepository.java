package ma.jadwal.referentiel.depot;

import ma.jadwal.referentiel.entite.Matiere;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatiereRepository extends JpaRepository<Matiere, Long> {

    List<Matiere> findByEtablissementIdOrderByLibelleAsc(Long etablissementId);

    Optional<Matiere> findByIdAndEtablissementId(Long id, Long etablissementId);

    boolean existsByEtablissementIdAndCode(Long etablissementId, String code);
}
