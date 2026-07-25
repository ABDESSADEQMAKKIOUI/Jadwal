package ma.jadwal.enseignant.depot;

import ma.jadwal.enseignant.entite.Enseignant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnseignantRepository extends JpaRepository<Enseignant, Long> {

    List<Enseignant> findByEtablissementIdOrderByNomCompletAsc(Long etablissementId);

    Optional<Enseignant> findByIdAndEtablissementId(Long id, Long etablissementId);
}
