package ma.jadwal.pedagogie.depot;

import ma.jadwal.pedagogie.entite.Ponderation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PonderationRepository extends JpaRepository<Ponderation, Long> {

    List<Ponderation> findByEtablissementId(Long etablissementId);

    Optional<Ponderation> findByEtablissementIdAndRegle(Long etablissementId, String regle);
}
