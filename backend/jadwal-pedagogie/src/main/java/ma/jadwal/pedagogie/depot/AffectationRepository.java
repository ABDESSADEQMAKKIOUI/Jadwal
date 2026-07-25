package ma.jadwal.pedagogie.depot;

import ma.jadwal.pedagogie.entite.Affectation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AffectationRepository extends JpaRepository<Affectation, Long> {

    List<Affectation> findByEtablissementId(Long etablissementId);

    List<Affectation> findByEtablissementIdAndGroupeId(Long etablissementId, Long groupeId);

    List<Affectation> findByEtablissementIdAndEnseignantId(Long etablissementId, Long enseignantId);

    Optional<Affectation> findByIdAndEtablissementId(Long id, Long etablissementId);

    boolean existsByGroupeIdAndMatiereIdAndEnseignantId(Long groupeId, Long matiereId, Long enseignantId);
}
