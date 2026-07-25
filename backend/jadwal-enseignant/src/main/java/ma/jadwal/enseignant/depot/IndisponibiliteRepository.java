package ma.jadwal.enseignant.depot;

import ma.jadwal.enseignant.entite.Indisponibilite;
import ma.jadwal.enseignant.entite.StatutIndispo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IndisponibiliteRepository extends JpaRepository<Indisponibilite, Long> {

    List<Indisponibilite> findByEnseignantIdOrderByJourAscIndexDebutAsc(Long enseignantId);

    List<Indisponibilite> findByEnseignantIdAndStatut(Long enseignantId, StatutIndispo statut);

    Optional<Indisponibilite> findByIdAndEnseignantId(Long id, Long enseignantId);
}
