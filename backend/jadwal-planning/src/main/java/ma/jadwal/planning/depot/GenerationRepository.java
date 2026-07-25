package ma.jadwal.planning.depot;

import ma.jadwal.planning.entite.Generation;
import ma.jadwal.planning.entite.StatutGeneration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GenerationRepository extends JpaRepository<Generation, Long> {

    List<Generation> findByEtablissementIdOrderByLanceeLeDesc(Long etablissementId);

    Optional<Generation> findByIdAndEtablissementId(Long id, Long etablissementId);

    List<Generation> findByEtablissementIdAndStatut(Long etablissementId, StatutGeneration statut);
}
