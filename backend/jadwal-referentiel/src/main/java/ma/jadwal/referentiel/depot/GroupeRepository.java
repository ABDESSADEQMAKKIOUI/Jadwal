package ma.jadwal.referentiel.depot;

import ma.jadwal.referentiel.entite.Groupe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupeRepository extends JpaRepository<Groupe, Long> {

    List<Groupe> findByEtablissementIdOrderByLibelleAsc(Long etablissementId);

    List<Groupe> findByEtablissementIdAndNiveauIdOrderByLibelleAsc(Long etablissementId, Long niveauId);

    List<Groupe> findByParentId(Long parentId);

    Optional<Groupe> findByIdAndEtablissementId(Long id, Long etablissementId);

    long countByNiveauId(Long niveauId);
}
